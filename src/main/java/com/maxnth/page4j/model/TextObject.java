package com.maxnth.page4j.model;

import com.maxnth.page4j.geometry.Polygon;
import com.maxnth.page4j.model.value.Production;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** An object that carries text: a text region, line, word or glyph. */
public sealed interface TextObject extends ContentObject permits TextRegion, TextLine, Word, Glyph {

  @Nullable Production production();

  @Nullable String custom();

  @Nullable String comments();

  List<AlternativeImage> alternativeImages();

  Polygon coords();

  /** The text, possibly several alternatives. */
  List<TextEquiv> textEquivs();

  @Nullable TextStyle textStyle();

  List<UserAttribute> userAttributes();

  List<LabelGroup> labels();
}
