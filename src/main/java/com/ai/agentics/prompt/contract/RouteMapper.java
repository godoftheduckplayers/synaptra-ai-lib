package com.ai.agentics.prompt.contract;

import java.util.HashMap;
import java.util.Map;

public record RouteMapper(String agent, String input, String objective) {

  public Map<String, Object> velocityContext() {
    Map<String, Object> context = new HashMap<>();
    context.put("agent", agent);
    context.put("input", input);
    context.put("objective", objective);
    return context;
  }
}
