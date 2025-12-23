package com.ducks.synaptra.velocity;

import java.io.StringWriter;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VelocityTemplateService {

  private final VelocityEngine velocityEngine;

  public String render(String templateContent, Map<String, Object> model) {
    VelocityContext ctx = new VelocityContext();
    model.forEach(ctx::put);

    StringWriter out = new StringWriter();

    velocityEngine.evaluate(ctx, out, "inline-template", templateContent);

    return out.toString();
  }
}
