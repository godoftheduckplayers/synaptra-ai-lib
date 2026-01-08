package com.ducks.synaptra.agent;

import com.ducks.synaptra.client.openai.data.Tool;
import com.ducks.synaptra.client.openai.data.ToolChoice;
import com.ducks.synaptra.model.agent.Agent;
import com.ducks.synaptra.model.agent.ProviderConfig;
import java.util.ArrayList;
import java.util.List;

/** UI-driven spec for building a generic agent. */
public record AgentSpec(
    String identifier,
    String name,
    String goal,
    String prompt,
    ProviderConfig providerConfig,
    boolean supportsInterimMessages,
    ToolChoice toolChoice,

    // Parent / children (agents as tools via routing)
    Agent parent,
    List<Agent> childAgents,
    boolean enableRoutingTool,

    // Custom tools and tool registrars
    List<Tool> customTools) {
  public AgentSpec {
    if (identifier == null || identifier.isBlank()) {
      throw new IllegalArgumentException("identifier is required");
    }
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name is required");
    }
    if (goal == null || goal.isBlank()) {
      throw new IllegalArgumentException("goal is required");
    }
    if (prompt == null || prompt.isBlank()) {
      throw new IllegalArgumentException("prompt is required");
    }
    if (providerConfig == null) {
      throw new IllegalArgumentException("providerConfig is required");
    }
    if (childAgents == null) {
      childAgents = new ArrayList<>();
    }
    if (customTools == null) {
      customTools = new ArrayList<>();
    }
  }
}
