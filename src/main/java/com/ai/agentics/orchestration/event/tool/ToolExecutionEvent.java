package com.ai.agentics.orchestration.event.tool;

import com.ai.agentics.orchestration.event.tool.contract.ToolResponseEvent;
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

  private final List<ToolExecutionListener> toolExecutionListenerList;
  private final ObjectMapper mapper;

  public ToolExecutionEvent(List<ToolExecutionListener> toolExecutionListenerList) {
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
    toolExecutionListenerList.forEach(
        listener -> listener.onToolExecutionResponseEvent(toolResponseEvent));
  }
}
