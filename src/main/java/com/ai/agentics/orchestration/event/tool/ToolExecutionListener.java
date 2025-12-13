package com.ai.agentics.orchestration.event.tool;

import com.ai.agentics.orchestration.event.tool.contract.ToolResponseEvent;

public interface ToolExecutionListener {

  void onToolExecutionResponseEvent(ToolResponseEvent toolResponseEvent);
}
