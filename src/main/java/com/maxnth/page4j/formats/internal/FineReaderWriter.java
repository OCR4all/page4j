package com.maxnth.page4j.formats.internal;

import com.maxnth.page4j.geometry.Point;
import com.maxnth.page4j.geometry.Polygon;
import com.maxnth.page4j.geometry.Rect;
import com.maxnth.page4j.model.Glyph;
import com.maxnth.page4j.model.GraphicRegion;
import com.maxnth.page4j.model.Page;
import com.maxnth.page4j.model.Region;
import com.maxnth.page4j.model.SeparatorRegion;
import com.maxnth.page4j.model.TableRegion;
import com.maxnth.page4j.model.TextEquiv;
import com.maxnth.page4j.model.TextLine;
import com.maxnth.page4j.model.TextRegion;
import com.maxnth.page4j.model.Word;
import com.maxnth.page4j.model.value.GraphicsType;
import com.maxnth.page4j.model.value.Language;
import com.maxnth.page4j.model.value.ResolutionUnit;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.jspecify.annotations.Nullable;

public final class FineReaderWriter {

  public static final String NAMESPACE =
      "http://www.abbyy.com/FineReader_xml/FineReader10-schema-v1.xml";
  private static final XMLOutputFactory FACTORY = XMLOutputFactory.newFactory();
  private static final String INDENT = "  ";
  private static final String FORMAT = "FineReader XML";

  private record CharBox(String text, Rect box, @Nullable Double conf, boolean wordStart) {}

  private final Page page;
  private final PageText pageText;
  private final XMLStreamWriter out;
  private int depth;

  private FineReaderWriter(Page page, XMLStreamWriter out) {
    this.page = page;
    this.pageText = new PageText(page);
    this.out = out;
  }

  public static byte[] write(Page page) {
    var bytes = new ByteArrayOutputStream();
    try {
      XMLStreamWriter out = FACTORY.createXMLStreamWriter(bytes, "UTF-8");
      new FineReaderWriter(page, out).document();
      out.close();
    } catch (XMLStreamException e) {
      throw new IllegalStateException("Cannot write FineReader XML", e);
    }
    return bytes.toByteArray();
  }

  private void document() throws XMLStreamException {
    out.writeStartDocument("UTF-8", "1.0");
    out.writeCharacters("\n");
    out.setDefaultNamespace(NAMESPACE);
    out.writeStartElement(NAMESPACE, "document");
    out.writeDefaultNamespace(NAMESPACE);
    out.writeNamespace("xsi", XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI);
    out.writeAttribute(
        "xsi",
        XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI,
        "schemaLocation",
        NAMESPACE + " " + NAMESPACE);
    out.writeAttribute("version", "1.0");
    out.writeAttribute(
        "producer", XmlText.checked(FORMAT, "the creator", page.metadata().creator()));
    out.writeAttribute("pagesCount", "1");
    attribute("mainLanguage", languageName(page.primaryLanguage()));
    depth++;
    start("page");
    out.writeAttribute("width", Integer.toString(page.imageWidth()));
    out.writeAttribute("height", Integer.toString(page.imageHeight()));
    Long resolution = resolution();
    if (resolution != null) {
      out.writeAttribute("resolution", Long.toString(resolution));
    }
    out.writeAttribute("originalCoords", "1");
    for (Region region : RegionOrder.flattened(page)) {
      block(region);
    }
    end();
    depth--;
    out.writeCharacters("\n");
    out.writeEndElement();
    out.writeEndDocument();
    out.writeCharacters("\n");
  }

  private @Nullable Long resolution() {
    Double x = page.imageXResolution();
    if (x == null || x <= 0) {
      return null;
    }
    ResolutionUnit unit = page.imageResolutionUnit();
    if (unit == ResolutionUnit.PPCM) {
      return Math.round(x * 2.54);
    }
    return unit == ResolutionUnit.OTHER ? null : Math.round(x);
  }

  private void block(Region region) throws XMLStreamException {
    List<TextLine> lines = region instanceof TextRegion text ? text.textLines() : List.of();
    Rect box = box(region.coords(), lines.stream().map(TextLine::coords).toList());
    if (box == null) {
      return;
    }
    start("block");
    out.writeAttribute("blockType", blockType(region));
    rect(box);
    start("region");
    empty("rect");
    rect(box);
    end();
    if (region instanceof TextRegion text && !lines.isEmpty()) {
      start("text");
      start("par");
      attribute("align", align(text));
      Language language =
          text.primaryLanguage() != null ? text.primaryLanguage() : page.primaryLanguage();
      for (TextLine line : lines) {
        line(line, language);
      }
      end();
      end();
    }
    end();
  }

  private static String blockType(Region region) {
    return switch (region) {
      case TextRegion r -> "Text";
      case TableRegion r -> "Table";
      case SeparatorRegion r -> "Separator";
      case GraphicRegion r when r.type() == GraphicsType.BARCODE -> "Barcode";
      default -> "Picture";
    };
  }

  private static @Nullable String align(TextRegion region) {
    if (region.align() == null) {
      return null;
    }
    return switch (region.align()) {
      case LEFT -> "Left";
      case CENTRE -> "Center";
      case RIGHT -> "Right";
      case JUSTIFY -> "Justified";
    };
  }

  private void line(TextLine line, @Nullable Language regionLanguage) throws XMLStreamException {
    Rect box = box(line.coords(), line.words().stream().map(Word::coords).toList());
    if (box == null) {
      return;
    }
    start("line");
    out.writeAttribute("baseline", Integer.toString(baseline(line, box)));
    rect(box);
    start("formatting");
    Language language = line.primaryLanguage() != null ? line.primaryLanguage() : regionLanguage;
    attribute("lang", languageName(language));
    List<CharBox> characters = characters(line, box);
    if (characters.isEmpty()) {
      out.writeCharacters(
          XmlText.checked(FORMAT, "TextLine '" + line.id() + "'", pageText.line(line)));
    } else {
      for (CharBox character : characters) {
        start("charParams");
        rect(character.box());
        out.writeAttribute("wordStart", character.wordStart() ? "1" : "0");
        if (character.conf() != null) {
          out.writeAttribute("charConfidence", Long.toString(Math.round(character.conf() * 100)));
        }
        out.writeCharacters(character.text());
        out.writeEndElement();
        depth--;
      }
    }
    if (characters.isEmpty()) {
      out.writeEndElement();
      depth--;
    } else {
      end();
    }
    end();
  }

  private static int baseline(TextLine line, Rect box) {
    Polygon baseline = line.baseline();
    if (baseline == null || baseline.isEmpty()) {
      return box.bottom();
    }
    double sum = 0;
    for (Point point : baseline.points()) {
      sum += point.y();
    }
    return (int) Math.round(sum / baseline.points().size());
  }

  private List<CharBox> characters(TextLine line, Rect lineBox) {
    List<Word> words = line.words();
    if (words.isEmpty()
        || words.stream().anyMatch(w -> w.coords().isEmpty())
        || !pageText.wordsSpell(line)) {
      return List.of();
    }
    List<CharBox> characters = new ArrayList<>();
    Rect previous = null;
    for (Word word : words) {
      String text = XmlText.checked(FORMAT, "Word '" + word.id() + "'", pageText.word(word));
      if (text.isEmpty()) {
        continue;
      }
      Rect box = word.coords().boundingBox();
      if (previous != null) {
        int left = Math.min(previous.right() + 1, box.left());
        int right = Math.max(left, box.left() - 1);
        characters.add(
            new CharBox(" ", new Rect(left, lineBox.top(), right, lineBox.bottom()), null, false));
      }
      characters.addAll(wordCharacters(word, text, box));
      previous = box;
    }
    return characters;
  }

  private List<CharBox> wordCharacters(Word word, String text, Rect box) {
    int[] codePoints = text.codePoints().toArray();
    TextEquiv main = PageText.main(word.textEquivs());
    Double wordConf = main == null ? null : main.conf();
    List<Glyph> glyphs = word.glyphs();
    boolean useGlyphs =
        glyphs.size() == codePoints.length && glyphs.stream().noneMatch(g -> g.coords().isEmpty());
    List<CharBox> characters = new ArrayList<>();
    for (int i = 0; i < codePoints.length; i++) {
      String character = Character.toString(codePoints[i]);
      if (useGlyphs) {
        Glyph glyph = glyphs.get(i);
        TextEquiv glyphMain = PageText.main(glyph.textEquivs());
        Double conf = glyphMain != null && glyphMain.conf() != null ? glyphMain.conf() : wordConf;
        characters.add(new CharBox(character, glyph.coords().boundingBox(), conf, i == 0));
      } else {
        int width = box.right() - box.left() + 1;
        int left = box.left() + i * width / codePoints.length;
        int right = Math.max(left, box.left() + (i + 1) * width / codePoints.length - 1);
        characters.add(
            new CharBox(
                character, new Rect(left, box.top(), right, box.bottom()), wordConf, i == 0));
      }
    }
    return characters;
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

  private static @Nullable String languageName(@Nullable Language language) {
    return language == null ? null : language.xmlValue();
  }

  private void rect(Rect box) throws XMLStreamException {
    out.writeAttribute("l", Integer.toString(box.left()));
    out.writeAttribute("t", Integer.toString(box.top()));
    out.writeAttribute("r", Integer.toString(box.right()));
    out.writeAttribute("b", Integer.toString(box.bottom()));
  }

  private void attribute(String name, @Nullable String value) throws XMLStreamException {
    if (value != null) {
      out.writeAttribute(name, value);
    }
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

  private void end() throws XMLStreamException {
    depth--;
    out.writeCharacters("\n" + INDENT.repeat(depth));
    out.writeEndElement();
  }
}
