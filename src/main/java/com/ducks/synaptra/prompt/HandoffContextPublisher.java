package com.ducks.synaptra.prompt;

import static com.ducks.synaptra.orchestration.event.record.RecordExecutionEvent.WAIT_AGENT_EXECUTION;

import com.ducks.synaptra.agent.Agent;
import com.ducks.synaptra.client.openai.data.Message;
import com.ducks.synaptra.memory.EpisodeMemory;
import com.ducks.synaptra.orchestration.event.agent.contract.AgentRequestEvent;
import com.ducks.synaptra.orchestration.event.tool.contract.ToolResponseEvent;
import com.ducks.synaptra.prompt.contract.RecordEvent;
import com.ducks.synaptra.prompt.contract.RouteMapper;
import com.ducks.synaptra.velocity.VelocityTemplateService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class HandoffContextPublisher {

  private final ApplicationEventPublisher publisher;
  private final VelocityTemplateService velocityTemplateService;
  private final EpisodeMemory episodeMemory;
  private final ObjectMapper mapper;

  private static final String BASE_PROMPT =
      """
    HANDOFF
    Objective $objective

    Constraints:
     - Do not assume missing data.
    """;

  public HandoffContextPublisher(
      ApplicationEventPublisher publisher,
      VelocityTemplateService velocityTemplateService,
      EpisodeMemory episodeMemory) {
    this.publisher = publisher;
    this.velocityTemplateService = velocityTemplateService;
    this.episodeMemory = episodeMemory;
    this.mapper = new ObjectMapper();
  }

  public void publishEvent(ToolResponseEvent toolResponseEvent) {
    publisher.publishEvent(buildAgentRequestEvent(toolResponseEvent));
  }

  private AgentRequestEvent buildAgentRequestEvent(ToolResponseEvent toolResponseEvent) {
    try {
      RouteMapper routeMapper =
          mapper.readValue(toolResponseEvent.toolCall().function().arguments(), RouteMapper.class);

      Agent agent = getAgent(routeMapper, toolResponseEvent);

      episodeMemory.registerEvent(
          toolResponseEvent.sessionId(),
          toolResponseEvent.agent(),
          new RecordEvent("Waiting for agent execution: " + agent.name(), WAIT_AGENT_EXECUTION));

      return new AgentRequestEvent(
          toolResponseEvent.sessionId(),
          agent,
          new Message(
              "system",
              velocityTemplateService.render(BASE_PROMPT, routeMapper.velocityContext()),
              null,
              null,
              null),
          toolResponseEvent.user());
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  private Agent getAgent(RouteMapper routeMapper, ToolResponseEvent toolResponseEvent) {
    assert toolResponseEvent.agent() != null;
    Optional<Agent> agent =
        toolResponseEvent.agent().agents().stream()
            .filter(a -> a.name().equals(routeMapper.agent()))
            .findFirst();
    return agent.orElseThrow(
        () -> new RuntimeException("Failed to execute agent: " + routeMapper.agent()));
  }
}
