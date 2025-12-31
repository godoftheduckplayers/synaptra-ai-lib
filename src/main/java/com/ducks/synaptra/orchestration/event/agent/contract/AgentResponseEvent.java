package com.ducks.synaptra.orchestration.event.agent.contract;

import com.ducks.synaptra.agent.Agent;
import com.ducks.synaptra.client.openai.data.ChatCompletionResponse;
import com.ducks.synaptra.client.openai.data.Message;
import org.springframework.lang.Nullable;

/**
 * Event published after an {@link Agent} execution completes and a {@link ChatCompletionResponse}
 * is produced.
 *
 * <p>This event represents the output of an agent execution cycle, carrying the raw response
 * returned by the AI provider. It can be consumed by orchestrators, supervisors, listeners, or
 * delivery components responsible for routing, persistence, post-processing, or user-facing output.
 *
 * <p>Typical publishing scenarios include:
 *
 * <ul>
 *   <li>Final response generation for a user interaction
 *   <li>Intermediate response propagation in multi-agent flows
 *   <li>Follow-up processing after tool execution or orchestration steps
 * </ul>
 *
 * <p>{@code agent} and {@code user} are nullable to support flows where attribution or the original
 * input message may be deferred, enriched later, or not applicable (e.g., supervisor aggregation).
 *
 * @param sessionId the unique identifier of the execution session
 * @param agent the agent that produced the response, or {@code null} if not applicable
 * @param user the original user message associated with this execution, or {@code null} if not
 *     available
 * @param chatCompletionResponse the chat completion response returned by the AI provider (required)
 * @author Leandro Marques
 * @since 1.0.0
 */
public record AgentResponseEvent(
    String sessionId,
    @Nullable Agent agent,
    @Nullable Message user,
    ChatCompletionResponse chatCompletionResponse) {}
