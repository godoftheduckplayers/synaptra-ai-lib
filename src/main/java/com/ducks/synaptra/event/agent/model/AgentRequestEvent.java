package com.ducks.synaptra.event.agent.model;

import com.ducks.synaptra.client.openai.data.ChatCompletionRequest;
import com.ducks.synaptra.client.openai.data.Message;
import com.ducks.synaptra.model.agent.Agent;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@ToString
@AllArgsConstructor
@Getter
public class AgentRequestEvent {

  private String sessionId;
  private Agent agent;
  private Message handoffContext;
  private Message episodicContext;
  private Message user;

  public AgentRequestEvent(String sessionId, Agent agent, Message handoffContext, Message user) {
    this(sessionId, agent, handoffContext, null, user);
  }

  public ChatCompletionRequest toChatCompletionRequest() {

    assert agent != null;
    assert user != null;

    List<Message> messageList = getMessages();

    return new ChatCompletionRequest(
        agent.getProviderConfig().model(),
        messageList,
        agent.getTools(),
        agent.getToolChoice().getValue(),
        agent.getProviderConfig().temperature(),
        agent.getProviderConfig().maxTokens(),
        agent.getProviderConfig().topP());
  }

  private List<Message> getMessages() {
    List<Message> messageList = new ArrayList<>();

    // Base system prompt (agent prompt rendered with its Velocity context)
    messageList.add(new Message("system", agent.getPrompt(), null, null, null));

    // Optional orchestration/handoff context
    if (handoffContext != null) {
      messageList.add(handoffContext);
    }

    // Optional episodic memory context (rehydration for running sessions)
    if (episodicContext != null) {
      messageList.add(episodicContext);
    }

    // Current user input
    messageList.add(user);
    return messageList;
  }
}
