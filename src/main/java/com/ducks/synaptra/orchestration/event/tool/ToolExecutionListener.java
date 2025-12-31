package com.ducks.synaptra.orchestration.event.tool;

import com.ducks.synaptra.orchestration.event.tool.contract.ToolResponseEvent;

/**
 * Listener contract for executing tools requested by an agent.
 *
 * <p>Implementations of this interface are responsible for performing the actual execution of tools
 * requested by an agent. These tools are considered external to the orchestration core and may
 * involve side effects such as I/O operations, database access, API calls, or integrations with
 * external systems.
 *
 * <p>This listener is invoked only for non-internal tool calls. Internal orchestration functions
 * (such as routing or record persistence) are handled directly by the orchestration layer and are
 * not forwarded to implementations of this interface.
 *
 * <p>Typical responsibilities of a {@code ToolExecutionListener} include:
 *
 * <ul>
 *   <li>Invoking the corresponding tool implementation
 *   <li>Managing execution lifecycle (success, failure, retries)
 *   <li>Publishing follow-up events based on the tool execution result
 *   <li>Emitting logs, metrics, and traces for observability
 * </ul>
 *
 * <p><strong>Design note:</strong> Implementations should not perform orchestration decisions or
 * agent execution logic. Their sole responsibility is to execute the requested tool and report the
 * outcome back to the orchestration pipeline.
 *
 * @author Leandro Marques
 * @since 1.0.0
 */
public interface ToolExecutionListener {

  /**
   * Executes a tool based on the provided tool response event.
   *
   * <p>This method is invoked when an agent requests the execution of an external tool. The {@link
   * ToolResponseEvent} contains the tool call details, execution context, and correlation
   * information.
   *
   * @param toolResponseEvent the event containing the tool execution request
   */
  void onToolExecutionResponseEvent(ToolResponseEvent toolResponseEvent);
}
