package com.ai.agentics.orchestration.event.record.contract;

import com.ai.agentics.agent.Agent;
import com.ai.agentics.client.openai.data.Message;
import com.ai.agentics.prompt.contract.RecordEvent;
import org.springframework.lang.Nullable;

public record RecordRequestEvent(
    String sessionId, @Nullable Agent agent, @Nullable Message user, RecordEvent recordEvent) {}
