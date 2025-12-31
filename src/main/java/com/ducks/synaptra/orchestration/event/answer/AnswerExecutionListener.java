package com.ducks.synaptra.orchestration.event.answer;

import com.ducks.synaptra.orchestration.event.answer.contract.AnswerResponseEvent;

/**
 * Listener contract for handling agent answer delivery events.
 *
 * <p>Implementations of this interface are responsible for receiving and processing an {@link
 * AnswerResponseEvent}, which represents an agent answer that is ready to be delivered to the
 * outside world (e.g., user interface, messaging system, persistence layer).
 *
 * <p>This interface defines the extension point for the final stage of the agentic execution
 * pipeline, where the agent's response is no longer being interpreted or reasoned about, but
 * delivered, stored, or forwarded.
 *
 * <p>Typical responsibilities of an {@code AnswerExecutionListener} include:
 *
 * <ul>
 *   <li>Sending the agent answer to a frontend (WebSocket, STOMP, HTTP stream)
 *   <li>Persisting the answer as part of a conversation or audit trail
 *   <li>Publishing the answer to external systems or message brokers
 *   <li>Triggering UI updates or notifications
 * </ul>
 *
 * <p><strong>Design note:</strong> Implementations should focus on side effects and must avoid
 * performing agent execution logic or orchestration decisions. Long-running operations should be
 * executed asynchronously when appropriate.
 *
 * <p>This contract intentionally has no return value, reinforcing the event-driven and fan-out
 * nature of the answer delivery stage.
 *
 * @author Leandro Marques
 * @since 1.0.0
 */
public interface AnswerExecutionListener {

  /**
   * Handles an agent answer delivery event.
   *
   * <p>This method is invoked when an agent answer has been produced and is ready to be delivered
   * or processed. The {@link AnswerResponseEvent} contains the session identifier, agent metadata,
   * and the final answer payload.
   *
   * @param answerResponseEvent the event containing the agent answer to be handled
   */
  void onAnswerExecutionResponseEvent(AnswerResponseEvent answerResponseEvent);
}
