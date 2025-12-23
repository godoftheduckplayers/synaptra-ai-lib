package com.ducks.synaptra.orchestration.event.record.contract;

import com.ducks.synaptra.agent.Agent;
import com.ducks.synaptra.client.openai.data.Message;
import com.ducks.synaptra.prompt.contract.RecordEvent;
import org.springframework.lang.Nullable;

public record RecordRequestEvent(
    String sessionId, @Nullable Agent agent, @Nullable Message user, RecordEvent recordEvent) {}
