package com.ducks.synaptra.state;

import com.ducks.synaptra.event.tool.model.ToolRequestEvent;
import java.util.ArrayList;
import java.util.List;

public class AgentExecutionState {

  private static final List<ToolRequestEvent> EXECUTION_LIST = new ArrayList<>();

  public static void registerToolRequestEvent(ToolRequestEvent toolRequestEvent) {
    EXECUTION_LIST.add(toolRequestEvent);
  }

  public static ToolRequestEvent getNextToolRequestEvent() {
    ToolRequestEvent nextToolRequestEvent = EXECUTION_LIST.getFirst();
    EXECUTION_LIST.remove(nextToolRequestEvent);
    return nextToolRequestEvent;
  }
}
