package com.ai.agentics.orchestration.event.answer;


import com.ai.agentics.orchestration.event.answer.contract.AnswerResponseEvent;

public interface AnswerExecutionListener {

  void onAnswerExecutionResponseEvent(AnswerResponseEvent answerResponseEvent);
}
