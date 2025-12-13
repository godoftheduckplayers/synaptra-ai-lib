package com.ai.agentics.orchestration;

import com.ai.agentics.orchestration.event.agent.contract.AgentResponseEvent;
import com.ai.agentics.orchestration.event.answer.contract.AnswerResponseEvent;
import com.ai.agentics.orchestration.event.tool.contract.ToolResponseEvent;
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
      AgentResponseEvent agentResponseEventMessageEvent) {
    agentResponseEventMessageEvent
        .chatCompletionResponse()
        .choices()
        .forEach(
            choice -> {
              String content = choice.message().content();
              if (content != null && !content.isEmpty()) {
                publisher.publishEvent(
                    new AnswerResponseEvent(
                        agentResponseEventMessageEvent.sessionId(),
                        agentResponseEventMessageEvent.agent(),
                        agentResponseEventMessageEvent.user(),
                        content));
              }
              if ("tool_calls".equals(choice.finishReason())) {
                choice
                    .message()
                    .toolCalls()
                    .forEach(
                        toolCall ->
                            publisher.publishEvent(
                                new ToolResponseEvent(
                                    agentResponseEventMessageEvent.sessionId(),
                                    agentResponseEventMessageEvent.agent(),
                                    agentResponseEventMessageEvent.user(),
                                    toolCall)));
                System.out.println(choice);
              }
            });
  }
}
