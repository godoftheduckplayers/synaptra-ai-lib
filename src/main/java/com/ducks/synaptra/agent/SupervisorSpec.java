package com.ducks.synaptra.agent;

import com.ducks.synaptra.client.openai.data.Tool;
import com.ducks.synaptra.client.openai.data.ToolChoice;
import com.ducks.synaptra.model.agent.Agent;
import com.ducks.synaptra.model.agent.ProviderConfig;
import java.util.ArrayList;
import java.util.List;

/**
 * UI-driven specification for building a Supervisor agent.
 *
 * <p>Main prompt is minimal and does not include agent descriptions. Supported topics for fallback
 * are derived from child agents and non-system tools.
 */
public record SupervisorSpec(
    String identifier,
    String name,
    String goal,
    ProviderConfig providerConfig,
    boolean supportsInterimMessages,
    ToolChoice toolChoice,
    List<Agent> childAgents,

    // Tools toggles
    boolean enableRoutingTool,
    boolean enableChitChatTool,
    boolean enableFallbackTool,
    boolean enableFinalizeTool,

    // UI dynamic fields
    String language,
    String tone,
    String additionalInstructions,

    // Optional non-system tools attached directly to supervisor
    List<Tool> extraSupervisorTools) {
  public SupervisorSpec {
    if (identifier == null || identifier.isBlank())
      throw new IllegalArgumentException("identifier is required");
    if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
    if (goal == null || goal.isBlank()) throw new IllegalArgumentException("goal is required");
    if (providerConfig == null) throw new IllegalArgumentException("providerConfig is required");
    if (childAgents == null) childAgents = new ArrayList<>();
    if (extraSupervisorTools == null) extraSupervisorTools = new ArrayList<>();
  }
}
