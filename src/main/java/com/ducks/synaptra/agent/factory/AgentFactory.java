package com.ducks.synaptra.agent.factory;

import com.ducks.synaptra.agent.AgentSpec;
import com.ducks.synaptra.client.openai.data.ToolChoice;
import com.ducks.synaptra.model.agent.Agent;
import com.ducks.synaptra.tool.factory.RouteAgentToolFactory;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Generic factory to build an {@link Agent} that may: - own custom tools - register tools via
 * pluggable registrars (tool factories) - have child agents (enabling routing tools) - have an
 * optional parent agent (enabling route-to-parent tool)
 *
 * <p>UI-friendly: everything is configured via {@link AgentSpec}.
 */
public final class AgentFactory {

  private AgentFactory() {}

  public static Agent build(AgentSpec spec) {
    Objects.requireNonNull(spec, "spec is required");
    Objects.requireNonNull(spec.providerConfig(), "providerConfig is required");

    List<Agent> children =
        spec.childAgents() == null ? Collections.emptyList() : spec.childAgents();

    // 1) Create base agent (tools list starts empty in Agent ctor if null)
    Agent agent =
        new Agent(
            spec.identifier(),
            spec.name(),
            spec.goal(),
            spec.prompt(),
            spec.providerConfig(),
            spec.supportsInterimMessages(),
            null,
            spec.toolChoice() == null ? ToolChoice.AUTO : spec.toolChoice(),
            null,
            children);

    // 2) Attach parent (optional). Your Agent.setParent will register route-to-parent tool.
    if (spec.parent() != null) {
      agent.setParent(spec.parent());
    }

    // 3) Optional: explicitly ensure routing tool exists if requested and there are children.
    // NOTE: Your Agent constructor already calls RouteAgentToolFactory.routeTool(this) when
    // children != empty.
    // This explicit call is harmless if RouteAgentToolFactory is idempotent; if not, you may want
    // to guard it.
    if (spec.enableRoutingTool() && !children.isEmpty()) {
      RouteAgentToolFactory.routeTool(agent);
    }

    // 4) Add custom tools (UI-provided tools)
    if (spec.customTools() != null) {
      spec.customTools().stream().filter(Objects::nonNull).forEach(agent::addTool);
    }

    return agent;
  }
}
