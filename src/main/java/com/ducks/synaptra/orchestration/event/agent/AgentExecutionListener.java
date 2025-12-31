package com.ducks.synaptra.orchestration.event.agent;

import com.ducks.synaptra.orchestration.event.agent.contract.AgentResponseEvent;

/**
 * Listener contract for handling agent execution response events.
 *
 * <p>Implementations of this interface are responsible for reacting to the result of an agent
 * execution after the OpenAI call has been completed. The listener is invoked when an {@link
 * AgentResponseEvent} is published by the orchestration layer.
 *
 * <p>This interface defines the extension point for post-execution behaviors, such as:
 *
 * <ul>
 *   <li>Persisting agent outputs (memory, audit, or history storage)
 *   <li>Routing the response to another agent or orchestration step
 *   <li>Triggering tool execution based on the agent output
 *   <li>Delivering responses back to the user or external systems
 * </ul>
 *
 * <p><strong>Design note:</strong> Implementations should be side effect oriented and must not
 * block or perform long-running operations unless explicitly executed asynchronously. Each listener
 * should focus on a single responsibility to keep the event-driven pipeline composable and
 * predictable.
 *
 * <p>This contract intentionally contains no return value, reinforcing the event-driven nature of
 * the architecture and allowing multiple listeners to react independently to the same agent
 * execution result.
 *
 * @author Leandro Marques
 * @version 1.0.0
 */
public interface AgentExecutionListener {

  /**
   * Handles an agent execution response event.
   *
   * <p>This method is invoked after an agent has completed its execution and produced a response.
   * The provided {@link AgentResponseEvent} contains the session identifier, the agent that
   * produced the response, and the raw completion result.
   *
   * <p>Implementations should interpret and act upon the response according to their specific
   * responsibility within the orchestration flow.
   *
   * @param agentResponseEvent the event containing the result of the agent execution
   */
  void onAgentResponseEvent(AgentResponseEvent agentResponseEvent);
}
