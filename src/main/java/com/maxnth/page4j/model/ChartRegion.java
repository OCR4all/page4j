package com.maxnth.page4j.model;

import com.maxnth.page4j.geometry.Polygon;
import com.maxnth.page4j.model.value.ChartType;
import com.maxnth.page4j.model.value.Colour;
import com.maxnth.page4j.model.value.Mirrored;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** A region containing a chart ({@code ChartRegion}). */
@PageRecord
public record ChartRegion(
    String id,
    @Nullable String custom,
    @Nullable String comments,
    @Nullable Boolean continuation,
    @Nullable Double orientation,
    @Nullable Mirrored mirrored,
    @Nullable ChartType type,
    @Nullable Integer numColours,
    @Nullable Colour bgColour,
    @Nullable Boolean embText,
    List<AlternativeImage> alternativeImages,
    Polygon coords,
    List<UserAttribute> userAttributes,
    List<LabelGroup> labels,
    @Nullable TableCellRole tableCellRole,
    List<Region> regions)
    implements Region, ChartRegionBuilder.With {
  public ChartRegion {
    Objects.requireNonNull(id, "id");
    alternativeImages = List.copyOf(alternativeImages);
    Objects.requireNonNull(coords, "coords");
    userAttributes = List.copyOf(userAttributes);
    labels = List.copyOf(labels);
    regions = List.copyOf(regions);
  }
}
