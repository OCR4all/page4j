package com.maxnth.page4j.model;

import com.maxnth.page4j.geometry.Polygon;
import com.maxnth.page4j.model.value.CharType;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** A grapheme with its own outline ({@code Grapheme}). */
@PageRecord
public record Grapheme(
    String id,
    int index,
    @Nullable Boolean ligature,
    @Nullable CharType charType,
    @Nullable String custom,
    @Nullable String comments,
    List<TextEquiv> textEquivs,
    Polygon coords)
    implements GraphemeElement, GraphemeBuilder.With {
  public Grapheme {
    Objects.requireNonNull(id, "id");
    textEquivs = List.copyOf(textEquivs);
    Objects.requireNonNull(coords, "coords");
  }
}
