package com.ducks.synaptra.memory;

import com.ducks.synaptra.agent.Agent;
import com.ducks.synaptra.log.LogTracer;
import com.ducks.synaptra.prompt.contract.RecordEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class EpisodeMemory {

  private static final Map<String, Map<String, List<RecordEvent>>> MEMORY = new HashMap<>();

  @LogTracer(spanName = "register_memory_event")
  public void registerEvent(String sessionId, Agent agent, RecordEvent recordEvent) {
    Map<String, List<RecordEvent>> events = getEvents(sessionId);
    List<RecordEvent> agentEvents = getAgentEvents(agent, events);
    agentEvents.add(recordEvent);
  }

  @LogTracer(spanName = "get_memory_event")
  public List<RecordEvent> getEpisodeMemory(String sessionId, Agent agent) {
    Map<String, List<RecordEvent>> events = getEvents(sessionId);
    return getAgentEvents(agent, events);
  }

  private List<RecordEvent> getAgentEvents(Agent agent, Map<String, List<RecordEvent>> events) {
    List<RecordEvent> recordEvents;
    if (events.containsKey(agent.identifier())) {
      recordEvents = events.get(agent.identifier());
    } else {
      recordEvents = new ArrayList<>();
      events.put(agent.identifier(), recordEvents);
    }
    return recordEvents;
  }

  private Map<String, List<RecordEvent>> getEvents(String sessionId) {
    Map<String, List<RecordEvent>> events;
    if (MEMORY.containsKey(sessionId)) {
      events = MEMORY.get(sessionId);
    } else {
      events = new HashMap<>();
      MEMORY.put(sessionId, events);
    }
    return events;
  }
}
