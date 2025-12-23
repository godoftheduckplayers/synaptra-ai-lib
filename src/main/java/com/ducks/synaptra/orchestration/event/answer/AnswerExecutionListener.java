package com.ducks.synaptra.orchestration.event.answer;


import com.ducks.synaptra.orchestration.event.answer.contract.AnswerResponseEvent;

public interface AnswerExecutionListener {

  void onAnswerExecutionResponseEvent(AnswerResponseEvent answerResponseEvent);
}
