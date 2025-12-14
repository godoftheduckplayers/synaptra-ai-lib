package com.ai.agentics.prompt;

import com.ai.agentics.agent.Agent;
import com.ai.agentics.client.openai.data.Message;
import com.ai.agentics.orchestration.event.agent.contract.AgentRequestEvent;
import com.ai.agentics.orchestration.event.tool.contract.ToolResponseEvent;
import com.ai.agentics.prompt.contract.RouteMapper;
import com.ai.agentics.velocity.VelocityTemplateService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class HandoffContextMessage {

  private final VelocityTemplateService velocityTemplateService;
  private final ObjectMapper mapper;

  private static final String BASE_PROMPT =
      """
    HANDOFF
    Objective $objective

    Constraints:
     - Do not assume missing data.
    """;

  public HandoffContextMessage(VelocityTemplateService velocityTemplateService) {
    this.velocityTemplateService = velocityTemplateService;
    this.mapper = new ObjectMapper();
  }

  public AgentRequestEvent handoffContext(ToolResponseEvent toolResponseEvent) {
    try {
      RouteMapper routeMapper =
          mapper.readValue(toolResponseEvent.toolCall().function().arguments(), RouteMapper.class);

      return new AgentRequestEvent(
          toolResponseEvent.sessionId(),
          getAgent(routeMapper, toolResponseEvent),
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
