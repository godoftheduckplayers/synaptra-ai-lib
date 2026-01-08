package com.ducks.synaptra.tool.factory;

import com.ducks.synaptra.client.openai.data.FunctionDef;
import com.ducks.synaptra.client.openai.data.Parameter;
import com.ducks.synaptra.client.openai.data.ParameterProperty;
import com.ducks.synaptra.client.openai.data.Tool;
import com.ducks.synaptra.model.agent.Agent;

public class RouteParentToolFactory {

  public static void routeToParentTool(Agent agent) {
    Parameter parameter = new Parameter();

    parameter.addProperty(
        "summary",
        new ParameterProperty(
            "string",
            "A concise summary of what has already been executed by this agent. "
                + "Include completed steps and relevant outcomes, without technical details."),
        true);

    if (agent.isSupportsInterimMessages()) {
      parameter.addProperty(
          "response",
          new ParameterProperty(
              "string",
              "An optional intermediate message informing the user that execution "
                  + "is being handed back to the parent agent for continuation."),
          true);
    }

    FunctionDef routeToParentFunction =
        new FunctionDef(
            "route_to_parent",
            "Returns control to the parent agent by summarizing what has been done "
                + "and what remains to be completed.",
            parameter);

    agent.addTool(new Tool(routeToParentFunction));
  }
}
