package com.maxnth.page4j.model;

import com.maxnth.page4j.model.value.CharType;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** A grapheme, non-printing character or grapheme group of a glyph ({@code GraphemeBaseType}). */
public sealed interface GraphemeElement extends ContentObject
    permits Grapheme, NonPrintingChar, GraphemeGroup {

  /** The position within the parent. */
  int index();

  @Nullable Boolean ligature();

  @Nullable CharType charType();

  @Nullable String custom();

  @Nullable String comments();

  List<TextEquiv> textEquivs();
}
