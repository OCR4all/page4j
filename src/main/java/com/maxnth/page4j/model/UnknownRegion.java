package com.maxnth.page4j.model;

import com.maxnth.page4j.geometry.Polygon;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** A region of unknown content ({@code UnknownRegion}). */
@PageRecord
public record UnknownRegion(
    String id,
    @Nullable String custom,
    @Nullable String comments,
    @Nullable Boolean continuation,
    List<AlternativeImage> alternativeImages,
    Polygon coords,
    List<UserAttribute> userAttributes,
    List<LabelGroup> labels,
    @Nullable TableCellRole tableCellRole,
    List<Region> regions)
    implements Region, UnknownRegionBuilder.With {
  public UnknownRegion {
    Objects.requireNonNull(id, "id");
    alternativeImages = List.copyOf(alternativeImages);
    Objects.requireNonNull(coords, "coords");
    userAttributes = List.copyOf(userAttributes);
    labels = List.copyOf(labels);
    regions = List.copyOf(regions);
  }
}
