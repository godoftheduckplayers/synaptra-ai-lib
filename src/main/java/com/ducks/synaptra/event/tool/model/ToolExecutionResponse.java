package com.ducks.synaptra.event.tool.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@ToString
@AllArgsConstructor
@Getter
public class ToolExecutionResponse {
  private Object details;
  private ToolExecutionStatus toolExecutionStatus;
}
