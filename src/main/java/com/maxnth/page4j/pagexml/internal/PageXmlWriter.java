package com.maxnth.page4j.pagexml.internal;

import com.maxnth.page4j.geometry.Point;
import com.maxnth.page4j.geometry.Polygon;
import com.maxnth.page4j.model.AdvertRegion;
import com.maxnth.page4j.model.AlternativeImage;
import com.maxnth.page4j.model.ChartRegion;
import com.maxnth.page4j.model.ChemRegion;
import com.maxnth.page4j.model.ContentObject;
import com.maxnth.page4j.model.CustomRegion;
import com.maxnth.page4j.model.FormRegion;
import com.maxnth.page4j.model.FrameRegion;
import com.maxnth.page4j.model.Glyph;
import com.maxnth.page4j.model.Grapheme;
import com.maxnth.page4j.model.GraphemeElement;
import com.maxnth.page4j.model.GraphemeGroup;
import com.maxnth.page4j.model.GraphicRegion;
import com.maxnth.page4j.model.Grid;
import com.maxnth.page4j.model.GridAddPoints;
import com.maxnth.page4j.model.GridPoints;
import com.maxnth.page4j.model.Group;
import com.maxnth.page4j.model.GroupMember;
import com.maxnth.page4j.model.ImageRegion;
import com.maxnth.page4j.model.Label;
import com.maxnth.page4j.model.LabelGroup;
import com.maxnth.page4j.model.Layer;
import com.maxnth.page4j.model.LineDrawingRegion;
import com.maxnth.page4j.model.MapRegion;
import com.maxnth.page4j.model.MathsRegion;
import com.maxnth.page4j.model.Metadata;
import com.maxnth.page4j.model.MetadataItem;
import com.maxnth.page4j.model.MusicRegion;
import com.maxnth.page4j.model.NoiseRegion;
import com.maxnth.page4j.model.NonPrintingChar;
import com.maxnth.page4j.model.OrderedGroup;
import com.maxnth.page4j.model.Page;
import com.maxnth.page4j.model.ReadingOrder;
import com.maxnth.page4j.model.Region;
import com.maxnth.page4j.model.RegionRef;
import com.maxnth.page4j.model.Relation;
import com.maxnth.page4j.model.SeparatorRegion;
import com.maxnth.page4j.model.TableCellRole;
import com.maxnth.page4j.model.TableRegion;
import com.maxnth.page4j.model.TextEquiv;
import com.maxnth.page4j.model.TextLine;
import com.maxnth.page4j.model.TextRegion;
import com.maxnth.page4j.model.TextStyle;
import com.maxnth.page4j.model.UnknownRegion;
import com.maxnth.page4j.model.UserAttribute;
import com.maxnth.page4j.model.Word;
import com.maxnth.page4j.model.value.XmlValue;
import com.maxnth.page4j.pagexml.PageReadException;
import com.maxnth.page4j.pagexml.PageWriteException;
import com.maxnth.page4j.pagexml.PageXmlVersion;
import com.maxnth.page4j.pagexml.internal.SchemaRules.AttributeRule;
import com.maxnth.page4j.pagexml.internal.SchemaRules.ElementRule;
import com.maxnth.page4j.pagexml.internal.SchemaRules.TypeRules;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import javax.xml.XMLConstants;
import org.jspecify.annotations.Nullable;

/**
 * Writes the model as PAGE XML, in any version from 2009-03-16 to 2024-07-15.
 *
 * <p>One method per element, attributes in schema order. Writing is strict: every element,
 * attribute, value and count is checked against the rules of the target version (read from its
 * XSD), and content the version can't express fails the write with a message naming the object. The
 * result is validated against the schema before it is returned, so nothing invalid is ever written.
 *
 * <p>Up to 2010 coordinates are written as {@code Point} elements, and a text region's style
 * ({@code textColour}, {@code bgColour}, {@code reverseVideo}, {@code fontSize}, {@code kerning})
 * as its own attributes.
 */
public final class PageXmlWriter {

  private static final String XSI = XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI;
  private static final String INDENT = "  ";

  private final XmlOut out = new XmlOut();
  private final PageXmlVersion version;
  private final boolean legacy;
  private final SchemaRules rules;
  private final Deque<Frame> frames = new ArrayDeque<>();
  private final List<String> problems = new ArrayList<>();

  /**
   * An open element: its rules, what it belongs to (for messages) and what was written. Its start
   * tag is held back until content follows, so an element without content is written as {@code
   * <X/>}.
   */
  private static final class Frame {
    final @Nullable String name;
    final @Nullable TypeRules rules;
    final String context;
    final Map<String, String> attributes = new LinkedHashMap<>();
    final Map<String, Integer> children = new HashMap<>();
    boolean started;
    boolean hasChildren;

    Frame(@Nullable String name, @Nullable TypeRules rules, String context) {
      this.name = name;
      this.rules = rules;
      this.context = context;
    }
  }

  /** Writes the held-back start tag of the element, if not written yet. */
  private void open(Frame frame) {
    if (!frame.started && frame.name != null) {
      out.startElement(frame.name);
      frame.attributes.forEach(out::attribute);
    }
    frame.started = true;
  }

  private PageXmlWriter(PageXmlVersion version) {
    this.version = version;
    this.legacy = version.compareTo(PageXmlVersion.V2013_07_15) < 0;
    this.rules = SchemaRules.of(version);
  }

  /**
   * Writes the page in the given version.
   *
   * @throws PageWriteException if the page holds content the version can't express
   */
  public static byte[] write(Page page, PageXmlVersion version) throws PageWriteException {
    PageXmlWriter writer = new PageXmlWriter(version);
    writer.document(page);
    if (!writer.problems.isEmpty()) {
      throw new PageWriteException(writer.problems, null);
    }
    byte[] xml = writer.out.toBytes();
    try {
      PageXmlSchemas.validate(xml, version);
    } catch (PageReadException e) {
      List<String> errors = new ArrayList<>();
      errors.add("The written document is not valid against the schema of " + version);
      errors.addAll(e.errors());
      throw new PageWriteException(errors, e);
    }
    return xml;
  }

  /** Writes the page to the stream; nothing is written if the page can't be written. */
  public static void write(Page page, PageXmlVersion version, OutputStream target)
      throws IOException {
    target.write(write(page, version));
  }

  // ---- Elements and attributes, checked against the rules of the version

  private void problem(String message) {
    problems.add(message);
  }

  private String context() {
    Frame frame = frames.peek();
    return frame == null ? "Page" : frame.context;
  }

  /**
   * Starts a child element of the current one. If the version does not allow it here, records a
   * problem and returns false; the caller then skips the element.
   *
   * @param context the object the element stands for, or null for a part of the current object
   */
  private boolean start(String name, @Nullable String context) {
    Frame parent = frames.peek();
    if (parent == null) {
      throw new IllegalStateException("No root element");
    }
    ElementRule rule = parent.rules == null ? null : parent.rules.children().get(name);
    if (rule == null) {
      problem(
          context != null
              ? context + " is not supported in " + version
              : parent.context + ": element " + name + " is not supported in " + version);
      return false;
    }
    int count = parent.children.merge(name, 1, Integer::sum);
    if (count > rule.maxOccurs()) {
      if (count == rule.maxOccurs() + 1) {
        problem(
            parent.context + ": " + version + " allows at most " + rule.maxOccurs() + " " + name);
      }
      return false;
    }
    open(parent);
    parent.hasChildren = true;
    out.characters("\n" + INDENT.repeat(frames.size()));
    frames.push(
        new Frame(name, rules.type(rule.type()), context != null ? context : parent.context));
    return true;
  }

  private void end() {
    Frame frame = frames.pop();
    TypeRules typeRules = frame.rules;
    if (typeRules != null) {
      typeRules
          .attributes()
          .forEach(
              (name, rule) -> {
                if (rule.required() && !frame.attributes.containsKey(name)) {
                  problem(frame.context + ": attribute " + name + " is required in " + version);
                }
              });
    }
    if (!frame.started && frame.name != null) {
      out.emptyElement(frame.name);
      frame.attributes.forEach(out::attribute);
      return;
    }
    if (frame.hasChildren) {
      out.characters("\n" + INDENT.repeat(frames.size()));
    }
    out.endElement();
  }

  private void attribute(String name, @Nullable Object value) {
    if (value == null) {
      return;
    }
    Frame frame = frames.element();
    AttributeRule rule = frame.rules == null ? null : frame.rules.attributes().get(name);
    if (rule == null) {
      problem(frame.context + ": attribute " + name + " is not supported in " + version);
      return;
    }
    String text = format(value);
    if (!isXmlText(frame.context + ": attribute " + name, text)) {
      return;
    }
    Set<String> values = rule.values();
    if (values != null && !values.contains(text)) {
      problem(frame.context + ": " + name + " '" + text + "' is not supported in " + version);
      return;
    }
    if (frame.started) {
      out.attribute(name, text);
    }
    frame.attributes.put(name, text);
  }

  /** An element with text content, written if the text is not null (also if it is empty). */
  private void text(String name, @Nullable Object text) {
    if (text == null) {
      return;
    }
    String formatted = format(text);
    if (isXmlText(context() + ": " + name, formatted) && start(name, null)) {
      open(frames.element());
      out.characters(formatted);
      end();
    }
  }

  /**
   * Checks that XML can hold the text; records a problem otherwise. Control characters other than
   * tab, line feed and carriage return can't be written, nor can a lone surrogate.
   */
  private boolean isXmlText(String what, String text) {
    int invalid = XmlOut.firstInvalid(text);
    if (invalid < 0) {
      return true;
    }
    problem(
        what
            + " contains the character U+"
            + String.format("%04X", invalid)
            + ", which XML can't hold");
    return false;
  }

  private static String format(Object value) {
    return switch (value) {
      case String s -> s;
      case Double d -> formatDouble(d);
      case XmlValue v -> v.xmlValue();
      case Polygon p -> points(p);
      default -> value.toString(); // Integer, Boolean
    };
  }

  /** Java's form ({@code 0.0}, {@code 0.5}) without exponent; {@code INF}, {@code NaN} as XSD. */
  static String formatDouble(double value) {
    if (Double.isNaN(value)) {
      return "NaN";
    }
    if (Double.isInfinite(value)) {
      return value > 0 ? "INF" : "-INF";
    }
    String text = Double.toString(value);
    if (text.indexOf('E') < 0) {
      return text;
    }
    String plain = BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    return plain.indexOf('.') < 0 ? plain + ".0" : plain;
  }

  private static String points(Polygon polygon) {
    var joiner = new StringJoiner(" ");
    for (Point point : polygon.points()) {
      joiner.add(point.x() + "," + point.y());
    }
    return joiner.toString();
  }

  private static String describe(ContentObject object) {
    return object.getClass().getSimpleName() + " '" + object.id() + "'";
  }

  // ---- Document and metadata

  private void document(Page page) {
    String namespace = version.namespace();
    out.declaration();
    out.characters("\n");
    out.startElement("PcGts");
    out.attribute("xmlns", namespace);
    out.attribute("xmlns:xsi", XSI);
    out.attribute("xsi:schemaLocation", namespace + " " + version.schemaLocation());
    Frame root = new Frame(null, rules.type("PcGtsType"), "Document");
    root.started = true; // written above, with the namespace declarations
    frames.push(root);
    attribute("pcGtsId", page.pcGtsId());
    metadata(page.metadata());
    page(page);
    end();
    out.characters("\n");
  }

  private void metadata(Metadata metadata) {
    if (!start("Metadata", "Metadata")) {
      return;
    }
    attribute("externalRef", metadata.externalRef());
    text("Creator", metadata.creator());
    text("Created", metadata.created());
    text("LastChange", metadata.lastChange());
    text("Comments", metadata.comments());
    userDefined(metadata.userAttributes());
    for (MetadataItem item : metadata.metadataItems()) {
      if (start("MetadataItem", null)) {
        attribute("type", item.type());
        attribute("name", item.name());
        attribute("value", item.value());
        attribute("date", item.date());
        labels(item.labels());
        end();
      }
    }
    end();
  }

  private void userDefined(List<UserAttribute> attributes) {
    if (attributes.isEmpty() || !start("UserDefined", null)) {
      return;
    }
    for (UserAttribute attribute : attributes) {
      if (start("UserAttribute", null)) {
        attribute("name", attribute.name());
        attribute("description", attribute.description());
        attribute("type", attribute.type());
        attribute("value", attribute.value());
        end();
      }
    }
    end();
  }

  private void labels(List<LabelGroup> groups) {
    for (LabelGroup group : groups) {
      if (start("Labels", null)) {
        attribute("externalModel", group.externalModel());
        attribute("externalId", group.externalId());
        attribute("prefix", group.prefix());
        attribute("comments", group.comments());
        for (Label label : group.labels()) {
          if (start("Label", null)) {
            attribute("value", label.value());
            attribute("type", label.type());
            attribute("comments", label.comments());
            end();
          }
        }
        end();
      }
    }
  }

  // ---- Page

  private void page(Page page) {
    if (!start("Page", "Page")) {
      return;
    }
    attribute("imageFilename", page.imageFilename());
    attribute("imageWidth", page.imageWidth());
    attribute("imageHeight", page.imageHeight());
    attribute("imageXResolution", page.imageXResolution());
    attribute("imageYResolution", page.imageYResolution());
    attribute("imageResolutionUnit", page.imageResolutionUnit());
    attribute("custom", page.custom());
    attribute("comments", page.comments());
    attribute("orientation", page.orientation());
    attribute("type", page.type());
    attribute("primaryLanguage", page.primaryLanguage());
    attribute("secondaryLanguage", page.secondaryLanguage());
    attribute("customLanguages", page.customLanguages());
    attribute("primaryScript", page.primaryScript());
    attribute("secondaryScript", page.secondaryScript());
    attribute("customScripts", page.customScripts());
    attribute("readingDirection", page.readingDirection());
    attribute("textLineOrder", page.textLineOrder());
    attribute("conf", page.conf());
    alternativeImages(page.alternativeImages());
    coordsContainer("Border", page.border());
    coordsContainer("PrintSpace", page.printSpace());
    readingOrder(page.readingOrder());
    layers(page.layers());
    relations(page.relations());
    textStyle(page.textStyle());
    userDefined(page.userAttributes());
    labels(page.labels());
    for (Region region : page.regions()) {
      region(region);
    }
    end();
  }

  private void coordsContainer(String name, @Nullable Polygon coords) {
    if (coords != null && start(name, null)) {
      coords("Coords", coords);
      end();
    }
  }

  /** {@code Coords}, {@code Baseline} and the other script lines. */
  private void coords(String name, @Nullable Polygon polygon) {
    if (polygon != null && start(name, null)) {
      if (legacy) {
        for (Point point : polygon.points()) {
          if (start("Point", null)) {
            attribute("x", point.x());
            attribute("y", point.y());
            end();
          }
        }
      } else {
        attribute("points", polygon);
      }
      attribute("conf", polygon.confidence());
      end();
    }
  }

  private void alternativeImages(List<AlternativeImage> images) {
    for (AlternativeImage image : images) {
      if (start("AlternativeImage", null)) {
        attribute("filename", image.filename());
        attribute("comments", image.comments());
        attribute("conf", image.conf());
        end();
      }
    }
  }

  private void textStyle(@Nullable TextStyle style) {
    if (style == null || !start("TextStyle", null)) {
      return;
    }
    attribute("fontFamily", style.fontFamily());
    attribute("serif", style.serif());
    attribute("monospace", style.monospace());
    attribute("fontSize", style.fontSize());
    attribute("xHeight", style.xHeight());
    attribute("kerning", style.kerning());
    attribute("textColour", style.textColour());
    attribute("textColourRgb", style.textColourRgb());
    attribute("bgColour", style.bgColour());
    attribute("bgColourRgb", style.bgColourRgb());
    attribute("reverseVideo", style.reverseVideo());
    attribute("bold", style.bold());
    attribute("italic", style.italic());
    attribute("underlined", style.underlined());
    attribute("underlineStyle", style.underlineStyle());
    attribute("subscript", style.subscript());
    attribute("superscript", style.superscript());
    attribute("strikethrough", style.strikethrough());
    attribute("smallCaps", style.smallCaps());
    attribute("letterSpaced", style.letterSpaced());
    end();
  }

  private void textEquivs(List<TextEquiv> equivs) {
    for (TextEquiv equiv : equivs) {
      if (start("TextEquiv", null)) {
        attribute("index", equiv.index());
        attribute("conf", equiv.conf());
        attribute("dataType", equiv.dataType());
        attribute("dataTypeDetails", equiv.dataTypeDetails());
        attribute("comments", equiv.comments());
        text("PlainText", equiv.plainText());
        text("Unicode", equiv.unicode());
        end();
      }
    }
  }

  // ---- Regions

  private void region(Region region) {
    String name = region.getClass().getSimpleName();
    if (!start(name, describe(region))) {
      return;
    }
    attribute("id", region.id());
    attribute("custom", region.custom());
    attribute("comments", region.comments());
    attribute("continuation", region.continuation());
    switch (region) {
      case TextRegion r -> {
        attribute("orientation", r.orientation());
        attribute("mirrored", r.mirrored());
        attribute("type", r.type());
        TextStyle style = legacy ? r.textStyle() : null;
        if (style != null) {
          attribute("textColour", style.textColour());
          attribute("bgColour", style.bgColour());
          attribute("reverseVideo", style.reverseVideo());
          attribute("fontSize", style.fontSize());
        }
        attribute("leading", r.leading());
        if (style != null) {
          attribute("kerning", style.kerning());
        }
        attribute("readingDirection", r.readingDirection());
        attribute("textLineOrder", r.textLineOrder());
        attribute("readingOrientation", r.readingOrientation());
        attribute("indented", r.indented());
        attribute("align", r.align());
        attribute("primaryLanguage", r.primaryLanguage());
        attribute("secondaryLanguage", r.secondaryLanguage());
        attribute("customLanguages", r.customLanguages());
        attribute("primaryScript", r.primaryScript());
        attribute("secondaryScript", r.secondaryScript());
        attribute("customScripts", r.customScripts());
        attribute("production", r.production());
      }
      case ImageRegion r -> {
        attribute("orientation", r.orientation());
        attribute("mirrored", r.mirrored());
        attribute("colourDepth", r.colourDepth());
        attribute("bgColour", r.bgColour());
        attribute("embText", r.embText());
      }
      case LineDrawingRegion r -> {
        attribute("orientation", r.orientation());
        attribute("mirrored", r.mirrored());
        attribute("penColour", r.penColour());
        attribute("bgColour", r.bgColour());
        attribute("embText", r.embText());
      }
      case GraphicRegion r -> {
        attribute("orientation", r.orientation());
        attribute("mirrored", r.mirrored());
        attribute("type", r.type());
        attribute("numColours", r.numColours());
        attribute("embText", r.embText());
      }
      case TableRegion r -> {
        attribute("orientation", r.orientation());
        attribute("mirrored", r.mirrored());
        attribute("rows", r.rows());
        attribute("columns", r.columns());
        attribute("lineColour", r.lineColour());
        attribute("bgColour", r.bgColour());
        attribute("lineSeparators", r.lineSeparators());
        attribute("embText", r.embText());
      }
      case ChartRegion r -> {
        attribute("orientation", r.orientation());
        attribute("mirrored", r.mirrored());
        attribute("type", r.type());
        attribute("numColours", r.numColours());
        attribute("bgColour", r.bgColour());
        attribute("embText", r.embText());
      }
      case MapRegion r -> {
        attribute("orientation", r.orientation());
        attribute("mirrored", r.mirrored());
      }
      case SeparatorRegion r -> {
        attribute("orientation", r.orientation());
        attribute("colour", r.colour());
      }
      case MathsRegion r -> {
        attribute("orientation", r.orientation());
        attribute("mirrored", r.mirrored());
        attribute("bgColour", r.bgColour());
      }
      case ChemRegion r -> {
        attribute("orientation", r.orientation());
        attribute("mirrored", r.mirrored());
        attribute("bgColour", r.bgColour());
      }
      case MusicRegion r -> {
        attribute("orientation", r.orientation());
        attribute("mirrored", r.mirrored());
        attribute("bgColour", r.bgColour());
      }
      case AdvertRegion r -> {
        attribute("orientation", r.orientation());
        attribute("mirrored", r.mirrored());
        attribute("bgColour", r.bgColour());
      }
      case FormRegion r -> {
        attribute("orientation", r.orientation());
        attribute("mirrored", r.mirrored());
        attribute("bgColour", r.bgColour());
      }
      case FrameRegion r -> {
        attribute("bgColour", r.bgColour());
        attribute("borderPresent", r.borderPresent());
      }
      case NoiseRegion r -> {}
      case UnknownRegion r -> {}
      case CustomRegion r -> attribute("type", r.type());
    }
    alternativeImages(region.alternativeImages());
    coords("Coords", region.coords());
    userDefined(region.userAttributes());
    labels(region.labels());
    roles(region.tableCellRole());
    for (Region nested : region.regions()) {
      region(nested);
    }
    if (region instanceof TextRegion r) {
      for (TextLine line : r.textLines()) {
        textLine(line);
      }
      textEquivs(r.textEquivs());
      if (legacy) {
        legacyTextStyle(r.textStyle());
      } else {
        textStyle(r.textStyle());
      }
    } else if (region instanceof TableRegion r) {
      grid(r.grid());
    }
    end();
  }

  /** Up to 2010 only part of a text region's style can be written, as attributes of the region. */
  private void legacyTextStyle(@Nullable TextStyle style) {
    if (style == null) {
      return;
    }
    Map<String, @Nullable Object> rest = new LinkedHashMap<>();
    rest.put("fontFamily", style.fontFamily());
    rest.put("serif", style.serif());
    rest.put("monospace", style.monospace());
    rest.put("xHeight", style.xHeight());
    rest.put("textColourRgb", style.textColourRgb());
    rest.put("bgColourRgb", style.bgColourRgb());
    rest.put("bold", style.bold());
    rest.put("italic", style.italic());
    rest.put("underlined", style.underlined());
    rest.put("underlineStyle", style.underlineStyle());
    rest.put("subscript", style.subscript());
    rest.put("superscript", style.superscript());
    rest.put("strikethrough", style.strikethrough());
    rest.put("smallCaps", style.smallCaps());
    rest.put("letterSpaced", style.letterSpaced());
    rest.forEach(
        (name, value) -> {
          if (value != null) {
            problem(
                context() + ": TextStyle attribute " + name + " is not supported in " + version);
          }
        });
  }

  private void roles(@Nullable TableCellRole role) {
    if (role == null || !start("Roles", null)) {
      return;
    }
    if (start("TableCellRole", null)) {
      attribute("rowIndex", role.rowIndex());
      attribute("columnIndex", role.columnIndex());
      attribute("rowSpan", role.rowSpan());
      attribute("colSpan", role.colSpan());
      attribute("header", role.header());
      end();
    }
    end();
  }

  private void grid(@Nullable Grid grid) {
    if (grid == null || !start("Grid", null)) {
      return;
    }
    for (GridPoints points : grid.gridPoints()) {
      if (start("GridPoints", null)) {
        attribute("index", points.index());
        attribute("points", points.points());
        end();
      }
    }
    for (GridAddPoints points : grid.addPoints()) {
      if (start("AddPoints", null)) {
        attribute("row", points.row());
        attribute("col", points.col());
        attribute("left", points.left());
        attribute("top", points.top());
        end();
      }
    }
    end();
  }

  // ---- Text

  private void textLine(TextLine line) {
    if (!start("TextLine", describe(line))) {
      return;
    }
    attribute("id", line.id());
    attribute("orientation", line.orientation());
    attribute("mirrored", line.mirrored());
    attribute("primaryLanguage", line.primaryLanguage());
    attribute("secondaryLanguage", line.secondaryLanguage());
    attribute("customLanguages", line.customLanguages());
    attribute("primaryScript", line.primaryScript());
    attribute("secondaryScript", line.secondaryScript());
    attribute("customScripts", line.customScripts());
    attribute("readingDirection", line.readingDirection());
    attribute("production", line.production());
    attribute("custom", line.custom());
    attribute("comments", line.comments());
    attribute("index", line.index());
    alternativeImages(line.alternativeImages());
    coords("Coords", line.coords());
    coords("AscentLine", line.ascentLine());
    coords("MeanLine", line.meanLine());
    coords("Baseline", line.baseline());
    coords("DescentLine", line.descentLine());
    for (Word word : line.words()) {
      word(word);
    }
    textEquivs(line.textEquivs());
    textStyle(line.textStyle());
    userDefined(line.userAttributes());
    labels(line.labels());
    end();
  }

  private void word(Word word) {
    if (!start("Word", describe(word))) {
      return;
    }
    attribute("id", word.id());
    attribute("orientation", word.orientation());
    attribute("mirrored", word.mirrored());
    attribute("language", word.language());
    attribute("customLanguages", word.customLanguages());
    attribute("primaryScript", word.primaryScript());
    attribute("secondaryScript", word.secondaryScript());
    attribute("customScripts", word.customScripts());
    attribute("readingDirection", word.readingDirection());
    attribute("production", word.production());
    attribute("custom", word.custom());
    attribute("comments", word.comments());
    alternativeImages(word.alternativeImages());
    coords("Coords", word.coords());
    for (Glyph glyph : word.glyphs()) {
      glyph(glyph);
    }
    textEquivs(word.textEquivs());
    textStyle(word.textStyle());
    userDefined(word.userAttributes());
    labels(word.labels());
    end();
  }

  private void glyph(Glyph glyph) {
    if (!start("Glyph", describe(glyph))) {
      return;
    }
    attribute("id", glyph.id());
    attribute("orientation", glyph.orientation());
    attribute("mirrored", glyph.mirrored());
    attribute("ligature", glyph.ligature());
    attribute("symbol", glyph.symbol());
    attribute("script", glyph.script());
    attribute("customScript", glyph.customScript());
    attribute("production", glyph.production());
    attribute("custom", glyph.custom());
    attribute("comments", glyph.comments());
    alternativeImages(glyph.alternativeImages());
    coords("Coords", glyph.coords());
    if (!glyph.graphemes().isEmpty() && start("Graphemes", null)) {
      for (GraphemeElement grapheme : glyph.graphemes()) {
        grapheme(grapheme);
      }
      end();
    }
    textEquivs(glyph.textEquivs());
    textStyle(glyph.textStyle());
    userDefined(glyph.userAttributes());
    labels(glyph.labels());
    end();
  }

  private void grapheme(GraphemeElement grapheme) {
    String name = grapheme.getClass().getSimpleName();
    if (!start(name, describe(grapheme))) {
      return;
    }
    attribute("id", grapheme.id());
    attribute("index", grapheme.index());
    attribute("ligature", grapheme.ligature());
    attribute("charType", grapheme.charType());
    attribute("custom", grapheme.custom());
    attribute("comments", grapheme.comments());
    textEquivs(grapheme.textEquivs());
    switch (grapheme) {
      case Grapheme g -> coords("Coords", g.coords());
      case NonPrintingChar c -> {}
      case GraphemeGroup g -> {
        for (GraphemeElement member : g.graphemes()) {
          grapheme(member);
        }
      }
    }
    end();
  }

  // ---- Reading order, layers, relations

  private void readingOrder(@Nullable ReadingOrder order) {
    if (order == null || !start("ReadingOrder", null)) {
      return;
    }
    attribute("conf", order.conf());
    group(order.group(), null);
    end();
  }

  /**
   * A group; in an ordered group it is the {@code …Indexed} element with the given index.
   *
   * @param index the position in the parent ordered group, or null outside one
   */
  private void group(Group group, @Nullable Integer index) {
    boolean ordered = group instanceof OrderedGroup;
    String name = (ordered ? "OrderedGroup" : "UnorderedGroup") + (index != null ? "Indexed" : "");
    if (!start(name, describe(group))) {
      return;
    }
    attribute("id", group.id());
    attribute("regionRef", group.regionRef());
    attribute("index", index);
    attribute("caption", group.caption());
    attribute("type", group.type());
    attribute("continuation", group.continuation());
    attribute("custom", group.custom());
    attribute("comments", group.comments());
    userDefined(group.userAttributes());
    labels(group.labels());
    List<GroupMember> members = group.members();
    for (int i = 0; i < members.size(); i++) {
      Integer memberIndex = ordered ? i : null;
      switch (members.get(i)) {
        case RegionRef ref -> {
          if (start(ordered ? "RegionRefIndexed" : "RegionRef", null)) {
            attribute("index", memberIndex);
            attribute("regionRef", ref.regionRef());
            end();
          }
        }
        case Group child -> group(child, memberIndex);
      }
    }
    end();
  }

  private void layers(List<Layer> layers) {
    if (layers.isEmpty() || !start("Layers", null)) {
      return;
    }
    for (Layer layer : layers) {
      if (start("Layer", describe(layer))) {
        attribute("id", layer.id());
        attribute("zIndex", layer.zIndex());
        attribute("caption", layer.caption());
        for (String ref : layer.regionRefs()) {
          regionRef("RegionRef", ref);
        }
        end();
      }
    }
    end();
  }

  private void regionRef(String name, String ref) {
    if (start(name, null)) {
      attribute("regionRef", ref);
      end();
    }
  }

  private void relations(List<Relation> relations) {
    if (relations.isEmpty() || !start("Relations", null)) {
      return;
    }
    for (Relation relation : relations) {
      String context = relation.id() == null ? "Relation" : "Relation '" + relation.id() + "'";
      if (!start("Relation", context)) {
        continue;
      }
      attribute("id", relation.id());
      attribute("type", relation.type());
      attribute("custom", relation.custom());
      attribute("comments", relation.comments());
      labels(relation.labels());
      TypeRules relationRules = frames.element().rules;
      if (relationRules != null && relationRules.children().containsKey("SourceRegionRef")) {
        regionRef("SourceRegionRef", relation.sourceRegionRef());
        regionRef("TargetRegionRef", relation.targetRegionRef());
      } else {
        // Up to 2017: two RegionRef elements, source then target.
        regionRef("RegionRef", relation.sourceRegionRef());
        regionRef("RegionRef", relation.targetRegionRef());
      }
      end();
    }
    end();
  }
}
