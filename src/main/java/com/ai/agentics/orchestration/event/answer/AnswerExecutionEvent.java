package com.ai.agentics.orchestration.event.answer;

import com.ai.agentics.orchestration.event.answer.contract.AnswerResponseEvent;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AnswerExecutionEvent {

  private static final Logger logger = LoggerFactory.getLogger(AnswerExecutionEvent.class);

  private final List<AnswerExecutionListener> answerExecutionListenerList;

  public AnswerExecutionEvent(List<AnswerExecutionListener> answerExecutionListenerList) {
    this.answerExecutionListenerList = answerExecutionListenerList;
  }

  @Async
  @EventListener
  public void onAnswerExecutionEvent(AnswerResponseEvent answerResponseEvent) {
    answerExecutionListenerList.forEach(
        listener -> listener.onAnswerExecutionResponseEvent(answerResponseEvent));
  }
}
