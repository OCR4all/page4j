package com.maxnth.page4j.convert;

import com.maxnth.page4j.model.Page;
import java.util.List;
import java.util.Objects;

/**
 * The result of {@link PageConverter#convert}: the page fitted to the target version, and what was
 * changed to make it fit.
 *
 * @param page the converted page, which can be written in the target version
 * @param messages one message per change, in document order; empty if the page already fitted
 */
public record Conversion(Page page, List<ConversionMessage> messages) {

  public Conversion {
    Objects.requireNonNull(page, "page");
    messages = List.copyOf(messages);
  }

  /** Whether content was removed, so converting back would not give the original page. */
  public boolean lossy() {
    return messages.stream().anyMatch(m -> m.kind() == ConversionMessage.Kind.REMOVED);
  }
}
