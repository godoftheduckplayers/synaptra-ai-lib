package com.ducks.synaptra.agent;

import com.ducks.synaptra.client.openai.data.FunctionDef;
import com.ducks.synaptra.client.openai.data.Parameter;
import com.ducks.synaptra.client.openai.data.ParameterProperty;
import com.ducks.synaptra.client.openai.data.Tool;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseAgent implements Agent {

  private Agent parent;

  @Override
  public List<Tool> getTools() {
    List<Tool> tools = new ArrayList<>();
    if (getAgents() != null && !getAgents().isEmpty()) {
      tools.add(routeToAgentFunction());
    }
    tools.add(stageTool());
    return tools;
  }

  @Override
  public Map<String, Object> getVelocityContext() {
    Map<String, Object> context = new HashMap<>();
    context.put("name", getName());
    context.put("goal", getGoal());
    context.put("agents", getAgents());
    return context;
  }

  private Tool routeToAgentFunction() {
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
    if (isSupportsInterimMessages()) {
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

    return new Tool(routeToAgentFunction);
  }

  private Tool stageTool() {
    Parameter parameter = new Parameter();
    parameter.addProperty(
        "status",
        new ParameterProperty(
            "string",
            """
                      This field must contain one of the following values: WAIT_USER_INPUT or FINISHED.
                      Each value should be used as follows:

                      - WAIT_USER_INPUT: when the agent asks the user for additional information in order to continue
                        the process.
                      - FINISHED: when the agent determines that it has completed the requested task.
                      """),
        true);

    parameter.addProperty(
        "content",
        new ParameterProperty(
            "string",
            """
                      This field must have its value determined by the current status. When the status is:

                      - WAIT_USER_INPUT: the value must contain the question(s) that will be presented to the user.
                      - FINISHED: the value must contain a summary of what was performed.
                      """),
        true);

    FunctionDef stageFunction =
        new FunctionDef(
            "record_event",
            """
                      Indicates the current execution state of the session. Possible values are:
                      WAIT_USER_INPUT and FINISHED. This state is used by the orchestrator to determine
                      the next execution step.
                      """,
            parameter);

    return new Tool(stageFunction);
  }

  @Override
  public Agent getParent() {
    return parent;
  }

  @Override
  public void setParent(Agent parent) {
    this.parent = parent;
  }
}
