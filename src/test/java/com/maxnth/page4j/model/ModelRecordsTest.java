package com.maxnth.page4j.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.maxnth.page4j.geometry.Point;
import com.maxnth.page4j.geometry.Polygon;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModelRecordsTest {

  private static final Polygon BOX = Polygon.of(new Point(0, 0), new Point(1, 1), new Point(0, 1));

  @Test
  void listsAreCopiedAndUnmodifiable() {
    List<TextEquiv> equivs =
        new ArrayList<>(List.of(new TextEquiv(null, null, null, null, null, null, "a")));
    Word word =
        new Word(
            "w", null, null, null, null, null, null, null, null, null, null, null, List.of(), BOX,
            List.of(), equivs, null, List.of(), List.of());
    equivs.clear();
    assertEquals(1, word.textEquivs().size());
    assertThrows(UnsupportedOperationException.class, () -> word.textEquivs().clear());
  }

  @Test
  void requiredComponentsMustBeSet() {
    assertThrows(NullPointerException.class, () -> WordBuilder.builder().id("w").build());
    assertThrows(NullPointerException.class, () -> WordBuilder.builder().coords(BOX).build());
  }

  @Test
  void buildersDefaultListsToEmptyAndAddSingleItems() {
    Word word =
        WordBuilder.builder()
            .id("w")
            .coords(BOX)
            .addTextEquivs(TextEquivBuilder.builder().unicode("a").build())
            .addTextEquivs(TextEquivBuilder.builder().unicode("b").index(1).build())
            .build();
    assertEquals(List.of(), word.glyphs());
    assertEquals(List.of("a", "b"), word.textEquivs().stream().map(TextEquiv::unicode).toList());
    assertNull(word.textEquivs().getFirst().index());
  }

  @Test
  void emptyAndAbsentPlainTextStayApart() {
    TextEquiv empty = TextEquivBuilder.builder().plainText("").unicode("").build();
    TextEquiv absent = TextEquivBuilder.builder().unicode("").build();
    assertEquals("", empty.plainText());
    assertNull(absent.plainText());
  }

  @Test
  void withersReturnChangedCopies() {
    TextRegion region = TextRegionBuilder.builder().id("r").coords(BOX).build();
    TextRegion turned = region.withOrientation(90.0);
    assertNull(region.orientation());
    assertEquals(90.0, turned.orientation());
    assertEquals(region, turned.withOrientation(null));
  }

  @Test
  void graphemeGroupsCannotBeNested() {
    Grapheme grapheme = GraphemeBuilder.builder().id("g").index(0).coords(BOX).build();
    GraphemeGroup inner =
        GraphemeGroupBuilder.builder().id("inner").index(0).addGraphemes(grapheme).build();
    assertThrows(
        IllegalArgumentException.class,
        () -> GraphemeGroupBuilder.builder().id("outer").index(0).addGraphemes(inner).build());
  }
}
