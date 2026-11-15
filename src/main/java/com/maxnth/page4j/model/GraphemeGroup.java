package com.maxnth.page4j.model;

import com.maxnth.page4j.model.value.CharType;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** A group of graphemes and non-printing characters ({@code GraphemeGroup}). */
@PageRecord
public record GraphemeGroup(
    String id,
    int index,
    @Nullable Boolean ligature,
    @Nullable CharType charType,
    @Nullable String custom,
    @Nullable String comments,
    List<TextEquiv> textEquivs,
    List<GraphemeElement> graphemes)
    implements GraphemeElement, GraphemeGroupBuilder.With {
  public GraphemeGroup {
    Objects.requireNonNull(id, "id");
    textEquivs = List.copyOf(textEquivs);
    graphemes = List.copyOf(graphemes);
    for (GraphemeElement grapheme : graphemes) {
      if (grapheme instanceof GraphemeGroup) {
        throw new IllegalArgumentException("Grapheme group '" + id + "' contains a grapheme group");
      }
    }
  }
}
