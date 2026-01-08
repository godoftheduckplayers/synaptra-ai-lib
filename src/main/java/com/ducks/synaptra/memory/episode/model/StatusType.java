package com.ducks.synaptra.memory.episode.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum StatusType {
  WAIT_USER_INPUT("WAIT_USER_INPUT"),
  USER_INPUT_REQUEST("USER_INPUT_REQUEST"),
  AGENT_EXECUTION("AGENT_EXECUTION"),
  WAIT_AGENT_EXECUTION("WAIT_AGENT_EXECUTION"),
  FINISHED_AGENT_EXECUTION("FINISHED_AGENT_EXECUTION"),
  WAIT_TOOL_EXECUTION("WAIT_TOOL_EXECUTION"),
  FINISHED_TOOL_EXECUTION("FINISHED_TOOL_EXECUTION");

  private final String value;
}
