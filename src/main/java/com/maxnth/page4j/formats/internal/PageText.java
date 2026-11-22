package com.maxnth.page4j.formats.internal;

import com.maxnth.page4j.geometry.Polygon;
import com.maxnth.page4j.geometry.Rect;
import com.maxnth.page4j.model.Glyph;
import com.maxnth.page4j.model.Page;
import com.maxnth.page4j.model.TextEquiv;
import com.maxnth.page4j.model.TextLine;
import com.maxnth.page4j.model.TextRegion;
import com.maxnth.page4j.model.Word;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

final class PageText {

  private final Map<String, String> propagatedLines = new HashMap<>();
  private final Map<String, String> propagatedWords = new HashMap<>();
  private final Map<String, String> propagatedGlyphs = new HashMap<>();

  PageText(Page page) {
    for (TextRegion region : page.all(TextRegion.class)) {
      if (!region.textLines().isEmpty()) {
        propagate(region);
      }
    }
  }

  private void propagate(TextRegion region) {
    String regionText = orEmpty(text(region.textEquivs()));
    if (regionText.isEmpty()) {
      regionText = String.join("\n", region.textLines().stream().map(PageText::lineText).toList());
    }
    String[] regionLines = regionText.split("\r\n|\n");
    List<TextLine> lines = sorted(region.textLines(), TextLine::coords, true);
    for (int t = 0; t < lines.size(); t++) {
      TextLine line = lines.get(t);
      String lineText = lineText(line);
      if (lineText.isEmpty() && t < regionLines.length) {
        lineText = regionLines[t];
        propagatedLines.put(line.id(), lineText);
      }
      String[] lineWords = lineText.split(" ");
      List<Word> words = sorted(line.words(), Word::coords, false);
      for (int w = 0; w < words.size(); w++) {
        Word word = words.get(w);
        String propagated = "";
        if (w < lineWords.length) {
          propagated = lineWords[w];
          propagatedWords.put(word.id(), propagated);
        }
        String wordText = wordText(word);
        if (wordText.isEmpty()) {
          wordText = propagated;
        }
        int[] characters = wordText.codePoints().toArray();
        List<Glyph> glyphs = sorted(word.glyphs(), Glyph::coords, false);
        for (int g = 0; g < glyphs.size() && g < characters.length; g++) {
          propagatedGlyphs.put(glyphs.get(g).id(), Character.toString(characters[g]));
        }
      }
    }
  }

  String propagatedLine(TextLine line) {
    return propagatedLines.getOrDefault(line.id(), "");
  }

  String propagatedWord(Word word) {
    return propagatedWords.getOrDefault(word.id(), "");
  }

  String propagatedGlyph(Glyph glyph) {
    return propagatedGlyphs.getOrDefault(glyph.id(), "");
  }

  String line(TextLine line) {
    String text = text(line.textEquivs());
    return text != null ? text : propagatedLine(line);
  }

  String word(Word word) {
    String text = wordText(word);
    return text.isEmpty() ? propagatedWord(word) : text;
  }

  String glyph(Glyph glyph) {
    String text = orEmpty(text(glyph.textEquivs()));
    return text.isEmpty() ? propagatedGlyph(glyph) : text;
  }

  boolean wordsSpell(TextLine line) {
    String own = text(line.textEquivs());
    if (own == null || own.isBlank()) {
      return true;
    }
    String words =
        String.join(
            " ", line.words().stream().map(this::word).filter(text -> !text.isEmpty()).toList());
    return normalized(own).equals(normalized(words));
  }

  private static String normalized(String text) {
    return text.strip().replaceAll("\\s+", " ");
  }

  static @Nullable TextEquiv main(List<TextEquiv> equivs) {
    return equivs.stream()
        .min(
            Comparator.comparing(
                TextEquiv::index, Comparator.nullsFirst(Comparator.naturalOrder())))
        .orElse(null);
  }

  static List<TextEquiv> others(List<TextEquiv> equivs) {
    TextEquiv main = main(equivs);
    return equivs.stream()
        .filter(e -> e != main)
        .sorted(
            Comparator.comparing(
                TextEquiv::index, Comparator.nullsFirst(Comparator.naturalOrder())))
        .toList();
  }

  static @Nullable String text(List<TextEquiv> equivs) {
    TextEquiv main = main(equivs);
    return main == null ? null : main.unicode();
  }

  static String orEmpty(@Nullable String text) {
    return text == null ? "" : text;
  }

  static String lineText(TextLine line) {
    String text = orEmpty(text(line.textEquivs()));
    return text.isEmpty()
        ? String.join(" ", line.words().stream().map(PageText::wordText).toList()).strip()
        : text;
  }

  static String wordText(Word word) {
    String text = orEmpty(text(word.textEquivs()));
    return text.isEmpty()
        ? String.join("", word.glyphs().stream().map(g -> orEmpty(text(g.textEquivs()))).toList())
        : text;
  }

  static <T> List<T> sorted(List<T> objects, Function<T, Polygon> coords, boolean vertical) {
    return objects.stream()
        .sorted(
            Comparator.comparingInt(
                o -> {
                  Polygon polygon = coords.apply(o);
                  if (polygon.isEmpty()) {
                    return 0;
                  }
                  Rect box = polygon.boundingBox();
                  return vertical ? (box.top() + box.bottom()) / 2 : (box.left() + box.right()) / 2;
                }))
        .toList();
  }
}
