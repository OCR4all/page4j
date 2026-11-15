package com.maxnth.page4j.formats.internal;

import com.maxnth.page4j.formats.AltoReadResult;
import com.maxnth.page4j.formats.MeasurementUnit;
import com.maxnth.page4j.formats.SourceImage;
import com.maxnth.page4j.geometry.Point;
import com.maxnth.page4j.geometry.Polygon;
import com.maxnth.page4j.geometry.Rect;
import com.maxnth.page4j.model.AdvertRegionBuilder;
import com.maxnth.page4j.model.AlternativeImage;
import com.maxnth.page4j.model.ChartRegionBuilder;
import com.maxnth.page4j.model.ChemRegionBuilder;
import com.maxnth.page4j.model.CustomRegionBuilder;
import com.maxnth.page4j.model.FormRegionBuilder;
import com.maxnth.page4j.model.FrameRegionBuilder;
import com.maxnth.page4j.model.Glyph;
import com.maxnth.page4j.model.GlyphBuilder;
import com.maxnth.page4j.model.GraphicRegionBuilder;
import com.maxnth.page4j.model.GroupMember;
import com.maxnth.page4j.model.ImageRegionBuilder;
import com.maxnth.page4j.model.InvalidPageException;
import com.maxnth.page4j.model.Label;
import com.maxnth.page4j.model.LabelGroup;
import com.maxnth.page4j.model.LabelGroupBuilder;
import com.maxnth.page4j.model.LineDrawingRegionBuilder;
import com.maxnth.page4j.model.MapRegionBuilder;
import com.maxnth.page4j.model.MathsRegionBuilder;
import com.maxnth.page4j.model.Metadata;
import com.maxnth.page4j.model.MetadataBuilder;
import com.maxnth.page4j.model.MetadataItem;
import com.maxnth.page4j.model.MetadataItemBuilder;
import com.maxnth.page4j.model.MusicRegionBuilder;
import com.maxnth.page4j.model.NoiseRegionBuilder;
import com.maxnth.page4j.model.OrderedGroupBuilder;
import com.maxnth.page4j.model.Page;
import com.maxnth.page4j.model.ReadingOrder;
import com.maxnth.page4j.model.ReadingOrderBuilder;
import com.maxnth.page4j.model.Region;
import com.maxnth.page4j.model.RegionRef;
import com.maxnth.page4j.model.SeparatorRegionBuilder;
import com.maxnth.page4j.model.TableRegionBuilder;
import com.maxnth.page4j.model.TextEquiv;
import com.maxnth.page4j.model.TextEquivBuilder;
import com.maxnth.page4j.model.TextLine;
import com.maxnth.page4j.model.TextLineBuilder;
import com.maxnth.page4j.model.TextRegionBuilder;
import com.maxnth.page4j.model.TextStyle;
import com.maxnth.page4j.model.TextStyleBuilder;
import com.maxnth.page4j.model.UnknownRegionBuilder;
import com.maxnth.page4j.model.UnorderedGroupBuilder;
import com.maxnth.page4j.model.Word;
import com.maxnth.page4j.model.WordBuilder;
import com.maxnth.page4j.model.value.Align;
import com.maxnth.page4j.model.value.Language;
import com.maxnth.page4j.model.value.MetadataItemType;
import com.maxnth.page4j.model.value.PageDateTime;
import com.maxnth.page4j.model.value.PageType;
import com.maxnth.page4j.model.value.TextType;
import com.maxnth.page4j.pagexml.PageReadException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.jspecify.annotations.Nullable;

/**
 * Reads ALTO (any version) into the model, by recursive descent: each method starts at the start
 * tag of its element and ends at its end tag. Only the first page is read.
 */
public final class AltoReader {

  /** The tag description the writer gives a text region type. */
  static final String TEXT_TYPE_TAG = "PAGE XML text region type";

  private static final List<String> REGION_KINDS =
      List.of(
          "TextRegion",
          "ImageRegion",
          "LineDrawingRegion",
          "GraphicRegion",
          "TableRegion",
          "ChartRegion",
          "MapRegion",
          "SeparatorRegion",
          "MathsRegion",
          "ChemRegion",
          "MusicRegion",
          "AdvertRegion",
          "FormRegion",
          "NoiseRegion",
          "UnknownRegion",
          "CustomRegion",
          "FrameRegion");

  private record ParagraphStyle(@Nullable Align align, @Nullable Integer leading) {}

  private record Tag(
      @Nullable String type, String label, @Nullable String description, @Nullable String uri) {}

  private record Next(String to, Attrs at) {}

  /** What every block has. */
  private record Block(
      String id,
      Polygon coords,
      @Nullable Double rotation,
      List<String> styleRefs,
      List<String> tagRefs,
      Attrs at) {}

  private final XMLStreamReader r;
  private final Ids ids;
  private final double scaleX;
  private final double scaleY;
  private final @Nullable SourceImage image;
  private final Diagnostics diagnostics = new Diagnostics();

  private MeasurementUnit unit = MeasurementUnit.PIXEL;
  private String imageFilename = "";
  private final List<MetadataItem> items = new ArrayList<>();
  private final List<String> comments = new ArrayList<>();
  private final Map<String, TextStyle> textStyles = new HashMap<>();
  private final Map<String, ParagraphStyle> paragraphStyles = new HashMap<>();
  private final Map<String, Tag> tags = new HashMap<>();
  private final Map<String, Next> next = new LinkedHashMap<>();

  private boolean pageRead;
  private boolean hasMargins;
  private @Nullable String pcGtsId;
  private int width;
  private int height;
  private @Nullable Double conf;
  private @Nullable PageType type;
  private @Nullable Polygon printSpace;
  private final List<Region> regions = new ArrayList<>();

  private AltoReader(
      XMLStreamReader r,
      Set<String> taken,
      double scaleX,
      double scaleY,
      @Nullable SourceImage image) {
    this.r = r;
    this.ids = new Ids(taken);
    this.scaleX = scaleX;
    this.scaleY = scaleY;
    this.image = image;
  }

  /**
   * Reads a document; with an image, coordinates in a physical unit are scaled to its pixels.
   *
   * @throws PageReadException if the document is not well-formed XML or not ALTO
   */
  public static AltoReadResult read(byte[] xml, @Nullable SourceImage image)
      throws PageReadException {
    Set<String> taken = XmlInput.attributeValues(xml, "ID");
    AltoReadResult result = parse(xml, taken, 1, 1, null);
    if (image == null || result.measurementUnit() == MeasurementUnit.PIXEL) {
      return result;
    }
    double[] scale = scale(result, image);
    if (scale[0] == 0 || scale[1] == 0) {
      return result;
    }
    return parse(xml, taken, scale[0], scale[1], image);
  }

  private static AltoReadResult parse(
      byte[] xml, Set<String> taken, double scaleX, double scaleY, @Nullable SourceImage image)
      throws PageReadException {
    XMLStreamReader r = XmlInput.open(xml);
    try {
      XmlInput.requireRoot(r, "alto", "ALTO");
      return new AltoReader(r, taken, scaleX, scaleY, image).alto();
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

  /**
   * The old library's rule: if the page size differs from the image size and all objects lie within
   * the page, scale by the ratio of the sizes, else by the resolution.
   */
  private static double[] scale(AltoReadResult result, SourceImage image) {
    Page page = result.page();
    if (page.imageWidth() != image.width()
        && page.imageHeight() != image.height()
        && page.imageWidth() > 0
        && page.imageHeight() > 0) {
      boolean withinPage =
          Stream.of(page.allRegions(), page.textLines(), page.words(), page.glyphs())
              .flatMap(List::stream)
              .flatMap(o -> coords(o).points().stream())
              .allMatch(p -> p.x() <= page.imageWidth() && p.y() <= page.imageHeight());
      if (withinPage) {
        return new double[] {
          (double) image.width() / page.imageWidth(), (double) image.height() / page.imageHeight()
        };
      }
    }
    double perInch = result.measurementUnit().perInch();
    return new double[] {image.xResolution() / perInch, image.yResolution() / perInch};
  }

  private static Polygon coords(Object object) {
    return switch (object) {
      case Region region -> region.coords();
      case TextLine line -> line.coords();
      case Word word -> word.coords();
      case Glyph glyph -> glyph.coords();
      default -> Polygon.EMPTY;
    };
  }

  // ---- Helpers

  private Attrs attrs() {
    return new Attrs(r, diagnostics);
  }

  private boolean nextChild() throws XMLStreamException {
    return XmlInput.nextChild(r);
  }

  private String name() {
    return r.getLocalName();
  }

  private String text() throws XMLStreamException {
    return XmlInput.text(r).strip();
  }

  /** Reports and skips the current element. */
  private void ignore(String parent) throws XMLStreamException {
    attrs().ignored(parent + ": element " + name());
    XmlInput.skip(r);
  }

  /** Finishes an element without children of interest. */
  private void leaf(Attrs a) throws XMLStreamException {
    while (nextChild()) {
      ignore(a.element());
    }
    a.done();
  }

  private int x(int value) {
    return scaleX == 1 ? value : (int) (value * scaleX + 0.5);
  }

  private int y(int value) {
    return scaleY == 1 ? value : (int) (value * scaleY + 0.5);
  }

  private Polygon scaled(Polygon polygon) {
    return scaleX == 1 && scaleY == 1 ? polygon : polygon.map(p -> new Point(x(p.x()), y(p.y())));
  }

  /** The box given by {@code HPOS}, {@code VPOS}, {@code WIDTH} and {@code HEIGHT}. */
  private Polygon box(Attrs a) {
    int left = a.coordinate("HPOS");
    int top = a.coordinate("VPOS");
    int width = a.coordinate("WIDTH");
    int height = a.coordinate("HEIGHT");
    return scaled(new Rect(left, top, left + width - 1, top + height - 1).toPolygon());
  }

  private static List<String> refs(@Nullable String value) {
    return value == null || value.isBlank() ? List.of() : List.of(value.strip().split("\\s+"));
  }

  // ---- Document and description

  private AltoReadResult alto() throws XMLStreamException {
    Attrs a = attrs();
    a.string("SCHEMAVERSION");
    while (nextChild()) {
      switch (name()) {
        case "Description" -> description();
        case "Styles" -> styles();
        case "Tags" -> tags();
        case "Layout" -> layout();
        default -> ignore("alto");
      }
    }
    a.done();
    return new AltoReadResult(page(), unit, diagnostics.list());
  }

  private Page page() {
    PageDateTime now = PageDateTime.now();
    String creator =
        items.stream().map(MetadataItem::name).filter(n -> n != null).findFirst().orElse("page4j");
    Metadata metadata =
        MetadataBuilder.builder()
            .creator(creator)
            .created(now)
            .lastChange(now)
            .comments(comments.isEmpty() ? null : String.join("\n", comments))
            .metadataItems(items)
            .build();
    return Page.builder()
        .pcGtsId(pcGtsId)
        .metadata(metadata)
        .imageFilename(imageFilename)
        .imageWidth(image != null ? image.width() : width)
        .imageHeight(image != null ? image.height() : height)
        .conf(conf)
        .type(type)
        .printSpace(printSpace)
        .readingOrder(readingOrder())
        .regions(regions)
        .build();
  }

  private void description() throws XMLStreamException {
    while (nextChild()) {
      switch (name()) {
        case "MeasurementUnit" -> {
          Attrs a = attrs();
          String value = text();
          MeasurementUnit known = MeasurementUnit.fromXml(value).orElse(null);
          if (known == null) {
            a.ignored("MeasurementUnit '" + value + "' is unknown, coordinates are read as pixels");
          } else {
            unit = known;
          }
        }
        case "sourceImageInformation" -> {
          while (nextChild()) {
            if (name().equals("fileName")) {
              imageFilename = text();
            } else {
              ignore("sourceImageInformation");
            }
          }
        }
        case "OCRProcessing" -> {
          Attrs a = attrs();
          a.string("ID");
          while (nextChild()) {
            switch (name()) {
              case "preProcessingStep" -> processing("preOperation");
              case "ocrProcessingStep" -> processing("contentGeneration");
              case "postProcessingStep" -> processing("postOperation");
              default -> ignore("OCRProcessing");
            }
          }
          a.done();
        }
        case "Processing" -> processing(null);
        default -> ignore("Description");
      }
    }
  }

  /** A processing step, read into a metadata item. */
  private void processing(@Nullable String category) throws XMLStreamException {
    Attrs a = attrs();
    a.string("ID");
    List<Label> labels = new ArrayList<>();
    if (category != null) {
      labels.add(new Label(category, "processingCategory", null));
    }
    String name = null;
    PageDateTime date = null;
    List<String> descriptions = new ArrayList<>();
    while (nextChild()) {
      String element = name();
      switch (element) {
        case "processingDateTime" -> {
          Attrs at = attrs();
          String value = text();
          try {
            date = PageDateTime.parse(value);
          } catch (RuntimeException e) {
            at.ignored("processingDateTime '" + value + "' is not a date and time");
          }
        }
        case "processingStepDescription" -> descriptions.add(text());
        case "processingCategory", "processingAgency", "processingStepSettings" ->
            labels.add(new Label(text(), element, null));
        case "processingSoftware" -> {
          while (nextChild()) {
            String part = name();
            switch (part) {
              case "softwareName" -> name = text();
              case "softwareCreator", "softwareVersion", "applicationDescription" ->
                  labels.add(new Label(text(), part, null));
              default -> ignore("processingSoftware");
            }
          }
        }
        default -> ignore(a.element());
      }
    }
    a.done();
    var item =
        MetadataItemBuilder.builder()
            .type(MetadataItemType.PROCESSING_STEP)
            .name(name)
            .value(String.join("\n", descriptions))
            .date(date);
    if (!labels.isEmpty()) {
      item.addLabels(LabelGroupBuilder.builder().labels(labels).build());
    }
    items.add(item.build());
  }

  // ---- Styles and tags

  private void styles() throws XMLStreamException {
    while (nextChild()) {
      switch (name()) {
        case "TextStyle" -> {
          Attrs a = attrs();
          String id = a.string("ID");
          var style = TextStyleBuilder.builder().fontFamily(a.string("FONTFAMILY"));
          String fontType = a.string("FONTTYPE");
          if (fontType != null) {
            switch (fontType) {
              case "serif" -> style.serif(true);
              case "sans-serif" -> style.serif(false);
              default -> a.ignored("TextStyle: FONTTYPE '" + fontType + "'");
            }
          }
          String fontWidth = a.string("FONTWIDTH");
          if (fontWidth != null) {
            switch (fontWidth) {
              case "fixed" -> style.monospace(true);
              case "proportional" -> style.monospace(false);
              default -> a.ignored("TextStyle: FONTWIDTH '" + fontWidth + "'");
            }
          }
          style.fontSize(a.decimal("FONTSIZE"));
          String colour = a.string("FONTCOLOR");
          if (colour != null) {
            try {
              int rgb = Integer.parseInt(colour.strip(), 16);
              style.textColourRgb((rgb >> 16) + (rgb & 0xff00) + ((rgb & 0xff) << 16));
            } catch (NumberFormatException e) {
              a.ignored("TextStyle: FONTCOLOR '" + colour + "' is not a hexadecimal colour");
            }
          }
          fontStyles(style, a.string("FONTSTYLE"), a, "FONTSTYLE");
          leaf(a);
          if (id != null) {
            textStyles.put(id, style.build());
          }
        }
        case "ParagraphStyle" -> {
          Attrs a = attrs();
          String id = a.string("ID");
          String alignment = a.string("ALIGN");
          Align align = null;
          if (alignment != null) {
            align =
                switch (alignment) {
                  case "Left" -> Align.LEFT;
                  case "Right" -> Align.RIGHT;
                  case "Center" -> Align.CENTRE;
                  case "Block" -> Align.JUSTIFY;
                  default -> {
                    a.ignored("ParagraphStyle: ALIGN '" + alignment + "'");
                    yield null;
                  }
                };
          }
          Double lineSpace = a.decimal("LINESPACE");
          leaf(a);
          if (id != null) {
            paragraphStyles.put(
                id,
                new ParagraphStyle(align, lineSpace == null ? null : (int) Math.round(lineSpace)));
          }
        }
        default -> ignore("Styles");
      }
    }
  }

  /** Sets the font styles of an ALTO list such as {@code "bold italics"}. */
  private static void fontStyles(
      TextStyleBuilder style, @Nullable String value, Attrs a, String attribute) {
    for (String token : refs(value)) {
      switch (token) {
        case "bold" -> style.bold(true);
        case "italics" -> style.italic(true);
        case "subscript" -> style.subscript(true);
        case "superscript" -> style.superscript(true);
        case "smallcaps" -> style.smallCaps(true);
        case "underline" -> style.underlined(true);
        default -> a.ignored(a.element() + ": " + attribute + " '" + token + "'");
      }
    }
  }

  private void tags() throws XMLStreamException {
    while (nextChild()) {
      Attrs a = attrs();
      String id = a.string("ID");
      String label = a.string("LABEL");
      String tagType = a.string("TYPE");
      String description = a.string("DESCRIPTION");
      String uri = a.string("URI");
      leaf(a);
      if (id != null && label != null) {
        tags.put(id, new Tag(tagType, label, description, uri));
      } else {
        a.ignored(a.element() + " without ID or LABEL");
      }
    }
  }

  // ---- Layout

  private void layout() throws XMLStreamException {
    Attrs layout = attrs();
    layout.string("STYLEREFS");
    while (nextChild()) {
      if (!name().equals("Page")) {
        ignore("Layout");
      } else if (pageRead) {
        attrs().ignored("Layout: page ignored, only the first page is read");
        XmlInput.skip(r);
      } else {
        pageRead = true;
        altoPage();
      }
    }
    layout.done();
  }

  private void altoPage() throws XMLStreamException {
    Attrs a = attrs();
    String id = a.string("ID");
    if (id != null) {
      if (ids.use(id)) {
        pcGtsId = id;
      } else {
        a.ignored("Page: ID '" + id + "' is not a valid or unique ID");
      }
    }
    Double w = a.decimal("WIDTH");
    Double h = a.decimal("HEIGHT");
    width = w == null ? 0 : (int) w.doubleValue();
    height = h == null ? 0 : (int) h.doubleValue();
    String pageClass = a.string("PAGECLASS");
    if (pageClass != null) {
      try {
        type = PageType.fromXml(pageClass.strip());
      } catch (IllegalArgumentException e) {
        comments.add("Page class: " + pageClass);
      }
    }
    conf = a.decimal("PC");
    comment(a, "PHYSICAL_IMG_NR", "Physical image number");
    comment(a, "PRINTED_IMG_NR", "Printed image number");
    comment(a, "QUALITY", "Quality");
    comment(a, "QUALITY_DETAIL", "Quality details");
    comment(a, "POSITION", "Position");
    comment(a, "PROCESSING", "Processing ID");
    comment(a, "ACCURACY", "Accuracy");
    while (nextChild()) {
      switch (name()) {
        case "TopMargin", "LeftMargin", "RightMargin", "BottomMargin" -> {
          hasMargins = true;
          space();
        }
        case "PrintSpace" -> {
          Polygon space = space();
          if (hasMargins) {
            printSpace = space;
          } else if (width <= 0) {
            Rect box = space.boundingBox();
            width = box.right() + 1;
            height = box.bottom() + 1;
          }
        }
        default -> ignore("Page");
      }
    }
    a.done();
  }

  private void comment(Attrs a, String attribute, String label) {
    String value = a.string(attribute);
    if (value != null) {
      comments.add(label + ": " + value);
    }
  }

  /** A margin or the print space: its outline, and its blocks added to the regions. */
  private Polygon space() throws XMLStreamException {
    Attrs a = attrs();
    a.string("ID");
    Polygon outline = box(a);
    while (nextChild()) {
      if (name().equals("Shape")) {
        Polygon shape = shape();
        if (shape != null) {
          outline = shape;
        }
      } else {
        block(regions, a.element());
      }
    }
    a.done();
    return outline;
  }

  /** Reads a block into the list, or reports and skips an element that is none. */
  private void block(List<Region> into, String parent) throws XMLStreamException {
    switch (name()) {
      case "TextBlock" -> into.add(textBlock());
      case "Illustration" -> into.add(illustration());
      case "GraphicalElement" -> into.add(graphicalElement());
      case "ComposedBlock" -> into.addAll(composedBlock());
      default -> ignore(parent);
    }
  }

  /** The attributes every block has. */
  private Block block(Attrs a, String prefix) {
    String id = ids.id(a.string("ID"), prefix, a);
    String to = a.string("IDNEXT");
    if (to != null) {
      next.put(id, new Next(to.strip(), a));
    }
    Double rotation = a.decimal("ROTATION");
    return new Block(
        id,
        box(a),
        rotation == null ? null : orientation(rotation),
        refs(a.string("STYLEREFS")),
        refs(a.string("TAGREFS")),
        a);
  }

  /** An angle in degrees in the PAGE orientation range (-180, 180]. */
  private static double orientation(double degrees) {
    double angle = degrees % 360;
    if (angle > 180) {
      return angle - 360;
    }
    return angle <= -180 ? angle + 360 : angle;
  }

  private Region textBlock() throws XMLStreamException {
    Attrs a = attrs();
    Block block = block(a, "r");
    String lang = a.string("LANG");
    Language language = lang == null ? null : Languages.read(lang, a, "LANG");
    Polygon coords = block.coords();
    List<TextLine> lines = new ArrayList<>();
    while (nextChild()) {
      switch (name()) {
        case "Shape" -> coords = shapeOr(coords);
        case "TextLine" -> lines.add(textLine());
        default -> ignore("TextBlock");
      }
    }
    a.done();
    var region =
        TextRegionBuilder.builder()
            .id(block.id())
            .coords(coords)
            .orientation(block.rotation())
            .primaryLanguage(language)
            .textLines(lines);
    for (String ref : block.styleRefs()) {
      TextStyle style = textStyles.get(ref);
      ParagraphStyle paragraph = paragraphStyles.get(ref);
      if (style != null) {
        region.textStyle(style);
      } else if (paragraph != null) {
        region.align(paragraph.align()).leading(paragraph.leading());
      } else {
        a.ignored("TextBlock: STYLEREFS '" + ref + "' refers to no style");
      }
    }
    List<LabelGroup> labels = new ArrayList<>();
    region.type(tagRefs(block, labels, true));
    region.labels(labels);
    String text = String.join("\n", lines.stream().map(AltoReader::text).toList());
    if (!text.isEmpty()) {
      region.addTextEquivs(TextEquivBuilder.builder().unicode(text).build());
    }
    return region.build();
  }

  /**
   * Reads the tag references into label groups (one per tag URI), and returns the text region type
   * a tag of the writer gives, if wanted.
   */
  private @Nullable TextType tagRefs(Block block, List<LabelGroup> into, boolean textType) {
    return tagRefs(block.tagRefs(), block.at(), into, textType);
  }

  private @Nullable TextType tagRefs(
      List<String> refs, Attrs a, List<LabelGroup> into, boolean textType) {
    TextType result = null;
    Map<@Nullable String, List<Label>> groups = new LinkedHashMap<>();
    for (String ref : refs) {
      Tag tag = tags.get(ref);
      if (tag == null) {
        a.ignored(a.element() + ": TAGREFS '" + ref + "' refers to no tag");
        continue;
      }
      if (textType && TEXT_TYPE_TAG.equals(tag.description()) && result == null) {
        try {
          result = TextType.fromXml(tag.label());
          continue;
        } catch (IllegalArgumentException e) {
          // an ordinary label then
        }
      }
      groups
          .computeIfAbsent(tag.uri(), k -> new ArrayList<>())
          .add(new Label(tag.label(), tag.type(), tag.description()));
    }
    groups.forEach(
        (uri, labels) ->
            into.add(LabelGroupBuilder.builder().externalModel(uri).labels(labels).build()));
    return result;
  }

  private static String text(TextLine line) {
    return line.textEquivs().isEmpty() ? "" : line.textEquivs().getFirst().unicode();
  }

  private TextLine textLine() throws XMLStreamException {
    Attrs a = attrs();
    String id = ids.id(a.string("ID"), "l", a);
    Polygon box = box(a);
    Polygon coords = box;
    String lang = a.string("LANG");
    Language language = lang == null ? null : Languages.read(lang, a, "LANG");
    TextStyle style = style(refs(a.string("STYLEREFS")), null, a);
    List<LabelGroup> labels = new ArrayList<>();
    tagRefs(refs(a.string("TAGREFS")), a, labels, false);
    Polygon baseline = baseline(a.string("BASELINE"), box, a);
    List<Word> words = new ArrayList<>();
    while (nextChild()) {
      switch (name()) {
        case "Shape" -> coords = shapeOr(coords);
        case "String" -> words.add(string());
        case "SP" -> XmlInput.skip(r); // the space between words is implied
        case "HYP" -> hyphen(words, box);
        default -> ignore("TextLine");
      }
    }
    a.done();
    String text =
        String.join(
            " ",
            words.stream()
                .map(w -> w.textEquivs().isEmpty() ? "" : w.textEquivs().getFirst().unicode())
                .toList());
    var line =
        TextLineBuilder.builder()
            .id(id)
            .coords(coords)
            .baseline(baseline)
            .primaryLanguage(language)
            .textStyle(style)
            .labels(labels)
            .words(words);
    if (!text.isEmpty()) {
      line.addTextEquivs(TextEquivBuilder.builder().unicode(text).build());
    }
    return line.build();
  }

  /** The text style of style references, with the font styles of a {@code STYLE} attribute. */
  private @Nullable TextStyle style(List<String> refs, @Nullable String fontStyles, Attrs a) {
    TextStyle style = null;
    for (String ref : refs) {
      TextStyle referenced = textStyles.get(ref);
      if (referenced != null) {
        style = style == null ? referenced : style;
      } else if (!paragraphStyles.containsKey(ref)) {
        a.ignored(a.element() + ": STYLEREFS '" + ref + "' refers to no style");
      }
    }
    if (fontStyles != null) {
      var builder = style == null ? TextStyleBuilder.builder() : TextStyleBuilder.builder(style);
      fontStyles(builder, fontStyles, a, "STYLE");
      style = builder.build();
    }
    return style == null || style.equals(TextStyleBuilder.builder().build()) ? null : style;
  }

  /** A baseline: points in ALTO 4.2 and later, a vertical position across the line before. */
  private @Nullable Polygon baseline(@Nullable String value, Polygon box, Attrs a) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String text = value.strip();
    if (text.indexOf(',') < 0 && text.split("\\s+").length == 1) {
      try {
        int position = (int) Double.parseDouble(text);
        Rect line = box.boundingBox();
        return Polygon.of(
            new Point(line.left(), y(position)), new Point(line.right(), y(position)));
      } catch (NumberFormatException e) {
        a.ignored("TextLine: BASELINE '" + value + "' is neither a position nor points");
        return null;
      }
    }
    List<Point> points = points(text);
    if (points == null || points.size() < 2) {
      a.ignored("TextLine: BASELINE '" + value + "' is neither a position nor points");
      return null;
    }
    return scaled(new Polygon(points));
  }

  private Word string() throws XMLStreamException {
    Attrs a = attrs();
    String id = ids.id(a.string("ID"), "w", a);
    Polygon coords = box(a);
    String content = a.string("CONTENT");
    Double wordConf = a.decimal("WC");
    String lang = a.string("LANG");
    Language language = lang == null ? null : Languages.read(lang, a, "LANG");
    TextStyle style = style(refs(a.string("STYLEREFS")), a.string("STYLE"), a);
    List<LabelGroup> labels = new ArrayList<>();
    tagRefs(refs(a.string("TAGREFS")), a, labels, false);
    List<String> alternatives = new ArrayList<>();
    List<Glyph> glyphs = new ArrayList<>();
    while (nextChild()) {
      switch (name()) {
        case "Shape" -> coords = shapeOr(coords);
        case "ALTERNATIVE" -> {
          Attrs alternative = attrs();
          alternative.string("PURPOSE");
          alternatives.add(text());
          alternative.done();
        }
        case "Glyph" -> glyphs.add(glyph());
        default -> ignore("String");
      }
    }
    a.done();
    return WordBuilder.builder()
        .id(id)
        .coords(coords)
        .language(language)
        .textStyle(style)
        .labels(labels)
        .glyphs(glyphs)
        .textEquivs(textEquivs(content, wordConf, alternatives, List.of()))
        .build();
  }

  /** The main text with its confidence, then the alternatives, indexed if there are several. */
  private static List<TextEquiv> textEquivs(
      @Nullable String content,
      @Nullable Double confidence,
      List<String> alternatives,
      List<@Nullable Double> alternativeConfidences) {
    List<TextEquiv> equivs = new ArrayList<>();
    boolean indexed = !alternatives.isEmpty();
    if (content != null) {
      equivs.add(
          TextEquivBuilder.builder()
              .index(indexed ? 1 : null)
              .conf(confidence)
              .unicode(content)
              .build());
    }
    for (int i = 0; i < alternatives.size(); i++) {
      equivs.add(
          TextEquivBuilder.builder()
              .index(equivs.size() + 1)
              .conf(i < alternativeConfidences.size() ? alternativeConfidences.get(i) : null)
              .unicode(alternatives.get(i))
              .build());
    }
    return equivs;
  }

  /** Appends a hyphen to the last word and extends the word to it. */
  private void hyphen(List<Word> words, Polygon lineBox) throws XMLStreamException {
    Attrs a = attrs();
    String content = a.string("CONTENT");
    Double hpos = a.decimal("HPOS");
    Double hypWidth = a.decimal("WIDTH");
    a.string("VPOS");
    leaf(a);
    if (words.isEmpty()) {
      a.ignored("TextLine: HYP without a String before it");
      return;
    }
    String hyphen = "-";
    if (content != null) {
      int code = 0;
      try {
        code = Integer.parseInt(content.strip());
      } catch (NumberFormatException e) {
        // the character itself
      }
      hyphen = code > 0 ? Character.toString(code) : content;
    }
    Word word = words.getLast();
    List<TextEquiv> equivs = new ArrayList<>(word.textEquivs());
    if (equivs.isEmpty()) {
      equivs.add(TextEquivBuilder.builder().unicode(hyphen).build());
    } else {
      TextEquiv first = equivs.getFirst();
      equivs.set(0, first.withUnicode(first.unicode() + hyphen));
    }
    Polygon polygon = word.coords();
    int right =
        hpos != null && hypWidth != null
            ? x((int) hpos.doubleValue() + (int) hypWidth.doubleValue() - 1)
            : lineBox.isEmpty() ? 0 : lineBox.boundingBox().right();
    if (polygon.size() == 4 && polygon.boundingBox().right() < right) {
      polygon =
          polygon
              .withPoint(1, new Point(right, polygon.points().get(1).y()))
              .withPoint(2, new Point(right, polygon.points().get(2).y()));
    }
    words.set(words.size() - 1, word.withTextEquivs(equivs).withCoords(polygon));
  }

  private Glyph glyph() throws XMLStreamException {
    Attrs a = attrs();
    String id = ids.id(a.string("ID"), "g", a);
    Polygon coords = box(a);
    String content = a.string("CONTENT");
    Double glyphConf = a.decimal("GC");
    TextStyle style = style(refs(a.string("STYLEREFS")), null, a);
    List<String> variants = new ArrayList<>();
    List<@Nullable Double> variantConfidences = new ArrayList<>();
    while (nextChild()) {
      switch (name()) {
        case "Shape" -> coords = shapeOr(coords);
        case "Variant" -> {
          Attrs variant = attrs();
          String text = variant.string("CONTENT");
          Double confidence = variant.decimal("VC");
          leaf(variant);
          if (text != null) {
            variants.add(text);
            variantConfidences.add(confidence);
          }
        }
        default -> ignore("Glyph");
      }
    }
    a.done();
    return GlyphBuilder.builder()
        .id(id)
        .coords(coords)
        .textStyle(style)
        .textEquivs(textEquivs(content, glyphConf, variants, variantConfidences))
        .build();
  }

  private Region illustration() throws XMLStreamException {
    Attrs a = attrs();
    Block block = block(a, "r");
    String kind = kind(a.string("TYPE"), a);
    String file = a.string("FILEID");
    Polygon coords = block.coords();
    while (nextChild()) {
      if (name().equals("Shape")) {
        coords = shapeOr(coords);
      } else {
        ignore("Illustration");
      }
    }
    a.done();
    return region(kind == null ? "ImageRegion" : kind, block, coords, file, List.of());
  }

  private Region graphicalElement() throws XMLStreamException {
    Attrs a = attrs();
    Block block = block(a, "r");
    Polygon coords = block.coords();
    while (nextChild()) {
      if (name().equals("Shape")) {
        coords = shapeOr(coords);
      } else {
        ignore("GraphicalElement");
      }
    }
    a.done();
    return region("SeparatorRegion", block, coords, null, List.of());
  }

  /** A region of the PAGE type its TYPE names, or else its blocks in its place. */
  private List<Region> composedBlock() throws XMLStreamException {
    Attrs a = attrs();
    Block block = block(a, "r");
    String typeName = a.string("TYPE");
    String kind = typeName == null ? null : kind(typeName, null);
    String file = a.string("FILEID");
    Polygon coords = block.coords();
    List<Region> children = new ArrayList<>();
    while (nextChild()) {
      if (name().equals("Shape")) {
        coords = shapeOr(coords);
      } else {
        block(children, "ComposedBlock");
      }
    }
    a.done();
    if (kind == null) {
      a.changed("ComposedBlock: replaced by its blocks, the model has no composed blocks");
      return children;
    }
    return List.of(region(kind, block, coords, file, children));
  }

  /** The PAGE region type an ALTO TYPE names, by element name or short name. */
  private static @Nullable String kind(@Nullable String value, @Nullable Attrs a) {
    if (value == null) {
      return null;
    }
    String name = value.strip().toLowerCase(Locale.ROOT);
    for (String kind : REGION_KINDS) {
      String lower = kind.toLowerCase(Locale.ROOT);
      if (name.equals(lower)
          || name.equals(lower.substring(0, lower.length() - "region".length()))) {
        return kind;
      }
    }
    if (a != null) {
      a.changed(
          "Illustration: TYPE '" + value + "' is not a PAGE region type, read as ImageRegion");
    }
    return null;
  }

  private Region region(
      String kind, Block block, Polygon coords, @Nullable String file, List<Region> nested) {
    String id = block.id();
    Double o = block.rotation();
    List<AlternativeImage> images =
        file == null ? List.of() : List.of(new AlternativeImage(file, null, null));
    List<LabelGroup> labels = new ArrayList<>();
    TextType textType = tagRefs(block, labels, kind.equals("TextRegion"));
    if (o != null
        && Set.of("NoiseRegion", "UnknownRegion", "CustomRegion", "FrameRegion").contains(kind)) {
      block.at().ignored(block.at().element() + ": ROTATION, a " + kind + " has no orientation");
    }
    if (!block.styleRefs().isEmpty() && !kind.equals("TextRegion")) {
      block.at().ignored(block.at().element() + ": STYLEREFS");
    }
    return switch (kind) {
      case "TextRegion" ->
          TextRegionBuilder.builder()
              .id(id)
              .coords(coords)
              .orientation(o)
              .type(textType)
              .alternativeImages(images)
              .labels(labels)
              .regions(nested)
              .build();
      case "ImageRegion" ->
          ImageRegionBuilder.builder()
              .id(id)
              .coords(coords)
              .orientation(o)
              .alternativeImages(images)
              .labels(labels)
              .regions(nested)
              .build();
      case "LineDrawingRegion" ->
          LineDrawingRegionBuilder.builder()
              .id(id)
              .coords(coords)
              .orientation(o)
              .alternativeImages(images)
              .labels(labels)
              .regions(nested)
              .build();
      case "GraphicRegion" ->
          GraphicRegionBuilder.builder()
              .id(id)
              .coords(coords)
              .orientation(o)
              .alternativeImages(images)
              .labels(labels)
              .regions(nested)
              .build();
      case "TableRegion" ->
          TableRegionBuilder.builder()
              .id(id)
              .coords(coords)
              .orientation(o)
              .alternativeImages(images)
              .labels(labels)
              .regions(nested)
              .build();
      case "ChartRegion" ->
          ChartRegionBuilder.builder()
              .id(id)
              .coords(coords)
              .orientation(o)
              .alternativeImages(images)
              .labels(labels)
              .regions(nested)
              .build();
      case "MapRegion" ->
          MapRegionBuilder.builder()
              .id(id)
              .coords(coords)
              .orientation(o)
              .alternativeImages(images)
              .labels(labels)
              .regions(nested)
              .build();
      case "SeparatorRegion" ->
          SeparatorRegionBuilder.builder()
              .id(id)
              .coords(coords)
              .orientation(o)
              .alternativeImages(images)
              .labels(labels)
              .regions(nested)
              .build();
      case "MathsRegion" ->
          MathsRegionBuilder.builder()
              .id(id)
              .coords(coords)
              .orientation(o)
              .alternativeImages(images)
              .labels(labels)
              .regions(nested)
              .build();
      case "ChemRegion" ->
          ChemRegionBuilder.builder()
              .id(id)
              .coords(coords)
              .orientation(o)
              .alternativeImages(images)
              .labels(labels)
              .regions(nested)
              .build();
      case "MusicRegion" ->
          MusicRegionBuilder.builder()
              .id(id)
              .coords(coords)
              .orientation(o)
              .alternativeImages(images)
              .labels(labels)
              .regions(nested)
              .build();
      case "AdvertRegion" ->
          AdvertRegionBuilder.builder()
              .id(id)
              .coords(coords)
              .orientation(o)
              .alternativeImages(images)
              .labels(labels)
              .regions(nested)
              .build();
      case "FormRegion" ->
          FormRegionBuilder.builder()
              .id(id)
              .coords(coords)
              .orientation(o)
              .alternativeImages(images)
              .labels(labels)
              .regions(nested)
              .build();
      case "NoiseRegion" ->
          NoiseRegionBuilder.builder()
              .id(id)
              .coords(coords)
              .alternativeImages(images)
              .labels(labels)
              .regions(nested)
              .build();
      case "UnknownRegion" ->
          UnknownRegionBuilder.builder()
              .id(id)
              .coords(coords)
              .alternativeImages(images)
              .labels(labels)
              .regions(nested)
              .build();
      case "CustomRegion" ->
          CustomRegionBuilder.builder()
              .id(id)
              .coords(coords)
              .alternativeImages(images)
              .labels(labels)
              .regions(nested)
              .build();
      case "FrameRegion" ->
          FrameRegionBuilder.builder()
              .id(id)
              .coords(coords)
              .alternativeImages(images)
              .labels(labels)
              .regions(nested)
              .build();
      default -> throw new IllegalArgumentException(kind);
    };
  }

  // ---- Shapes

  /** The shape in a {@code Shape} element, or the given outline if it has none that is usable. */
  private Polygon shapeOr(Polygon outline) throws XMLStreamException {
    Polygon shape = shape();
    return shape != null ? shape : outline;
  }

  private @Nullable Polygon shape() throws XMLStreamException {
    Polygon result = null;
    while (nextChild()) {
      Attrs a = attrs();
      Polygon shape =
          switch (name()) {
            case "Polygon" -> polygon(a);
            case "Ellipse" -> {
              Double cx = a.decimal("HPOS");
              Double cy = a.decimal("VPOS");
              Double w = a.decimal("HLENGTH");
              Double h = a.decimal("VLENGTH");
              yield cx == null || cy == null || w == null || h == null
                  ? incomplete(a)
                  : ellipse(cx, cy, w / 2, h / 2);
            }
            case "Circle" -> {
              Double cx = a.decimal("HPOS");
              Double cy = a.decimal("VPOS");
              Double radius = a.decimal("RADIUS");
              yield cx == null || cy == null || radius == null
                  ? incomplete(a)
                  : ellipse(cx, cy, radius, radius);
            }
            default -> {
              a.ignored("Shape: element " + name());
              yield null;
            }
          };
      leaf(a);
      if (shape != null && shape.size() >= 3) {
        result = scaled(shape);
      }
    }
    return result;
  }

  private static @Nullable Polygon incomplete(Attrs a) {
    a.ignored(a.element() + ": incomplete shape, the box is used");
    return null;
  }

  private static @Nullable Polygon polygon(Attrs a) {
    String value = a.string("POINTS");
    List<Point> points = value == null ? null : points(value);
    if (points == null || points.size() < 3) {
      a.ignored("Polygon: POINTS '" + value + "' are not a polygon, the box is used");
      return null;
    }
    return new Polygon(points);
  }

  /** Points as {@code "x,y x,y"} or {@code "x y x y"}, or null if they can't be parsed. */
  private static @Nullable List<Point> points(String value) {
    String[] tokens = value.strip().split("[\\s,]+");
    if (tokens.length % 2 != 0) {
      return null;
    }
    List<Point> points = new ArrayList<>(tokens.length / 2);
    try {
      for (int i = 0; i < tokens.length; i += 2) {
        points.add(
            new Point(
                (int) Double.parseDouble(tokens[i]), (int) Double.parseDouble(tokens[i + 1])));
      }
    } catch (NumberFormatException e) {
      return null;
    }
    return points;
  }

  /** An ellipse as a polygon, a point every half degree. */
  private static Polygon ellipse(double centreX, double centreY, double radiusX, double radiusY) {
    Polygon.Builder polygon = Polygon.builder();
    int previousX = -1;
    int previousY = -1;
    for (double angle = 0; angle < 6.2832; angle += 0.00873) {
      int px = (int) (centreX + Math.cos(angle) * radiusX);
      int py = (int) (centreY + Math.sin(angle) * radiusY);
      if (px != previousX || py != previousY) {
        polygon.add(px, py);
      }
      previousX = px;
      previousY = py;
    }
    return polygon.build();
  }

  // ---- Reading order

  /** The reading order of the {@code IDNEXT} chains: one ordered group, or one per chain. */
  private @Nullable ReadingOrder readingOrder() {
    Set<String> regionIds = new HashSet<>();
    collectIds(regions, regionIds);
    Map<String, String> links = new LinkedHashMap<>();
    next.forEach(
        (from, link) -> {
          if (!regionIds.contains(from)) {
            link.at().ignored(link.at().element() + ": IDNEXT of a block that was replaced");
          } else if (!regionIds.contains(link.to())) {
            link.at()
                .ignored(link.at().element() + ": IDNEXT '" + link.to() + "' refers to no block");
          } else if (!link.to().equals(from)) {
            links.put(from, link.to());
          }
        });
    if (links.isEmpty()) {
      return null;
    }
    Set<String> targets = new HashSet<>(links.values());
    Set<String> visited = new HashSet<>();
    List<List<String>> chains = new ArrayList<>();
    for (boolean cycles : new boolean[] {false, true}) {
      for (String start : links.keySet()) {
        if (visited.contains(start) || (!cycles && targets.contains(start))) {
          continue;
        }
        List<String> chain = new ArrayList<>();
        for (String id = start; id != null && visited.add(id); id = links.get(id)) {
          chain.add(id);
        }
        if (chain.size() > 1) {
          chains.add(chain);
        }
      }
    }
    if (chains.size() == 1) {
      return ReadingOrderBuilder.builder().group(ordered(chains.getFirst())).build();
    }
    List<GroupMember> groups = new ArrayList<>();
    for (List<String> chain : chains) {
      groups.add(ordered(chain));
    }
    return ReadingOrderBuilder.builder()
        .group(UnorderedGroupBuilder.builder().id(ids.generate("ro")).members(groups).build())
        .build();
  }

  private com.maxnth.page4j.model.OrderedGroup ordered(List<String> chain) {
    return OrderedGroupBuilder.builder()
        .id(ids.generate("ro"))
        .members(chain.stream().<GroupMember>map(RegionRef::new).toList())
        .build();
  }

  private static void collectIds(List<Region> regions, Set<String> into) {
    for (Region region : regions) {
      into.add(region.id());
      collectIds(region.regions(), into);
    }
  }
}
