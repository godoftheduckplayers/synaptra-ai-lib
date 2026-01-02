package com.ducks.synaptra.publisher.contract;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents routing instructions used to hand off execution to another agent, with optional
 * support for partial or intermediate user-facing responses.
 *
 * <p>A {@code RouteMapper} is typically produced as the output of an internal routing tool (e.g.
 * {@code route_to_agent}) and describes how execution should transition from the current agent to a
 * target agent.
 *
 * <p>In addition to defining the routing target and objective, this record may also carry a {@code
 * response} field, which allows the system to emit a partial or intermediate message to the user
 * while the routed agent is being prepared, executed, or orchestrated.
 *
 * <h2>Fields</h2>
 *
 * <ul>
 *   <li><b>{@code agent}</b> — the identifier or name of the target agent to be executed
 *   <li><b>{@code input}</b> — optional contextual input to be considered by the target agent
 *   <li><b>{@code objective}</b> — a clear statement of what the target agent is expected to
 *       achieve
 *   <li><b>{@code response}</b> — an optional partial or intermediate response intended to be
 *       delivered to the user before or during agent handoff
 * </ul>
 *
 * <h2>Usage of {@code response}</h2>
 *
 * <p>The {@code response} field is designed to improve user experience in multi-step, asynchronous,
 * or long-running agent workflows.
 *
 * <p>Typical use cases include:
 *
 * <ul>
 *   <li>Informing the user that their request is being routed to a specialized agent
 *   <li>Providing feedback that a background task or analysis has started
 *   <li>Emitting a short explanation of what will happen next in the execution flow
 * </ul>
 *
 * <p>This field should contain a user-friendly, generic message and must not be treated as the
 * final response of the workflow. The final output is expected to be produced by the routed agent
 * itself.
 *
 * <p>If {@code response} is {@code null}, the system may choose to remain silent during the routing
 * phase.
 *
 * <h2>Velocity integration</h2>
 *
 * <p>This record can expose a Velocity-compatible context via {@link #velocityContext()}, enabling
 * it to be rendered into a system handoff prompt for the target agent.
 *
 * @param agent the name or identifier of the target agent
 * @param input contextual input relevant to the handoff (may be {@code null})
 * @param objective the objective or goal that the target agent must accomplish
 * @param response optional partial or intermediate message to be delivered to the user
 * @author Leandro Marques
 * @since 1.0.0
 */
public record RouteMapper(String agent, String input, String objective, String response) {

  /**
   * Builds a Velocity template context from this routing instruction.
   *
   * <p>The returned map can be used to render handoff prompts that clearly communicate the routing
   * intent and execution objective to the next agent.
   *
   * <p>The {@code response} field is intentionally excluded from this context, as it is meant for
   * user-facing feedback rather than agent-to-agent communication.
   *
   * @return a map containing {@code agent}, {@code input}, and {@code objective} entries
   */
  public Map<String, Object> velocityContext() {
    Map<String, Object> context = new HashMap<>();
    context.put("agent", agent);
    context.put("input", input);
    context.put("objective", objective);
    return context;
  }
}
