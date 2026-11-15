package com.maxnth.page4j.pagexml.internal;

import com.maxnth.page4j.geometry.Point;
import com.maxnth.page4j.geometry.Polygon;
import com.maxnth.page4j.model.AdvertRegionBuilder;
import com.maxnth.page4j.model.AlternativeImage;
import com.maxnth.page4j.model.ChartRegionBuilder;
import com.maxnth.page4j.model.ChemRegionBuilder;
import com.maxnth.page4j.model.CustomRegionBuilder;
import com.maxnth.page4j.model.FormRegionBuilder;
import com.maxnth.page4j.model.FrameRegionBuilder;
import com.maxnth.page4j.model.Glyph;
import com.maxnth.page4j.model.GlyphBuilder;
import com.maxnth.page4j.model.GraphemeBuilder;
import com.maxnth.page4j.model.GraphemeElement;
import com.maxnth.page4j.model.GraphemeGroupBuilder;
import com.maxnth.page4j.model.GraphicRegionBuilder;
import com.maxnth.page4j.model.Grid;
import com.maxnth.page4j.model.GridAddPoints;
import com.maxnth.page4j.model.GridPoints;
import com.maxnth.page4j.model.Group;
import com.maxnth.page4j.model.GroupMember;
import com.maxnth.page4j.model.ImageRegionBuilder;
import com.maxnth.page4j.model.InvalidPageException;
import com.maxnth.page4j.model.Label;
import com.maxnth.page4j.model.LabelGroup;
import com.maxnth.page4j.model.Layer;
import com.maxnth.page4j.model.LineDrawingRegionBuilder;
import com.maxnth.page4j.model.MapRegionBuilder;
import com.maxnth.page4j.model.MathsRegionBuilder;
import com.maxnth.page4j.model.Metadata;
import com.maxnth.page4j.model.MetadataItem;
import com.maxnth.page4j.model.MusicRegionBuilder;
import com.maxnth.page4j.model.NoiseRegionBuilder;
import com.maxnth.page4j.model.NonPrintingCharBuilder;
import com.maxnth.page4j.model.OrderedGroupBuilder;
import com.maxnth.page4j.model.Page;
import com.maxnth.page4j.model.ReadingOrder;
import com.maxnth.page4j.model.Region;
import com.maxnth.page4j.model.RegionRef;
import com.maxnth.page4j.model.Relation;
import com.maxnth.page4j.model.SeparatorRegionBuilder;
import com.maxnth.page4j.model.TableCellRole;
import com.maxnth.page4j.model.TableRegionBuilder;
import com.maxnth.page4j.model.TextEquiv;
import com.maxnth.page4j.model.TextLine;
import com.maxnth.page4j.model.TextLineBuilder;
import com.maxnth.page4j.model.TextRegionBuilder;
import com.maxnth.page4j.model.TextStyle;
import com.maxnth.page4j.model.TextStyleBuilder;
import com.maxnth.page4j.model.UnknownRegionBuilder;
import com.maxnth.page4j.model.UnorderedGroupBuilder;
import com.maxnth.page4j.model.UserAttribute;
import com.maxnth.page4j.model.Word;
import com.maxnth.page4j.model.WordBuilder;
import com.maxnth.page4j.model.value.Align;
import com.maxnth.page4j.model.value.CharType;
import com.maxnth.page4j.model.value.ChartType;
import com.maxnth.page4j.model.value.Colour;
import com.maxnth.page4j.model.value.ColourDepth;
import com.maxnth.page4j.model.value.GraphicsType;
import com.maxnth.page4j.model.value.GroupType;
import com.maxnth.page4j.model.value.Language;
import com.maxnth.page4j.model.value.MetadataItemType;
import com.maxnth.page4j.model.value.Mirrored;
import com.maxnth.page4j.model.value.PageDateTime;
import com.maxnth.page4j.model.value.PageType;
import com.maxnth.page4j.model.value.Production;
import com.maxnth.page4j.model.value.ReadingDirection;
import com.maxnth.page4j.model.value.RelationType;
import com.maxnth.page4j.model.value.ResolutionUnit;
import com.maxnth.page4j.model.value.Script;
import com.maxnth.page4j.model.value.TextDataType;
import com.maxnth.page4j.model.value.TextLineOrder;
import com.maxnth.page4j.model.value.TextType;
import com.maxnth.page4j.model.value.UnderlineStyle;
import com.maxnth.page4j.model.value.UserAttributeType;
import com.maxnth.page4j.pagexml.PageReadException;
import com.maxnth.page4j.pagexml.PageXmlReadResult;
import com.maxnth.page4j.pagexml.PageXmlVersion;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.jspecify.annotations.Nullable;

/**
 * Reads PAGE XML into the model, all versions from 2009-03-16 to 2024-07-15.
 *
 * <p>A document is first validated against the schema of its version, then parsed with StAX: one
 * method per element type, context in parameters and locals. Every attribute must be read and every
 * element known, so content can't be dropped silently.
 *
 * <p>The 2009 and 2010 versions differ in shape, not only in content: coordinates are {@code Point}
 * elements, a text region's style is given by its own attributes ({@code textColour}, {@code
 * bgColour}, {@code reverseVideo}, {@code fontSize}, {@code kerning}; read as its {@link
 * TextStyle}), and up to 2010-01-12 the reading order can hold several groups and region
 * references. These are read into a root {@code UnorderedGroup} with a generated ID, since the
 * model's reading order has one root group.
 */
public final class PageXmlReader {

  private static final XMLInputFactory FACTORY = newFactory();

  private static final Set<String> REGIONS =
      Set.of(
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

  private final XMLStreamReader reader;
  private final boolean legacy;

  /** A legacy reading order with several top-level members, wrapped once all IDs are known. */
  private @Nullable LegacyReadingOrder legacyReadingOrder;

  private record LegacyReadingOrder(@Nullable Double conf, List<GroupMember> members) {}

  private PageXmlReader(XMLStreamReader reader, PageXmlVersion version) {
    this.reader = reader;
    this.legacy = version.compareTo(PageXmlVersion.V2013_07_15) < 0;
  }

  /** Reads and closes the stream. */
  public static PageXmlReadResult read(InputStream in) throws IOException {
    try (in) {
      return read(in.readAllBytes());
    }
  }

  public static PageXmlReadResult read(byte[] xml) throws PageReadException {
    PageXmlVersion version = detectVersion(xml);
    PageXmlSchemas.validate(xml, version);
    XMLStreamReader stream = null;
    try {
      stream = FACTORY.createXMLStreamReader(new ByteArrayInputStream(xml));
      toRoot(stream);
      Page page = new PageXmlReader(stream, version).pcGts();
      return new PageXmlReadResult(page, version);
    } catch (XMLStreamException e) {
      Location location = e.getLocation();
      throw new PageReadException(
          location == null ? -1 : location.getLineNumber(),
          location == null ? -1 : location.getColumnNumber(),
          String.valueOf(e.getMessage()),
          e);
    } catch (ReadError e) {
      throw e.toException();
    } finally {
      if (stream != null) {
        try {
          stream.close();
        } catch (XMLStreamException e) {
          // Reading is done; nothing to release beyond the in-memory stream.
        }
      }
    }
  }

  /** The version given by the namespace of the root element. */
  public static PageXmlVersion detectVersion(byte[] xml) throws PageReadException {
    try {
      XMLStreamReader stream = FACTORY.createXMLStreamReader(new ByteArrayInputStream(xml));
      try {
        toRoot(stream);
        String namespace = stream.getNamespaceURI();
        if (!"PcGts".equals(stream.getLocalName()) || namespace == null) {
          throw new PageReadException(
              stream.getLocation().getLineNumber(),
              stream.getLocation().getColumnNumber(),
              "Not a PAGE document: the root element is " + stream.getName(),
              null);
        }
        return PageXmlVersion.fromNamespace(namespace)
            .orElseThrow(
                () ->
                    new PageReadException(
                        stream.getLocation().getLineNumber(),
                        stream.getLocation().getColumnNumber(),
                        "Unknown PAGE namespace '" + namespace + "'",
                        null));
      } finally {
        stream.close();
      }
    } catch (XMLStreamException e) {
      Location location = e.getLocation();
      throw new PageReadException(
          location == null ? -1 : location.getLineNumber(),
          location == null ? -1 : location.getColumnNumber(),
          "Not well-formed XML: " + e.getMessage(),
          e);
    }
  }

  /** Moves past the prolog (comments, processing instructions, DOCTYPE) to the root element. */
  private static void toRoot(XMLStreamReader stream) throws XMLStreamException {
    while (stream.next() != XMLStreamConstants.START_ELEMENT) {
      // skip
    }
  }

  private static XMLInputFactory newFactory() {
    XMLInputFactory factory = XMLInputFactory.newFactory();
    factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
    factory.setProperty(XMLInputFactory.IS_COALESCING, true);
    // A PAGE document needs no DTD; without DTD support no entity can be declared or resolved.
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    return factory;
  }

  // ---- Navigation

  /** Moves to the next child start tag, or to the end tag of the current element (false). */
  private boolean nextChild() throws XMLStreamException {
    return reader.nextTag() == XMLStreamConstants.START_ELEMENT;
  }

  private String name() {
    return reader.getLocalName();
  }

  private Attributes attributes() {
    return Attributes.of(reader);
  }

  /** Finishes an element without children, after the caller read all its attributes. */
  private void end(Attributes attributes) throws XMLStreamException {
    if (nextChild()) {
      throw unexpected();
    }
    attributes.done();
  }

  private String text() throws XMLStreamException {
    return reader.getElementText();
  }

  private ReadError unexpected() {
    Location location = reader.getLocation();
    return new ReadError(
        location.getLineNumber(),
        location.getColumnNumber(),
        "Unexpected element " + reader.getLocalName());
  }

  private static <T> T required(@Nullable T value, String child, Attributes parent) {
    if (value == null) {
      throw parent.error(parent.element() + " has no " + child);
    }
    return value;
  }

  // ---- Document and metadata

  private Page pcGts() throws XMLStreamException {
    Attributes a = attributes();
    String pcGtsId = a.string("pcGtsId");
    Metadata metadata = null;
    Page.Builder page = null;
    while (nextChild()) {
      switch (name()) {
        case "Metadata" -> metadata = metadata();
        case "Page" -> page = page();
        default -> throw unexpected();
      }
    }
    a.done();
    Page.Builder builder = required(page, "Page", a);
    try {
      Page result = builder.pcGtsId(pcGtsId).metadata(required(metadata, "Metadata", a)).build();
      LegacyReadingOrder order = legacyReadingOrder;
      if (order != null) {
        String id = "readingOrder";
        for (int i = 2; result.find(id).isPresent() || id.equals(pcGtsId); i++) {
          id = "readingOrder" + i;
        }
        Group root = UnorderedGroupBuilder.builder().id(id).members(order.members()).build();
        result = result.withReadingOrder(new ReadingOrder(order.conf(), root));
      }
      return result;
    } catch (InvalidPageException e) {
      throw a.error(e.getMessage() == null ? "Invalid page" : e.getMessage());
    }
  }

  private Metadata metadata() throws XMLStreamException {
    Attributes a = attributes();
    String externalRef = a.string("externalRef");
    String creator = null;
    PageDateTime created = null;
    PageDateTime lastChange = null;
    String comments = null;
    List<UserAttribute> userAttributes = List.of();
    List<MetadataItem> items = new ArrayList<>();
    while (nextChild()) {
      switch (name()) {
        case "Creator" -> creator = text();
        case "Created" -> created = dateTime();
        case "LastChange" -> lastChange = dateTime();
        case "Comments" -> comments = text();
        case "UserDefined" -> userAttributes = userDefined();
        case "MetadataItem" -> items.add(metadataItem());
        default -> throw unexpected();
      }
    }
    a.done();
    return new Metadata(
        externalRef,
        required(creator, "Creator", a),
        required(created, "Created", a),
        required(lastChange, "LastChange", a),
        comments,
        userAttributes,
        items);
  }

  private PageDateTime dateTime() throws XMLStreamException {
    Location location = reader.getLocation();
    String text = text();
    try {
      return PageDateTime.parse(text.strip());
    } catch (IllegalArgumentException e) {
      throw new ReadError(
          location.getLineNumber(), location.getColumnNumber(), "Invalid timestamp '" + text + "'");
    }
  }

  private MetadataItem metadataItem() throws XMLStreamException {
    Attributes a = attributes();
    MetadataItemType type = a.value("type", MetadataItemType::fromXml);
    String name = a.string("name");
    String value = a.requiredString("value");
    PageDateTime date = a.dateTime("date");
    List<LabelGroup> labels = new ArrayList<>();
    while (nextChild()) {
      switch (name()) {
        case "Labels" -> labels.add(labels());
        default -> throw unexpected();
      }
    }
    a.done();
    return new MetadataItem(type, name, value, date, labels);
  }

  private List<UserAttribute> userDefined() throws XMLStreamException {
    Attributes a = attributes();
    List<UserAttribute> attributes = new ArrayList<>();
    while (nextChild()) {
      switch (name()) {
        case "UserAttribute" -> {
          Attributes u = attributes();
          attributes.add(
              new UserAttribute(
                  u.string("name"),
                  u.string("description"),
                  u.value("type", UserAttributeType::fromXml),
                  u.string("value")));
          end(u);
        }
        default -> throw unexpected();
      }
    }
    a.done();
    return attributes;
  }

  private LabelGroup labels() throws XMLStreamException {
    Attributes a = attributes();
    String externalModel = a.string("externalModel");
    String externalId = a.string("externalId");
    String prefix = a.string("prefix");
    String comments = a.string("comments");
    List<Label> labels = new ArrayList<>();
    while (nextChild()) {
      switch (name()) {
        case "Label" -> {
          Attributes l = attributes();
          labels.add(new Label(l.requiredString("value"), l.string("type"), l.string("comments")));
          end(l);
        }
        default -> throw unexpected();
      }
    }
    a.done();
    return new LabelGroup(externalModel, externalId, prefix, comments, labels);
  }

  // ---- Page

  private Page.Builder page() throws XMLStreamException {
    Attributes a = attributes();
    Page.Builder page =
        Page.builder()
            .imageFilename(a.requiredString("imageFilename"))
            .imageWidth(a.requiredInteger("imageWidth"))
            .imageHeight(a.requiredInteger("imageHeight"))
            .imageXResolution(a.decimal("imageXResolution"))
            .imageYResolution(a.decimal("imageYResolution"))
            .imageResolutionUnit(a.value("imageResolutionUnit", ResolutionUnit::fromXml))
            .custom(a.string("custom"))
            .comments(a.string("comments"))
            .orientation(a.decimal("orientation"))
            .type(a.value("type", PageType::fromXml))
            .primaryLanguage(a.value("primaryLanguage", Language::fromXml))
            .secondaryLanguage(a.value("secondaryLanguage", Language::fromXml))
            .customLanguages(a.string("customLanguages"))
            .primaryScript(a.value("primaryScript", Script::fromXml))
            .secondaryScript(a.value("secondaryScript", Script::fromXml))
            .customScripts(a.string("customScripts"))
            .readingDirection(a.value("readingDirection", ReadingDirection::fromXml))
            .textLineOrder(a.value("textLineOrder", TextLineOrder::fromXml))
            .conf(a.decimal("conf"));
    while (nextChild()) {
      String child = name();
      switch (child) {
        case "AlternativeImage" -> page.addAlternativeImages(alternativeImage());
        case "Border" -> page.border(coordsContainer());
        case "PrintSpace" -> page.printSpace(coordsContainer());
        case "ReadingOrder" -> page.readingOrder(readingOrder());
        case "Layers" -> page.layers(layers());
        case "Relations" -> page.relations(relations());
        case "TextStyle" -> page.textStyle(textStyle());
        case "UserDefined" -> page.userAttributes(userDefined());
        case "Labels" -> page.addLabels(labels());
        default -> {
          if (!REGIONS.contains(child)) {
            throw unexpected();
          }
          page.addRegions(region(child));
        }
      }
    }
    a.done();
    return page;
  }

  /** {@code Border} and {@code PrintSpace}: an element holding only {@code Coords}. */
  private Polygon coordsContainer() throws XMLStreamException {
    Attributes a = attributes();
    Polygon coords = null;
    while (nextChild()) {
      switch (name()) {
        case "Coords" -> coords = coords();
        default -> throw unexpected();
      }
    }
    a.done();
    return required(coords, "Coords", a);
  }

  /**
   * {@code Coords}, {@code Baseline} and the other script lines. Up to 2010 coordinates are {@code
   * Point} elements, possibly none.
   */
  private Polygon coords() throws XMLStreamException {
    Attributes a = attributes();
    if (!legacy) {
      Polygon polygon = a.polygon();
      end(a);
      return polygon;
    }
    List<Point> points = new ArrayList<>();
    while (nextChild()) {
      switch (name()) {
        case "Point" -> {
          Attributes p = attributes();
          points.add(new Point(p.requiredInteger("x"), p.requiredInteger("y")));
          end(p);
        }
        default -> throw unexpected();
      }
    }
    a.done();
    return new Polygon(points);
  }

  private AlternativeImage alternativeImage() throws XMLStreamException {
    Attributes a = attributes();
    var image =
        new AlternativeImage(a.requiredString("filename"), a.string("comments"), a.decimal("conf"));
    end(a);
    return image;
  }

  private TextStyle textStyle() throws XMLStreamException {
    Attributes a = attributes();
    var style =
        new TextStyle(
            a.string("fontFamily"),
            a.bool("serif"),
            a.bool("monospace"),
            a.decimal("fontSize"),
            a.integer("xHeight"),
            a.integer("kerning"),
            a.value("textColour", Colour::fromXml),
            a.integer("textColourRgb"),
            a.value("bgColour", Colour::fromXml),
            a.integer("bgColourRgb"),
            a.bool("reverseVideo"),
            a.bool("bold"),
            a.bool("italic"),
            a.bool("underlined"),
            a.value("underlineStyle", UnderlineStyle::fromXml),
            a.bool("subscript"),
            a.bool("superscript"),
            a.bool("strikethrough"),
            a.bool("smallCaps"),
            a.bool("letterSpaced"));
    end(a);
    return style;
  }

  private TextEquiv textEquiv() throws XMLStreamException {
    Attributes a = attributes();
    Integer index = a.integer("index");
    Double conf = a.decimal("conf");
    TextDataType dataType = a.value("dataType", TextDataType::fromXml);
    String dataTypeDetails = a.string("dataTypeDetails");
    String comments = a.string("comments");
    String plainText = null;
    String unicode = null;
    while (nextChild()) {
      switch (name()) {
        case "PlainText" -> plainText = text();
        case "Unicode" -> unicode = text();
        default -> throw unexpected();
      }
    }
    a.done();
    return new TextEquiv(
        index,
        conf,
        dataType,
        dataTypeDetails,
        comments,
        plainText,
        required(unicode, "Unicode", a));
  }

  // ---- Regions

  /** The content shared by all region types, read before the type-specific part is built. */
  private static final class RegionContent {
    final List<AlternativeImage> alternativeImages = new ArrayList<>();
    @Nullable Polygon coords;
    List<UserAttribute> userAttributes = List.of();
    final List<LabelGroup> labels = new ArrayList<>();
    @Nullable TableCellRole tableCellRole;
    final List<Region> regions = new ArrayList<>();
    final List<TextLine> textLines = new ArrayList<>();
    final List<TextEquiv> textEquivs = new ArrayList<>();
    @Nullable TextStyle textStyle;
    @Nullable Grid grid;
  }

  private Region region(String type) throws XMLStreamException {
    Attributes a = attributes();
    String id = a.requiredString("id");
    String custom = a.string("custom");
    String comments = a.string("comments");
    Boolean continuation = a.bool("continuation");
    RegionContent c = new RegionContent();
    while (nextChild()) {
      String child = name();
      switch (child) {
        case "AlternativeImage" -> c.alternativeImages.add(alternativeImage());
        case "Coords" -> c.coords = coords();
        case "UserDefined" -> c.userAttributes = userDefined();
        case "Labels" -> c.labels.add(labels());
        case "Roles" -> c.tableCellRole = roles();
        case "TextLine" -> c.textLines.add(textLine());
        case "TextEquiv" -> c.textEquivs.add(textEquiv());
        case "TextStyle" -> c.textStyle = textStyle();
        case "Grid" -> c.grid = grid();
        default -> {
          if (!REGIONS.contains(child)) {
            throw unexpected();
          }
          c.regions.add(region(child));
        }
      }
    }
    boolean text = type.equals("TextRegion");
    if (!text && (!c.textLines.isEmpty() || !c.textEquivs.isEmpty() || c.textStyle != null)) {
      throw a.error(type + " can't contain text");
    }
    if (c.grid != null && !type.equals("TableRegion")) {
      throw a.error(type + " can't contain a Grid");
    }
    Polygon coords = required(c.coords, "Coords", a);
    if (legacy && text) {
      c.textStyle = legacyTextStyle(a);
    }
    Region region =
        switch (type) {
          case "TextRegion" ->
              TextRegionBuilder.builder()
                  .id(id)
                  .custom(custom)
                  .comments(comments)
                  .continuation(continuation)
                  .orientation(a.decimal("orientation"))
                  .mirrored(a.value("mirrored", Mirrored::fromXml))
                  .type(a.value("type", TextType::fromXml))
                  .leading(a.integer("leading"))
                  .readingDirection(a.value("readingDirection", ReadingDirection::fromXml))
                  .textLineOrder(a.value("textLineOrder", TextLineOrder::fromXml))
                  .readingOrientation(a.decimal("readingOrientation"))
                  .indented(a.bool("indented"))
                  .align(a.value("align", Align::fromXml))
                  .primaryLanguage(a.value("primaryLanguage", Language::fromXml))
                  .secondaryLanguage(a.value("secondaryLanguage", Language::fromXml))
                  .customLanguages(a.string("customLanguages"))
                  .primaryScript(a.value("primaryScript", Script::fromXml))
                  .secondaryScript(a.value("secondaryScript", Script::fromXml))
                  .customScripts(a.string("customScripts"))
                  .production(a.value("production", Production::fromXml))
                  .alternativeImages(c.alternativeImages)
                  .coords(coords)
                  .userAttributes(c.userAttributes)
                  .labels(c.labels)
                  .tableCellRole(c.tableCellRole)
                  .regions(c.regions)
                  .textLines(c.textLines)
                  .textEquivs(c.textEquivs)
                  .textStyle(c.textStyle)
                  .build();
          case "ImageRegion" ->
              ImageRegionBuilder.builder()
                  .id(id)
                  .custom(custom)
                  .comments(comments)
                  .continuation(continuation)
                  .orientation(a.decimal("orientation"))
                  .mirrored(a.value("mirrored", Mirrored::fromXml))
                  .colourDepth(a.value("colourDepth", ColourDepth::fromXml))
                  .bgColour(a.value("bgColour", Colour::fromXml))
                  .embText(a.bool("embText"))
                  .alternativeImages(c.alternativeImages)
                  .coords(coords)
                  .userAttributes(c.userAttributes)
                  .labels(c.labels)
                  .tableCellRole(c.tableCellRole)
                  .regions(c.regions)
                  .build();
          case "LineDrawingRegion" ->
              LineDrawingRegionBuilder.builder()
                  .id(id)
                  .custom(custom)
                  .comments(comments)
                  .continuation(continuation)
                  .orientation(a.decimal("orientation"))
                  .mirrored(a.value("mirrored", Mirrored::fromXml))
                  .penColour(a.value("penColour", Colour::fromXml))
                  .bgColour(a.value("bgColour", Colour::fromXml))
                  .embText(a.bool("embText"))
                  .alternativeImages(c.alternativeImages)
                  .coords(coords)
                  .userAttributes(c.userAttributes)
                  .labels(c.labels)
                  .tableCellRole(c.tableCellRole)
                  .regions(c.regions)
                  .build();
          case "GraphicRegion" ->
              GraphicRegionBuilder.builder()
                  .id(id)
                  .custom(custom)
                  .comments(comments)
                  .continuation(continuation)
                  .orientation(a.decimal("orientation"))
                  .mirrored(a.value("mirrored", Mirrored::fromXml))
                  .type(a.value("type", GraphicsType::fromXml))
                  .numColours(a.integer("numColours"))
                  .embText(a.bool("embText"))
                  .alternativeImages(c.alternativeImages)
                  .coords(coords)
                  .userAttributes(c.userAttributes)
                  .labels(c.labels)
                  .tableCellRole(c.tableCellRole)
                  .regions(c.regions)
                  .build();
          case "TableRegion" ->
              TableRegionBuilder.builder()
                  .id(id)
                  .custom(custom)
                  .comments(comments)
                  .continuation(continuation)
                  .orientation(a.decimal("orientation"))
                  .mirrored(a.value("mirrored", Mirrored::fromXml))
                  .rows(a.integer("rows"))
                  .columns(a.integer("columns"))
                  .lineColour(a.value("lineColour", Colour::fromXml))
                  .bgColour(a.value("bgColour", Colour::fromXml))
                  .lineSeparators(a.bool("lineSeparators"))
                  .embText(a.bool("embText"))
                  .alternativeImages(c.alternativeImages)
                  .coords(coords)
                  .userAttributes(c.userAttributes)
                  .labels(c.labels)
                  .tableCellRole(c.tableCellRole)
                  .regions(c.regions)
                  .grid(c.grid)
                  .build();
          case "ChartRegion" ->
              ChartRegionBuilder.builder()
                  .id(id)
                  .custom(custom)
                  .comments(comments)
                  .continuation(continuation)
                  .orientation(a.decimal("orientation"))
                  .mirrored(a.value("mirrored", Mirrored::fromXml))
                  .type(a.value("type", ChartType::fromXml))
                  .numColours(a.integer("numColours"))
                  .bgColour(a.value("bgColour", Colour::fromXml))
                  .embText(a.bool("embText"))
                  .alternativeImages(c.alternativeImages)
                  .coords(coords)
                  .userAttributes(c.userAttributes)
                  .labels(c.labels)
                  .tableCellRole(c.tableCellRole)
                  .regions(c.regions)
                  .build();
          case "MapRegion" ->
              MapRegionBuilder.builder()
                  .id(id)
                  .custom(custom)
                  .comments(comments)
                  .continuation(continuation)
                  .orientation(a.decimal("orientation"))
                  .mirrored(a.value("mirrored", Mirrored::fromXml))
                  .alternativeImages(c.alternativeImages)
                  .coords(coords)
                  .userAttributes(c.userAttributes)
                  .labels(c.labels)
                  .tableCellRole(c.tableCellRole)
                  .regions(c.regions)
                  .build();
          case "SeparatorRegion" ->
              SeparatorRegionBuilder.builder()
                  .id(id)
                  .custom(custom)
                  .comments(comments)
                  .continuation(continuation)
                  .orientation(a.decimal("orientation"))
                  .colour(a.value("colour", Colour::fromXml))
                  .alternativeImages(c.alternativeImages)
                  .coords(coords)
                  .userAttributes(c.userAttributes)
                  .labels(c.labels)
                  .tableCellRole(c.tableCellRole)
                  .regions(c.regions)
                  .build();
          case "MathsRegion" ->
              MathsRegionBuilder.builder()
                  .id(id)
                  .custom(custom)
                  .comments(comments)
                  .continuation(continuation)
                  .orientation(a.decimal("orientation"))
                  .mirrored(a.value("mirrored", Mirrored::fromXml))
                  .bgColour(a.value("bgColour", Colour::fromXml))
                  .alternativeImages(c.alternativeImages)
                  .coords(coords)
                  .userAttributes(c.userAttributes)
                  .labels(c.labels)
                  .tableCellRole(c.tableCellRole)
                  .regions(c.regions)
                  .build();
          case "ChemRegion" ->
              ChemRegionBuilder.builder()
                  .id(id)
                  .custom(custom)
                  .comments(comments)
                  .continuation(continuation)
                  .orientation(a.decimal("orientation"))
                  .mirrored(a.value("mirrored", Mirrored::fromXml))
                  .bgColour(a.value("bgColour", Colour::fromXml))
                  .alternativeImages(c.alternativeImages)
                  .coords(coords)
                  .userAttributes(c.userAttributes)
                  .labels(c.labels)
                  .tableCellRole(c.tableCellRole)
                  .regions(c.regions)
                  .build();
          case "MusicRegion" ->
              MusicRegionBuilder.builder()
                  .id(id)
                  .custom(custom)
                  .comments(comments)
                  .continuation(continuation)
                  .orientation(a.decimal("orientation"))
                  .mirrored(a.value("mirrored", Mirrored::fromXml))
                  .bgColour(a.value("bgColour", Colour::fromXml))
                  .alternativeImages(c.alternativeImages)
                  .coords(coords)
                  .userAttributes(c.userAttributes)
                  .labels(c.labels)
                  .tableCellRole(c.tableCellRole)
                  .regions(c.regions)
                  .build();
          case "AdvertRegion" ->
              AdvertRegionBuilder.builder()
                  .id(id)
                  .custom(custom)
                  .comments(comments)
                  .continuation(continuation)
                  .orientation(a.decimal("orientation"))
                  .mirrored(a.value("mirrored", Mirrored::fromXml))
                  .bgColour(a.value("bgColour", Colour::fromXml))
                  .alternativeImages(c.alternativeImages)
                  .coords(coords)
                  .userAttributes(c.userAttributes)
                  .labels(c.labels)
                  .tableCellRole(c.tableCellRole)
                  .regions(c.regions)
                  .build();
          case "FormRegion" ->
              FormRegionBuilder.builder()
                  .id(id)
                  .custom(custom)
                  .comments(comments)
                  .continuation(continuation)
                  .orientation(a.decimal("orientation"))
                  .mirrored(a.value("mirrored", Mirrored::fromXml))
                  .bgColour(a.value("bgColour", Colour::fromXml))
                  .alternativeImages(c.alternativeImages)
                  .coords(coords)
                  .userAttributes(c.userAttributes)
                  .labels(c.labels)
                  .tableCellRole(c.tableCellRole)
                  .regions(c.regions)
                  .build();
          case "NoiseRegion" ->
              NoiseRegionBuilder.builder()
                  .id(id)
                  .custom(custom)
                  .comments(comments)
                  .continuation(continuation)
                  .alternativeImages(c.alternativeImages)
                  .coords(coords)
                  .userAttributes(c.userAttributes)
                  .labels(c.labels)
                  .tableCellRole(c.tableCellRole)
                  .regions(c.regions)
                  .build();
          case "UnknownRegion" ->
              UnknownRegionBuilder.builder()
                  .id(id)
                  .custom(custom)
                  .comments(comments)
                  .continuation(continuation)
                  .alternativeImages(c.alternativeImages)
                  .coords(coords)
                  .userAttributes(c.userAttributes)
                  .labels(c.labels)
                  .tableCellRole(c.tableCellRole)
                  .regions(c.regions)
                  .build();
          case "FrameRegion" ->
              FrameRegionBuilder.builder()
                  .id(id)
                  .custom(custom)
                  .comments(comments)
                  .continuation(continuation)
                  .bgColour(a.value("bgColour", Colour::fromXml))
                  .borderPresent(a.bool("borderPresent"))
                  .alternativeImages(c.alternativeImages)
                  .coords(coords)
                  .userAttributes(c.userAttributes)
                  .labels(c.labels)
                  .tableCellRole(c.tableCellRole)
                  .regions(c.regions)
                  .build();
          case "CustomRegion" ->
              CustomRegionBuilder.builder()
                  .id(id)
                  .custom(custom)
                  .comments(comments)
                  .continuation(continuation)
                  .type(a.string("type"))
                  .alternativeImages(c.alternativeImages)
                  .coords(coords)
                  .userAttributes(c.userAttributes)
                  .labels(c.labels)
                  .tableCellRole(c.tableCellRole)
                  .regions(c.regions)
                  .build();
          default -> throw a.error("Unknown region type " + type);
        };
    a.done();
    return region;
  }

  /** The style of a text region up to 2010, given by its attributes; null if it has none. */
  private static @Nullable TextStyle legacyTextStyle(Attributes a) {
    TextStyle style =
        TextStyleBuilder.builder()
            .textColour(a.value("textColour", Colour::fromXml))
            .bgColour(a.value("bgColour", Colour::fromXml))
            .reverseVideo(a.bool("reverseVideo"))
            .fontSize(a.decimal("fontSize"))
            .kerning(a.integer("kerning"))
            .build();
    return style.equals(TextStyleBuilder.builder().build()) ? null : style;
  }

  private @Nullable TableCellRole roles() throws XMLStreamException {
    Attributes a = attributes();
    TableCellRole role = null;
    while (nextChild()) {
      switch (name()) {
        case "TableCellRole" -> {
          Attributes r = attributes();
          role =
              new TableCellRole(
                  r.requiredInteger("rowIndex"),
                  r.requiredInteger("columnIndex"),
                  r.integer("rowSpan"),
                  r.integer("colSpan"),
                  r.bool("header"));
          end(r);
        }
        default -> throw unexpected();
      }
    }
    a.done();
    return role;
  }

  private Grid grid() throws XMLStreamException {
    Attributes a = attributes();
    List<GridPoints> gridPoints = new ArrayList<>();
    List<GridAddPoints> addPoints = new ArrayList<>();
    while (nextChild()) {
      switch (name()) {
        case "GridPoints" -> {
          Attributes p = attributes();
          int index = p.requiredInteger("index");
          gridPoints.add(new GridPoints(index, p.polygon()));
          end(p);
        }
        case "AddPoints" -> {
          Attributes p = attributes();
          addPoints.add(
              new GridAddPoints(
                  p.requiredInteger("row"),
                  p.requiredInteger("col"),
                  p.string("left"),
                  p.string("top")));
          end(p);
        }
        default -> throw unexpected();
      }
    }
    a.done();
    return new Grid(gridPoints, addPoints);
  }

  // ---- Text

  private TextLine textLine() throws XMLStreamException {
    Attributes a = attributes();
    TextLineBuilder line =
        TextLineBuilder.builder()
            .id(a.requiredString("id"))
            .orientation(a.decimal("orientation"))
            .mirrored(a.value("mirrored", Mirrored::fromXml))
            .primaryLanguage(a.value("primaryLanguage", Language::fromXml))
            .secondaryLanguage(a.value("secondaryLanguage", Language::fromXml))
            .customLanguages(a.string("customLanguages"))
            .primaryScript(a.value("primaryScript", Script::fromXml))
            .secondaryScript(a.value("secondaryScript", Script::fromXml))
            .customScripts(a.string("customScripts"))
            .readingDirection(a.value("readingDirection", ReadingDirection::fromXml))
            .production(a.value("production", Production::fromXml))
            .custom(a.string("custom"))
            .comments(a.string("comments"))
            .index(a.integer("index"));
    Polygon coords = null;
    while (nextChild()) {
      switch (name()) {
        case "AlternativeImage" -> line.addAlternativeImages(alternativeImage());
        case "Coords" -> coords = coords();
        case "AscentLine" -> line.ascentLine(coords());
        case "MeanLine" -> line.meanLine(coords());
        case "Baseline" -> line.baseline(coords());
        case "DescentLine" -> line.descentLine(coords());
        case "Word" -> line.addWords(word());
        case "TextEquiv" -> line.addTextEquivs(textEquiv());
        case "TextStyle" -> line.textStyle(textStyle());
        case "UserDefined" -> line.userAttributes(userDefined());
        case "Labels" -> line.addLabels(labels());
        default -> throw unexpected();
      }
    }
    a.done();
    return line.coords(required(coords, "Coords", a)).build();
  }

  private Word word() throws XMLStreamException {
    Attributes a = attributes();
    WordBuilder word =
        WordBuilder.builder()
            .id(a.requiredString("id"))
            .orientation(a.decimal("orientation"))
            .mirrored(a.value("mirrored", Mirrored::fromXml))
            .language(a.value("language", Language::fromXml))
            .customLanguages(a.string("customLanguages"))
            .primaryScript(a.value("primaryScript", Script::fromXml))
            .secondaryScript(a.value("secondaryScript", Script::fromXml))
            .customScripts(a.string("customScripts"))
            .readingDirection(a.value("readingDirection", ReadingDirection::fromXml))
            .production(a.value("production", Production::fromXml))
            .custom(a.string("custom"))
            .comments(a.string("comments"));
    Polygon coords = null;
    while (nextChild()) {
      switch (name()) {
        case "AlternativeImage" -> word.addAlternativeImages(alternativeImage());
        case "Coords" -> coords = coords();
        case "Glyph" -> word.addGlyphs(glyph());
        case "TextEquiv" -> word.addTextEquivs(textEquiv());
        case "TextStyle" -> word.textStyle(textStyle());
        case "UserDefined" -> word.userAttributes(userDefined());
        case "Labels" -> word.addLabels(labels());
        default -> throw unexpected();
      }
    }
    a.done();
    return word.coords(required(coords, "Coords", a)).build();
  }

  private Glyph glyph() throws XMLStreamException {
    Attributes a = attributes();
    GlyphBuilder glyph =
        GlyphBuilder.builder()
            .id(a.requiredString("id"))
            .orientation(a.decimal("orientation"))
            .mirrored(a.value("mirrored", Mirrored::fromXml))
            .ligature(a.bool("ligature"))
            .symbol(a.bool("symbol"))
            .script(a.value("script", Script::fromXml))
            .customScript(a.string("customScript"))
            .production(a.value("production", Production::fromXml))
            .custom(a.string("custom"))
            .comments(a.string("comments"));
    Polygon coords = null;
    while (nextChild()) {
      switch (name()) {
        case "AlternativeImage" -> glyph.addAlternativeImages(alternativeImage());
        case "Coords" -> coords = coords();
        case "Graphemes" -> glyph.graphemes(graphemes());
        case "TextEquiv" -> glyph.addTextEquivs(textEquiv());
        case "TextStyle" -> glyph.textStyle(textStyle());
        case "UserDefined" -> glyph.userAttributes(userDefined());
        case "Labels" -> glyph.addLabels(labels());
        default -> throw unexpected();
      }
    }
    a.done();
    return glyph.coords(required(coords, "Coords", a)).build();
  }

  private List<GraphemeElement> graphemes() throws XMLStreamException {
    Attributes a = attributes();
    List<GraphemeElement> graphemes = new ArrayList<>();
    while (nextChild()) {
      graphemes.add(grapheme(true));
    }
    a.done();
    return graphemes;
  }

  /** A {@code Grapheme}, {@code NonPrintingChar} or (if allowed) {@code GraphemeGroup}. */
  private GraphemeElement grapheme(boolean groupAllowed) throws XMLStreamException {
    String type = name();
    if (!type.equals("Grapheme")
        && !type.equals("NonPrintingChar")
        && !(groupAllowed && type.equals("GraphemeGroup"))) {
      throw unexpected();
    }
    Attributes a = attributes();
    String id = a.requiredString("id");
    int index = a.requiredInteger("index");
    Boolean ligature = a.bool("ligature");
    CharType charType = a.value("charType", CharType::fromXml);
    String custom = a.string("custom");
    String comments = a.string("comments");
    List<TextEquiv> textEquivs = new ArrayList<>();
    Polygon coords = null;
    List<GraphemeElement> members = new ArrayList<>();
    while (nextChild()) {
      switch (name()) {
        case "TextEquiv" -> textEquivs.add(textEquiv());
        case "Coords" -> {
          if (!type.equals("Grapheme")) {
            throw unexpected();
          }
          coords = coords();
        }
        default -> {
          if (!type.equals("GraphemeGroup")) {
            throw unexpected();
          }
          members.add(grapheme(false));
        }
      }
    }
    a.done();
    return switch (type) {
      case "Grapheme" ->
          GraphemeBuilder.builder()
              .id(id)
              .index(index)
              .ligature(ligature)
              .charType(charType)
              .custom(custom)
              .comments(comments)
              .textEquivs(textEquivs)
              .coords(required(coords, "Coords", a))
              .build();
      case "NonPrintingChar" ->
          NonPrintingCharBuilder.builder()
              .id(id)
              .index(index)
              .ligature(ligature)
              .charType(charType)
              .custom(custom)
              .comments(comments)
              .textEquivs(textEquivs)
              .build();
      default ->
          GraphemeGroupBuilder.builder()
              .id(id)
              .index(index)
              .ligature(ligature)
              .charType(charType)
              .custom(custom)
              .comments(comments)
              .textEquivs(textEquivs)
              .graphemes(members)
              .build();
    };
  }

  // ---- Reading order, layers, relations

  private @Nullable ReadingOrder readingOrder() throws XMLStreamException {
    Attributes a = attributes();
    Double conf = a.decimal("conf");
    List<GroupMember> members = new ArrayList<>();
    while (nextChild()) {
      switch (name()) {
        case "OrderedGroup", "UnorderedGroup" -> members.add(group().member());
        case "RegionRef" -> members.add(new RegionRef(regionRef()));
        default -> throw unexpected();
      }
    }
    a.done();
    if (members.size() == 1 && members.getFirst() instanceof Group group) {
      return new ReadingOrder(conf, group);
    }
    if (members.isEmpty()) {
      throw a.error("ReadingOrder has no group");
    }
    // Up to 2010-01-12: several members, wrapped in a root group once all IDs are known.
    legacyReadingOrder = new LegacyReadingOrder(conf, members);
    return null;
  }

  /** A group member with the {@code index} it had in the document (0 if it had none). */
  private record Indexed<T extends GroupMember>(int index, T member) {}

  /** Any of the four group elements; the reader is at its start tag. */
  private Indexed<Group> group() throws XMLStreamException {
    String type = name();
    boolean ordered = type.startsWith("Ordered");
    boolean indexed = type.endsWith("Indexed");
    Attributes a = attributes();
    int index = indexed ? a.requiredInteger("index") : 0;
    String id = a.requiredString("id");
    String regionRef = a.string("regionRef");
    String caption = a.string("caption");
    GroupType groupType = a.value("type", GroupType::fromXml);
    Boolean continuation = a.bool("continuation");
    String custom = a.string("custom");
    String comments = a.string("comments");
    List<UserAttribute> userAttributes = List.of();
    List<LabelGroup> labels = new ArrayList<>();
    List<Indexed<?>> members = new ArrayList<>();
    while (nextChild()) {
      String child = name();
      switch (child) {
        case "UserDefined" -> userAttributes = userDefined();
        case "Labels" -> labels.add(labels());
        case "RegionRefIndexed", "RegionRef" -> {
          if (child.endsWith("Indexed") != ordered) {
            throw unexpected();
          }
          Attributes r = attributes();
          int memberIndex = ordered ? r.requiredInteger("index") : 0;
          members.add(new Indexed<>(memberIndex, new RegionRef(r.requiredString("regionRef"))));
          end(r);
        }
        case "OrderedGroupIndexed", "UnorderedGroupIndexed", "OrderedGroup", "UnorderedGroup" -> {
          if (child.endsWith("Indexed") != ordered) {
            throw unexpected();
          }
          members.add(group());
        }
        default -> throw unexpected();
      }
    }
    a.done();
    if (ordered) {
      // Stable: members with the same index keep their document order.
      members.sort(Comparator.comparingInt(Indexed::index));
    }
    List<GroupMember> sorted = members.stream().<GroupMember>map(Indexed::member).toList();
    Group group =
        ordered
            ? OrderedGroupBuilder.builder()
                .id(id)
                .regionRef(regionRef)
                .caption(caption)
                .type(groupType)
                .continuation(continuation)
                .custom(custom)
                .comments(comments)
                .userAttributes(userAttributes)
                .labels(labels)
                .members(sorted)
                .build()
            : UnorderedGroupBuilder.builder()
                .id(id)
                .regionRef(regionRef)
                .caption(caption)
                .type(groupType)
                .continuation(continuation)
                .custom(custom)
                .comments(comments)
                .userAttributes(userAttributes)
                .labels(labels)
                .members(sorted)
                .build();
    return new Indexed<>(index, group);
  }

  private List<Layer> layers() throws XMLStreamException {
    Attributes a = attributes();
    List<Layer> layers = new ArrayList<>();
    while (nextChild()) {
      switch (name()) {
        case "Layer" -> layers.add(layer());
        default -> throw unexpected();
      }
    }
    a.done();
    return layers;
  }

  private Layer layer() throws XMLStreamException {
    Attributes a = attributes();
    String id = a.requiredString("id");
    int zIndex = a.requiredInteger("zIndex");
    String caption = a.string("caption");
    List<String> refs = new ArrayList<>();
    while (nextChild()) {
      switch (name()) {
        case "RegionRef" -> refs.add(regionRef());
        default -> throw unexpected();
      }
    }
    a.done();
    return new Layer(id, zIndex, caption, refs);
  }

  /** An element with only a {@code regionRef} attribute. */
  private String regionRef() throws XMLStreamException {
    Attributes a = attributes();
    String ref = a.requiredString("regionRef");
    end(a);
    return ref;
  }

  private List<Relation> relations() throws XMLStreamException {
    Attributes a = attributes();
    List<Relation> relations = new ArrayList<>();
    while (nextChild()) {
      switch (name()) {
        case "Relation" -> relations.add(relation());
        default -> throw unexpected();
      }
    }
    a.done();
    return relations;
  }

  private Relation relation() throws XMLStreamException {
    Attributes a = attributes();
    String id = a.string("id");
    RelationType type = a.value("type", RelationType::fromXml);
    String custom = a.string("custom");
    String comments = a.string("comments");
    List<LabelGroup> labels = new ArrayList<>();
    String source = null;
    String target = null;
    while (nextChild()) {
      switch (name()) {
        case "Labels" -> labels.add(labels());
        case "SourceRegionRef" -> source = regionRef();
        case "TargetRegionRef" -> target = regionRef();
        // Up to 2017, a relation holds two RegionRef elements: source, then target.
        case "RegionRef" -> {
          if (source == null) {
            source = regionRef();
          } else if (target == null) {
            target = regionRef();
          } else {
            throw unexpected();
          }
        }
        default -> throw unexpected();
      }
    }
    a.done();
    return new Relation(
        id,
        type,
        custom,
        comments,
        labels,
        required(source, "source", a),
        required(target, "target", a));
  }
}
