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

/** A line of text ({@code TextLine}). */
@PageRecord
public record TextLine(
    String id,
    @Nullable Double orientation,
    @Nullable Mirrored mirrored,
    @Nullable Language primaryLanguage,
    @Nullable Language secondaryLanguage,
    @Nullable String customLanguages,
    @Nullable Script primaryScript,
    @Nullable Script secondaryScript,
    @Nullable String customScripts,
    @Nullable ReadingDirection readingDirection,
    @Nullable Production production,
    @Nullable String custom,
    @Nullable String comments,
    @Nullable Integer index,
    List<AlternativeImage> alternativeImages,
    Polygon coords,
    @Nullable Polygon ascentLine,
    @Nullable Polygon meanLine,
    @Nullable Polygon baseline,
    @Nullable Polygon descentLine,
    List<Word> words,
    List<TextEquiv> textEquivs,
    @Nullable TextStyle textStyle,
    List<UserAttribute> userAttributes,
    List<LabelGroup> labels)
    implements TextObject, TextLineBuilder.With {
  public TextLine {
    Objects.requireNonNull(id, "id");
    alternativeImages = List.copyOf(alternativeImages);
    Objects.requireNonNull(coords, "coords");
    words = List.copyOf(words);
    textEquivs = List.copyOf(textEquivs);
    userAttributes = List.copyOf(userAttributes);
    labels = List.copyOf(labels);
  }
}
