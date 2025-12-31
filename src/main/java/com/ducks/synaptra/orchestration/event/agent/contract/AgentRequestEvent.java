package com.ducks.synaptra.orchestration.event.agent.contract;

import com.ducks.synaptra.agent.Agent;
import com.ducks.synaptra.client.openai.data.ChatCompletionRequest;
import com.ducks.synaptra.client.openai.data.Message;
import com.ducks.synaptra.velocity.VelocityTemplateService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.lang.Nullable;

/**
 * Event representing a request to execute an {@link Agent} within a specific session.
 *
 * <p>This event is the contract between the orchestration layer and the execution layer. It carries
 * all contextual information required to build a {@link ChatCompletionRequest} for an AI provider,
 * including the agent configuration, optional handoff context, optional episodic context, and the
 * current user message.
 *
 * <h2>Usage</h2>
 *
 * <p>An {@code AgentRequestEvent} is typically published when:
 *
 * <ul>
 *   <li>A user provides input and the system needs to trigger (or continue) an agent execution
 *   <li>An orchestrator/supervisor selects an agent to handle the request
 *   <li>The execution flow needs enrichment with additional context before the provider call
 * </ul>
 *
 * <p>The fields {@code agent}, {@code handoffContext}, {@code episodicContext}, and {@code user}
 * may be {@code null} to support multi-stage orchestration (e.g., deferred agent resolution, event
 * enrichment, or rehydration of running sessions).
 *
 * <p><strong>Execution preconditions:</strong> the execution layer must validate that {@code agent}
 * and {@code user} are present before calling the AI provider. This record uses {@code assert}
 * checks inside {@link #toChatCompletionRequest(VelocityTemplateService)} to enforce these
 * assumptions during development.
 *
 * @param sessionId unique identifier of the execution session (used for correlation and continuity)
 * @param agent the agent selected to execute the request, or {@code null} if not yet resolved
 * @param handoffContext optional system context used for handoff/route transitions, or {@code null}
 * @param episodicContext optional system context built from episodic memory, or {@code null}
 * @param user the user message/input, or {@code null} if not yet available
 * @author Leandro Marques
 * @since 1.0.0
 */
public record AgentRequestEvent(
    String sessionId,
    @Nullable Agent agent,
    @Nullable Message handoffContext,
    @Nullable Message episodicContext,
    @Nullable Message user) {

  /**
   * Convenience constructor for request events where no episodic context exists yet.
   *
   * @param sessionId unique identifier of the execution session
   * @param agent the agent selected to execute the request
   * @param handoffContext optional system context used for handoff/route transitions
   * @param user the user message/input
   */
  public AgentRequestEvent(String sessionId, Agent agent, Message handoffContext, Message user) {
    this(sessionId, agent, handoffContext, null, user);
  }

  /**
   * Builds a {@link ChatCompletionRequest} from this event.
   *
   * <p>This method assembles the provider request in the following order:
   *
   * <ol>
   *   <li>System prompt rendered from the agent base prompt using {@link VelocityTemplateService}
   *   <li>Optional handoff context message (if present)
   *   <li>Optional episodic context message (if present)
   *   <li>User message (required)
   * </ol>
   *
   * <p>It also propagates the agent configuration:
   *
   * <ul>
   *   <li>Model name and decoding parameters (temperature, max tokens, top-p)
   *   <li>Tool definitions and tool choice strategy
   * </ul>
   *
   * <p><strong>Preconditions:</strong>
   *
   * <ul>
   *   <li>{@code agent} must not be {@code null}
   *   <li>{@code user} must not be {@code null}
   *   <li>{@code velocityTemplateService} must not be {@code null}
   * </ul>
   *
   * <p>If {@code agent} or {@code user} is missing, an {@link AssertionError} will be thrown (when
   * assertions are enabled).
   *
   * @param velocityTemplateService the service used to render Velocity templates for prompts
   * @return a fully constructed {@link ChatCompletionRequest} ready to be sent to the AI provider
   */
  public ChatCompletionRequest toChatCompletionRequest(
      VelocityTemplateService velocityTemplateService) {

    assert agent != null;
    assert user != null;

    List<Message> messageList = new ArrayList<>();

    // Base system prompt (agent prompt rendered with its Velocity context)
    messageList.add(
        new Message(
            "system",
            velocityTemplateService.render(agent.getPrompt(), agent.getVelocityContext()),
            null,
            null,
            null));

    // Optional orchestration/handoff context
    if (handoffContext != null) {
      messageList.add(handoffContext);
    }

    // Optional episodic memory context (rehydration for running sessions)
    if (episodicContext != null) {
      messageList.add(episodicContext);
    }

    // Current user input
    messageList.add(user);

    return new ChatCompletionRequest(
        agent.getProviderConfig().model(),
        messageList,
        agent.getTools(),
        agent.getToolChoice().getValue(),
        agent.getProviderConfig().temperature(),
        agent.getProviderConfig().maxTokens(),
        agent.getProviderConfig().topP());
  }
}
