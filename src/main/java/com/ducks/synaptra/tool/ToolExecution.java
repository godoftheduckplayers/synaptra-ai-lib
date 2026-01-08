package com.ducks.synaptra.tool;

import com.ducks.synaptra.event.tool.model.ToolExecutionType;
import com.ducks.synaptra.event.tool.model.ToolRequestEvent;

public interface ToolExecution {

  ToolExecutionType toolExecutionType();

  void resolve(ToolRequestEvent toolRequestEvent);
}
