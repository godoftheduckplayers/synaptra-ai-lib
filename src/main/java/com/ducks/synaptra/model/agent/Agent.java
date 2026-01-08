package com.ducks.synaptra.model.agent;

import com.ducks.synaptra.client.openai.data.Tool;
import com.ducks.synaptra.client.openai.data.ToolChoice;
import com.ducks.synaptra.tool.factory.RouteAgentToolFactory;
import com.ducks.synaptra.tool.factory.RouteParentToolFactory;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.util.CollectionUtils;

@ToString(exclude = {"parent", "agents", "tools"})
@Getter
@Setter
public class Agent implements Serializable {

  @Serial private static final long serialVersionUID = -5379164516921644174L;

  private String identifier;
  private String name;
  private String goal;
  private String prompt;
  private ProviderConfig providerConfig;
  private boolean isSupportsInterimMessages;
  private List<Tool> tools;
  private ToolChoice toolChoice;
  private Agent parent;
  private List<Agent> agents;

  public Agent(
      String identifier,
      String name,
      String goal,
      String prompt,
      ProviderConfig providerConfig,
      boolean isSupportsInterimMessages,
      List<Tool> tools,
      ToolChoice toolChoice,
      Agent parent,
      List<Agent> agents) {
    this.identifier = identifier;
    this.name = name;
    this.goal = goal;
    this.prompt = prompt;
    this.providerConfig = providerConfig;
    this.isSupportsInterimMessages = isSupportsInterimMessages;
    this.tools = CollectionUtils.isEmpty(tools) ? new ArrayList<>() : tools;
    this.toolChoice = toolChoice;
    this.parent = parent;
    this.agents = agents;

    if (!CollectionUtils.isEmpty(this.agents)) {
      RouteAgentToolFactory.routeTool(this);
    }

    if (parent != null) {
      RouteParentToolFactory.routeToParentTool(this);
    }
  }

  public void addTool(Tool tool) {
    tools.add(tool);
  }

  public void setParent(Agent parent) {
    if (parent != null) {
      this.parent = parent;
      RouteParentToolFactory.routeToParentTool(this);
    }
  }
}
