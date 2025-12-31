package com.ducks.synaptra.velocity;

import java.io.StringWriter;
import java.util.Collections;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.stereotype.Service;

/**
 * Renders Apache Velocity templates from in-memory strings.
 *
 * <p>This service is designed for prompt engineering use cases where templates are stored and
 * assembled dynamically (e.g., agent prompts, episodic context, handoff context), without relying
 * on file-based template loaders.
 *
 * <p>Typical flow:
 *
 * <ol>
 *   <li>Receive the template content as a {@link String}
 *   <li>Receive a model map with variables (e.g. {@code agent}, {@code records}, {@code objective})
 *   <li>Evaluate the template against the model and return the rendered output
 * </ol>
 *
 * <p><strong>Notes:</strong>
 *
 * <ul>
 *   <li>This service performs a single-pass evaluation using {@link VelocityEngine#evaluate}.
 *   <li>Null-safe model handling: a {@code null} model is treated as an empty map.
 *   <li>The caller is responsible for ensuring that the template content is safe and trusted (i.e.
 *       not allowing untrusted users to inject arbitrary template directives).
 * </ul>
 *
 * @author Leandro Marques
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class VelocityTemplateService {

  /** Underlying Velocity engine used to evaluate templates. */
  private final VelocityEngine velocityEngine;

  /**
   * Renders the provided Velocity template content using the given model.
   *
   * <p>All key/value pairs from {@code model} are inserted into a {@link VelocityContext} and
   * become available to the template (e.g., {@code $objective}, {@code $agent}, {@code
   * #foreach(...) }).
   *
   * @param templateContent the Velocity template content to be evaluated
   * @param model the variables to be exposed to the template evaluation (may be {@code null})
   * @return the rendered template output
   * @throws IllegalArgumentException if {@code templateContent} is {@code null}
   */
  public String render(String templateContent, Map<String, Object> model) {
    if (templateContent == null) {
      throw new IllegalArgumentException("templateContent must not be null");
    }

    Map<String, Object> safeModel = (model != null) ? model : Collections.emptyMap();

    VelocityContext ctx = new VelocityContext();
    safeModel.forEach(ctx::put);

    StringWriter out = new StringWriter();
    velocityEngine.evaluate(ctx, out, "inline-template", templateContent);

    return out.toString();
  }
}
