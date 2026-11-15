package com.maxnth.page4j.model;

import com.maxnth.page4j.geometry.Polygon;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** A region of a type that PAGE does not define ({@code CustomRegion}). */
@PageRecord
public record CustomRegion(
    String id,
    @Nullable String custom,
    @Nullable String comments,
    @Nullable Boolean continuation,
    @Nullable String type,
    List<AlternativeImage> alternativeImages,
    Polygon coords,
    List<UserAttribute> userAttributes,
    List<LabelGroup> labels,
    @Nullable TableCellRole tableCellRole,
    List<Region> regions)
    implements Region, CustomRegionBuilder.With {
  public CustomRegion {
    Objects.requireNonNull(id, "id");
    alternativeImages = List.copyOf(alternativeImages);
    Objects.requireNonNull(coords, "coords");
    userAttributes = List.copyOf(userAttributes);
    labels = List.copyOf(labels);
    regions = List.copyOf(regions);
  }
}
