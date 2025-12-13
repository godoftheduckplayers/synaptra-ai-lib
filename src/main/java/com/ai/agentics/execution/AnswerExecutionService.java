package com.ai.agentics.execution;

import com.ai.agentics.execution.event.response.AnswerExecutionListener;
import com.ai.agentics.execution.event.response.AnswerExecutionResponseEvent;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AnswerExecutionService {

  private static final Logger logger = LoggerFactory.getLogger(AnswerExecutionService.class);

  private final List<AnswerExecutionListener> answerExecutionListenerList;

  public AnswerExecutionService(List<AnswerExecutionListener> answerExecutionListenerList) {
    this.answerExecutionListenerList = answerExecutionListenerList;
  }

  @Async
  @EventListener
  public void onAnswerExecutionEvent(AnswerExecutionResponseEvent answerExecutionResponseEvent) {
    answerExecutionListenerList.forEach(
        listener -> listener.onAnswerExecutionResponseEvent(answerExecutionResponseEvent));
  }
}
