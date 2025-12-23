package com.ducks.synaptra.orchestration.event.tool;

import com.ducks.synaptra.orchestration.event.tool.contract.ToolResponseEvent;

public interface ToolExecutionListener {

  void onToolExecutionResponseEvent(ToolResponseEvent toolResponseEvent);
}
