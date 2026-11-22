package com.maxnth.page4j.formats.internal;

import com.maxnth.page4j.geometry.Point;
import com.maxnth.page4j.geometry.Polygon;
import com.maxnth.page4j.geometry.Rect;
import com.maxnth.page4j.model.ChemRegion;
import com.maxnth.page4j.model.ImageRegion;
import com.maxnth.page4j.model.LineDrawingRegion;
import com.maxnth.page4j.model.MathsRegion;
import com.maxnth.page4j.model.Page;
import com.maxnth.page4j.model.Region;
import com.maxnth.page4j.model.SeparatorRegion;
import com.maxnth.page4j.model.TableRegion;
import com.maxnth.page4j.model.TextEquiv;
import com.maxnth.page4j.model.TextLine;
import com.maxnth.page4j.model.TextRegion;
import com.maxnth.page4j.model.Word;
import com.maxnth.page4j.model.value.Language;
import com.maxnth.page4j.model.value.ReadingDirection;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import javax.xml.XMLConstants;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.jspecify.annotations.Nullable;

public final class HocrWriter {

  public static final String NAMESPACE = "http://www.w3.org/1999/xhtml";
  private static final XMLOutputFactory FACTORY = XMLOutputFactory.newFactory();
  private static final String INDENT = " ";
  private static final String FORMAT = "hOCR";
  private static final String CAPABILITIES =
      "ocr_page ocr_carea ocr_par ocr_line ocrx_word ocr_photo ocr_separator ocr_table"
          + " ocr_linedrawing ocr_math ocr_chem ocr_float ocrp_lang ocrp_dir ocrp_wconf";

  private final Page page;
  private final PageText pageText;
  private final XMLStreamWriter out;
  private final Set<String> generated = new HashSet<>();
  private int depth;

  private HocrWriter(Page page, XMLStreamWriter out) {
    this.page = page;
    this.pageText = new PageText(page);
    this.out = out;
  }

  public static byte[] write(Page page) {
    var bytes = new ByteArrayOutputStream();
    try {
      XMLStreamWriter out = FACTORY.createXMLStreamWriter(bytes, "UTF-8");
      new HocrWriter(page, out).document();
      out.close();
    } catch (XMLStreamException e) {
      throw new IllegalStateException("Cannot write hOCR", e);
    }
    return bytes.toByteArray();
  }

  private void document() throws XMLStreamException {
    out.writeStartDocument("UTF-8", "1.0");
    out.writeCharacters("\n");
    out.writeDTD("<!DOCTYPE html>");
    out.writeCharacters("\n");
    out.setDefaultNamespace(NAMESPACE);
    out.writeStartElement(NAMESPACE, "html");
    out.writeDefaultNamespace(NAMESPACE);
    String language = languageCode(page.primaryLanguage());
    if (language != null) {
      out.writeAttribute("xml", XMLConstants.XML_NS_URI, "lang", language);
      out.writeAttribute("lang", language);
    }
    depth++;
    start("head");
    start("title");
    out.writeCharacters(XmlText.checked(FORMAT, "the image filename", page.imageFilename()));
    endInline();
    meta("http-equiv", "Content-Type", "text/html;charset=utf-8");
    if (!page.metadata().creator().isBlank()) {
      meta("name", "ocr-system", XmlText.checked(FORMAT, "the creator", page.metadata().creator()));
    }
    meta("name", "ocr-capabilities", CAPABILITIES);
    end();
    start("body");
    start("div");
    out.writeAttribute("class", "ocr_page");
    out.writeAttribute("id", page.pcGtsId() != null ? page.pcGtsId() : unique("page_1"));
    language(page.primaryLanguage());
    StringJoiner title = new StringJoiner("; ");
    title.add("image \"" + page.imageFilename().replace("\\", "\\\\").replace("\"", "\\\"") + "\"");
    title.add("bbox 0 0 " + page.imageWidth() + " " + page.imageHeight());
    title.add("ppageno 0");
    if (page.imageXResolution() != null && page.imageYResolution() != null) {
      title.add(
          "scan_res "
              + Math.round(page.imageXResolution())
              + " "
              + Math.round(page.imageYResolution()));
    }
    out.writeAttribute("title", title.toString());
    for (Region region : RegionOrder.flattened(page)) {
      region(region);
    }
    end();
    end();
    depth--;
    out.writeCharacters("\n");
    out.writeEndElement();
    out.writeEndDocument();
    out.writeCharacters("\n");
  }

  private void meta(String kind, String name, String content) throws XMLStreamException {
    empty("meta");
    out.writeAttribute(kind, name);
    out.writeAttribute("content", content);
  }

  private void region(Region region) throws XMLStreamException {
    if (region instanceof TextRegion text) {
      textRegion(text);
      return;
    }
    if (region.coords().isEmpty()) {
      return;
    }
    start("div");
    out.writeAttribute("class", hocrClass(region));
    out.writeAttribute("id", region.id());
    out.writeAttribute("title", "bbox " + bbox(region.coords().boundingBox()));
    endInline();
  }

  private static String hocrClass(Region region) {
    return switch (region) {
      case ImageRegion r -> "ocr_photo";
      case SeparatorRegion r -> "ocr_separator";
      case TableRegion r -> "ocr_table";
      case MathsRegion r -> "ocr_math";
      case ChemRegion r -> "ocr_chem";
      case LineDrawingRegion r -> "ocr_linedrawing";
      default -> "ocr_float";
    };
  }

  private void textRegion(TextRegion region) throws XMLStreamException {
    Rect box = box(region.coords(), region.textLines().stream().map(TextLine::coords).toList());
    if (box == null) {
      return;
    }
    start("div");
    out.writeAttribute("class", "ocr_carea");
    out.writeAttribute("id", unique("block_" + region.id()));
    out.writeAttribute("title", "bbox " + bbox(box));
    start("p");
    out.writeAttribute("class", "ocr_par");
    out.writeAttribute("id", region.id());
    language(region.primaryLanguage());
    direction(region.readingDirection());
    out.writeAttribute("title", "bbox " + bbox(box));
    for (TextLine line : region.textLines()) {
      line(line);
    }
    if (region.textLines().isEmpty()) {
      endInline();
    } else {
      end();
    }
    end();
  }

  private void line(TextLine line) throws XMLStreamException {
    Rect box = box(line.coords(), line.words().stream().map(Word::coords).toList());
    start("span");
    out.writeAttribute("class", "ocr_line");
    out.writeAttribute("id", line.id());
    language(line.primaryLanguage());
    direction(line.readingDirection());
    StringJoiner title = new StringJoiner("; ");
    if (box != null) {
      title.add("bbox " + bbox(box));
      String baseline = baseline(line.baseline(), box);
      if (baseline != null) {
        title.add("baseline " + baseline);
      }
    }
    if (line.orientation() != null && line.orientation() != 0) {
      title.add("textangle " + number(line.orientation(), 2));
    }
    if (title.length() > 0) {
      out.writeAttribute("title", title.toString());
    }
    List<Word> words = line.words().stream().filter(w -> !pageText.word(w).isEmpty()).toList();
    if (words.isEmpty()
        || words.stream().anyMatch(w -> w.coords().isEmpty())
        || !pageText.wordsSpell(line)) {
      out.writeCharacters(
          XmlText.checked(FORMAT, "TextLine '" + line.id() + "'", pageText.line(line)));
      endInline();
      return;
    }
    for (int i = 0; i < words.size(); i++) {
      if (i > 0) {
        out.writeCharacters(" ");
      }
      word(words.get(i));
    }
    end();
  }

  private void word(Word word) throws XMLStreamException {
    start("span");
    out.writeAttribute("class", "ocrx_word");
    out.writeAttribute("id", word.id());
    language(word.language());
    StringJoiner title = new StringJoiner("; ");
    if (!word.coords().isEmpty()) {
      title.add("bbox " + bbox(word.coords().boundingBox()));
    }
    TextEquiv main = PageText.main(word.textEquivs());
    if (main != null && main.conf() != null) {
      title.add("x_wconf " + Math.round(main.conf() * 100));
    }
    if (title.length() > 0) {
      out.writeAttribute("title", title.toString());
    }
    out.writeCharacters(XmlText.checked(FORMAT, "Word '" + word.id() + "'", pageText.word(word)));
    endInline();
  }

  private static @Nullable String baseline(@Nullable Polygon baseline, Rect box) {
    if (baseline == null || baseline.points().size() < 2) {
      return null;
    }
    Point first = baseline.points().getFirst();
    Point last = baseline.points().getLast();
    int dx = last.x() - first.x();
    double slope = dx == 0 ? 0 : (double) (last.y() - first.y()) / dx;
    double yAtLeft = first.y() + slope * (box.left() - first.x());
    return number(slope, 3) + " " + Math.round(yAtLeft - box.bottom());
  }

  private static String number(double value, int scale) {
    BigDecimal rounded =
        BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).stripTrailingZeros();
    return rounded.signum() == 0 ? "0" : rounded.toPlainString();
  }

  private static String bbox(Rect box) {
    return box.left() + " " + box.top() + " " + box.right() + " " + box.bottom();
  }

  private static @Nullable Rect box(Polygon coords, List<Polygon> children) {
    if (!coords.isEmpty()) {
      return coords.boundingBox();
    }
    Polygon.Builder corners = Polygon.builder();
    for (Polygon child : children) {
      if (!child.isEmpty()) {
        Rect box = child.boundingBox();
        corners.add(box.left(), box.top()).add(box.right(), box.bottom());
      }
    }
    return corners.size() == 0 ? null : corners.build().boundingBox();
  }

  private void language(@Nullable Language language) throws XMLStreamException {
    String code = languageCode(language);
    if (code != null) {
      out.writeAttribute("lang", code);
    }
  }

  private static @Nullable String languageCode(@Nullable Language language) {
    return language == null ? null : Languages.toCode(language);
  }

  private void direction(@Nullable ReadingDirection direction) throws XMLStreamException {
    if (direction == ReadingDirection.LEFT_TO_RIGHT) {
      out.writeAttribute("dir", "ltr");
    } else if (direction == ReadingDirection.RIGHT_TO_LEFT) {
      out.writeAttribute("dir", "rtl");
    }
  }

  private String unique(String base) {
    String id = base;
    for (int i = 2;
        page.find(id).isPresent() || id.equals(page.pcGtsId()) || generated.contains(id);
        i++) {
      id = base + "_" + i;
    }
    generated.add(id);
    return id;
  }

  private void start(String name) throws XMLStreamException {
    out.writeCharacters("\n" + INDENT.repeat(depth));
    out.writeStartElement(NAMESPACE, name);
    depth++;
  }

  private void empty(String name) throws XMLStreamException {
    out.writeCharacters("\n" + INDENT.repeat(depth));
    out.writeEmptyElement(NAMESPACE, name);
  }

  private void endInline() throws XMLStreamException {
    depth--;
    out.writeEndElement();
  }

  private void end() throws XMLStreamException {
    depth--;
    out.writeCharacters("\n" + INDENT.repeat(depth));
    out.writeEndElement();
  }
}
