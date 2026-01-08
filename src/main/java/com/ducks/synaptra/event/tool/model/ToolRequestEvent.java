package com.ducks.synaptra.event.tool.model;

import com.ducks.synaptra.client.openai.data.Message;
import com.ducks.synaptra.client.openai.data.ToolCall;
import com.ducks.synaptra.model.agent.Agent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@ToString
@AllArgsConstructor
@Getter
public class ToolRequestEvent {
  private String sessionId;
  private Agent agent;
  private Message user;
  private ToolCall toolCall;
}
