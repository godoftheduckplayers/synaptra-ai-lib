package com.ai.agentics.execution;

import com.ai.agentics.execution.event.tool.ToolExecutionListener;
import com.ai.agentics.execution.event.tool.ToolExecutionResponseEvent;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ToolExecutionService {

  private static final Logger logger = LoggerFactory.getLogger(ToolExecutionService.class);

  private final List<ToolExecutionListener> toolExecutionListenerList;

  public ToolExecutionService(List<ToolExecutionListener> toolExecutionListenerList) {
    this.toolExecutionListenerList = toolExecutionListenerList;
  }

  @Async
  @EventListener
  public void onToolExecutionEvent(ToolExecutionResponseEvent toolExecutionResponseEvent) {
    toolExecutionListenerList.forEach(
        listener -> listener.onToolExecutionResponseEvent(toolExecutionResponseEvent));
  }
}
