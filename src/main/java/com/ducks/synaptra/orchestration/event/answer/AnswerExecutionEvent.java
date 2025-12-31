package com.ducks.synaptra.orchestration.event.answer;

import com.ducks.synaptra.log.LogTracer;
import com.ducks.synaptra.orchestration.event.answer.contract.AnswerResponseEvent;
import java.util.List;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Publishes agent answers to all registered {@link AnswerExecutionListener} implementations.
 *
 * <p>This component is part of the orchestration event pipeline. It listens for {@link
 * AnswerResponseEvent} events (which represent an agent's final or intermediate answer ready to be
 * delivered) and dispatches them to interested listeners.
 *
 * <p>Typical listeners may:
 *
 * <ul>
 *   <li>Send the answer to a websocket/STOMP destination
 *   <li>Persist the final answer to a database
 *   <li>Append the answer to an interaction history log
 *   <li>Trigger UI notifications or downstream integration events
 * </ul>
 *
 * <p><strong>Responsibility:</strong> deliver/dispatch an agent-produced answer to specialized
 * listeners. This class does not interpret or transform the answer content.
 *
 * @author Leandro Marques
 * @since 1.0.0
 */
@Service
public class AnswerExecutionEvent {

  private final List<AnswerExecutionListener> answerExecutionListenerList;

  public AnswerExecutionEvent(List<AnswerExecutionListener> answerExecutionListenerList) {
    this.answerExecutionListenerList = answerExecutionListenerList;
  }

  /**
   * Handles answer delivery events and forwards them to registered listeners.
   *
   * <p>This method is executed asynchronously using the {@code agentExecutionExecutor} executor. It
   * logs the answer metadata and then notifies each listener.
   *
   * <p><strong>Preconditions:</strong>
   *
   * <ul>
   *   <li>{@code answerResponseEvent.agent()} must not be {@code null}
   * </ul>
   *
   * @param answerResponseEvent the event containing the session id, agent metadata, and the answer
   *     to be delivered
   */
  @LogTracer(spanName = "answer_delivery_event")
  @Async("agentExecutionExecutor")
  @EventListener
  public void onAnswerExecutionEvent(AnswerResponseEvent answerResponseEvent) {
    answerExecutionListenerList.forEach(
        listener -> listener.onAnswerExecutionResponseEvent(answerResponseEvent));
  }
}
