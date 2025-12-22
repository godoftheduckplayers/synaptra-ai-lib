package com.ai.agentics.prompt;

import com.ai.agentics.agent.Agent;
import com.ai.agentics.client.openai.data.Message;
import com.ai.agentics.memory.EpisodeMemory;
import com.ai.agentics.orchestration.event.agent.contract.AgentRequestEvent;
import com.ai.agentics.prompt.contract.RecordEvent;
import com.ai.agentics.velocity.VelocityTemplateService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@RequiredArgsConstructor
@Service
public class UserInputPublisher {

  private final EpisodeMemory episodeMemory;
  private final VelocityTemplateService velocityTemplateService;
  private final ApplicationEventPublisher publisher;

  public void publishEvent(String sessionId, Agent agent, String userInput) {
    publisher.publishEvent(buildAgentRequestEvent(sessionId, agent, userInput));
  }

  private AgentRequestEvent buildAgentRequestEvent(
      String sessionId, Agent agent, String userInput) {
    Message episodicContext = null;
    List<RecordEvent> recordEventList = episodeMemory.getEpisodeMemory(sessionId, agent);

    if (!CollectionUtils.isEmpty(recordEventList)) {
      Map<String, Object> velocityContext = new HashMap<>();
      velocityContext.put("records", recordEventList);
      String episodicContextPrompt =
          """
            EPISODIC_MEMORY_CONTEXT
            Purpose:
            This section summarizes what has already happened in the current session.
            It exists to ensure continuity and avoid repetition.

            The last events:
            #foreach($record in $records)
             # Event - status: $record.status(), content: $record.content()
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
    return new AgentRequestEvent(
        sessionId, agent, null, episodicContext, new Message("user", userInput, null, null, null));
  }
}
