package com.ai.agentics.execution;

import com.ai.agentics.client.openai.OpenAIClient;
import com.ai.agentics.client.openai.data.ChatCompletionRequest;
import com.ai.agentics.client.openai.data.ChatCompletionResponse;
import com.ai.agentics.execution.event.agent.AgentExecutionListener;
import com.ai.agentics.execution.event.agent.AgentExecutionRequestEvent;
import com.ai.agentics.execution.event.agent.AgentExecutionResponseEvent;
import com.ai.agentics.tracer.AIAgenticTracer;
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
public class AgentExecutionService {

  private static final Logger logger = LoggerFactory.getLogger(AgentExecutionService.class);

  private final Tracer tracer;
  private final OpenAIClient openAIClient;
  private final ObjectMapper mapper;
  private final ApplicationEventPublisher publisher;
  private final List<AgentExecutionListener> agentExecutionListenerList;

  public AgentExecutionService(
      AIAgenticTracer AIAgenticTracer,
      OpenAIClient openAIClient,
      ApplicationEventPublisher publisher,
      List<AgentExecutionListener> agentExecutionListenerList) {
    this.tracer = AIAgenticTracer.getTracer();
    this.openAIClient = openAIClient;
    this.publisher = publisher;
    this.agentExecutionListenerList = agentExecutionListenerList;
    this.mapper = new ObjectMapper();
  }

  @Async
  @EventListener
  public void executeAgent(AgentExecutionRequestEvent agentExecutionRequestEvent) {
    Span span = tracer.spanBuilder("call-openai").startSpan();
    try {
      ChatCompletionRequest chatCompletionRequest =
          agentExecutionRequestEvent.toChatCompletionRequest();

      logger.debug("[OPENAI] call openai with request: {}", chatCompletionRequest);

      span.setAttribute("request", mapper.writeValueAsString(chatCompletionRequest));
      ChatCompletionResponse chatCompletionResponse =
          openAIClient.call(agentExecutionRequestEvent.sessionId(), chatCompletionRequest);
      publisher.publishEvent(
          new AgentExecutionResponseEvent(
              agentExecutionRequestEvent.sessionId(),
              agentExecutionRequestEvent.agent(),
              agentExecutionRequestEvent.user(),
              chatCompletionResponse));
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    } finally {
      span.end();
    }
  }

  @Async
  @EventListener
  public void onAgentExecutionEvent(
      AgentExecutionResponseEvent agentExecutionResponseEventMessageEvent) {
    agentExecutionListenerList.forEach(
        listener ->
            listener.onAgentExecutionResponseEvent(agentExecutionResponseEventMessageEvent));
  }
}
