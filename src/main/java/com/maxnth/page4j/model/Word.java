package com.maxnth.page4j.model;

import com.maxnth.page4j.geometry.Polygon;
import com.maxnth.page4j.model.value.Language;
import com.maxnth.page4j.model.value.Mirrored;
import com.maxnth.page4j.model.value.Production;
import com.maxnth.page4j.model.value.ReadingDirection;
import com.maxnth.page4j.model.value.Script;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** A word ({@code Word}). */
@PageRecord
public record Word(
    String id,
    @Nullable Double orientation,
    @Nullable Mirrored mirrored,
    @Nullable Language language,
    @Nullable String customLanguages,
    @Nullable Script primaryScript,
    @Nullable Script secondaryScript,
    @Nullable String customScripts,
    @Nullable ReadingDirection readingDirection,
    @Nullable Production production,
    @Nullable String custom,
    @Nullable String comments,
    List<AlternativeImage> alternativeImages,
    Polygon coords,
    List<Glyph> glyphs,
    List<TextEquiv> textEquivs,
    @Nullable TextStyle textStyle,
    List<UserAttribute> userAttributes,
    List<LabelGroup> labels)
    implements TextObject, WordBuilder.With {
  public Word {
    Objects.requireNonNull(id, "id");
    alternativeImages = List.copyOf(alternativeImages);
    Objects.requireNonNull(coords, "coords");
    glyphs = List.copyOf(glyphs);
    textEquivs = List.copyOf(textEquivs);
    userAttributes = List.copyOf(userAttributes);
    labels = List.copyOf(labels);
  }
}
