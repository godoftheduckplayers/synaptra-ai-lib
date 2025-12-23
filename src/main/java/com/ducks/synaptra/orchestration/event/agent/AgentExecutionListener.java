package com.ducks.synaptra.orchestration.event.agent;

import com.ducks.synaptra.orchestration.event.agent.contract.AgentResponseEvent;

public interface AgentExecutionListener {

  void onAgentResponseEvent(AgentResponseEvent agentResponseEvent);
}
