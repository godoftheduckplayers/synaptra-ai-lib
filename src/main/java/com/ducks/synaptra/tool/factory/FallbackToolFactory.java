package com.ducks.synaptra.tool.factory;

import com.ducks.synaptra.client.openai.data.FunctionDef;
import com.ducks.synaptra.client.openai.data.Parameter;
import com.ducks.synaptra.client.openai.data.ParameterProperty;
import com.ducks.synaptra.client.openai.data.Tool;
import com.ducks.synaptra.model.agent.Agent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Registers a fallback tool for any agent.
 *
 * <p>This tool must be used when the user's request is outside the agent's current supported scope.
 * The fallback response must:
 *
 * <ul>
 *   <li>Briefly explain the limitation
 *   <li>List what the agent CAN do right now, based on a provided tools catalog
 *   <li>Not mention system/internal tools
 * </ul>
 *
 * <p>The supported topics are derived from:
 *
 * <ul>
 *   <li>The agent's non-system tools
 *   <li>Plus an optional list of additional tools provided at registration time
 * </ul>
 */
public final class FallbackToolFactory {

  private FallbackToolFactory() {}

  /**
   * Registers the fallback tool on the given agent.
   *
   * @param agent the agent that will own the tool
   * @param additionalTools optional list of extra tools to enrich the capabilities catalog
   */
  public static void registerFallbackTool(Agent agent, List<Tool> additionalTools) {
    if (agent == null) {
      throw new IllegalArgumentException("agent must not be null");
    }

    String capabilitiesCatalog = buildCapabilitiesCatalog(agent, additionalTools);

    Parameter parameter = new Parameter();

    parameter.addProperty(
        "response",
        new ParameterProperty(
            "string",
            "A short message explaining that the request is not supported right now, "
                + "followed by what IS supported based on the provided capabilities catalog. "
                + "Do NOT mention internal/system tools. Use the same language as the user."),
        true);

    parameter.addProperty(
        "supported_topics",
        new ParameterProperty(
            "string",
            "A list of supported topics selected ONLY from the capabilities catalog. Do not invent new topics."),
        true);

    if (agent.isSupportsInterimMessages()) {
      parameter.addProperty(
          "interim_message",
          new ParameterProperty(
              "string",
              "Optional intermediate message shown before sending the final fallback response."),
          false);
    }

    FunctionDef fn =
        new FunctionDef(
            "fallback",
            "Use this tool when the user request is outside the agent's current supported scope.\n"
                + "Your response MUST:\n"
                + "- Briefly state you can't do that request right now.\n"
                + "- Provide the supported topics compatible with the current setup.\n"
                + "- Supported topics MUST be chosen only from this capabilities catalog:\n"
                + capabilitiesCatalog
                + "\n"
                + "Do NOT mention internal/system tools (routing/fallback/chit-chat/finalize) in the response.",
            parameter);

    agent.addTool(new Tool(fn));
  }

  // ---------------------------------------------------------------------------
  // Catalog derivation
  // ---------------------------------------------------------------------------

  private static String buildCapabilitiesCatalog(Agent agent, List<Tool> additionalTools) {
    Set<String> topics = new LinkedHashSet<>();

    // 1) Agent's current non-system tools
    if (agent.getTools() != null) {
      for (Tool tool : agent.getTools()) {
        addToolIfNonSystem(topics, tool);
      }
    }

    // 2) Additional tools provided by caller
    if (additionalTools != null) {
      for (Tool tool : additionalTools) {
        addToolIfNonSystem(topics, tool);
      }
    }

    if (topics.isEmpty()) {
      topics.add("Handle requests within the currently configured tools");
    }

    StringBuilder sb = new StringBuilder();
    for (String t : topics) {
      sb.append("- ").append(t).append("\n");
    }
    return sb.toString().trim();
  }

  private static void addToolIfNonSystem(Set<String> topics, Tool tool) {
    if (tool == null || tool.function() == null || tool.function().getName() == null) {
      return;
    }

    String name = tool.function().getName().trim();
    if (isSystemTool(name)) {
      return;
    }

    String label = humanLabelForTool(tool);
    if (label != null && !label.isBlank()) {
      topics.add(label);
    }
  }

  /**
   * Creates a human-friendly label for a tool: prefers the first non-empty line of the description,
   * otherwise uses the function name.
   */
  private static String humanLabelForTool(Tool tool) {
    String desc = tool.function().getDescription();
    if (desc != null && !desc.isBlank()) {
      for (String line : desc.split("\\R")) {
        if (line != null && !line.trim().isBlank()) {
          return line.trim();
        }
      }
    }
    return tool.function().getName();
  }

  /** Identifies system/internal tools that must not be exposed as "supported topics". */
  private static boolean isSystemTool(String toolName) {
    if (toolName == null) return true;

    String n = toolName.toLowerCase(Locale.ROOT).trim();

    // Common internal/system tool names (adjust as needed for your stack)
    if (n.equals("route_to_agent")
        || n.equals("route_to_parent")
        || n.equals("supervisor_chit_chat")
        || n.equals("chit_chat")
        || n.equals("supervisor_fallback")
        || n.equals("fallback")
        || n.equals("finalize")
        || n.equals("finalize_request")) {
      return true;
    }

    // Conservative: any routing infrastructure
    return n.startsWith("route_") || n.contains("route");
  }

  // Convenience overload
  public static void registerFallbackTool(Agent agent) {
    registerFallbackTool(agent, new ArrayList<>());
  }
}
