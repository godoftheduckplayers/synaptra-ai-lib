package com.ai.agentics.prompt.contract;

import java.util.HashMap;
import java.util.Map;

public record RecordEvent(String content, String status) {

  public Map<String, Object> velocityContext() {
    Map<String, Object> context = new HashMap<>();
    context.put("content", content);
    context.put("status", status);
    return context;
  }
}
