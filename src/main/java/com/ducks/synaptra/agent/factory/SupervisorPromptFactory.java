package com.ducks.synaptra.agent.factory;

import com.ducks.synaptra.model.agent.Agent;
import com.ducks.synaptra.agent.SupervisorSpec;

import java.util.List;

public final class SupervisorPromptFactory {

  private SupervisorPromptFactory() {}

  public static String buildPrompt(SupervisorSpec spec, List<Agent> childAgents) {
    String language = blankToDefault(spec.language(), "pt-BR");
    String tone = blankToDefault(spec.tone(), "concise");

    StringBuilder sb = new StringBuilder(900);

    sb.append("You are the SUPERVISOR agent.\n\n");

    sb.append("OBJECTIVE:\n");
    sb.append(spec.goal()).append("\n\n");

    sb.append("PROCEDURAL RULES (STRICT):\n");
    sb.append("- You only orchestrate; you do NOT execute domain work yourself.\n");
    sb.append("- Routing decisions must be performed using the routing tool (when enabled).\n");
    sb.append("- For chit-chat, you MUST use the chit-chat tool.\n");
    sb.append("- For out-of-scope or unsupported requests, you MUST use the fallback tool.\n");
    sb.append("- If the user request is ambiguous, ask ONE clarification question and wait.\n");
    sb.append("- Never claim an action is completed unless a tool/agent result confirms it.\n\n");

    sb.append("LANGUAGE & STYLE:\n");
    sb.append("- Respond in: ").append(language).append("\n");
    sb.append("- Tone: ").append(tone).append("\n");
    sb.append("- Keep responses short and actionable.\n\n");

    sb.append("CONTEXT:\n");
    sb.append("- Linked child agents: ").append(childAgents == null ? 0 : childAgents.size()).append("\n");

    if (!isBlank(spec.additionalInstructions())) {
      sb.append("\nADDITIONAL INSTRUCTIONS:\n");
      sb.append(spec.additionalInstructions()).append("\n");
    }

    return sb.toString();
  }

  private static boolean isBlank(String s) {
    return s == null || s.trim().isEmpty();
  }

  private static String blankToDefault(String value, String defaultValue) {
    return isBlank(value) ? defaultValue : value.trim();
  }
}
