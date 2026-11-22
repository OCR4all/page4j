package com.maxnth.page4j.formats.internal;

import com.maxnth.page4j.geometry.Point;
import com.maxnth.page4j.geometry.Polygon;
import com.maxnth.page4j.geometry.Rect;
import com.maxnth.page4j.model.AdvertRegion;
import com.maxnth.page4j.model.ChartRegion;
import com.maxnth.page4j.model.ChemRegion;
import com.maxnth.page4j.model.FormRegion;
import com.maxnth.page4j.model.Glyph;
import com.maxnth.page4j.model.GraphicRegion;
import com.maxnth.page4j.model.Group;
import com.maxnth.page4j.model.GroupMember;
import com.maxnth.page4j.model.ImageRegion;
import com.maxnth.page4j.model.Label;
import com.maxnth.page4j.model.LabelGroup;
import com.maxnth.page4j.model.LineDrawingRegion;
import com.maxnth.page4j.model.MapRegion;
import com.maxnth.page4j.model.MathsRegion;
import com.maxnth.page4j.model.MetadataItem;
import com.maxnth.page4j.model.MusicRegion;
import com.maxnth.page4j.model.OrderedGroup;
import com.maxnth.page4j.model.Page;
import com.maxnth.page4j.model.ReadingOrder;
import com.maxnth.page4j.model.Region;
import com.maxnth.page4j.model.RegionRef;
import com.maxnth.page4j.model.SeparatorRegion;
import com.maxnth.page4j.model.TableRegion;
import com.maxnth.page4j.model.TextEquiv;
import com.maxnth.page4j.model.TextLine;
import com.maxnth.page4j.model.TextRegion;
import com.maxnth.page4j.model.TextStyle;
import com.maxnth.page4j.model.UnorderedGroup;
import com.maxnth.page4j.model.Word;
import com.maxnth.page4j.model.value.Language;
import com.maxnth.page4j.model.value.MetadataItemType;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import javax.xml.XMLConstants;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.jspecify.annotations.Nullable;

/**
 * Writes a page as ALTO 4.1. The document is built as a small tree first, so styles and tags can be
 * collected while writing the layout and still precede it.
 */
public final class AltoWriter {

  public static final String NAMESPACE = "http://www.loc.gov/standards/alto/ns-v4#";
  private static final String SCHEMA = "http://www.loc.gov/standards/alto/v4/alto-4-1.xsd";
  private static final XMLOutputFactory FACTORY = XMLOutputFactory.newFactory();
  private static final String INDENT = "  ";
  private static final Set<String> PROCESSING_CATEGORIES =
      Set.of("contentGeneration", "contentModification", "preOperation", "postOperation", "other");

  /** An element of the document being built. */
  private static final class Node {
    final String name;
    final Map<String, String> attributes = new LinkedHashMap<>();
    final List<Node> children = new ArrayList<>();
    @Nullable String text;

    Node(String name) {
      this.name = name;
    }

    Node add(String child) {
      Node node = new Node(child);
      children.add(node);
      return node;
    }

    void text(String child, String value) {
      add(child).text = value;
    }

    void attribute(String attribute, @Nullable Object value) {
      if (value != null) {
        attributes.put(attribute, format(value));
      }
    }
  }

  private record AltoTextStyle(
      @Nullable String fontFamily,
      @Nullable String fontType,
      @Nullable String fontWidth,
      double fontSize,
      @Nullable String fontColor,
      @Nullable String fontStyle) {}

  private record ParagraphStyle(@Nullable String align, @Nullable Integer lineSpace) {}

  private record Tag(
      @Nullable String type, String label, @Nullable String description, @Nullable String uri) {}

  private final Page page;
  private final PageText pageText;
  private final Map<String, String> nextRegion = new HashMap<>();
  private final Map<AltoTextStyle, String> textStyles = new LinkedHashMap<>();
  private final Map<ParagraphStyle, String> paragraphStyles = new LinkedHashMap<>();
  private final Map<Tag, String> tags = new LinkedHashMap<>();

  private AltoWriter(Page page) {
    this.page = page;
    this.pageText = new PageText(page);
  }

  public static byte[] write(Page page) {
    return new AltoWriter(page).document();
  }

  private byte[] document() {
    ReadingOrder order = page.readingOrder();
    if (order != null) {
      collectNext(order.group());
    }
    Node layout = layout();
    Node root = new Node("alto");
    root.attribute("SCHEMAVERSION", "4.1");
    root.children.add(description());
    if (!textStyles.isEmpty() || !paragraphStyles.isEmpty()) {
      Node styles = root.add("Styles");
      textStyles.forEach((style, id) -> textStyle(styles.add("TextStyle"), style, id));
      paragraphStyles.forEach(
          (style, id) -> {
            Node node = styles.add("ParagraphStyle");
            node.attribute("ID", id);
            node.attribute("ALIGN", style.align());
            node.attribute(
                "LINESPACE", style.lineSpace() == null ? null : (double) style.lineSpace());
          });
    }
    if (!tags.isEmpty()) {
      Node tagsNode = root.add("Tags");
      tags.forEach(
          (tag, id) -> {
            Node node = tagsNode.add("OtherTag");
            node.attribute("ID", id);
            node.attribute("TYPE", tag.type());
            node.attribute("LABEL", tag.label());
            node.attribute("DESCRIPTION", tag.description());
            node.attribute("URI", tag.uri());
          });
    }
    root.children.add(layout);
    return serialize(root);
  }

  /** An ID for the writer's own elements, avoiding the page's IDs. */
  private String unique(String base) {
    String id = base;
    for (int i = 2; page.find(id).isPresent() || id.equals(page.pcGtsId()); i++) {
      id = base + "_" + i;
    }
    return id;
  }

  // ---- Description

  private Node description() {
    Node description = new Node("Description");
    description.text("MeasurementUnit", "pixel");
    description.add("sourceImageInformation").text("fileName", page.imageFilename());
    int number = 1;
    for (MetadataItem item : page.metadata().metadataItems()) {
      if (item.type() != MetadataItemType.PROCESSING_STEP) {
        continue;
      }
      Node processing = description.add("Processing");
      processing.attribute("ID", unique("pro" + number++));
      Map<String, String> labels = new HashMap<>();
      for (LabelGroup group : item.labels()) {
        for (Label label : group.labels()) {
          if (label.type() != null) {
            labels.putIfAbsent(label.type(), label.value());
          }
        }
      }
      String category = labels.get("processingCategory");
      if (category != null && PROCESSING_CATEGORIES.contains(category)) {
        processing.text("processingCategory", category);
      }
      if (item.date() != null) {
        processing.text("processingDateTime", item.date().xmlValue());
      }
      optionalText(processing, "processingAgency", labels);
      if (!item.value().isEmpty()) {
        processing.text("processingStepDescription", item.value());
      }
      optionalText(processing, "processingStepSettings", labels);
      if (item.name() != null
          || labels.containsKey("softwareCreator")
          || labels.containsKey("softwareVersion")
          || labels.containsKey("applicationDescription")) {
        Node software = processing.add("processingSoftware");
        optionalText(software, "softwareCreator", labels);
        if (item.name() != null) {
          software.text("softwareName", item.name());
        }
        optionalText(software, "softwareVersion", labels);
        optionalText(software, "applicationDescription", labels);
      }
    }
    return description;
  }

  private static void optionalText(Node parent, String name, Map<String, String> values) {
    String value = values.get(name);
    if (value != null) {
      parent.text(name, value);
    }
  }

  private static void textStyle(Node node, AltoTextStyle style, String id) {
    node.attribute("ID", id);
    node.attribute("FONTFAMILY", style.fontFamily());
    node.attribute("FONTTYPE", style.fontType());
    node.attribute("FONTWIDTH", style.fontWidth());
    node.attribute("FONTSIZE", style.fontSize());
    node.attribute("FONTCOLOR", style.fontColor());
    node.attribute("FONTSTYLE", style.fontStyle());
  }

  // ---- Layout

  private Node layout() {
    Node layout = new Node("Layout");
    Node pageNode = layout.add("Page");
    pageNode.attribute("ID", page.pcGtsId() != null ? page.pcGtsId() : unique("p0"));
    pageNode.attribute("PHYSICAL_IMG_NR", 0);
    pageNode.attribute("WIDTH", page.imageWidth());
    pageNode.attribute("HEIGHT", page.imageHeight());
    pageNode.attribute("PAGECLASS", page.type());
    pageNode.attribute("PC", page.conf());
    margins(pageNode);
    Node printSpace = pageNode.add("PrintSpace");
    printSpace.attribute("ID", unique("PageSpaceTypeID0"));
    Polygon space = page.printSpace();
    if (space != null) {
      position(printSpace, space);
      shape(printSpace, space);
    }
    for (Region region : page.regions()) {
      block(printSpace, region);
    }
    return layout;
  }

  /** Everything outside the print space, or outside the regions if there is none, is margin. */
  private void margins(Node pageNode) {
    Node top = pageNode.add("TopMargin");
    top.attribute("ID", unique("TopMarginTypeID0"));
    Node left = pageNode.add("LeftMargin");
    left.attribute("ID", unique("LeftMarginTypeID0"));
    Node right = pageNode.add("RightMargin");
    right.attribute("ID", unique("RightMarginTypeID0"));
    Node bottom = pageNode.add("BottomMargin");
    bottom.attribute("ID", unique("BottomMarginTypeID0"));

    int width = page.imageWidth() - 1;
    int height = page.imageHeight() - 1;
    Rect space;
    Polygon printSpace = page.printSpace();
    if (printSpace != null && !printSpace.isEmpty()) {
      space = printSpace.boundingBox();
    } else {
      Polygon.Builder corners = Polygon.builder();
      for (Region region : page.regions()) {
        if (!region.coords().isEmpty()) {
          Rect box = region.coords().boundingBox();
          corners.add(box.left(), box.top()).add(box.right(), box.bottom());
        }
      }
      space = corners.size() == 0 ? new Rect(0, 0, width, height) : corners.build().boundingBox();
    }
    // The text area, clipped to the image; a margin it leaves no room for is empty, at the edge.
    int spaceLeft = Math.clamp(space.left(), 0, width + 1);
    int spaceTop = Math.clamp(space.top(), 0, height + 1);
    int spaceRight = Math.clamp(space.right(), spaceLeft - 1, width);
    int spaceBottom = Math.clamp(space.bottom(), spaceTop - 1, height);
    int innerHeight = spaceBottom - spaceTop + 1;
    margin(top, 0, 0, width + 1, spaceTop);
    margin(left, 0, spaceTop, spaceLeft, innerHeight);
    margin(right, spaceRight + 1, spaceTop, width - spaceRight, innerHeight);
    margin(bottom, 0, spaceBottom + 1, width + 1, height - spaceBottom);
  }

  private static void margin(Node node, int hpos, int vpos, int width, int height) {
    node.attribute("HPOS", hpos);
    node.attribute("VPOS", vpos);
    node.attribute("WIDTH", Math.max(0, width));
    node.attribute("HEIGHT", Math.max(0, height));
  }

  private void block(Node parent, Region region) {
    String kind = region.getClass().getSimpleName();
    String element =
        !region.regions().isEmpty()
            ? "ComposedBlock"
            : region instanceof TextRegion
                ? "TextBlock"
                : region instanceof SeparatorRegion ? "GraphicalElement" : "Illustration";
    Node block = parent.add(element);
    block.attribute("ID", region.id());
    if (element.equals("ComposedBlock") || element.equals("Illustration")) {
      block.attribute("TYPE", kind);
    }
    position(block, region.coords());
    block.attribute("ROTATION", orientation(region));
    List<String> tagRefs = new ArrayList<>(tagRefs(region.labels()));
    if (region instanceof TextRegion text) {
      StringJoiner styleRefs = new StringJoiner(" ");
      String textStyle = textStyle(text.textStyle());
      if (textStyle != null) {
        styleRefs.add(textStyle);
      }
      String paragraphStyle = paragraphStyle(text);
      if (paragraphStyle != null) {
        styleRefs.add(paragraphStyle);
      }
      block.attribute("STYLEREFS", styleRefs.length() == 0 ? null : styleRefs.toString());
      if (text.type() != null) {
        tagRefs.add(tag(new Tag(null, text.type().xmlValue(), AltoReader.TEXT_TYPE_TAG, null)));
      }
    }
    block.attribute("IDNEXT", nextRegion.get(region.id()));
    block.attribute("TAGREFS", tagRefs.isEmpty() ? null : String.join(" ", tagRefs));
    if (!(region instanceof TextRegion) && !(region instanceof SeparatorRegion)) {
      if (!region.alternativeImages().isEmpty()) {
        block.attribute("FILEID", region.alternativeImages().getFirst().filename());
      }
    }
    shape(block, region.coords());
    if (!region.regions().isEmpty()) {
      for (Region child : region.regions()) {
        block(block, child);
      }
    } else if (region instanceof TextRegion text) {
      if (text.primaryLanguage() != null) {
        block.attribute("LANG", Languages.toCode(text.primaryLanguage()));
      }
      for (TextLine line : text.textLines()) {
        textLine(block, line);
      }
    }
  }

  private void textLine(Node block, TextLine line) {
    Node node = block.add("TextLine");
    node.attribute("ID", line.id());
    language(node, line.primaryLanguage());
    position(node, line.coords());
    node.attribute("STYLEREFS", textStyle(line.textStyle()));
    List<String> tagRefs = tagRefs(line.labels());
    node.attribute("TAGREFS", tagRefs.isEmpty() ? null : String.join(" ", tagRefs));
    shape(node, line.coords());
    if (line.words().isEmpty()) {
      // ALTO needs a string in every line: one for the whole line.
      Node string = node.add("String");
      string.attribute("CONTENT", pageText.line(line));
      position(string, line.coords());
      return;
    }
    for (Word word : line.words()) {
      word(node, word);
    }
  }

  private void word(Node line, Word word) {
    Node node = line.add("String");
    node.attribute("ID", word.id());
    node.attribute("CONTENT", pageText.word(word));
    language(node, word.language());
    position(node, word.coords());
    node.attribute("STYLEREFS", textStyle(word.textStyle()));
    TextEquiv main = PageText.main(word.textEquivs());
    node.attribute("WC", main == null ? null : main.conf());
    List<String> tagRefs = tagRefs(word.labels());
    node.attribute("TAGREFS", tagRefs.isEmpty() ? null : String.join(" ", tagRefs));
    shape(node, word.coords());
    for (TextEquiv alternative : PageText.others(word.textEquivs())) {
      node.text("ALTERNATIVE", alternative.unicode());
    }
    for (Glyph glyph : word.glyphs()) {
      glyph(node, glyph);
    }
  }

  private void glyph(Node word, Glyph glyph) {
    Node node = word.add("Glyph");
    node.attribute("ID", glyph.id());
    String content = pageText.glyph(glyph);
    node.attribute("CONTENT", content.isEmpty() ? "?" : content);
    position(node, glyph.coords());
    TextEquiv main = PageText.main(glyph.textEquivs());
    node.attribute("GC", main == null ? null : main.conf());
    shape(node, glyph.coords());
    for (TextEquiv variant : PageText.others(glyph.textEquivs())) {
      Node variantNode = node.add("Variant");
      variantNode.attribute("CONTENT", variant.unicode());
      variantNode.attribute("VC", variant.conf());
    }
  }

  private static void language(Node node, @Nullable Language language) {
    if (language != null) {
      node.attribute("LANG", Languages.toCode(language));
    }
  }

  private static void position(Node node, Polygon coords) {
    if (coords.isEmpty()) {
      return;
    }
    Rect box = coords.boundingBox();
    node.attribute("HPOS", box.left());
    node.attribute("VPOS", box.top());
    node.attribute("WIDTH", box.width());
    node.attribute("HEIGHT", box.height());
  }

  private static void shape(Node node, Polygon coords) {
    if (coords.isEmpty()) {
      return;
    }
    StringJoiner points = new StringJoiner(" ");
    for (Point p : coords.points()) {
      points.add(p.x() + "," + p.y());
    }
    node.add("Shape").add("Polygon").attribute("POINTS", points.toString());
  }

  private static @Nullable Double orientation(Region region) {
    return switch (region) {
      case TextRegion r -> r.orientation();
      case ImageRegion r -> r.orientation();
      case LineDrawingRegion r -> r.orientation();
      case GraphicRegion r -> r.orientation();
      case TableRegion r -> r.orientation();
      case ChartRegion r -> r.orientation();
      case MapRegion r -> r.orientation();
      case SeparatorRegion r -> r.orientation();
      case MathsRegion r -> r.orientation();
      case ChemRegion r -> r.orientation();
      case MusicRegion r -> r.orientation();
      case AdvertRegion r -> r.orientation();
      case FormRegion r -> r.orientation();
      default -> null;
    };
  }

  // ---- Styles, tags and reading order

  /** The ID of the ALTO text style of a PAGE text style; ALTO requires a font size. */
  private @Nullable String textStyle(@Nullable TextStyle style) {
    if (style == null || style.fontSize() == null) {
      return null;
    }
    StringJoiner fontStyle = new StringJoiner(" ");
    if (Boolean.TRUE.equals(style.bold())) fontStyle.add("bold");
    if (Boolean.TRUE.equals(style.italic())) fontStyle.add("italics");
    if (Boolean.TRUE.equals(style.subscript())) fontStyle.add("subscript");
    if (Boolean.TRUE.equals(style.superscript())) fontStyle.add("superscript");
    if (Boolean.TRUE.equals(style.smallCaps())) fontStyle.add("smallcaps");
    if (Boolean.TRUE.equals(style.underlined())) fontStyle.add("underline");
    Integer rgb = style.textColourRgb();
    var alto =
        new AltoTextStyle(
            style.fontFamily() == null || style.fontFamily().isEmpty() ? null : style.fontFamily(),
            style.serif() == null ? null : style.serif() ? "serif" : "sans-serif",
            style.monospace() == null ? null : style.monospace() ? "fixed" : "proportional",
            style.fontSize(),
            rgb == null
                ? null
                : String.format("%02X%02X%02X", rgb % 256, (rgb / 256) % 256, (rgb / 65536) % 256),
            fontStyle.length() == 0 ? null : fontStyle.toString());
    return textStyles.computeIfAbsent(alto, s -> unique("ts" + (textStyles.size() + 1)));
  }

  private @Nullable String paragraphStyle(TextRegion region) {
    String align =
        region.align() == null
            ? null
            : switch (region.align()) {
              case LEFT -> "Left";
              case RIGHT -> "Right";
              case CENTRE -> "Center";
              case JUSTIFY -> "Block";
            };
    if (align == null && region.leading() == null) {
      return null;
    }
    return paragraphStyles.computeIfAbsent(
        new ParagraphStyle(align, region.leading()),
        s -> unique("ps" + (paragraphStyles.size() + 1)));
  }

  private List<String> tagRefs(List<LabelGroup> groups) {
    List<String> refs = new ArrayList<>();
    for (LabelGroup group : groups) {
      for (Label label : group.labels()) {
        if (!label.value().isEmpty()) {
          String ref =
              tag(new Tag(label.type(), label.value(), label.comments(), group.externalModel()));
          if (!refs.contains(ref)) {
            refs.add(ref);
          }
        }
      }
    }
    return refs;
  }

  private String tag(Tag tag) {
    return tags.computeIfAbsent(tag, t -> unique("tag" + (tags.size() + 1)));
  }

  /** Each region in an ordered group points to the region after it. */
  private void collectNext(Group group) {
    List<GroupMember> members =
        switch (group) {
          case OrderedGroup g -> g.members();
          case UnorderedGroup g -> g.members();
        };
    for (int i = 0; i < members.size(); i++) {
      GroupMember member = members.get(i);
      if (member instanceof Group child) {
        collectNext(child);
      } else if (group instanceof OrderedGroup
          && member instanceof RegionRef ref
          && i + 1 < members.size()
          && members.get(i + 1) instanceof RegionRef after) {
        nextRegion.putIfAbsent(ref.regionRef(), after.regionRef());
      }
    }
  }

  // ---- Output

  private static String format(Object value) {
    return switch (value) {
      case String s -> s;
      case Double d -> formatDouble(d);
      case com.maxnth.page4j.model.value.XmlValue v -> v.xmlValue();
      default -> value.toString();
    };
  }

  /** Java's form ({@code 0.0}, {@code 0.5}) without exponent. */
  private static String formatDouble(double value) {
    String text = Double.toString(value);
    if (text.indexOf('E') < 0) {
      return text;
    }
    String plain = BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    return plain.indexOf('.') < 0 ? plain + ".0" : plain;
  }

  private static byte[] serialize(Node root) {
    var bytes = new ByteArrayOutputStream();
    try {
      XMLStreamWriter out = FACTORY.createXMLStreamWriter(bytes, "UTF-8");
      out.writeStartDocument("UTF-8", "1.0");
      out.writeCharacters("\n");
      out.setDefaultNamespace(NAMESPACE);
      out.writeStartElement(NAMESPACE, root.name);
      out.writeDefaultNamespace(NAMESPACE);
      out.writeNamespace("xsi", XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI);
      out.writeAttribute(
          "xsi",
          XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI,
          "schemaLocation",
          NAMESPACE + " " + SCHEMA);
      attributesAndContent(out, root, 0);
      out.writeEndDocument();
      out.writeCharacters("\n");
      out.close();
    } catch (XMLStreamException e) {
      throw new IllegalStateException("Cannot write ALTO", e);
    }
    return bytes.toByteArray();
  }

  private static void node(XMLStreamWriter out, Node node, int depth) throws XMLStreamException {
    out.writeCharacters("\n" + INDENT.repeat(depth));
    if (node.children.isEmpty() && node.text == null) {
      out.writeEmptyElement(NAMESPACE, node.name);
      for (Map.Entry<String, String> attribute : node.attributes.entrySet()) {
        out.writeAttribute(attribute.getKey(), checked(node, attribute));
      }
      return;
    }
    out.writeStartElement(NAMESPACE, node.name);
    attributesAndContent(out, node, depth);
  }

  private static void attributesAndContent(XMLStreamWriter out, Node node, int depth)
      throws XMLStreamException {
    for (Map.Entry<String, String> attribute : node.attributes.entrySet()) {
      out.writeAttribute(attribute.getKey(), checked(node, attribute));
    }
    if (node.text != null) {
      out.writeCharacters(XmlText.checked("ALTO", element(node), node.text));
    }
    for (Node child : node.children) {
      node(out, child, depth + 1);
    }
    if (!node.children.isEmpty()) {
      out.writeCharacters("\n" + INDENT.repeat(depth));
    }
    out.writeEndElement();
  }

  private static String checked(Node node, Map.Entry<String, String> attribute) {
    return XmlText.checked(
        "ALTO", element(node) + " attribute " + attribute.getKey(), attribute.getValue());
  }

  private static String element(Node node) {
    String id = node.attributes.get("ID");
    return id == null ? node.name : node.name + " '" + id + "'";
  }
}
