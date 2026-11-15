package com.maxnth.page4j.formats.internal;

import com.maxnth.page4j.formats.ReadResult;
import com.maxnth.page4j.geometry.Point;
import com.maxnth.page4j.geometry.Polygon;
import com.maxnth.page4j.geometry.Rect;
import com.maxnth.page4j.model.ImageRegionBuilder;
import com.maxnth.page4j.model.InvalidPageException;
import com.maxnth.page4j.model.MetadataBuilder;
import com.maxnth.page4j.model.Page;
import com.maxnth.page4j.model.Region;
import com.maxnth.page4j.model.SeparatorRegionBuilder;
import com.maxnth.page4j.model.TextEquivBuilder;
import com.maxnth.page4j.model.TextLine;
import com.maxnth.page4j.model.TextLineBuilder;
import com.maxnth.page4j.model.TextRegionBuilder;
import com.maxnth.page4j.model.Word;
import com.maxnth.page4j.model.WordBuilder;
import com.maxnth.page4j.model.value.Language;
import com.maxnth.page4j.model.value.PageDateTime;
import com.maxnth.page4j.model.value.ReadingDirection;
import com.maxnth.page4j.model.value.ResolutionUnit;
import com.maxnth.page4j.pagexml.PageReadException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.jspecify.annotations.Nullable;

/**
 * Reads hOCR (well-formed XHTML) by recursive descent over all elements; the hOCR class of an
 * element decides what it becomes. Only the first page is read.
 */
public final class HocrReader {

  private static final Set<String> LINES =
      Set.of("ocr_line", "ocr_textfloat", "ocr_header", "ocr_caption");

  /** The properties in an hOCR {@code title}, recording which were read. */
  private static final class Title {
    private final Map<String, String> properties = new LinkedHashMap<>();
    private final Set<String> read = new HashSet<>();
    private final Attrs at;

    Title(@Nullable String title, Attrs at) {
      this.at = at;
      if (title != null) {
        for (String part : title.split(";")) {
          String property = part.strip();
          if (!property.isEmpty()) {
            int space = property.indexOf(' ');
            properties.put(
                space < 0 ? property : property.substring(0, space),
                space < 0 ? "" : property.substring(space + 1).strip());
          }
        }
      }
    }

    @Nullable String get(String name) {
      read.add(name);
      return properties.get(name);
    }

    /** The numbers of a property, or null if it is absent or not as many numbers. */
    double @Nullable [] numbers(String name, int count) {
      String value = get(name);
      if (value == null) {
        return null;
      }
      String[] parts = value.split("\\s+");
      if (parts.length == count) {
        try {
          double[] numbers = new double[count];
          for (int i = 0; i < count; i++) {
            numbers[i] = Double.parseDouble(parts[i]);
          }
          return numbers;
        } catch (NumberFormatException e) {
          // reported below
        }
      }
      at.ignored(at.element() + ": title property " + name + " '" + value + "' is not valid");
      return null;
    }

    @Nullable Rect bbox() {
      double[] box = numbers("bbox", 4);
      return box == null ? null : new Rect((int) box[0], (int) box[1], (int) box[2], (int) box[3]);
    }

    void done(String hocrClass) {
      for (String name : properties.keySet()) {
        if (!read.contains(name)) {
          at.ignored(hocrClass + ": title property " + name);
        }
      }
    }
  }

  /** A text region being read (from a paragraph or an area), collecting its lines. */
  private static final class Container {
    final List<TextLine> lines = new ArrayList<>();
  }

  private final XMLStreamReader r;
  private final Diagnostics diagnostics = new Diagnostics();
  private final Ids ids;

  private String creator = "page4j";
  private final List<String> comments = new ArrayList<>();
  private boolean pageRead;
  private @Nullable String pcGtsId;
  private String imageFilename = "";
  private int width;
  private int height;
  private @Nullable Double xResolution;
  private @Nullable Double yResolution;
  private final List<Region> regions = new ArrayList<>();

  private HocrReader(XMLStreamReader r, Set<String> taken) {
    this.r = r;
    this.ids = new Ids(taken);
  }

  /**
   * @throws PageReadException if the document is not well-formed XML or not HTML
   */
  public static ReadResult read(byte[] xml) throws PageReadException {
    Set<String> taken = XmlInput.attributeValues(xml, "id");
    XMLStreamReader r = XmlInput.open(xml);
    try {
      XmlInput.requireRoot(r, "html", "hOCR");
      HocrReader reader = new HocrReader(r, taken);
      reader.children(null, null);
      return reader.result();
    } catch (XMLStreamException e) {
      throw XmlInput.notWellFormed(e);
    } catch (XmlInput.Failure e) {
      throw e.toException();
    } catch (InvalidPageException e) {
      throw new PageReadException(-1, -1, "Cannot build the page: " + e.getMessage(), e);
    } finally {
      XmlInput.close(r);
    }
  }

  private ReadResult result() {
    PageDateTime now = PageDateTime.now();
    Page page =
        Page.builder()
            .pcGtsId(pcGtsId)
            .metadata(
                MetadataBuilder.builder()
                    .creator(creator)
                    .created(now)
                    .lastChange(now)
                    .comments(comments.isEmpty() ? null : String.join("\n", comments))
                    .build())
            .imageFilename(imageFilename)
            .imageWidth(width)
            .imageHeight(height)
            .imageXResolution(xResolution)
            .imageYResolution(yResolution)
            .imageResolutionUnit(xResolution == null ? null : ResolutionUnit.PPI)
            .regions(regions)
            .build();
    return new ReadResult(page, diagnostics.list());
  }

  /**
   * Reads the children of the current element, up to its end tag, appending text outside elements
   * to {@code text} if given.
   */
  private void children(@Nullable Container container, @Nullable StringBuilder text)
      throws XMLStreamException {
    while (true) {
      switch (r.next()) {
        case XMLStreamConstants.START_ELEMENT -> element(container);
        case XMLStreamConstants.END_ELEMENT, XMLStreamConstants.END_DOCUMENT -> {
          return;
        }
        case XMLStreamConstants.CHARACTERS, XMLStreamConstants.CDATA, XMLStreamConstants.SPACE -> {
          if (text != null) {
            text.append(r.getText());
          }
        }
        default -> {
          // comments, processing instructions
        }
      }
    }
  }

  private void element(@Nullable Container container) throws XMLStreamException {
    if (r.getLocalName().equals("meta")) {
      meta();
      return;
    }
    String hocrClass = hocrClass(r.getAttributeValue(null, "class"));
    if (hocrClass == null) {
      children(container, null);
      return;
    }
    Attrs a = new Attrs(r, diagnostics);
    a.string("class");
    Title title = new Title(a.string("title"), a);
    switch (hocrClass) {
      case "ocr_page" -> page(a, title, container);
      case "ocr_carea" -> {
        var area = new Container();
        Region region = textRegion(a, title, area, "ocr_carea");
        if (!area.lines.isEmpty() && region != null) {
          regions.add(region);
        }
      }
      case "ocr_par" -> {
        Region region = textRegion(a, title, new Container(), "ocr_par");
        if (region != null) {
          regions.add(region);
        }
      }
      case "ocrx_word" -> {
        a.ignored(hocrClass + " outside a line ignored");
        XmlInput.skip(r);
      }
      case "ocr_photo", "ocr_image", "ocr_separator" -> {
        String id = ids.id(a.string("id"), "r", a);
        Rect box = title.bbox();
        children(null, null);
        title.done(hocrClass);
        a.done();
        if (box == null) {
          a.ignored(hocrClass + " without bbox ignored");
        } else if (hocrClass.equals("ocr_separator")) {
          regions.add(SeparatorRegionBuilder.builder().id(id).coords(box.toPolygon()).build());
        } else {
          regions.add(ImageRegionBuilder.builder().id(id).coords(box.toPolygon()).build());
        }
      }
      default -> {
        if (LINES.contains(hocrClass)) {
          if (container == null) {
            a.ignored(hocrClass + " outside a paragraph or area ignored");
            XmlInput.skip(r);
          } else {
            container.lines.add(line(a, title, hocrClass));
          }
        } else {
          a.ignored("hOCR class " + hocrClass + " ignored, its content is read");
          children(container, null);
        }
      }
    }
  }

  /** The first hOCR class ({@code ocr_…} or {@code ocrx_…}) of a class attribute. */
  private static @Nullable String hocrClass(@Nullable String classes) {
    if (classes == null) {
      return null;
    }
    for (String token : classes.strip().split("\\s+")) {
      if (token.startsWith("ocr")) {
        return token;
      }
    }
    return null;
  }

  private void meta() throws XMLStreamException {
    String name = r.getAttributeValue(null, "name");
    String content = r.getAttributeValue(null, "content");
    if (name != null && content != null && !content.isEmpty()) {
      comments.add(name + ": " + content);
      if (name.equals("ocr-system")) {
        creator = content;
      }
    }
    XmlInput.skip(r);
  }

  private void page(Attrs a, Title title, @Nullable Container container) throws XMLStreamException {
    if (pageRead) {
      a.ignored("ocr_page ignored, only the first page is read");
      XmlInput.skip(r);
      return;
    }
    pageRead = true;
    String id = a.string("id");
    if (id != null) {
      if (ids.use(id)) {
        pcGtsId = id;
      } else {
        a.ignored("ocr_page: id '" + id + "' is not a valid or unique ID");
      }
    }
    String image = title.get("image");
    if (image != null) {
      String file = image.strip();
      if (file.startsWith("\"") && file.endsWith("\"") && file.length() >= 2) {
        file = file.substring(1, file.length() - 1);
      }
      imageFilename = file.substring(Math.max(file.lastIndexOf('/'), file.lastIndexOf('\\')) + 1);
    }
    Rect box = title.bbox();
    if (box != null) {
      // The old reader's reading: x1 and y1 are the width and height.
      width = box.right();
      height = box.bottom();
    }
    double[] resolution = title.numbers("scan_res", 2);
    if (resolution != null) {
      xResolution = resolution[0];
      yResolution = resolution[1];
    }
    title.get("ppageno");
    title.done("ocr_page");
    a.done();
    children(container, null);
  }

  /** A paragraph or area with its lines, or null if it has neither lines nor a box. */
  private @Nullable Region textRegion(Attrs a, Title title, Container area, String hocrClass)
      throws XMLStreamException {
    String id = ids.id(a.string("id"), "r", a);
    Rect box = title.bbox();
    Language language = language(a);
    ReadingDirection direction = direction(a);
    title.done(hocrClass);
    a.done();
    children(area, null);
    Polygon coords = box != null ? box.toPolygon() : outline(area.lines);
    if (coords == null) {
      return null;
    }
    var region =
        TextRegionBuilder.builder()
            .id(id)
            .coords(coords)
            .primaryLanguage(language)
            .readingDirection(direction)
            .textLines(area.lines);
    String text = String.join("\n", area.lines.stream().map(HocrReader::text).toList());
    if (!text.isEmpty()) {
      region.addTextEquivs(TextEquivBuilder.builder().unicode(text).build());
    }
    return region.build();
  }

  private static @Nullable Polygon outline(List<TextLine> lines) {
    Polygon.Builder corners = Polygon.builder();
    for (TextLine line : lines) {
      if (!line.coords().isEmpty()) {
        Rect box = line.coords().boundingBox();
        corners.add(box.left(), box.top()).add(box.right(), box.bottom());
      }
    }
    return corners.size() == 0 ? null : corners.build().boundingBox().toPolygon();
  }

  private static String text(TextLine line) {
    return line.textEquivs().isEmpty() ? "" : line.textEquivs().getFirst().unicode();
  }

  private @Nullable Language language(Attrs a) {
    String lang = a.string("lang");
    return lang == null ? null : Languages.read(lang, a, "lang");
  }

  private static @Nullable ReadingDirection direction(Attrs a) {
    String dir = a.string("dir");
    if (dir == null) {
      return null;
    }
    return switch (dir.strip()) {
      case "ltr" -> ReadingDirection.LEFT_TO_RIGHT;
      case "rtl" -> ReadingDirection.RIGHT_TO_LEFT;
      default -> {
        a.ignored(a.element() + ": dir '" + dir + "'");
        yield null;
      }
    };
  }

  private TextLine line(Attrs a, Title title, String hocrClass) throws XMLStreamException {
    String id = ids.id(a.string("id"), "l", a);
    Rect box = title.bbox();
    Language language = language(a);
    ReadingDirection direction = direction(a);
    Polygon baseline = null;
    double[] slopeOffset = title.numbers("baseline", 2);
    if (slopeOffset != null && box != null) {
      // Relative to the bottom left corner of the box.
      double slope = slopeOffset[0];
      double offset = slopeOffset[1];
      int width = box.right() - box.left();
      baseline =
          Polygon.of(
              new Point(box.left(), (int) Math.round(box.bottom() + offset)),
              new Point(box.right(), (int) Math.round(box.bottom() + offset + slope * width)));
    }
    double[] angle = title.numbers("textangle", 1);
    title.done(hocrClass);
    a.done();
    var container = new LineContent();
    var ownText = new StringBuilder();
    lineChildren(container, ownText);
    var line =
        TextLineBuilder.builder()
            .id(id)
            .coords(box != null ? box.toPolygon() : Polygon.EMPTY)
            .baseline(baseline)
            .orientation(angle == null ? null : orientation(angle[0]))
            .primaryLanguage(language)
            .readingDirection(direction)
            .words(container.words);
    String text =
        container.words.isEmpty()
            ? ownText.toString().strip().replaceAll("\\s+", " ")
            : String.join(
                " ",
                container.words.stream()
                    .map(w -> w.textEquivs().isEmpty() ? "" : w.textEquivs().getFirst().unicode())
                    .toList());
    if (!text.isEmpty()) {
      line.addTextEquivs(TextEquivBuilder.builder().unicode(text).build());
    }
    return line.build();
  }

  private static double orientation(double degrees) {
    double angle = degrees % 360;
    if (angle > 180) {
      return angle - 360;
    }
    return angle <= -180 ? angle + 360 : angle;
  }

  private static final class LineContent {
    final List<Word> words = new ArrayList<>();
  }

  /** The content of a line: words, and text outside them. */
  private void lineChildren(LineContent line, StringBuilder text) throws XMLStreamException {
    while (true) {
      switch (r.next()) {
        case XMLStreamConstants.START_ELEMENT -> {
          String hocrClass = hocrClass(r.getAttributeValue(null, "class"));
          if ("ocrx_word".equals(hocrClass)) {
            line.words.add(word());
          } else {
            if (hocrClass != null) {
              new Attrs(r, diagnostics).ignored("hOCR class " + hocrClass + " in a line ignored");
            }
            lineChildren(line, text);
          }
        }
        case XMLStreamConstants.END_ELEMENT -> {
          return;
        }
        case XMLStreamConstants.CHARACTERS, XMLStreamConstants.CDATA, XMLStreamConstants.SPACE ->
            text.append(r.getText());
        default -> {
          // comments, processing instructions
        }
      }
    }
  }

  private Word word() throws XMLStreamException {
    Attrs a = new Attrs(r, diagnostics);
    a.string("class");
    String id = ids.id(a.string("id"), "w", a);
    Title title = new Title(a.string("title"), a);
    Rect box = title.bbox();
    double[] confidence = title.numbers("x_wconf", 1);
    String lang = a.string("lang");
    Language language = lang == null ? null : Languages.read(lang, a, "lang");
    title.done("ocrx_word");
    a.done();
    String text = XmlInput.text(r).strip();
    var word =
        WordBuilder.builder()
            .id(id)
            .coords(box != null ? box.toPolygon() : Polygon.EMPTY)
            .language(language);
    if (!text.isEmpty()) {
      word.addTextEquivs(
          TextEquivBuilder.builder()
              .unicode(text)
              .conf(confidence == null ? null : Math.clamp(confidence[0] / 100, 0.0, 1.0))
              .build());
    }
    return word.build();
  }
}
