package com.ducks.synaptra.event.agent;

import com.ducks.synaptra.event.agent.model.AgentRequestEvent;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface AgentExecutionPublisher {

  void publisherAgentRequestEvent(AgentRequestEvent agentRequestEvent) throws JsonProcessingException;
}
