package com.ducks.synaptra.event.agent;

import com.ducks.synaptra.client.openai.OpenAIClient;
import com.ducks.synaptra.client.openai.data.ChatCompletionRequest;
import com.ducks.synaptra.client.openai.data.ChatCompletionResponse;
import com.ducks.synaptra.event.agent.model.AgentRequestEvent;
import com.ducks.synaptra.event.agent.model.AgentResponseEvent;
import com.ducks.synaptra.log.LogTracer;
import com.ducks.synaptra.log.tracing.SpanManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class AgentExecutionPublisherImpl implements AgentExecutionPublisher {

  private static final Logger logger = LogManager.getLogger(AgentExecutionPublisherImpl.class);

  private final SpanManager spanManager;
  private final Tracer tracer;
  private final OpenAIClient openAIClient;
  private final ObjectMapper mapper = new ObjectMapper();
  private final List<AgentExecutionListener> agentExecutionListenerList;

  @LogTracer(spanName = "agent_request_received")
  @EventListener
  @Async("agentExecutionExecutor")
  @Override
  public void publisherAgentRequestEvent(AgentRequestEvent agentRequestEvent)
      throws JsonProcessingException {
    Span span = spanManager.createSpan("openai_chat_completion");

    try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
      logAgentExecutionRequest(agentRequestEvent, span);

      ChatCompletionRequest chatCompletionRequest = agentRequestEvent.toChatCompletionRequest();

      logChatCompletionRequest(agentRequestEvent, chatCompletionRequest, span);

      ChatCompletionResponse chatCompletionResponse =
          openAIClient.call(agentRequestEvent.getSessionId(), chatCompletionRequest);

      logChatCompletionResponse(agentRequestEvent, chatCompletionResponse, span);

      AgentResponseEvent agentResponseEvent =
          new AgentResponseEvent(
              agentRequestEvent.getSessionId(),
              agentRequestEvent.getAgent(),
              agentRequestEvent.getUser(),
              chatCompletionResponse);

      agentExecutionListenerList.forEach(
          agentExecutionListener ->
              agentExecutionListener.onAgentResponseEvent(agentResponseEvent));

      spanManager.addEvent(span, "agent_response_published");
    } catch (Exception ex) {
      spanManager.addEvent(span, "openai_call_failed: " + ex.getClass().getSimpleName());
      throw ex;
    } finally {
      spanManager.endSpan(span);
    }
  }

  private void logAgentExecutionRequest(AgentRequestEvent agentRequestEvent, Span span) {
    spanManager.addEvent(span, "agent_request_received");
    spanManager.addEvent(span, "session_id: " + agentRequestEvent.getSessionId());
    spanManager.addEvent(span, "agent_id: " + agentRequestEvent.getAgent().getIdentifier());

    logger.debug(
        "[agent-exec] agent_request_received - sessionId: {}, agent: {}",
        agentRequestEvent.getAgent(),
        agentRequestEvent.getAgent().getIdentifier());
  }

  private void logChatCompletionRequest(
      AgentRequestEvent agentRequestEvent, ChatCompletionRequest chatCompletionRequest, Span span)
      throws JsonProcessingException {

    String chatCompletionRequestJson = mapper.writeValueAsString(chatCompletionRequest);

    logger.debug(
        "[openai] chat_completion_request - sessionId: {}, agent: {}, payload: {}",
        agentRequestEvent.getSessionId(),
        agentRequestEvent.getAgent().getIdentifier(),
        chatCompletionRequestJson);

    spanManager.addEvent(span, "openai_call_started");
    spanManager.addEvent(span, "openai_request: " + chatCompletionRequestJson);
  }

  private void logChatCompletionResponse(
      AgentRequestEvent agentRequestEvent, ChatCompletionResponse chatCompletionResponse, Span span)
      throws JsonProcessingException {

    String chatCompletionResponseJson = mapper.writeValueAsString(chatCompletionResponse);

    logger.debug(
        "[openai] chat_completion_response - sessionId: {}, agent: {}, payload: {}",
        agentRequestEvent.getSessionId(),
        agentRequestEvent.getAgent().getIdentifier(),
        chatCompletionResponseJson);

    spanManager.addEvent(span, "openai_call_succeeded");
    spanManager.addEvent(span, "openai_response: " + chatCompletionResponseJson);
  }
}
