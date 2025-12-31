package com.ducks.synaptra.orchestration.event.agent;

import com.ducks.synaptra.client.openai.OpenAIClient;
import com.ducks.synaptra.client.openai.data.ChatCompletionRequest;
import com.ducks.synaptra.client.openai.data.ChatCompletionResponse;
import com.ducks.synaptra.log.LogTracer;
import com.ducks.synaptra.log.tracing.SpanManager;
import com.ducks.synaptra.orchestration.event.agent.contract.AgentRequestEvent;
import com.ducks.synaptra.orchestration.event.agent.contract.AgentResponseEvent;
import com.ducks.synaptra.velocity.VelocityTemplateService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Event-driven component responsible for executing an AI agent request by calling OpenAI and
 * publishing the corresponding response event.
 *
 * <p>This service listens for {@link AgentRequestEvent} events, builds the {@link
 * ChatCompletionRequest} (including template rendering), performs the OpenAI call through {@link
 * OpenAIClient}, and then publishes an {@link AgentResponseEvent} so downstream components can
 * handle the agent execution result.
 *
 * <p>Tracing and structured logs are emitted for observability:
 *
 * <ul>
 *   <li>OpenTelemetry/Micrometer spans are created for the OpenAI call boundary
 *   <li>Span events are added with session/agent identifiers and request/response payloads
 *   <li>Debug logs include request/response JSON for inspection (use with care in production)
 * </ul>
 *
 * <p><strong>Responsibility boundary:</strong> This class only orchestrates the OpenAI call and
 * event publication. It does not interpret responses or apply business rules; such processing is
 * delegated to {@link AgentExecutionListener} implementations, triggered by {@link
 * AgentResponseEvent}.
 *
 * @author Leandro Marques
 * @version 1.0.0
 */
@Service
public class AgentExecutionEvent {

  private static final Logger logger = LogManager.getLogger(AgentExecutionEvent.class);

  /** Service for creating/ending spans and adding span events for observability. */
  private final SpanManager spanManager;

  /** Tracer for managing span scope. */
  private final Tracer tracer;

  /** Client responsible for performing OpenAI chat completion calls. */
  private final OpenAIClient openAIClient;

  /** JSON mapper used to serialize request/response payloads for logs and span events. */
  private final ObjectMapper mapper;

  /** Publisher used to emit downstream orchestration events. */
  private final ApplicationEventPublisher publisher;

  /** Subscribers that will process the agent response event. */
  private final List<AgentExecutionListener> agentExecutionListenerList;

  /** Service used to render templates that can compose the final prompt/context. */
  private final VelocityTemplateService velocityTemplateService;

  public AgentExecutionEvent(
      SpanManager spanManager,
      Tracer tracer,
      OpenAIClient openAIClient,
      ApplicationEventPublisher publisher,
      List<AgentExecutionListener> agentExecutionListenerList,
      VelocityTemplateService velocityTemplateService) {
    this.spanManager = spanManager;
    this.tracer = tracer;
    this.openAIClient = openAIClient;
    this.publisher = publisher;
    this.agentExecutionListenerList = agentExecutionListenerList;
    this.velocityTemplateService = velocityTemplateService;
    this.mapper = new ObjectMapper();
  }

  /**
   * Consumes an {@link AgentRequestEvent}, calls OpenAI, and publishes an {@link
   * AgentResponseEvent}.
   *
   * <p>This listener is asynchronous and runs on the {@code agentExecutionExecutor} thread pool. A
   * dedicated span is created around the OpenAI call to capture timing and important attributes.
   *
   * @param agentRequestEvent the agent request event containing session id, agent configuration and
   *     context messages
   * @throws JsonProcessingException if request/response payload serialization fails for
   *     logging/tracing
   */
  @LogTracer(spanName = "agent_request_received")
  @Async("agentExecutionExecutor")
  @EventListener
  public void callAgentExecutionEvent(AgentRequestEvent agentRequestEvent)
      throws JsonProcessingException {

    // Span name suggestion: "openai_chat_completion" (more specific than "call_openai")
    Span span = spanManager.createSpan("openai_chat_completion");

    try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
      logAgentExecutionRequest(agentRequestEvent, span);

      ChatCompletionRequest chatCompletionRequest =
          agentRequestEvent.toChatCompletionRequest(velocityTemplateService);

      logChatCompletionRequest(agentRequestEvent, chatCompletionRequest, span);

      ChatCompletionResponse chatCompletionResponse =
          openAIClient.call(agentRequestEvent.sessionId(), chatCompletionRequest);

      logChatCompletionResponse(agentRequestEvent, chatCompletionResponse, span);

      publisher.publishEvent(
          new AgentResponseEvent(
              agentRequestEvent.sessionId(),
              agentRequestEvent.agent(),
              agentRequestEvent.user(),
              chatCompletionResponse));

      spanManager.addEvent(span, "agent_response_published");
    } catch (Exception ex) {
      spanManager.addEvent(span, "openai_call_failed: " + ex.getClass().getSimpleName());
      throw ex;
    } finally {
      spanManager.endSpan(span);
    }
  }

  /**
   * Dispatches {@link AgentResponseEvent} to all registered {@link AgentExecutionListener}s.
   *
   * <p>This listener is asynchronous and runs on the {@code agentExecutionExecutor} thread pool. It
   * does not mutate the event; it simply forwards it to downstream processors.
   *
   * @param agentResponseEvent the OpenAI result wrapped as an orchestration response event
   */
  @LogTracer(spanName = "agent_response_dispatched")
  @Async("agentExecutionExecutor")
  @EventListener
  public void onAgentExecutionEvent(AgentResponseEvent agentResponseEvent) {
    agentExecutionListenerList.forEach(
        listener -> listener.onAgentResponseEvent(agentResponseEvent));
  }

  private void logAgentExecutionRequest(AgentRequestEvent agentRequestEvent, Span span) {
    assert agentRequestEvent.agent() != null;

    spanManager.addEvent(span, "agent_request_received");
    spanManager.addEvent(span, "session_id: " + agentRequestEvent.sessionId());
    spanManager.addEvent(span, "agent_id: " + agentRequestEvent.agent().getIdentifier());

    logger.debug(
        "[agent-exec] agent_request_received - sessionId: {}, agent: {}",
        agentRequestEvent.sessionId(),
        agentRequestEvent.agent().getIdentifier());
  }

  private void logChatCompletionRequest(
      AgentRequestEvent agentRequestEvent, ChatCompletionRequest chatCompletionRequest, Span span)
      throws JsonProcessingException {

    assert agentRequestEvent.agent() != null;

    String chatCompletionRequestJson = mapper.writeValueAsString(chatCompletionRequest);

    // Suggested log name: "[openai] chat_completion_request"
    logger.debug(
        "[openai] chat_completion_request - sessionId: {}, agent: {}, payload: {}",
        agentRequestEvent.sessionId(),
        agentRequestEvent.agent().getIdentifier(),
        chatCompletionRequestJson);

    // Suggested span event name: "openai_call_started"
    spanManager.addEvent(span, "openai_call_started");
    spanManager.addEvent(span, "openai_request: " + chatCompletionRequestJson);
  }

  private void logChatCompletionResponse(
      AgentRequestEvent agentRequestEvent, ChatCompletionResponse chatCompletionResponse, Span span)
      throws JsonProcessingException {

    assert agentRequestEvent.agent() != null;

    String chatCompletionResponseJson = mapper.writeValueAsString(chatCompletionResponse);

    // Suggested log name: "[openai] chat_completion_response"
    logger.debug(
        "[openai] chat_completion_response - sessionId: {}, agent: {}, payload: {}",
        agentRequestEvent.sessionId(),
        agentRequestEvent.agent().getIdentifier(),
        chatCompletionResponseJson);

    // Suggested span event name: "openai_call_succeeded"
    spanManager.addEvent(span, "openai_call_succeeded");
    spanManager.addEvent(span, "openai_response: " + chatCompletionResponseJson);
  }
}
