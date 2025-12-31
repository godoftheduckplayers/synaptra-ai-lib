package com.ducks.synaptra.orchestration;

import com.ducks.synaptra.client.openai.data.Choice;
import com.ducks.synaptra.log.LogTracer;
import com.ducks.synaptra.orchestration.event.agent.contract.AgentResponseEvent;
import com.ducks.synaptra.orchestration.event.answer.contract.AnswerResponseEvent;
import com.ducks.synaptra.orchestration.event.tool.contract.ToolResponseEvent;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Interprets agent execution results and emits follow-up orchestration events.
 *
 * <p>This service acts as the interpretation layer between the raw agent execution response and the
 * rest of the orchestration pipeline. It consumes {@link AgentResponseEvent} events and, for each
 * model choice:
 *
 * <ul>
 *   <li>Emits {@link AnswerResponseEvent} when user-facing content is present
 *   <li>Emits {@link ToolResponseEvent} when the agent requests tool execution
 * </ul>
 *
 * <p>This class does not execute tools and does not deliver answers directly. Its sole
 * responsibility is to interpret the agent output and translate it into domain-specific
 * orchestration events.
 *
 * <p>All processing is asynchronous and executed on the {@code agentExecutionExecutor}.
 *
 * @author Leandro Marques
 * @since 1.0.0
 */
@RequiredArgsConstructor
@Service
public class OrchestrationExecutionService {

  private static final Logger logger = LogManager.getLogger(OrchestrationExecutionService.class);

  private static final String FINISH_REASON_TOOL_CALLS = "tool_calls";

  private final ApplicationEventPublisher publisher;

  /**
   * Handles an {@link AgentResponseEvent} and emits answer and/or tool execution events based on
   * the agent output.
   *
   * <p>Each choice returned by the model is independently interpreted. A single agent execution may
   * therefore result in:
   *
   * <ul>
   *   <li>One or more {@link AnswerResponseEvent}s
   *   <li>One or more {@link ToolResponseEvent}s
   * </ul>
   *
   * @param agentResponseEvent the event containing the raw agent execution result
   */
  @LogTracer(spanName = "interpret_agent_execution_response")
  @Async("agentExecutionExecutor")
  @EventListener
  public void onAgentExecutionEvent(AgentResponseEvent agentResponseEvent) {
    Objects.requireNonNull(agentResponseEvent);

    agentResponseEvent
        .chatCompletionResponse()
        .choices()
        .forEach(choice -> handleChoice(agentResponseEvent, choice));
  }

  private void handleChoice(AgentResponseEvent agentResponseEvent, Choice choice) {
    publishAnswerIfPresent(agentResponseEvent, choice);
    publishToolCallsIfPresent(agentResponseEvent, choice);
  }

  private void publishToolCallsIfPresent(AgentResponseEvent agentResponseEvent, Choice choice) {

    if (!FINISH_REASON_TOOL_CALLS.equals(choice.finishReason())) {
      return;
    }

    if (choice.message().toolCalls() == null) {
      return;
    }

    choice
        .message()
        .toolCalls()
        .forEach(
            toolCall -> {
              logger.debug(
                  "[TOOL_CALL_REQUESTED] sessionId={}, agent={}, tool={}",
                  agentResponseEvent.sessionId(),
                  agentResponseEvent.agent() != null
                      ? agentResponseEvent.agent().getIdentifier()
                      : "null",
                  toolCall.function().name());

              publisher.publishEvent(
                  new ToolResponseEvent(
                      agentResponseEvent.sessionId(),
                      agentResponseEvent.agent(),
                      agentResponseEvent.user(),
                      toolCall));
            });
  }

  private void publishAnswerIfPresent(AgentResponseEvent agentResponseEvent, Choice choice) {

    String content = choice.message().content();
    if (content == null || content.isBlank()) {
      return;
    }

    logger.debug(
        "[ANSWER_EMITTED] sessionId={}, agent={}, contentLength={}",
        agentResponseEvent.sessionId(),
        agentResponseEvent.agent() != null ? agentResponseEvent.agent().getIdentifier() : "null",
        content.length());

    publisher.publishEvent(
        new AnswerResponseEvent(
            agentResponseEvent.sessionId(),
            agentResponseEvent.agent(),
            agentResponseEvent.user(),
            content));
  }
}
