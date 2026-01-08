package com.ducks.synaptra.event.answer;

import com.ducks.synaptra.event.answer.model.AnswerRequestEvent;

public interface AnswerExecutionListener {

  void onAnswerExecutionResponseEvent(AnswerRequestEvent answerRequestEvent);
}
