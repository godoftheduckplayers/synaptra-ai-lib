package com.ai.agentics.prompt;

import com.ai.agentics.orchestration.event.record.contract.RecordRequestEvent;
import com.ai.agentics.orchestration.event.tool.contract.ToolResponseEvent;
import com.ai.agentics.prompt.contract.RecordEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class RecordEventPublisher {

  private final ApplicationEventPublisher publisher;
  private final ObjectMapper mapper;

  public RecordEventPublisher(ApplicationEventPublisher publisher) {
    this.publisher = publisher;
    this.mapper = new ObjectMapper();
  }

  public void publishEvent(ToolResponseEvent toolResponseEvent) {
    publisher.publishEvent(buildRecordEvent(toolResponseEvent));
  }

  private RecordRequestEvent buildRecordEvent(ToolResponseEvent toolResponseEvent) {
    try {
      RecordEvent recordEvent =
          mapper.readValue(toolResponseEvent.toolCall().function().arguments(), RecordEvent.class);

      return new RecordRequestEvent(
          toolResponseEvent.sessionId(),
          toolResponseEvent.agent(),
          toolResponseEvent.user(),
          recordEvent);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
