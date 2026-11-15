package com.maxnth.page4j.model;

import com.maxnth.page4j.model.value.CharType;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** A character without a visible outline, such as a zero-width joiner ({@code NonPrintingChar}). */
@PageRecord
public record NonPrintingChar(
    String id,
    int index,
    @Nullable Boolean ligature,
    @Nullable CharType charType,
    @Nullable String custom,
    @Nullable String comments,
    List<TextEquiv> textEquivs)
    implements GraphemeElement, NonPrintingCharBuilder.With {
  public NonPrintingChar {
    Objects.requireNonNull(id, "id");
    textEquivs = List.copyOf(textEquivs);
  }
}
