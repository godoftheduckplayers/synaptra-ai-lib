package com.ducks.synaptra.event.agent;

import com.ducks.synaptra.event.agent.model.AgentResponseEvent;

public interface AgentExecutionListener {

  void onAgentResponseEvent(AgentResponseEvent agentResponseEvent);
}
