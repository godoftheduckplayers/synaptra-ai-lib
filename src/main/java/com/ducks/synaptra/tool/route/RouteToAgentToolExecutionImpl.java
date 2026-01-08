package com.ducks.synaptra.tool.route;

import static com.ducks.synaptra.memory.episode.model.StatusType.AGENT_EXECUTION;
import static com.ducks.synaptra.memory.episode.model.StatusType.WAIT_AGENT_EXECUTION;

import com.ducks.synaptra.client.openai.data.Message;
import com.ducks.synaptra.event.agent.model.AgentRequestEvent;
import com.ducks.synaptra.event.answer.AnswerExecutionListener;
import com.ducks.synaptra.event.answer.model.AnswerRequestEvent;
import com.ducks.synaptra.event.tool.model.ToolExecutionType;
import com.ducks.synaptra.event.tool.model.ToolRequestEvent;
import com.ducks.synaptra.memory.episode.EpisodeMemory;
import com.ducks.synaptra.memory.episode.model.RecordEvent;
import com.ducks.synaptra.model.agent.Agent;
import com.ducks.synaptra.tool.ToolExecution;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class RouteToAgentToolExecutionImpl implements ToolExecution {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final String BASE_PROMPT =
      """
      HANDOFF: AGENT-REQUEST

      Objective: $objective

      Constraints:
       - Do not assume missing data.
      """;

  private final EpisodeMemory episodeMemory;
  private final ApplicationEventPublisher publisher;
  private final List<AnswerExecutionListener> answerExecutionListenerList;

  @Override
  public ToolExecutionType toolExecutionType() {
    return ToolExecutionType.ROUTE_TO_AGENT;
  }

  @Override
  public void resolve(ToolRequestEvent toolRequestEvent) {

    try {
      RouteMapper routeMapper =
          MAPPER.readValue(
              toolRequestEvent.getToolCall().function().arguments(), RouteMapper.class);

      if (toolRequestEvent.getAgent().isSupportsInterimMessages()) {
        answerExecutionListenerList.forEach(
            answerExecutionListener ->
                answerExecutionListener.onAnswerExecutionResponseEvent(
                    new AnswerRequestEvent(
                        toolRequestEvent.getSessionId(),
                        toolRequestEvent.getAgent(),
                        toolRequestEvent.getUser(),
                        routeMapper.response())));
      }

      Agent targetAgent = resolveTargetAgent(routeMapper, toolRequestEvent);

      // Record the orchestration step so episodic memory reflects the pending agent execution.
      episodeMemory.registerEvent(
          toolRequestEvent.getSessionId(),
          toolRequestEvent.getAgent(),
          new RecordEvent(
              "Waiting for agent '"
                  + targetAgent.getName()
                  + "' execution with the aim of: "
                  + routeMapper.objective(),
              WAIT_AGENT_EXECUTION));

      Message handoffContext =
          new Message(
              "system",
              BASE_PROMPT.replace("$objective", routeMapper.objective()),
              null,
              null,
              null);

      episodeMemory.registerEvent(
          toolRequestEvent.getSessionId(),
          targetAgent,
          new RecordEvent(
              "You are being invoked to fulfill the following objective: "
                  + routeMapper.objective(),
              AGENT_EXECUTION));

      publisher.publishEvent(
          new AgentRequestEvent(
              toolRequestEvent.getSessionId(),
              targetAgent,
              handoffContext,
              toolRequestEvent.getUser()));

    } catch (JsonProcessingException e) {
      throw new RuntimeException(
          "Failed to parse route_to_agent arguments: "
              + toolRequestEvent.getToolCall().function().arguments(),
          e);
    }
  }

  private Agent resolveTargetAgent(RouteMapper routeMapper, ToolRequestEvent toolRequestEvent) {

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
