package com.ducks.synaptra.event.tool.model;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ToolExecutionType {
  SELF_REFLECTION("self_reflection"),
  ROUTE_TO_AGENT("route_to_agent"),
  ROUTE_TO_PARENT("route_to_parent"),
  FINALIZE_REQUEST("finalize_request");

  private final String value;

  static final Map<String, ToolExecutionType> MAP =
      Arrays.stream(ToolExecutionType.values())
          .collect(Collectors.toMap(ToolExecutionType::getValue, Function.identity()));

  public static ToolExecutionType fromValue(String value) {
    return MAP.get(value);
  }
}
