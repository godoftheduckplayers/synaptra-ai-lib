package com.ducks.synaptra.orchestration.event.record;

import com.ducks.synaptra.client.openai.data.Message;
import com.ducks.synaptra.log.LogTracer;
import com.ducks.synaptra.memory.EpisodeMemory;
import com.ducks.synaptra.orchestration.event.agent.contract.AgentRequestEvent;
import com.ducks.synaptra.orchestration.event.answer.contract.AnswerResponseEvent;
import com.ducks.synaptra.orchestration.event.record.contract.RecordRequestEvent;
import com.ducks.synaptra.prompt.contract.RecordEvent;
import com.ducks.synaptra.velocity.VelocityTemplateService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class RecordExecutionEvent {

  private static final Logger logger = LoggerFactory.getLogger(RecordExecutionEvent.class);
  public static final String WAIT_USER_INPUT = "WAIT_USER_INPUT";
  public static final String FINISHED = "FINISHED";
  public static final String WAIT_AGENT_EXECUTION = "WAIT_AGENT_EXECUTION";
  public static final String WAIT_TOOL_EXECUTION = "WAIT_TOOL_EXECUTION";
  public static final String FINISHED_TOOL_EXECUTION = "FINISHED_TOOL_EXECUTION";

  private final ApplicationEventPublisher publisher;
  private final EpisodeMemory episodeMemory;
  private final ObjectMapper mapper;
  private final VelocityTemplateService velocityTemplateService;

  public RecordExecutionEvent(
      ApplicationEventPublisher publisher,
      EpisodeMemory episodeMemory,
      VelocityTemplateService velocityTemplateService) {
    this.publisher = publisher;
    this.episodeMemory = episodeMemory;
    this.velocityTemplateService = velocityTemplateService;
    this.mapper = new ObjectMapper();
  }

  @LogTracer(spanName = "record_response_event")
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
      publisher.publishEvent(getAnswerResponseEvent(recordRequestEvent));
    }
    if (FINISHED.equals(recordRequestEvent.recordEvent().status())) {
      Message episodicContext = null;
      if (recordRequestEvent.agent().parent() != null) {
        List<RecordEvent> recordEventList =
            episodeMemory.getEpisodeMemory(
                recordRequestEvent.sessionId(), recordRequestEvent.agent().parent());

        if (!CollectionUtils.isEmpty(recordEventList)) {
          Map<String, Object> velocityContext = new HashMap<>();
          velocityContext.put("records", recordEventList);
          velocityContext.put("agent", recordRequestEvent.agent());
          velocityContext.put("content", recordRequestEvent.recordEvent().content());

          String episodicContextPrompt =
              """
                        EPISODIC_MEMORY_CONTEXT
                        Purpose:
                        This section summarizes what has already happened in the current session.
                        It exists to ensure continuity and avoid repetition.

                        The last events:
                        #foreach($record in $records)
                         # Event - status: $record.status(), content: $record.content()
                         The agent '$agent.name()' execution is finished, resume: $content
                        #end

                        Constraints:
                        - Do not repeat previously asked questions.
                        - Do not redo completed steps.
                        - Resume execution from the last known state.
                      """;
          episodicContextPrompt =
              velocityTemplateService.render(episodicContextPrompt, velocityContext);
          episodicContext = new Message("system", episodicContextPrompt, null, null, null);
        }
        String handoffContext =
            """
                  Check whether all required tasks have been completed.
                  If so, produce the final user-facing response.
                  If not, continue processing until completion criteria are me
                """;
        publisher.publishEvent(
            new AgentRequestEvent(
                recordRequestEvent.sessionId(),
                recordRequestEvent.agent().parent(),
                new Message("system", handoffContext, null, null, null),
                episodicContext,
                recordRequestEvent.user()));
      } else {
        publisher.publishEvent(getAnswerResponseEvent(recordRequestEvent));
      }
    }
    if (FINISHED_TOOL_EXECUTION.equals(recordRequestEvent.recordEvent().status())) {
      publisher.publishEvent(
          new AgentRequestEvent(
              recordRequestEvent.sessionId(),
              recordRequestEvent.agent().parent(),
              new Message("system", recordRequestEvent.recordEvent().content(), null, null, null),
              null,
              recordRequestEvent.user()));
    }
  }

  private AnswerResponseEvent getAnswerResponseEvent(RecordRequestEvent recordRequestEvent) {
    return new AnswerResponseEvent(
        recordRequestEvent.sessionId(),
        recordRequestEvent.agent(),
        recordRequestEvent.user(),
        recordRequestEvent.recordEvent().content());
  }
}
