package com.ducks.synaptra.tool.factory;

import com.ducks.synaptra.client.openai.data.FunctionDef;
import com.ducks.synaptra.client.openai.data.Parameter;
import com.ducks.synaptra.client.openai.data.ParameterProperty;
import com.ducks.synaptra.client.openai.data.Tool;
import com.ducks.synaptra.model.agent.Agent;

/**
 * Registers a chit-chat tool for any agent.
 *
 * <p>This tool is intended ONLY for casual conversation (greetings, acknowledgements, polite
 * responses, and small talk). It must NOT be used for orchestration, routing, domain execution, or
 * limitation handling.
 *
 * <p>Any agent may invoke this tool when the user intent is purely conversational.
 */
public final class ChitChatToolFactory {

  private ChitChatToolFactory() {}

  /**
   * Registers the chit-chat tool on the given agent.
   *
   * @param agent the agent that will own the tool
   */
  public static void registerChitChatTool(Agent agent) {
    if (agent == null) {
      throw new IllegalArgumentException("agent must not be null");
    }

    Parameter parameter = getParameter(agent);

    FunctionDef chitChatFunction =
        new FunctionDef(
            "chit_chat",
            """
                        Handle casual conversation only (greetings, acknowledgements, small talk).
                        Use this tool ONLY when no orchestration, routing, or domain action is required.
                        Do NOT perform planning, delegation, data collection, or any domain processing.
                        .""",
            parameter);

    agent.addTool(new Tool(chitChatFunction));
  }

  private static Parameter getParameter(Agent agent) {
    Parameter parameter = new Parameter();

    parameter.addProperty(
        "message",
        new ParameterProperty(
            "string",
            "A short friendly reply to the user for casual conversation. "
                + "Keep it brief, natural, and in the same language as the user."),
        true);

    // Optional interim UX message (only if the agent supports it)
    if (agent.isSupportsInterimMessages()) {
      parameter.addProperty(
          "interim_message",
          new ParameterProperty(
              "string",
              "Optional intermediate message shown before sending the final chit-chat reply."),
          false);
    }
    return parameter;
  }
}
