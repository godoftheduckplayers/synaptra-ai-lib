package com.ducks.synaptra.event.tool.model;

import com.ducks.synaptra.client.openai.data.ToolCall;
import com.ducks.synaptra.model.agent.Agent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@ToString
@AllArgsConstructor
@Getter
public class ToolExecutionRequest {
  private String sessionId;
  private Agent agent;
  private ToolCall toolCall;

  public ToolExecutionRequest(ToolRequestEvent toolRequestEvent) {
    this(
        toolRequestEvent.getSessionId(),
        toolRequestEvent.getAgent(),
        toolRequestEvent.getToolCall());
  }
}
