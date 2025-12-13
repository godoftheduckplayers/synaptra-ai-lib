package com.ai.agentics.orchestration.event.agent;

import com.ai.agentics.orchestration.event.agent.contract.AgentResponseEvent;

public interface AgentExecutionListener {

  void onAgentResponseEvent(AgentResponseEvent agentResponseEvent);
}
