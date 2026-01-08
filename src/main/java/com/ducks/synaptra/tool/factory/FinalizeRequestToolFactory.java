package com.ducks.synaptra.tool.factory;

import com.ducks.synaptra.client.openai.data.FunctionDef;
import com.ducks.synaptra.client.openai.data.Parameter;
import com.ducks.synaptra.client.openai.data.ParameterProperty;
import com.ducks.synaptra.client.openai.data.Tool;
import com.ducks.synaptra.model.agent.Agent;

public final class FinalizeRequestToolFactory {

  private FinalizeRequestToolFactory() {}

  public static void finalizeRequestTool(Agent agent) {
    Parameter parameter = new Parameter();

    parameter.addProperty(
        "summary",
        new ParameterProperty(
            "string",
            "A final user-facing message that summarizes what was completed and confirms closure. "
                + "Generate it in the same language the user is using."),
        true);

    FunctionDef fn =
        new FunctionDef(
            "finalize_request",
            "Use this function to close the current request when no further actions, inputs, or delegations are required.",
            parameter);

    agent.addTool(new Tool(fn));
  }
}
