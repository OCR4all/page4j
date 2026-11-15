package com.maxnth.page4j.model;

import com.maxnth.page4j.geometry.Polygon;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** A region containing noise, such as dirt or scanner artefacts ({@code NoiseRegion}). */
@PageRecord
public record NoiseRegion(
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
    implements Region, NoiseRegionBuilder.With {
  public NoiseRegion {
    Objects.requireNonNull(id, "id");
    alternativeImages = List.copyOf(alternativeImages);
    Objects.requireNonNull(coords, "coords");
    userAttributes = List.copyOf(userAttributes);
    labels = List.copyOf(labels);
    regions = List.copyOf(regions);
  }
}
