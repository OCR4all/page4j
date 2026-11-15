package com.maxnth.page4j.model;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** A semantic label ({@code Label}). */
@PageRecord
public record Label(String value, @Nullable String type, @Nullable String comments)
    implements LabelBuilder.With {
  public Label {
    Objects.requireNonNull(value, "value");
  }
}
