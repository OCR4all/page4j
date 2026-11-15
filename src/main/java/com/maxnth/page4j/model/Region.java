package com.maxnth.page4j.model;

import com.maxnth.page4j.geometry.Polygon;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** A region of the page ({@code RegionType}). Regions can contain further regions. */
public sealed interface Region extends ContentObject
    permits TextRegion,
        ImageRegion,
        LineDrawingRegion,
        GraphicRegion,
        TableRegion,
        ChartRegion,
        MapRegion,
        SeparatorRegion,
        MathsRegion,
        ChemRegion,
        MusicRegion,
        AdvertRegion,
        FormRegion,
        FrameRegion,
        NoiseRegion,
        UnknownRegion,
        CustomRegion {

  @Nullable String custom();

  @Nullable String comments();

  /** Whether the region continues on the next page or column. */
  @Nullable Boolean continuation();

  List<AlternativeImage> alternativeImages();

  Polygon coords();

  List<UserAttribute> userAttributes();

  List<LabelGroup> labels();

  @Nullable TableCellRole tableCellRole();

  /** The regions nested in this one. */
  List<Region> regions();
}
