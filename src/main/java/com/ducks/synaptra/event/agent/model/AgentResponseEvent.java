package com.ducks.synaptra.event.agent.model;

import com.ducks.synaptra.client.openai.data.ChatCompletionResponse;
import com.ducks.synaptra.client.openai.data.Message;
import com.ducks.synaptra.model.agent.Agent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.springframework.lang.Nullable;

@ToString
@AllArgsConstructor
@Getter
public class AgentResponseEvent {
  String sessionId;
  @Nullable Agent agent;
  @Nullable Message user;
  ChatCompletionResponse chatCompletionResponse;
}
