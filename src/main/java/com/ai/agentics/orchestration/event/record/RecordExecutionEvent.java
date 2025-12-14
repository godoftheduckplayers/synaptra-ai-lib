package com.ai.agentics.orchestration.event.record;

import com.ai.agentics.memory.EpisodeMemory;
import com.ai.agentics.orchestration.event.answer.contract.AnswerResponseEvent;
import com.ai.agentics.orchestration.event.record.contract.RecordRequestEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class RecordExecutionEvent {

  private static final Logger logger = LoggerFactory.getLogger(RecordExecutionEvent.class);
  public static final String WAIT_USER_INPUT = "WAIT_USER_INPUT";
  public static final String FINISHED = "FINISHED";

  private final ApplicationEventPublisher publisher;
  private final EpisodeMemory episodeMemory;
  private final ObjectMapper mapper;

  public RecordExecutionEvent(ApplicationEventPublisher publisher, EpisodeMemory episodeMemory) {
    this.publisher = publisher;
    this.episodeMemory = episodeMemory;
    this.mapper = new ObjectMapper();
  }

  @Async("agentExecutionExecutor")
  @EventListener
  public void onRecordExecutionEvent(RecordRequestEvent recordRequestEvent)
      throws JsonProcessingException {
    assert recordRequestEvent.agent() != null;
    logger.debug(
        "[RECORD_EVENT] - sessionId: {}, agent: {}, recordEvent: {}",
        recordRequestEvent.sessionId(),
        recordRequestEvent.agent().identifier(),
        mapper.writeValueAsString(recordRequestEvent.recordEvent()));
    episodeMemory.registerEvent(
        recordRequestEvent.sessionId(),
        recordRequestEvent.agent(),
        recordRequestEvent.recordEvent());
    if (WAIT_USER_INPUT.equals(recordRequestEvent.recordEvent().status())) {
      publisher.publishEvent(
          new AnswerResponseEvent(
              recordRequestEvent.sessionId(),
              recordRequestEvent.agent(),
              recordRequestEvent.user(),
              recordRequestEvent.recordEvent().content()));
    }
    if (FINISHED.equals(recordRequestEvent.recordEvent().status())) {
      System.out.println(recordRequestEvent);
    }
  }
}
