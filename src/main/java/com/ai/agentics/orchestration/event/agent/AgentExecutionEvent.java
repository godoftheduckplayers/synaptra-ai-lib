package com.ai.agentics.orchestration.event.agent;

import com.ai.agentics.client.openai.OpenAIClient;
import com.ai.agentics.client.openai.data.ChatCompletionRequest;
import com.ai.agentics.client.openai.data.ChatCompletionResponse;
import com.ai.agentics.orchestration.event.agent.contract.AgentRequestEvent;
import com.ai.agentics.orchestration.event.agent.contract.AgentResponseEvent;
import com.ai.agentics.tracer.AIAgenticTracer;
import com.ai.agentics.velocity.VelocityTemplateService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AgentExecutionEvent {

  private static final Logger logger = LoggerFactory.getLogger(AgentExecutionEvent.class);

  private final Tracer tracer;
  private final OpenAIClient openAIClient;
  private final ObjectMapper mapper;
  private final ApplicationEventPublisher publisher;
  private final List<AgentExecutionListener> agentExecutionListenerList;
  private final VelocityTemplateService velocityTemplateService;

  public AgentExecutionEvent(
      AIAgenticTracer AIAgenticTracer,
      OpenAIClient openAIClient,
      ApplicationEventPublisher publisher,
      List<AgentExecutionListener> agentExecutionListenerList,
      VelocityTemplateService velocityTemplateService) {
    this.tracer = AIAgenticTracer.getTracer();
    this.openAIClient = openAIClient;
    this.publisher = publisher;
    this.agentExecutionListenerList = agentExecutionListenerList;
    this.velocityTemplateService = velocityTemplateService;
    this.mapper = new ObjectMapper();
  }

  @Async
  @EventListener
  public void callAgentExecutionEvent(AgentRequestEvent agentRequestEvent)
      throws JsonProcessingException {
    Span span = tracer.spanBuilder("call-openai").startSpan();
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
  }

  @Async
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
    span.setAttribute("sessionId", agentRequestEvent.sessionId());
    span.setAttribute("agent", agentRequestEvent.agent().identifier());
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
    span.setAttribute("openai-response", chatCompletionResponseJson);
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
    span.setAttribute("openai-request", chatCompletionRequestJson);
  }
}
