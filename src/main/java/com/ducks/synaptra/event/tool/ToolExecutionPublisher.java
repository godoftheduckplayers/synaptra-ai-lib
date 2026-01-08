package com.ducks.synaptra.event.tool;

import com.ducks.synaptra.event.tool.model.ToolRequestEvent;

public interface ToolExecutionPublisher {

  void publisherToolRequestEvent(ToolRequestEvent toolRequestEvent);
}
