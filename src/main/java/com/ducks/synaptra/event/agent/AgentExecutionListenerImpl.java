package com.ducks.synaptra.event.agent;

import com.ducks.synaptra.client.openai.data.Choice;
import com.ducks.synaptra.client.openai.data.ToolCall;
import com.ducks.synaptra.event.agent.model.AgentResponseEvent;
import com.ducks.synaptra.event.answer.AnswerExecutionListener;
import com.ducks.synaptra.event.answer.model.AnswerRequestEvent;
import com.ducks.synaptra.event.tool.ToolExecutionPublisherImpl;
import com.ducks.synaptra.event.tool.model.ToolExecutionType;
import com.ducks.synaptra.event.tool.model.ToolRequestEvent;
import com.ducks.synaptra.log.LogTracer;
import com.ducks.synaptra.memory.episode.EpisodeMemory;
import com.ducks.synaptra.memory.episode.model.RecordEvent;
import com.ducks.synaptra.memory.episode.model.StatusType;
import com.ducks.synaptra.model.agent.Agent;
import com.ducks.synaptra.state.AgentExecutionState;
import com.ducks.synaptra.tool.route.RouteMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class AgentExecutionListenerImpl implements AgentExecutionListener {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final String FINISH_REASON_TOOL_CALLS = "tool_calls";

  private final ToolExecutionPublisherImpl toolExecutionPublisher;
  private final List<AnswerExecutionListener> answerExecutionListenerList;
  private final EpisodeMemory episodeMemory;

  @LogTracer(spanName = "interpret_agent_execution_response")
  @Override
  public void onAgentResponseEvent(AgentResponseEvent agentResponseEvent) {
    agentResponseEvent
        .getChatCompletionResponse()
        .choices()
        .forEach(choice -> handleChoice(agentResponseEvent, choice));
  }

  private void handleChoice(AgentResponseEvent agentResponseEvent, Choice choice) {
    publishAnswerIfPresent(agentResponseEvent, choice);
    publishToolCallsIfPresent(agentResponseEvent, choice);
  }

  private void publishToolCallsIfPresent(AgentResponseEvent agentResponseEvent, Choice choice) {
    if (FINISH_REASON_TOOL_CALLS.equals(choice.finishReason())
        && choice.message().toolCalls() != null) {
      Optional<ToolCall> toolCallOptional = choice.message().toolCalls().stream().findFirst();
      toolCallOptional.ifPresent(
          toolCall -> {
            choice.message().toolCalls().stream()
                .filter(
                    t -> {
                      ToolExecutionType toolExecutionType =
                          ToolExecutionType.fromValue(t.function().name());
                      return !t.equals(toolCall)
                          && ToolExecutionType.ROUTE_TO_AGENT == toolExecutionType;
                    })
                .forEach(
                    t ->
                        AgentExecutionState.registerToolRequestEvent(
                            new ToolRequestEvent(
                                agentResponseEvent.getSessionId(),
                                agentResponseEvent.getAgent(),
                                agentResponseEvent.getUser(),
                                t)));
            toolExecutionPublisher.publisherToolRequestEvent(
                new ToolRequestEvent(
                    agentResponseEvent.getSessionId(),
                    agentResponseEvent.getAgent(),
                    agentResponseEvent.getUser(),
                    toolCall));
          });
    }
  }

  private void publishAnswerIfPresent(AgentResponseEvent agentResponseEvent, Choice choice) {
    String content = choice.message().content();
    if (content != null && !content.isBlank()) {

      episodeMemory.registerEvent(
          agentResponseEvent.getSessionId(),
          agentResponseEvent.getAgent(),
          new RecordEvent(
              "The agent then asked the user the following question:" + content,
              StatusType.WAIT_USER_INPUT));

      answerExecutionListenerList.forEach(
          answerExecutionListener ->
              answerExecutionListener.onAnswerExecutionResponseEvent(
                  new AnswerRequestEvent(
                      agentResponseEvent.getSessionId(),
                      agentResponseEvent.getAgent(),
                      agentResponseEvent.getUser(),
                      content)));
    }
  }

  private Agent resolveTargetAgent(RouteMapper routeMapper, AgentResponseEvent toolRequestEvent) {

    assert toolRequestEvent.getAgent() != null;
    Optional<Agent> agent =
        toolRequestEvent.getAgent().getAgents().stream()
            .filter(
                a ->
                    a.getName().equals(routeMapper.agent())
                        || a.getIdentifier().equals(routeMapper.agent()))
            .findFirst();

    return agent.orElseThrow(
        () ->
            new RuntimeException(
                "Failed to route execution to agent '"
                    + routeMapper.agent()
                    + "': agent not found."));
  }
}
