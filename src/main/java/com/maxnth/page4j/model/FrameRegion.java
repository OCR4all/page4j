package com.maxnth.page4j.model;

import com.maxnth.page4j.geometry.Polygon;
import com.maxnth.page4j.model.value.Colour;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A frame around other regions ({@code FrameRegion}), only in PAGE 2009-03-16 to 2010-03-19. Later
 * versions have a {@link GraphicRegion} of type {@code frame} instead, which the converter maps it
 * to.
 */
@PageRecord
public record FrameRegion(
    String id,
    @Nullable String custom,
    @Nullable String comments,
    @Nullable Boolean continuation,
    @Nullable Colour bgColour,
    @Nullable Boolean borderPresent,
    List<AlternativeImage> alternativeImages,
    Polygon coords,
    List<UserAttribute> userAttributes,
    List<LabelGroup> labels,
    @Nullable TableCellRole tableCellRole,
    List<Region> regions)
    implements Region, FrameRegionBuilder.With {
  public FrameRegion {
    Objects.requireNonNull(id, "id");
    alternativeImages = List.copyOf(alternativeImages);
    Objects.requireNonNull(coords, "coords");
    userAttributes = List.copyOf(userAttributes);
    labels = List.copyOf(labels);
    regions = List.copyOf(regions);
  }
}
