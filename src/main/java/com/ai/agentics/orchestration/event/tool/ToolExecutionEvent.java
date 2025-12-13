package com.ai.agentics.orchestration.event.tool;

import java.util.List;

import com.ai.agentics.orchestration.event.tool.contract.ToolResponseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ToolExecutionEvent {

  private static final Logger logger = LoggerFactory.getLogger(ToolExecutionEvent.class);

  private final List<ToolExecutionListener> toolExecutionListenerList;

  public ToolExecutionEvent(List<ToolExecutionListener> toolExecutionListenerList) {
    this.toolExecutionListenerList = toolExecutionListenerList;
  }

  @Async
  @EventListener
  public void onToolExecutionEvent(ToolResponseEvent toolResponseEvent) {
    toolExecutionListenerList.forEach(
        listener -> listener.onToolExecutionResponseEvent(toolResponseEvent));
  }
}
