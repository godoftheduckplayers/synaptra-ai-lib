package com.ducks.synaptra.publisher;

import com.ducks.synaptra.log.LogTracer;
import com.ducks.synaptra.orchestration.event.record.contract.RecordRequestEvent;
import com.ducks.synaptra.orchestration.event.tool.contract.ToolResponseEvent;
import com.ducks.synaptra.publisher.contract.RecordEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * Adapter responsible for publishing {@link RecordRequestEvent} events to the orchestration layer.
 *
 * <p>This publisher supports two main use cases:
 *
 * <ol>
 *   <li>Forward an already built {@link RecordRequestEvent} to the application event bus.
 *   <li>Build a {@link RecordRequestEvent} from a {@link ToolResponseEvent} when an agent invokes
 *       the internal tool function <strong>{@code record_event}</strong>. In this case, the tool
 *       arguments are expected to match the {@link RecordEvent} contract (e.g. {@code status} and
 *       {@code content}), which will be parsed and then persisted/processed by the record execution
 *       flow.
 * </ol>
 *
 * <p>This class is commonly used by the orchestrator pipeline to convert tool outputs into domain
 * events that can be persisted in episodic memory and drive the next step of the session execution.
 *
 * @author Leandro Marques
 * @since 1.0.0
 */
@Service
public class RecordEventPublisher {

  /** Spring application event publisher used to dispatch record events. */
  private final ApplicationEventPublisher publisher;

  /** JSON mapper used to parse internal tool arguments into {@link RecordEvent}. */
  private final ObjectMapper mapper;

  /**
   * Creates a new record event publisher.
   *
   * @param publisher the Spring event publisher used to dispatch {@link RecordRequestEvent} events
   */
  public RecordEventPublisher(ApplicationEventPublisher publisher) {
    this.publisher = publisher;
    this.mapper = new ObjectMapper();
  }

  /**
   * Publishes a {@link RecordRequestEvent} derived from a {@link ToolResponseEvent}.
   *
   * <p>This method is intended to handle the internal tool function <strong>{@code record_event}
   * </strong>, where the tool's JSON arguments represent the {@link RecordEvent} payload.
   *
   * <p>Expected tool arguments JSON shape:
   *
   * <pre>
   * {
   *   "status": "WAIT_USER_INPUT" | "FINISHED",
   *   "content": "..."
   * }
   * </pre>
   *
   * @param toolResponseEvent the tool response event containing the tool call and its arguments
   * @throws RuntimeException if the tool arguments cannot be parsed into a {@link RecordEvent}
   */
  @LogTracer(spanName = "publish_record_event_from_tool_call")
  public void publishEvent(ToolResponseEvent toolResponseEvent) {
    publisher.publishEvent(buildRecordRequestEvent(toolResponseEvent));
  }

  /**
   * Publishes the given {@link RecordRequestEvent} to the application event bus.
   *
   * <p>This overload is used when the {@link RecordRequestEvent} has already been assembled by an
   * upstream component and only needs to be dispatched.
   *
   * @param recordRequestEvent the event to publish
   */
  @LogTracer(spanName = "publish_record_request_event")
  public void publishEvent(RecordRequestEvent recordRequestEvent) {
    publisher.publishEvent(recordRequestEvent);
  }

  /**
   * Builds a {@link RecordRequestEvent} from a {@link ToolResponseEvent} by parsing the tool call
   * arguments into a {@link RecordEvent}.
   *
   * <p>This conversion is required because internal tool calls communicate their output using JSON
   * arguments, while the orchestration layer expects a typed {@link RecordRequestEvent}.
   *
   * @param toolResponseEvent the tool response event containing the tool call arguments
   * @return a {@link RecordRequestEvent} ready to be published
   * @throws RuntimeException if parsing fails
   */
  private RecordRequestEvent buildRecordRequestEvent(ToolResponseEvent toolResponseEvent) {
    try {
      RecordEvent recordEvent =
          mapper.readValue(toolResponseEvent.toolCall().function().arguments(), RecordEvent.class);

      return new RecordRequestEvent(
          toolResponseEvent.sessionId(),
          toolResponseEvent.agent(),
          toolResponseEvent.user(),
          recordEvent);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(
          "Failed to parse tool arguments into RecordEvent for tool 'record_event'.", e);
    }
  }
}
