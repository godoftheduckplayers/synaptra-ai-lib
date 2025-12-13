package com.ai.agentics.execution;

import com.ai.agentics.execution.event.agent.AgentExecutionResponseEvent;
import com.ai.agentics.execution.event.response.AnswerExecutionResponseEvent;
import com.ai.agentics.execution.event.tool.ToolExecutionResponseEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;

@RequiredArgsConstructor
@RequestMapping
@Service
public class OrchestrationExecutionService {

  private final ApplicationEventPublisher publisher;

  @Async
  @EventListener
  public void onAgentExecutionEvent(
      AgentExecutionResponseEvent agentExecutionResponseEventMessageEvent) {
    agentExecutionResponseEventMessageEvent
        .chatCompletionResponse()
        .choices()
        .forEach(
            choice -> {
              String content = choice.message().content();
              if (content != null && !content.isEmpty()) {
                publisher.publishEvent(
                    new AnswerExecutionResponseEvent(
                        agentExecutionResponseEventMessageEvent.sessionId(),
                        agentExecutionResponseEventMessageEvent.agent(),
                        agentExecutionResponseEventMessageEvent.user(),
                        content));
              }
              if ("tool_calls".equals(choice.finishReason())) {
                choice
                    .message()
                    .toolCalls()
                    .forEach(
                        toolCall ->
                            publisher.publishEvent(
                                new ToolExecutionResponseEvent(
                                    agentExecutionResponseEventMessageEvent.sessionId(),
                                    agentExecutionResponseEventMessageEvent.agent(),
                                    agentExecutionResponseEventMessageEvent.user(),
                                    toolCall)));
                System.out.println(choice);
              }
            });
  }
}
