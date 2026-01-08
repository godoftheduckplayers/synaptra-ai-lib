package com.ducks.synaptra.event.tool;

import com.ducks.synaptra.event.tool.model.ToolExecutionRequest;
import com.ducks.synaptra.event.tool.model.ToolExecutionResponse;

public interface ToolExecutionListener {

  ToolExecutionResponse onToolResponseEvent(ToolExecutionRequest toolExecutionRequest);
}
