package com.maxnth.page4j.model;

import com.maxnth.page4j.geometry.Polygon;
import com.maxnth.page4j.model.value.Align;
import com.maxnth.page4j.model.value.Language;
import com.maxnth.page4j.model.value.Mirrored;
import com.maxnth.page4j.model.value.Production;
import com.maxnth.page4j.model.value.ReadingDirection;
import com.maxnth.page4j.model.value.Script;
import com.maxnth.page4j.model.value.TextLineOrder;
import com.maxnth.page4j.model.value.TextType;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** A region containing text ({@code TextRegion}). */
@PageRecord
public record TextRegion(
    String id,
    @Nullable String custom,
    @Nullable String comments,
    @Nullable Boolean continuation,
    @Nullable Double orientation,
    @Nullable Mirrored mirrored,
    @Nullable TextType type,
    @Nullable Integer leading,
    @Nullable ReadingDirection readingDirection,
    @Nullable TextLineOrder textLineOrder,
    @Nullable Double readingOrientation,
    @Nullable Boolean indented,
    @Nullable Align align,
    @Nullable Language primaryLanguage,
    @Nullable Language secondaryLanguage,
    @Nullable String customLanguages,
    @Nullable Script primaryScript,
    @Nullable Script secondaryScript,
    @Nullable String customScripts,
    @Nullable Production production,
    List<AlternativeImage> alternativeImages,
    Polygon coords,
    List<UserAttribute> userAttributes,
    List<LabelGroup> labels,
    @Nullable TableCellRole tableCellRole,
    List<Region> regions,
    List<TextLine> textLines,
    List<TextEquiv> textEquivs,
    @Nullable TextStyle textStyle)
    implements Region, TextObject, TextRegionBuilder.With {
  public TextRegion {
    Objects.requireNonNull(id, "id");
    alternativeImages = List.copyOf(alternativeImages);
    Objects.requireNonNull(coords, "coords");
    userAttributes = List.copyOf(userAttributes);
    labels = List.copyOf(labels);
    regions = List.copyOf(regions);
    textLines = List.copyOf(textLines);
    textEquivs = List.copyOf(textEquivs);
  }
}
