package com.ducks.synaptra.tool.factory;

import com.ducks.synaptra.client.openai.data.FunctionDef;
import com.ducks.synaptra.client.openai.data.Parameter;
import com.ducks.synaptra.client.openai.data.ParameterProperty;
import com.ducks.synaptra.client.openai.data.Tool;
import com.ducks.synaptra.model.agent.Agent;

public class RouteAgentToolFactory {

  public static void routeTool(Agent agent) {
    Parameter parameter = new Parameter();
    parameter.addProperty(
        "agent",
        new ParameterProperty(
            "string",
            "This field specifies exactly which agent is responsible for handling the requested operation.\n"
                + "The value must match one of the available agents provided to the routing agent."),
        true);
    parameter.addProperty(
        "objective",
        new ParameterProperty(
            "string",
            "This field defines what the agent is expected to accomplish, without prescribing how the task should be performed. The objective must be clear, concise, and scoped to a single responsibility."),
        true);
    parameter.addProperty(
        "input",
        new ParameterProperty(
            "string",
            "This field contains the user-provided data or contextual information that the target agent will process in order to fulfill the objective."),
        true);
    if (agent.isSupportsInterimMessages()) {
      parameter.addProperty(
          "response",
          new ParameterProperty(
              "string",
              "Generates an intermediate response during agent routing, informing the user that the system is processing the request and preparing an action based on the user’s input."),
          true);
    }

    FunctionDef routeToAgentFunction =
        new FunctionDef(
            "route_to_agent",
            "Select, from the available options, the agent that best fulfills the user’s request. One of the provided agents (parameters) must be chosen to execute the task.",
            parameter);

    agent.addTool(new Tool(routeToAgentFunction));
  }
}
