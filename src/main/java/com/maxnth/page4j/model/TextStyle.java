package com.maxnth.page4j.model;

import com.maxnth.page4j.model.value.Colour;
import com.maxnth.page4j.model.value.UnderlineStyle;
import org.jspecify.annotations.Nullable;

/** Typographic style of text ({@code TextStyle}). */
@PageRecord
public record TextStyle(
    @Nullable String fontFamily,
    @Nullable Boolean serif,
    @Nullable Boolean monospace,
    @Nullable Double fontSize,
    @Nullable Integer xHeight,
    @Nullable Integer kerning,
    @Nullable Colour textColour,
    @Nullable Integer textColourRgb,
    @Nullable Colour bgColour,
    @Nullable Integer bgColourRgb,
    @Nullable Boolean reverseVideo,
    @Nullable Boolean bold,
    @Nullable Boolean italic,
    @Nullable Boolean underlined,
    @Nullable UnderlineStyle underlineStyle,
    @Nullable Boolean subscript,
    @Nullable Boolean superscript,
    @Nullable Boolean strikethrough,
    @Nullable Boolean smallCaps,
    @Nullable Boolean letterSpaced)
    implements TextStyleBuilder.With {
  public TextStyle {}
}
