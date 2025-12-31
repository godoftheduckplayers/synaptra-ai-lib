package com.ducks.synaptra.prompt;

import com.ducks.synaptra.log.LogTracer;
import com.ducks.synaptra.orchestration.event.record.contract.RecordRequestEvent;
import com.ducks.synaptra.orchestration.event.tool.contract.ToolResponseEvent;
import com.ducks.synaptra.prompt.contract.RecordEvent;
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

  @LogTracer(spanName = "record_tool_response_event")
  public void publishEvent(ToolResponseEvent toolResponseEvent) {
    publisher.publishEvent(buildRecordEvent(toolResponseEvent));
  }

  @LogTracer(spanName = "record_publisher_event")
  public void publishEvent(RecordRequestEvent recordRequestEvent) {
    publisher.publishEvent(recordRequestEvent);
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
