package com.ducks.synaptra.publisher.contract;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents routing instructions used to hand off execution to another agent.
 *
 * <p>A {@code RouteMapper} is typically produced as the output of an internal routing tool (e.g.
 * {@code route_to_agent}) and describes how execution should transition from the current agent to a
 * target agent.
 *
 * <p>The record contains:
 *
 * <ul>
 *   <li>{@code agent}: the identifier or name of the target agent to be executed
 *   <li>{@code input}: optional contextual input to be considered by the target agent
 *   <li>{@code objective}: a clear statement of what the target agent is expected to achieve
 * </ul>
 *
 * <p>This record can expose a Velocity-compatible context via {@link #velocityContext()}, enabling
 * it to be rendered into a system handoff prompt for the target agent.
 *
 * @param agent the name or identifier of the target agent
 * @param input contextual input relevant to the handoff (may be {@code null})
 * @param objective the objective or goal that the target agent must accomplish
 * @author Leandro Marques
 * @since 1.0.0
 */
public record RouteMapper(String agent, String input, String objective) {

  /**
   * Builds a Velocity template context from this routing instruction.
   *
   * <p>The returned map can be used to render handoff prompts that clearly communicate the routing
   * intent and execution objective to the next agent.
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
