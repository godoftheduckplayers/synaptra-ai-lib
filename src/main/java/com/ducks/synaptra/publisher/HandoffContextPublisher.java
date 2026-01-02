package com.ducks.synaptra.publisher;

import static com.ducks.synaptra.orchestration.event.record.RecordExecutionEvent.WAIT_AGENT_EXECUTION;

import com.ducks.synaptra.agent.Agent;
import com.ducks.synaptra.client.openai.data.Message;
import com.ducks.synaptra.log.LogTracer;
import com.ducks.synaptra.memory.EpisodeMemory;
import com.ducks.synaptra.orchestration.event.agent.contract.AgentRequestEvent;
import com.ducks.synaptra.orchestration.event.answer.contract.AnswerResponseEvent;
import com.ducks.synaptra.orchestration.event.tool.contract.ToolResponseEvent;
import com.ducks.synaptra.publisher.contract.RecordEvent;
import com.ducks.synaptra.publisher.contract.RouteMapper;
import com.ducks.synaptra.velocity.VelocityTemplateService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * Publishes agent handoff (routing) events based on tool execution results.
 *
 * <p>This component acts as an adapter between a routing tool call response (e.g. {@code
 * route_to_agent}) and the orchestration layer by:
 *
 * <ul>
 *   <li>Parsing the tool call arguments into a {@link RouteMapper}
 *   <li>Resolving the target {@link Agent} to execute from the current agent's available agents
 *   <li>Registering an episodic memory record indicating that the next agent execution is pending
 *   <li>Publishing an {@link AgentRequestEvent} with a system handoff context message
 * </ul>
 *
 * <p>The produced system message is rendered with {@link VelocityTemplateService} using the routing
 * context so that the selected agent receives clear instructions about the next objective to
 * execute.
 *
 * @author Leandro Marques
 * @since 1.0.0
 */
@Service
public class HandoffContextPublisher {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  /**
   * Base system prompt injected during handoff to guide the next agent execution.
   *
   * <p>Rendered with the Velocity context from {@link RouteMapper#velocityContext()}.
   */
  private static final String BASE_PROMPT =
      """
          HANDOFF
          Objective: $objective

          Constraints:
           - Do not assume missing data.
          """;

  private final ApplicationEventPublisher publisher;
  private final VelocityTemplateService velocityTemplateService;
  private final EpisodeMemory episodeMemory;

  /**
   * Creates a new {@link HandoffContextPublisher}.
   *
   * @param publisher Spring event publisher used to emit {@link AgentRequestEvent}s
   * @param velocityTemplateService template renderer used to build the handoff system message
   * @param episodeMemory episodic memory store used to register the routing step
   */
  public HandoffContextPublisher(
      ApplicationEventPublisher publisher,
      VelocityTemplateService velocityTemplateService,
      EpisodeMemory episodeMemory) {
    this.publisher = publisher;
    this.velocityTemplateService = velocityTemplateService;
    this.episodeMemory = episodeMemory;
  }

  /**
   * Publishes an {@link AgentRequestEvent} derived from a routing tool call response.
   *
   * <p>This method is typically invoked when the agent finishes with a {@code tool_calls} response
   * and the tool name indicates a routing action (e.g. {@code route_to_agent}).
   *
   * <p>It will:
   *
   * <ul>
   *   <li>Parse the tool arguments into {@link RouteMapper}
   *   <li>Resolve the target {@link Agent}
   *   <li>Persist a "waiting for agent execution" record in episodic memory
   *   <li>Publish the corresponding {@link AgentRequestEvent}
   * </ul>
   *
   * @param toolResponseEvent the tool response event containing the routing tool call
   * @throws RuntimeException if arguments cannot be parsed or the target agent cannot be resolved
   */
  @LogTracer(spanName = "agent_handoff_publish_event")
  public void publishEvent(ToolResponseEvent toolResponseEvent) {
    publisher.publishEvent(buildAgentRequestEvent(toolResponseEvent));
  }

  /**
   * Builds an {@link AgentRequestEvent} from a {@link ToolResponseEvent} produced by a routing
   * tool.
   *
   * <p>This method parses the routing tool arguments into a {@link RouteMapper}, resolves the
   * target agent, registers an orchestration step into episodic memory, and finally builds the
   * handoff system {@link Message} rendered via Velocity before returning the resulting {@link
   * AgentRequestEvent}.
   *
   * <h3>Interim response behavior</h3>
   *
   * <p>If the current agent supports interim messages (i.e. {@code
   * toolResponseEvent.agent().isSupportsInterimMessages()} is {@code true}), this method will
   * publish an {@link AnswerResponseEvent} <b>before</b> routing to the target agent. The published
   * message uses {@link RouteMapper#response()} as its content and is intended to provide the user
   * with a partial/progress update while the orchestration continues asynchronously.
   *
   * <p>In this flow, {@link RouteMapper#response()} should contain a short, user-friendly, generic
   * "waiting/progress" message describing what will be executed next based on the request (without
   * leaking internal details). If {@code response} is {@code null} or empty, the event may still be
   * published depending on downstream handling, so producers should ensure a meaningful interim
   * response when interim messaging is enabled.
   *
   * <h3>Episodic memory registration</h3>
   *
   * <p>The method registers an orchestration record (e.g. "Waiting for agent execution: X") so
   * episodic memory accurately reflects that an agent handoff is pending.
   *
   * @param toolResponseEvent the tool response event containing the routing tool call
   * @return the constructed {@link AgentRequestEvent} to execute the resolved target agent
   * @throws RuntimeException if the routing tool arguments cannot be parsed into {@link
   *     RouteMapper}
   */
  private AgentRequestEvent buildAgentRequestEvent(ToolResponseEvent toolResponseEvent) {
    try {
      RouteMapper routeMapper =
          MAPPER.readValue(toolResponseEvent.toolCall().function().arguments(), RouteMapper.class);

      assert toolResponseEvent.agent() != null;
      if (toolResponseEvent.agent().isSupportsInterimMessages()) {
        publisher.publishEvent(
            new AnswerResponseEvent(
                toolResponseEvent.sessionId(),
                toolResponseEvent.agent(),
                toolResponseEvent.user(),
                routeMapper.response()));
      }

      Agent targetAgent = resolveTargetAgent(routeMapper, toolResponseEvent);

      // Record the orchestration step so episodic memory reflects the pending agent execution.
      episodeMemory.registerEvent(
          toolResponseEvent.sessionId(),
          toolResponseEvent.agent(),
          new RecordEvent(
              "Waiting for agent execution: " + targetAgent.getName(), WAIT_AGENT_EXECUTION));

      Message handoffContext =
          new Message(
              "system",
              velocityTemplateService.render(BASE_PROMPT, routeMapper.velocityContext()),
              null,
              null,
              null);

      return new AgentRequestEvent(
          toolResponseEvent.sessionId(), targetAgent, handoffContext, toolResponseEvent.user());

    } catch (JsonProcessingException e) {
      throw new RuntimeException(
          "Failed to parse route_to_agent arguments: "
              + toolResponseEvent.toolCall().function().arguments(),
          e);
    }
  }

  /**
   * Resolves the target {@link Agent} specified by {@link RouteMapper#agent()} from the current
   * agent's available agents list.
   *
   * <p><strong>Preconditions:</strong> {@code toolResponseEvent.agent()} must not be {@code null}.
   *
   * @param routeMapper parsed routing information indicating the agent to execute
   * @param toolResponseEvent the tool response event containing the current agent context
   * @return the resolved target agent
   * @throws RuntimeException if the target agent cannot be found
   */
  private Agent resolveTargetAgent(RouteMapper routeMapper, ToolResponseEvent toolResponseEvent) {
    assert toolResponseEvent.agent() != null;

    Optional<Agent> agent =
        toolResponseEvent.agent().getAgents().stream()
            .filter(a -> a.getName().equals(routeMapper.agent()))
            .findFirst();

    return agent.orElseThrow(
        () ->
            new RuntimeException(
                "Failed to route execution to agent '"
                    + routeMapper.agent()
                    + "': agent not found."));
  }
}
