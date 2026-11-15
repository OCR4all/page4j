package com.maxnth.page4j.model;

import com.maxnth.page4j.model.value.RelationType;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** A relation between two objects ({@code Relation}). */
@PageRecord
public record Relation(
    @Nullable String id,
    @Nullable RelationType type,
    @Nullable String custom,
    @Nullable String comments,
    List<LabelGroup> labels,
    String sourceRegionRef,
    String targetRegionRef)
    implements RelationBuilder.With {
  public Relation {
    labels = List.copyOf(labels);
    Objects.requireNonNull(sourceRegionRef, "sourceRegionRef");
    Objects.requireNonNull(targetRegionRef, "targetRegionRef");
  }
}
