package com.ai.agentics.orchestration.event.tool;

import com.ai.agentics.client.openai.data.Message;
import com.ai.agentics.model.Agent;
import com.ai.agentics.orchestration.event.agent.contract.AgentRequestEvent;
import com.ai.agentics.orchestration.event.tool.contract.RouteMapper;
import com.ai.agentics.orchestration.event.tool.contract.ToolResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ToolExecutionEvent {

  private static final Logger logger = LoggerFactory.getLogger(ToolExecutionEvent.class);

  private final ApplicationEventPublisher publisher;
  private final List<ToolExecutionListener> toolExecutionListenerList;
  private final ObjectMapper mapper;

  public ToolExecutionEvent(
      ApplicationEventPublisher publisher, List<ToolExecutionListener> toolExecutionListenerList) {
    this.publisher = publisher;
    this.toolExecutionListenerList = toolExecutionListenerList;
    this.mapper = new ObjectMapper();
  }

  @Async
  @EventListener
  public void onToolExecutionEvent(ToolResponseEvent toolResponseEvent)
      throws JsonProcessingException {
    assert toolResponseEvent.agent() != null;
    logger.debug(
        "[TOOL_EXECUTION_EVENT] - sessionId: {}, agent: {}, tooCall: {}",
        toolResponseEvent.sessionId(),
        toolResponseEvent.agent().identifier(),
        mapper.writeValueAsString(toolResponseEvent.toolCall()));
    publishRouteEvent(toolResponseEvent);
    toolExecutionListenerList.forEach(
        listener -> listener.onToolExecutionResponseEvent(toolResponseEvent));
  }

  private void publishRouteEvent(ToolResponseEvent toolResponseEvent)
      throws JsonProcessingException {
    if ("route_to_agent".equals(toolResponseEvent.toolCall().function().name())) {
      RouteMapper routeMapper =
          mapper.readValue(toolResponseEvent.toolCall().function().arguments(), RouteMapper.class);
      publisher.publishEvent(
          new AgentRequestEvent(
              toolResponseEvent.sessionId(),
              getAgent(routeMapper, toolResponseEvent),
              new Message(
                  "system",
                  "The user input is: "
                      + routeMapper.input()
                      + ", your objective is: "
                      + routeMapper.objective(),
                  null,
                  null,
                  null),
              toolResponseEvent.user()));
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
