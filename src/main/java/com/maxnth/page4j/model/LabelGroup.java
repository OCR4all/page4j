package com.maxnth.page4j.model;

import java.util.List;
import org.jspecify.annotations.Nullable;

/** Semantic labels from one external model ({@code Labels}). */
@PageRecord
public record LabelGroup(
    @Nullable String externalModel,
    @Nullable String externalId,
    @Nullable String prefix,
    @Nullable String comments,
    List<Label> labels)
    implements LabelGroupBuilder.With {
  public LabelGroup {
    labels = List.copyOf(labels);
  }
}
