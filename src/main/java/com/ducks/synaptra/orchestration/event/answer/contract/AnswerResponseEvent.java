package com.ducks.synaptra.orchestration.event.answer.contract;

import com.ducks.synaptra.agent.Agent;
import com.ducks.synaptra.client.openai.data.Message;
import org.springframework.lang.Nullable;

/**
 * Event representing an agent answer that is ready to be delivered.
 *
 * <p>This event is published after the agent execution has completed and the resulting output has
 * been interpreted, formatted, or finalized into a deliverable answer. It represents the final (or
 * intermediate) message that can be sent to the user or external consumers.
 *
 * <p>This event belongs to the <strong>answer delivery stage</strong> of the agentic pipeline and
 * is intentionally decoupled from provider-specific response formats (e.g., {@code
 * ChatCompletionResponse}).
 *
 * <p>Typical publishing scenarios include:
 *
 * <ul>
 *   <li>Delivering the final response of an agent to the user
 *   <li>Streaming or dispatching intermediate answers
 *   <li>Forwarding agent answers to external systems or integrations
 * </ul>
 *
 * <p>The {@code agent} and {@code user} fields are nullable to support supervisor, aggregator, or
 * relay scenarios where attribution or the original input message may not be required or may be
 * resolved at a later stage.
 *
 * @param sessionId the unique identifier of the execution session
 * @param agent the agent that produced the answer, or {@code null} if not applicable
 * @param user the original user message associated with this answer, or {@code null}
 * @param response the finalized textual answer produced by the agent (required)
 * @author Leandro Marques
 * @since 1.0.0
 */
public record AnswerResponseEvent(
    String sessionId, @Nullable Agent agent, @Nullable Message user, String response) {}
