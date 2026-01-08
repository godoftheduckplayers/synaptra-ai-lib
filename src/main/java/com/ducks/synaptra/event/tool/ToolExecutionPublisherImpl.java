package com.ducks.synaptra.event.tool;

import static com.ducks.synaptra.event.tool.model.ToolExecutionType.*;

import com.ducks.synaptra.event.answer.AnswerExecutionListener;
import com.ducks.synaptra.event.answer.model.AnswerRequestEvent;
import com.ducks.synaptra.event.tool.model.ToolExecutionRequest;
import com.ducks.synaptra.event.tool.model.ToolExecutionResponse;
import com.ducks.synaptra.event.tool.model.ToolExecutionType;
import com.ducks.synaptra.event.tool.model.ToolRequestEvent;
import com.ducks.synaptra.log.LogTracer;
import com.ducks.synaptra.memory.episode.EpisodeMemory;
import com.ducks.synaptra.memory.episode.model.RecordEvent;
import com.ducks.synaptra.memory.episode.model.StatusType;
import com.ducks.synaptra.tool.route.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ToolExecutionPublisherImpl implements ToolExecutionPublisher {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final ToolExecutionListener toolExecutionListener;
  private final RouteToAgentToolExecutionImpl routeToAgentToolExecution;
  private final RouteToParentToolExecutionImpl routeToParentToolExecution;
  private final SelfReflectionToolExecutionImpl selfReflectionToolExecution;
  private final List<AnswerExecutionListener> answerExecutionListenerList;
  private final EpisodeMemory episodeMemory;

  @LogTracer(spanName = "tool_execution_event")
  @EventListener
  @Async("agentExecutionExecutor")
  @Override
  public void publisherToolRequestEvent(ToolRequestEvent toolRequestEvent) {
    ToolExecutionType toolExecutionType =
        ToolExecutionType.fromValue(toolRequestEvent.getToolCall().function().name());

    handleInternalOrchestration(toolExecutionType, toolRequestEvent);

    if (!isInternalFunction(toolExecutionType)) {
      episodeMemory.registerEvent(
          toolRequestEvent.getSessionId(),
          toolRequestEvent.getAgent(),
          new RecordEvent(
              "The system is waiting for the tool '"
                  + toolRequestEvent.getToolCall().function().name()
                  + "' execution to complete.",
              StatusType.WAIT_TOOL_EXECUTION));

      ToolExecutionResponse toolExecutionResponse =
          toolExecutionListener.onToolResponseEvent(new ToolExecutionRequest(toolRequestEvent));

      try {
        episodeMemory.registerEvent(
            toolRequestEvent.getSessionId(),
            toolRequestEvent.getAgent(),
            new RecordEvent(
                "The tool '"
                    + toolRequestEvent.getToolCall().function().name()
                    + "'execution completed successfully! Tool execution details: "
                    + toolExecutionResponse.getToolExecutionStatus()
                    + ", toolExecutionDetails: "
                    + MAPPER.writeValueAsString(toolExecutionResponse.getDetails()),
                StatusType.FINISHED_TOOL_EXECUTION));
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }

      selfReflectionToolExecution.resolve(toolRequestEvent);
      // parei aqui,
      // Notificando que uma tool deve ser executada OK
      // registrando o evento de aguardando execução OK
      // executar a tool OK
      // registrar o evento de tool executada OK
      // agente fazer uma auto reflexão
      // se tiver que continuar, ele deve continuar automaticamente
      // se não tiver registrar o evento de agente finalizado (esse deve ser gerado para o pai) e ai
      // apagar os eventos do agente atual
      // chamar o supervisor para que ele veja o que tem que fazer e finalizar (limpar os eventos
      // neste tempo)
    }
  }

  private void handleInternalOrchestration(
      ToolExecutionType toolExecutionType, ToolRequestEvent toolRequestEvent) {

    if (ROUTE_TO_AGENT.equals(toolExecutionType)) {
      routeToAgentToolExecution.resolve(toolRequestEvent);
    }

    if (ROUTE_TO_PARENT.equals(toolExecutionType)) {
      routeToParentToolExecution.resolve(toolRequestEvent);
    }

    if (FINALIZE_REQUEST.equals(toolExecutionType)) {
      try {
        FinalizeMapper finalizeMapper =
            MAPPER.readValue(
                toolRequestEvent.getToolCall().function().arguments(), FinalizeMapper.class);
        answerExecutionListenerList.forEach(
            answerExecutionListener ->
                answerExecutionListener.onAnswerExecutionResponseEvent(
                    new AnswerRequestEvent(
                        toolRequestEvent.getSessionId(),
                        toolRequestEvent.getAgent(),
                        toolRequestEvent.getUser(),
                        finalizeMapper.summary())));
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public boolean isInternalFunction(ToolExecutionType toolExecutionType) {
    return ROUTE_TO_AGENT.equals(toolExecutionType)
        || ROUTE_TO_PARENT.equals(toolExecutionType)
        || FINALIZE_REQUEST.equals(toolExecutionType);
  }
}
