package com.ai.agentics.prompt;

import com.ai.agentics.agent.Agent;
import com.ai.agentics.client.openai.data.Message;
import com.ai.agentics.orchestration.event.agent.contract.AgentRequestEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserInputPublisher {

  private final ApplicationEventPublisher publisher;

  public void publishEvent(String sessionId, Agent agent, String userInput) {
    publisher.publishEvent(buildAgentRequestEvent(sessionId, agent, userInput));
  }

  private AgentRequestEvent buildAgentRequestEvent(
      String sessionId, Agent agent, String userInput) {
    return new AgentRequestEvent(
        sessionId, agent, null, new Message("user", userInput, null, null, null));
  }
}
