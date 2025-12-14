package com.ai.agentics.orchestration.event.tool;

import com.ai.agentics.orchestration.event.tool.contract.ToolResponseEvent;
import com.ai.agentics.prompt.HandoffContextPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ToolExecutionEvent {

  private static final Logger logger = LoggerFactory.getLogger(ToolExecutionEvent.class);
  private static final String ROUTE_TO_AGENT = "route_to_agent";

  private final HandoffContextPublisher handoffContextPublisher;
  private final List<ToolExecutionListener> toolExecutionListenerList;
  private final ObjectMapper mapper;

  public ToolExecutionEvent(
      HandoffContextPublisher handoffContextPublisher,
      List<ToolExecutionListener> toolExecutionListenerList) {
    this.handoffContextPublisher = handoffContextPublisher;
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

  private void publishRouteEvent(ToolResponseEvent toolResponseEvent) {
    if (ROUTE_TO_AGENT.equals(toolResponseEvent.toolCall().function().name())) {
      handoffContextPublisher.publishEvent(toolResponseEvent);
    }
  }
}
