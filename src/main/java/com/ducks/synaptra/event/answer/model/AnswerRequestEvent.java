package com.ducks.synaptra.event.answer.model;

import com.ducks.synaptra.client.openai.data.Message;
import com.ducks.synaptra.model.agent.Agent;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class AnswerRequestEvent {
  private String sessionId;
  private Agent agent;
  private Message user;
  private String response;
}
