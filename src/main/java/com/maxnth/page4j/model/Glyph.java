package com.maxnth.page4j.model;

import com.maxnth.page4j.geometry.Polygon;
import com.maxnth.page4j.model.value.Mirrored;
import com.maxnth.page4j.model.value.Production;
import com.maxnth.page4j.model.value.Script;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** A glyph ({@code Glyph}). */
@PageRecord
public record Glyph(
    String id,
    @Nullable Double orientation,
    @Nullable Mirrored mirrored,
    @Nullable Boolean ligature,
    @Nullable Boolean symbol,
    @Nullable Script script,
    @Nullable String customScript,
    @Nullable Production production,
    @Nullable String custom,
    @Nullable String comments,
    List<AlternativeImage> alternativeImages,
    Polygon coords,
    List<GraphemeElement> graphemes,
    List<TextEquiv> textEquivs,
    @Nullable TextStyle textStyle,
    List<UserAttribute> userAttributes,
    List<LabelGroup> labels)
    implements TextObject, GlyphBuilder.With {
  public Glyph {
    Objects.requireNonNull(id, "id");
    alternativeImages = List.copyOf(alternativeImages);
    Objects.requireNonNull(coords, "coords");
    graphemes = List.copyOf(graphemes);
    textEquivs = List.copyOf(textEquivs);
    userAttributes = List.copyOf(userAttributes);
    labels = List.copyOf(labels);
  }
}
