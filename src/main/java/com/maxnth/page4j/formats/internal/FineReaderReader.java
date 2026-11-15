package com.maxnth.page4j.formats.internal;

import com.maxnth.page4j.formats.ReadResult;
import com.maxnth.page4j.geometry.Point;
import com.maxnth.page4j.geometry.Polygon;
import com.maxnth.page4j.geometry.Rect;
import com.maxnth.page4j.model.GraphicRegionBuilder;
import com.maxnth.page4j.model.ImageRegionBuilder;
import com.maxnth.page4j.model.InvalidPageException;
import com.maxnth.page4j.model.MetadataBuilder;
import com.maxnth.page4j.model.Page;
import com.maxnth.page4j.model.Region;
import com.maxnth.page4j.model.SeparatorRegionBuilder;
import com.maxnth.page4j.model.TableRegionBuilder;
import com.maxnth.page4j.model.TextEquivBuilder;
import com.maxnth.page4j.model.TextLine;
import com.maxnth.page4j.model.TextLineBuilder;
import com.maxnth.page4j.model.TextRegionBuilder;
import com.maxnth.page4j.model.UnknownRegionBuilder;
import com.maxnth.page4j.model.value.Align;
import com.maxnth.page4j.model.value.GraphicsType;
import com.maxnth.page4j.model.value.Language;
import com.maxnth.page4j.model.value.PageDateTime;
import com.maxnth.page4j.model.value.ResolutionUnit;
import com.maxnth.page4j.pagexml.PageReadException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.jspecify.annotations.Nullable;

/**
 * Reads ABBYY FineReader 10 XML (and the similar earlier versions) by recursive descent. Only the
 * first page is read.
 */
public final class FineReaderReader {

  private final XMLStreamReader r;
  private final Diagnostics diagnostics = new Diagnostics();
  private final Ids ids = new Ids(Set.of());

  private String creator = "page4j";
  private @Nullable Language language;
  private boolean pageRead;
  private int width;
  private int height;
  private @Nullable Double resolution;
  private final List<String> comments = new ArrayList<>();
  private final List<Region> regions = new ArrayList<>();

  private FineReaderReader(XMLStreamReader r) {
    this.r = r;
  }

  /**
   * @throws PageReadException if the document is not well-formed XML or not FineReader XML
   */
  public static ReadResult read(byte[] xml) throws PageReadException {
    XMLStreamReader r = XmlInput.open(xml);
    try {
      XmlInput.requireRoot(r, "document", "FineReader XML");
      return new FineReaderReader(r).document();
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

  private Attrs attrs() {
    return new Attrs(r, diagnostics);
  }

  private boolean nextChild() throws XMLStreamException {
    return XmlInput.nextChild(r);
  }

  private String name() {
    return r.getLocalName();
  }

  private void ignore(String parent) throws XMLStreamException {
    attrs().ignored(parent + ": element " + name());
    XmlInput.skip(r);
  }

  private ReadResult document() throws XMLStreamException {
    Attrs a = attrs();
    String producer = a.string("producer");
    if (producer != null) {
      creator = producer;
    }
    a.string("version");
    a.string("pagesCount");
    String mainLanguage = a.string("mainLanguage");
    if (mainLanguage != null) {
      try {
        language = Language.fromXml(mainLanguage);
      } catch (IllegalArgumentException e) {
        a.ignored("document: mainLanguage '" + mainLanguage + "' is not a PAGE language");
      }
    }
    while (nextChild()) {
      if (!name().equals("page")) {
        ignore("document");
      } else if (pageRead) {
        attrs().ignored("document: page ignored, only the first page is read");
        XmlInput.skip(r);
      } else {
        pageRead = true;
        page();
      }
    }
    a.done();
    PageDateTime now = PageDateTime.now();
    return new ReadResult(
        Page.builder()
            .metadata(
                MetadataBuilder.builder()
                    .creator(creator)
                    .created(now)
                    .lastChange(now)
                    .comments(comments.isEmpty() ? null : String.join("\n", comments))
                    .build())
            .imageFilename("")
            .imageWidth(width)
            .imageHeight(height)
            .imageXResolution(resolution)
            .imageYResolution(resolution)
            .imageResolutionUnit(resolution == null ? null : ResolutionUnit.PPI)
            .primaryLanguage(language)
            .regions(regions)
            .build(),
        diagnostics.list());
  }

  private void page() throws XMLStreamException {
    Attrs a = attrs();
    width = a.coordinate("width");
    height = a.coordinate("height");
    resolution = a.decimal("resolution");
    String originalCoords = a.string("originalCoords");
    if (originalCoords != null) {
      boolean original =
          "1".equals(originalCoords.strip()) || "true".equals(originalCoords.strip());
      comments.add("Original coords: " + original);
    }
    while (nextChild()) {
      if (name().equals("block")) {
        regions.addAll(block());
      } else {
        ignore("page");
      }
    }
    a.done();
  }

  /** A block's region, or the paragraphs of its text in its place. */
  private List<Region> block() throws XMLStreamException {
    Attrs a = attrs();
    String type = a.string("blockType");
    Polygon box = rect(a);
    Polygon coords = null;
    List<Region> paragraphs = new ArrayList<>();
    boolean hasText = false;
    while (nextChild()) {
      switch (name()) {
        case "region" -> coords = region();
        case "text" -> {
          hasText = true;
          text(paragraphs);
        }
        case "row" -> {
          Attrs row = attrs();
          while (nextChild()) {
            if (!name().equals("cell")) {
              ignore("row");
              continue;
            }
            Attrs cell = attrs();
            while (nextChild()) {
              if (name().equals("text")) {
                hasText = true;
                text(paragraphs);
              } else {
                ignore("cell");
              }
            }
            cell.done();
          }
          row.done();
        }
        default -> ignore("block");
      }
    }
    a.done();
    if (hasText) {
      return paragraphs;
    }
    String id = ids.generate("r");
    Polygon outline = coords != null ? coords : box;
    Region region =
        switch (type == null ? "" : type) {
          case "Text" -> TextRegionBuilder.builder().id(id).coords(outline).build();
          case "Table" -> TableRegionBuilder.builder().id(id).coords(outline).build();
          case "Picture" -> ImageRegionBuilder.builder().id(id).coords(outline).build();
          case "Barcode" ->
              GraphicRegionBuilder.builder()
                  .id(id)
                  .coords(outline)
                  .type(GraphicsType.BARCODE)
                  .build();
          case "Separator", "SeparatorsBox" ->
              SeparatorRegionBuilder.builder().id(id).coords(outline).build();
          default -> {
            a.changed("block: blockType '" + type + "' read as UnknownRegion");
            yield UnknownRegionBuilder.builder().id(id).coords(outline).build();
          }
        };
    return List.of(region);
  }

  /** The {@code l}, {@code t}, {@code r} and {@code b} attributes as a rectangle. */
  private static Polygon rect(Attrs a) {
    return new Rect(a.coordinate("l"), a.coordinate("t"), a.coordinate("r"), a.coordinate("b"))
        .toPolygon();
  }

  /** The outline of a region's rectangles, or null if it has none. */
  private @Nullable Polygon region() throws XMLStreamException {
    Attrs a = attrs();
    List<Rect> rects = new ArrayList<>();
    while (nextChild()) {
      if (name().equals("rect")) {
        Attrs rect = attrs();
        Polygon polygon = rect(rect);
        rects.add(polygon.boundingBox());
        while (nextChild()) {
          ignore("rect");
        }
        rect.done();
      } else {
        ignore("region");
      }
    }
    a.done();
    if (rects.isEmpty()) {
      return null;
    }
    if (rects.size() == 1) {
      return rects.getFirst().toPolygon();
    }
    // Sorted top to bottom: down the right sides, then up the left sides.
    rects.sort(Comparator.comparingInt(rect -> (rect.top() + rect.bottom()) / 2));
    Polygon.Builder polygon = Polygon.builder();
    for (Rect rect : rects) {
      polygon.add(rect.right(), rect.top()).add(rect.right(), rect.bottom());
    }
    for (Rect rect : rects.reversed()) {
      polygon.add(rect.left(), rect.bottom()).add(rect.left(), rect.top());
    }
    return polygon.build();
  }

  /** Adds the paragraphs that have lines as text regions. */
  private void text(List<Region> into) throws XMLStreamException {
    Attrs a = attrs();
    while (nextChild()) {
      if (name().equals("par")) {
        Region paragraph = paragraph();
        if (paragraph != null) {
          into.add(paragraph);
        }
      } else {
        ignore("text");
      }
    }
    a.done();
  }

  private @Nullable Region paragraph() throws XMLStreamException {
    Attrs a = attrs();
    String alignment = a.string("align");
    Align align = null;
    if (alignment != null) {
      align =
          switch (alignment) {
            case "Left" -> Align.LEFT;
            case "Center" -> Align.CENTRE;
            case "Right" -> Align.RIGHT;
            case "Justified" -> Align.JUSTIFY;
            default -> {
              a.ignored("par: align '" + alignment + "'");
              yield null;
            }
          };
    }
    List<TextLine> lines = new ArrayList<>();
    while (nextChild()) {
      if (name().equals("line")) {
        lines.add(line());
      } else {
        ignore("par");
      }
    }
    a.done();
    if (lines.isEmpty()) {
      return null; // an empty paragraph has no content
    }
    int left = Integer.MAX_VALUE;
    int top = Integer.MAX_VALUE;
    int right = Integer.MIN_VALUE;
    int bottom = Integer.MIN_VALUE;
    for (TextLine line : lines) {
      Rect box = line.coords().boundingBox();
      left = Math.min(left, box.left());
      top = Math.min(top, box.top());
      right = Math.max(right, box.right());
      bottom = Math.max(bottom, box.bottom());
    }
    var region =
        TextRegionBuilder.builder()
            .id(ids.generate("r"))
            .coords(new Rect(left, top, right, bottom).toPolygon())
            .align(align)
            .textLines(lines);
    String text =
        String.join(
            "\n",
            lines.stream()
                .map(l -> l.textEquivs().isEmpty() ? "" : l.textEquivs().getFirst().unicode())
                .toList());
    if (!text.isEmpty()) {
      region.addTextEquivs(TextEquivBuilder.builder().unicode(text).build());
    }
    return region.build();
  }

  private TextLine line() throws XMLStreamException {
    Attrs a = attrs();
    Polygon coords = rect(a);
    Double baseline = a.decimal("baseline");
    var text = new StringBuilder();
    while (nextChild()) {
      if (name().equals("formatting")) {
        text.append(formatting());
      } else {
        ignore("line");
      }
    }
    a.done();
    var line = TextLineBuilder.builder().id(ids.generate("l")).coords(coords);
    if (baseline != null) {
      Rect box = coords.boundingBox();
      int y = (int) baseline.doubleValue();
      line.baseline(Polygon.of(new Point(box.left(), y), new Point(box.right(), y)));
    }
    if (!text.isEmpty()) {
      line.addTextEquivs(TextEquivBuilder.builder().unicode(text.toString()).build());
    }
    return line.build();
  }

  /** The characters of a formatting run: its character elements, or else its text. */
  private String formatting() throws XMLStreamException {
    Attrs a = attrs();
    var plain = new StringBuilder();
    var characters = new StringBuilder();
    boolean hasCharacters = false;
    while (true) {
      int event = r.next();
      if (event == XMLStreamConstants.END_ELEMENT) {
        break;
      } else if (event == XMLStreamConstants.START_ELEMENT) {
        if (name().equals("charParams")) {
          Attrs character = attrs();
          characters.append(XmlInput.text(r));
          character.done();
          hasCharacters = true;
        } else {
          ignore("formatting");
        }
      } else if (event == XMLStreamConstants.CHARACTERS
          || event == XMLStreamConstants.CDATA
          || event == XMLStreamConstants.SPACE) {
        plain.append(r.getText());
      }
    }
    a.done();
    return hasCharacters ? characters.toString() : plain.toString();
  }
}
