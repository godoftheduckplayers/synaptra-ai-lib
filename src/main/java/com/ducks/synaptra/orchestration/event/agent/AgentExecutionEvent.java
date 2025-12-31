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

@Service
public class AgentExecutionEvent {

  private static final Logger logger = LogManager.getLogger(AgentExecutionEvent.class);

  /** Service for managing OpenTelemetry spans. */
  private final SpanManager spanManager;

  /** Tracer for managing span scope. */
  private final Tracer tracer;

  private final OpenAIClient openAIClient;
  private final ObjectMapper mapper;
  private final ApplicationEventPublisher publisher;
  private final List<AgentExecutionListener> agentExecutionListenerList;
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

  @LogTracer(spanName = "agent_request_event")
  @Async("agentExecutionExecutor")
  @EventListener
  public void callAgentExecutionEvent(AgentRequestEvent agentRequestEvent)
      throws JsonProcessingException {
    Span span = spanManager.createSpan("call_openai");

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
    } finally {
      spanManager.endSpan(span);
    }
  }

  @LogTracer(spanName = "agent_response_event")
  @Async("agentExecutionExecutor")
  @EventListener
  public void onAgentExecutionEvent(AgentResponseEvent agentResponseEvent) {
    assert agentResponseEvent.agent() != null;
    logger.debug(
        "[AGENT_RESPONSE_EVENT] - sessionId: {}, agent: {}",
        agentResponseEvent.sessionId(),
        agentResponseEvent.agent().identifier());
    agentExecutionListenerList.forEach(
        listener -> listener.onAgentResponseEvent(agentResponseEvent));
  }

  private void logAgentExecutionRequest(AgentRequestEvent agentRequestEvent, Span span) {
    assert agentRequestEvent.agent() != null;
    logger.debug(
        "[AGENT_REQUEST_EVENT] - sessionId: {}, agent: {}",
        agentRequestEvent.sessionId(),
        agentRequestEvent.agent().identifier());
    spanManager.addEvent(span, "sessionId: " + agentRequestEvent.sessionId());
    spanManager.addEvent(span, "agent: " + agentRequestEvent.agent().identifier());
  }

  private void logChatCompletionResponse(
      AgentRequestEvent agentRequestEvent, ChatCompletionResponse chatCompletionResponse, Span span)
      throws JsonProcessingException {
    assert agentRequestEvent.agent() != null;
    String chatCompletionResponseJson = mapper.writeValueAsString(chatCompletionResponse);
    logger.debug(
        "[OPENAI] - sessionId: {}, agent: {}, response: {}",
        agentRequestEvent.sessionId(),
        agentRequestEvent.agent().identifier(),
        chatCompletionResponseJson);
    spanManager.addEvent(span, "openai-response: " + chatCompletionResponseJson);
  }

  private void logChatCompletionRequest(
      AgentRequestEvent agentRequestEvent, ChatCompletionRequest chatCompletionRequest, Span span)
      throws JsonProcessingException {
    assert agentRequestEvent.agent() != null;
    String chatCompletionRequestJson = mapper.writeValueAsString(chatCompletionRequest);
    logger.debug(
        "[OPENAI] - sessionId: {}, agent: {}, request: {}",
        agentRequestEvent.sessionId(),
        agentRequestEvent.agent().identifier(),
        chatCompletionRequestJson);
    spanManager.addEvent(span, "openai-request: " + chatCompletionRequestJson);
  }
}
