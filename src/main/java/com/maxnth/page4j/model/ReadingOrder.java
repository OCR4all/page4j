package com.maxnth.page4j.model;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** The reading order of the page ({@code ReadingOrder}). */
@PageRecord
public record ReadingOrder(@Nullable Double conf, Group group) implements ReadingOrderBuilder.With {
  public ReadingOrder {
    Objects.requireNonNull(group, "group");
  }
}
