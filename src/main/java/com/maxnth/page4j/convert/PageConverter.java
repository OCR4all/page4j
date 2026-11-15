package com.maxnth.page4j.convert;

import static com.maxnth.page4j.pagexml.PageXmlVersion.V2009_03_16;
import static com.maxnth.page4j.pagexml.PageXmlVersion.V2010_01_12;
import static com.maxnth.page4j.pagexml.PageXmlVersion.V2010_03_19;
import static com.maxnth.page4j.pagexml.PageXmlVersion.V2013_07_15;
import static com.maxnth.page4j.pagexml.PageXmlVersion.V2016_07_15;
import static com.maxnth.page4j.pagexml.PageXmlVersion.V2017_07_15;
import static com.maxnth.page4j.pagexml.PageXmlVersion.V2018_07_15;
import static com.maxnth.page4j.pagexml.PageXmlVersion.V2019_07_15;
import static com.maxnth.page4j.pagexml.PageXmlVersion.V2024_07_15;

import com.maxnth.page4j.convert.ConversionMessage.Kind;
import com.maxnth.page4j.geometry.Point;
import com.maxnth.page4j.geometry.Polygon;
import com.maxnth.page4j.model.AdvertRegion;
import com.maxnth.page4j.model.AlternativeImage;
import com.maxnth.page4j.model.ChartRegion;
import com.maxnth.page4j.model.ChemRegion;
import com.maxnth.page4j.model.CustomRegion;
import com.maxnth.page4j.model.CustomRegionBuilder;
import com.maxnth.page4j.model.FormRegion;
import com.maxnth.page4j.model.FrameRegion;
import com.maxnth.page4j.model.FrameRegionBuilder;
import com.maxnth.page4j.model.Glyph;
import com.maxnth.page4j.model.Grapheme;
import com.maxnth.page4j.model.GraphemeElement;
import com.maxnth.page4j.model.GraphemeGroup;
import com.maxnth.page4j.model.GraphicRegion;
import com.maxnth.page4j.model.GraphicRegionBuilder;
import com.maxnth.page4j.model.Grid;
import com.maxnth.page4j.model.Group;
import com.maxnth.page4j.model.GroupMember;
import com.maxnth.page4j.model.ImageRegion;
import com.maxnth.page4j.model.LabelGroup;
import com.maxnth.page4j.model.Layer;
import com.maxnth.page4j.model.LineDrawingRegion;
import com.maxnth.page4j.model.MapRegion;
import com.maxnth.page4j.model.MathsRegion;
import com.maxnth.page4j.model.Metadata;
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
import com.maxnth.page4j.model.TextStyleBuilder;
import com.maxnth.page4j.model.UnknownRegion;
import com.maxnth.page4j.model.UnknownRegionBuilder;
import com.maxnth.page4j.model.UnorderedGroup;
import com.maxnth.page4j.model.UserAttribute;
import com.maxnth.page4j.model.Word;
import com.maxnth.page4j.model.value.Colour;
import com.maxnth.page4j.model.value.ColourDepth;
import com.maxnth.page4j.model.value.GraphicsType;
import com.maxnth.page4j.model.value.Language;
import com.maxnth.page4j.model.value.Mirrored;
import com.maxnth.page4j.model.value.RelationType;
import com.maxnth.page4j.model.value.Script;
import com.maxnth.page4j.model.value.TextType;
import com.maxnth.page4j.model.value.XmlValue;
import com.maxnth.page4j.pagexml.PageXmlVersion;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Fits a page to a PAGE version, so it can be written in that version.
 *
 * <p>The model holds the content of all versions, so a page doesn't know its version and the
 * conversion needs only the target: whatever the target can't hold is removed or replaced, and what
 * it requires is added. Every change is reported. A page that already fits comes back equal and
 * without messages.
 *
 * <p>The rules, for all versions from 2009-03-16 to 2024-07-15:
 *
 * <ul>
 *   <li>Attributes and elements a version doesn't define are removed, for example {@code mirrored}
 *       before 2024, labels and alternative images of regions before 2018, user-defined attributes
 *       and table cell roles before 2017. Graphemes before 2017 are removed together with the
 *       relations that refer to them.
 *   <li>Region types: before 2024 a {@code FormRegion} becomes a {@code CustomRegion} of type
 *       {@code form}, and a {@code MapRegion} nested in another region one of type {@code map}.
 *       Before 2018 map and custom regions (and form regions) become {@code UnknownRegion}s.
 *   <li>Scripts: 2013 names ({@code Arabic}) and the ISO 15924 values of 2016 and later ({@code
 *       Arab - Arabic}) are mapped onto each other; a script 2013 doesn't name becomes {@code
 *       other}.
 *   <li>The text type {@code list-label} becomes {@code other} before 2017.
 *   <li>Relations get an ID from 2018, where it is required, and lose it before. Before 2018 a
 *       relation without a type gets the type {@code link}, which those versions require.
 *   <li>2013 allows one {@code TextEquiv} per text object: the one with the lowest index is kept.
 *   <li>Up to 2010: a {@code FrameRegion} is a {@code GraphicRegion} of type {@code frame} from
 *       2013 on, and the other way round. Advert, chemistry and music regions become {@code
 *       UnknownRegion}s. Regions nest only in frames, so other nested regions are moved next to the
 *       region that held them. Values these versions lack are removed (colour {@code other}, newer
 *       languages, text and graphic types), relations are removed, and a {@code TextEquiv} without
 *       {@code PlainText}, which they require, gets a copy of its {@code Unicode}. Of a text
 *       region's style only {@code textColour}, {@code bgColour}, {@code reverseVideo}, {@code
 *       fontSize} and {@code kerning} remain. 2009-03-16 requires a region, so a page without one
 *       gets an empty {@code UnknownRegion}.
 * </ul>
 */
public final class PageConverter {

  /** 2013 script names and the ISO 15924 values that replaced them in 2016. */
  private static final Map<Script, Script> ISO_SCRIPTS = new EnumMap<>(Script.class);

  private static final Map<Script, Script> OLD_SCRIPTS = new EnumMap<>(Script.class);

  static {
    script("Arabic", "Arab - Arabic");
    script("Bengali", "Beng - Bengali");
    script("Chinese-simplified", "Hans - Han (Simplified variant)");
    script("Chinese-traditional", "Hant - Han (Traditional variant)");
    script("Cyrillic", "Cyrl - Cyrillic");
    script("Devangari", "Deva - Devanagari (Nagari)");
    script("Ethiopic", "Ethi - Ethiopic");
    script("Greek", "Grek - Greek");
    script("Gujarati", "Gujr - Gujarati");
    script("Gurmukhi", "Guru - Gurmukhi");
    script("Hebrew", "Hebr - Hebrew");
    script("Latin", "Latn - Latin");
    script("Thai", "Thai - Thai");
  }

  private static void script(String old, String iso) {
    ISO_SCRIPTS.put(Script.fromXml(old), Script.fromXml(iso));
    OLD_SCRIPTS.put(Script.fromXml(iso), Script.fromXml(old));
  }

  /** The languages of 2009-03-16 to 2010-03-19; 2013 added many. */
  private static final Set<Language> LEGACY_LANGUAGES = EnumSet.noneOf(Language.class);

  static {
    for (String name :
        List.of(
            "Afrikaans",
            "Albanian",
            "Amharic",
            "Arabic",
            "Basque",
            "Bengali",
            "Bulgarian",
            "Cambodian",
            "Cantonese",
            "Chinese",
            "Czech",
            "Danish",
            "Dutch",
            "English",
            "Estonian",
            "Finnish",
            "French",
            "German",
            "Greek",
            "Gujarati",
            "Hebrew",
            "Hindi",
            "Hungarian",
            "Icelandic",
            "Gaelic",
            "Italian",
            "Japanese",
            "Korean",
            "Latin",
            "Latvian",
            "Malay",
            "Norwegian",
            "Polish",
            "Portuguese",
            "Punjabi",
            "Russian",
            "Spanish",
            "Swedish",
            "Thai",
            "Turkish",
            "Urdu",
            "Welsh",
            "other")) {
      LEGACY_LANGUAGES.add(Language.fromXml(name));
    }
  }

  /** Text types added in 2010-03-19. */
  private static final Set<TextType> TEXT_TYPES_2010_03_19 =
      EnumSet.of(
          TextType.SIGNATURE_MARK,
          TextType.CATCH_WORD,
          TextType.MARGINALIA,
          TextType.FOOTNOTE,
          TextType.FOOTNOTE_CONTINUED,
          TextType.TOC_ENTRY);

  /** Text types added in 2013 ({@code list-label} came in 2017). */
  private static final Set<TextType> TEXT_TYPES_2013 =
      EnumSet.of(TextType.ENDNOTE, TextType.OTHER, TextType.LIST_LABEL);

  private static final TextStyle NO_STYLE = TextStyleBuilder.builder().build();

  private final PageXmlVersion target;
  private final List<ConversionMessage> messages = new ArrayList<>();

  private PageConverter(PageXmlVersion target) {
    this.target = target;
  }

  /** Fits the page to the given version. */
  public static Conversion convert(Page page, PageXmlVersion target) {
    PageConverter converter = new PageConverter(target);
    return new Conversion(converter.page(page), converter.messages);
  }

  // ---- Messages and the checks behind them

  private boolean lacks(PageXmlVersion since) {
    return target.compareTo(since) < 0;
  }

  private void removed(String subject, String what) {
    messages.add(
        new ConversionMessage(
            Kind.REMOVED, subject + ": removed " + what + ", not supported in " + target));
  }

  private void changed(String subject, String what) {
    messages.add(new ConversionMessage(Kind.CHANGED, subject + ": " + what));
  }

  private void added(String subject, String what) {
    messages.add(new ConversionMessage(Kind.ADDED, subject + ": " + what));
  }

  /** The attribute's value, or null if the target lacks the attribute. */
  private <V> @Nullable V attribute(
      PageXmlVersion since, String subject, String name, @Nullable V value) {
    if (value != null && lacks(since)) {
      removed(subject, "attribute " + name);
      return null;
    }
    return value;
  }

  /** The element, or null if the target lacks it. */
  private <V> @Nullable V element(
      PageXmlVersion since, String subject, String name, @Nullable V value) {
    if (value != null && lacks(since)) {
      removed(subject, "element " + name);
      return null;
    }
    return value;
  }

  /** The elements, or none if the target lacks them. */
  private <V> List<V> elements(PageXmlVersion since, String subject, String name, List<V> values) {
    if (!values.isEmpty() && lacks(since)) {
      int n = values.size();
      removed(subject, n + " " + name + (n == 1 ? " element" : " elements"));
      return List.of();
    }
    return values;
  }

  /** The value, or null if the target lacks it (the attribute itself is supported). */
  private <V extends XmlValue> @Nullable V value(
      String subject, String name, @Nullable V value, boolean supported) {
    if (value != null && !supported) {
      messages.add(
          new ConversionMessage(
              Kind.REMOVED,
              subject + ": removed " + name + " " + quote(value) + ", not supported in " + target));
      return null;
    }
    return value;
  }

  /** Colours: {@code other} exists from 2013. */
  private @Nullable Colour colour(String subject, String name, @Nullable Colour colour) {
    return value(subject, name, colour, colour != Colour.OTHER || !lacks(V2013_07_15));
  }

  private @Nullable Language language(String subject, String name, @Nullable Language language) {
    return value(
        subject, name, language, !lacks(V2013_07_15) || LEGACY_LANGUAGES.contains(language));
  }

  /** User-defined attributes, written in a {@code UserDefined} element (from 2017). */
  private List<UserAttribute> userDefined(String subject, List<UserAttribute> attributes) {
    if (!attributes.isEmpty() && lacks(V2017_07_15)) {
      removed(subject, "element UserDefined");
      return List.of();
    }
    return attributes;
  }

  private List<LabelGroup> labels(String subject, List<LabelGroup> labels) {
    return elements(V2018_07_15, subject, "Labels", labels);
  }

  private static String describe(Object object) {
    return switch (object) {
      case Region r -> r.getClass().getSimpleName() + " '" + r.id() + "'";
      case TextLine l -> "TextLine '" + l.id() + "'";
      case Word w -> "Word '" + w.id() + "'";
      case Glyph g -> "Glyph '" + g.id() + "'";
      case Group g -> g.getClass().getSimpleName() + " '" + g.id() + "'";
      case Layer l -> "Layer '" + l.id() + "'";
      case Relation r ->
          r.id() != null
              ? "Relation '" + r.id() + "'"
              : "Relation '" + r.sourceRegionRef() + "' -> '" + r.targetRegionRef() + "'";
      default -> object.getClass().getSimpleName();
    };
  }

  private static String quote(XmlValue value) {
    return "'" + value.xmlValue() + "'";
  }

  // ---- Shared parts

  /**
   * Coordinates and script lines. Their confidence exists from 2018. From 2013 they need at least
   * two points (up to 2010 any number): a single point is repeated, and an empty polygon becomes
   * {@code 0,0 0,0}.
   */
  private @Nullable Polygon polygon(@Nullable Polygon polygon, String subject, String name) {
    if (polygon == null) {
      return null;
    }
    Polygon fitted = polygon;
    if (polygon.confidence() != null && lacks(V2018_07_15)) {
      removed(subject + ", " + name, "attribute conf");
      fitted = fitted.withConfidence(null);
    }
    if (polygon.points().size() < 2 && !lacks(V2013_07_15)) {
      Point point = polygon.points().isEmpty() ? new Point(0, 0) : polygon.points().getFirst();
      fitted = new Polygon(List.of(point, point), fitted.confidence());
      changed(
          subject + ", " + name,
          (polygon.points().isEmpty() ? "set the empty points to '" : "repeated the point, '")
              + point.x()
              + ","
              + point.y()
              + " "
              + point.x()
              + ","
              + point.y()
              + "', "
              + target
              + " requires at least two");
    }
    return fitted;
  }

  private Polygon coords(Polygon coords, String subject) {
    Polygon fitted = polygon(coords, subject, "Coords");
    return fitted == null ? coords : fitted;
  }

  private List<AlternativeImage> alternativeImages(String subject, List<AlternativeImage> images) {
    return images.stream()
        .map(
            i ->
                i.withConf(
                    attribute(V2018_07_15, subject + ", AlternativeImage", "conf", i.conf())))
        .toList();
  }

  private @Nullable Script script(
      PageXmlVersion since, String subject, String name, @Nullable Script script) {
    Script value = attribute(since, subject, name, script);
    if (value == null) {
      return null;
    }
    Script fitted;
    if (lacks(V2016_07_15)) {
      fitted =
          ISO_SCRIPTS.containsKey(value) || value == Script.OTHER
              ? value
              : OLD_SCRIPTS.getOrDefault(value, Script.OTHER);
    } else {
      fitted = ISO_SCRIPTS.getOrDefault(value, value);
    }
    if (fitted != value) {
      changed(
          subject,
          "changed "
              + name
              + " "
              + quote(value)
              + " to "
              + quote(fitted)
              + ", "
              + quote(value)
              + " is not supported in "
              + target);
    }
    return fitted;
  }

  private @Nullable TextStyle textStyle(String owner, @Nullable TextStyle style) {
    if (style == null) {
      return null;
    }
    String s = owner + ", TextStyle";
    if (lacks(V2013_07_15)) {
      return legacyTextStyle(s, style);
    }
    return style.with(
        b ->
            b.xHeight(attribute(V2016_07_15, s, "xHeight", style.xHeight()))
                .textColourRgb(attribute(V2017_07_15, s, "textColourRgb", style.textColourRgb()))
                .bgColourRgb(attribute(V2017_07_15, s, "bgColourRgb", style.bgColourRgb()))
                .underlineStyle(
                    attribute(V2019_07_15, s, "underlineStyle", style.underlineStyle())));
  }

  /**
   * Up to 2010 a text region's style is given by five of its attributes (and other objects have
   * none: their {@code TextStyle} element is removed before).
   */
  private @Nullable TextStyle legacyTextStyle(String s, TextStyle style) {
    TextStyle fitted =
        style.with(
            b ->
                b.fontFamily(attribute(V2013_07_15, s, "fontFamily", style.fontFamily()))
                    .serif(attribute(V2013_07_15, s, "serif", style.serif()))
                    .monospace(attribute(V2013_07_15, s, "monospace", style.monospace()))
                    .xHeight(attribute(V2013_07_15, s, "xHeight", style.xHeight()))
                    .textColour(colour(s, "textColour", style.textColour()))
                    .textColourRgb(
                        attribute(V2013_07_15, s, "textColourRgb", style.textColourRgb()))
                    .bgColour(colour(s, "bgColour", style.bgColour()))
                    .bgColourRgb(attribute(V2013_07_15, s, "bgColourRgb", style.bgColourRgb()))
                    .bold(attribute(V2013_07_15, s, "bold", style.bold()))
                    .italic(attribute(V2013_07_15, s, "italic", style.italic()))
                    .underlined(attribute(V2013_07_15, s, "underlined", style.underlined()))
                    .underlineStyle(
                        attribute(V2013_07_15, s, "underlineStyle", style.underlineStyle()))
                    .subscript(attribute(V2013_07_15, s, "subscript", style.subscript()))
                    .superscript(attribute(V2013_07_15, s, "superscript", style.superscript()))
                    .strikethrough(
                        attribute(V2013_07_15, s, "strikethrough", style.strikethrough()))
                    .smallCaps(attribute(V2013_07_15, s, "smallCaps", style.smallCaps()))
                    .letterSpaced(attribute(V2013_07_15, s, "letterSpaced", style.letterSpaced())));
    return fitted.equals(NO_STYLE) ? null : fitted;
  }

  /** The text alternatives; 2013 allows one, so the one with the lowest index is kept. */
  private List<TextEquiv> textEquivs(String owner, List<TextEquiv> equivs) {
    List<TextEquiv> kept = equivs;
    if (equivs.size() > 1 && lacks(V2016_07_15)) {
      TextEquiv first = equivs.getFirst();
      for (TextEquiv equiv : equivs) {
        if (index(equiv) < index(first)) {
          first = equiv;
        }
      }
      kept = List.of(first);
      int n = equivs.size();
      messages.add(
          new ConversionMessage(
              Kind.REMOVED,
              owner
                  + ": removed "
                  + (n - 1)
                  + " of "
                  + n
                  + " TextEquiv elements, "
                  + target
                  + " allows one"));
    }
    String s = owner + ", TextEquiv";
    return kept.stream()
        .map(
            e ->
                e.with(
                    b ->
                        b.index(attribute(V2016_07_15, s, "index", e.index()))
                            .conf(attribute(V2013_07_15, s, "conf", e.conf()))
                            .plainText(plainText(s, e))
                            .dataType(attribute(V2016_07_15, s, "dataType", e.dataType()))
                            .dataTypeDetails(
                                attribute(V2016_07_15, s, "dataTypeDetails", e.dataTypeDetails()))
                            .comments(attribute(V2016_07_15, s, "comments", e.comments()))))
        .toList();
  }

  /** Up to 2010 {@code PlainText} is required; a copy of {@code Unicode} is added if missing. */
  private @Nullable String plainText(String s, TextEquiv equiv) {
    if (equiv.plainText() == null && lacks(V2013_07_15)) {
      added(s, "added PlainText, a copy of Unicode, required in " + target);
      return equiv.unicode();
    }
    return equiv.plainText();
  }

  private static int index(TextEquiv equiv) {
    Integer index = equiv.index();
    return index == null ? Integer.MAX_VALUE : index;
  }

  // ---- Page and metadata

  private Page page(Page original) {
    Page page = lacks(V2017_07_15) ? withoutGraphemes(original) : original;
    String s = "Page";
    Page.Builder b = page.toBuilder();
    b.imageXResolution(attribute(V2018_07_15, s, "imageXResolution", page.imageXResolution()))
        .imageYResolution(attribute(V2018_07_15, s, "imageYResolution", page.imageYResolution()))
        .imageResolutionUnit(
            attribute(V2018_07_15, s, "imageResolutionUnit", page.imageResolutionUnit()))
        .custom(attribute(V2013_07_15, s, "custom", page.custom()))
        .comments(attribute(V2024_07_15, s, "comments", page.comments()))
        .orientation(attribute(V2019_07_15, s, "orientation", page.orientation()))
        .type(attribute(V2013_07_15, s, "type", page.type()))
        .primaryLanguage(attribute(V2016_07_15, s, "primaryLanguage", page.primaryLanguage()))
        .secondaryLanguage(attribute(V2016_07_15, s, "secondaryLanguage", page.secondaryLanguage()))
        .customLanguages(attribute(V2024_07_15, s, "customLanguages", page.customLanguages()))
        .primaryScript(script(V2016_07_15, s, "primaryScript", page.primaryScript()))
        .secondaryScript(script(V2016_07_15, s, "secondaryScript", page.secondaryScript()))
        .customScripts(attribute(V2024_07_15, s, "customScripts", page.customScripts()))
        .readingDirection(attribute(V2016_07_15, s, "readingDirection", page.readingDirection()))
        .textLineOrder(attribute(V2016_07_15, s, "textLineOrder", page.textLineOrder()))
        .conf(attribute(V2018_07_15, s, "conf", page.conf()))
        .metadata(metadata(page.metadata()))
        .alternativeImages(
            alternativeImages(
                s, elements(V2013_07_15, s, "AlternativeImage", page.alternativeImages())))
        .border(polygon(page.border(), s, "Border"))
        .printSpace(polygon(page.printSpace(), s, "PrintSpace"))
        .readingOrder(readingOrder(page.readingOrder()))
        .layers(page.layers().stream().map(this::layer).toList())
        .relations(
            lacks(V2013_07_15)
                ? elements(V2013_07_15, s, "Relation", page.relations())
                : relations(page))
        .textStyle(textStyle(s, element(V2019_07_15, s, "TextStyle", page.textStyle())))
        .userAttributes(userDefined(s, page.userAttributes()))
        .labels(labels(s, page.labels()))
        .regions(regions(page.regions(), false));
    Page result = b.build();
    if (result.regions().isEmpty() && lacks(V2010_01_12)) {
      String id = "region_1";
      for (int i = 2; result.find(id).isPresent() || id.equals(result.pcGtsId()); i++) {
        id = "region_" + i;
      }
      added(s, "added UnknownRegion '" + id + "' without coordinates, " + target + " requires one");
      result =
          result.withRegions(
              List.of(
                  UnknownRegionBuilder.builder().id(id).coords(new Polygon(List.of())).build()));
    }
    return result;
  }

  private Layer layer(Layer layer) {
    return layer.withCaption(attribute(V2013_07_15, describe(layer), "caption", layer.caption()));
  }

  private Metadata metadata(Metadata metadata) {
    String s = "Metadata";
    return metadata.with(
        b ->
            b.externalRef(attribute(V2016_07_15, s, "externalRef", metadata.externalRef()))
                .userAttributes(userDefined(s, metadata.userAttributes()))
                .metadataItems(elements(V2018_07_15, s, "MetadataItem", metadata.metadataItems())));
  }

  /**
   * Removes the graphemes (from 2017) with the relations that refer to them. They have IDs, so
   * {@link Page#removeAll} takes care of the references.
   */
  private Page withoutGraphemes(Page page) {
    List<String> ids = new ArrayList<>();
    for (Glyph glyph : page.glyphs()) {
      if (!glyph.graphemes().isEmpty()) {
        removed(describe(glyph), "element Graphemes");
        glyph.graphemes().stream().map(GraphemeElement::id).forEach(ids::add);
      }
    }
    if (ids.isEmpty()) {
      return page;
    }
    Page result = page.removeAll(ids);
    Set<Relation> kept = new HashSet<>(result.relations());
    for (Relation relation : page.relations()) {
      if (!kept.contains(relation)) {
        messages.add(
            new ConversionMessage(
                Kind.REMOVED, describe(relation) + ": removed, it refers to a removed grapheme"));
      }
    }
    if (!references(page).equals(references(result))) {
      messages.add(
          new ConversionMessage(
              Kind.REMOVED,
              "ReadingOrder and Layers: removed the references to the removed graphemes"));
    }
    return result;
  }

  private static List<Object> references(Page page) {
    List<Object> refs = new ArrayList<>(page.layers());
    ReadingOrder order = page.readingOrder();
    if (order != null) {
      refs.add(order);
    }
    return refs;
  }

  // ---- Reading order, layers and relations

  private @Nullable ReadingOrder readingOrder(@Nullable ReadingOrder order) {
    if (order == null) {
      return null;
    }
    return order.with(
        b ->
            b.conf(attribute(V2018_07_15, "ReadingOrder", "conf", order.conf()))
                .group(group(order.group())));
  }

  private Group group(Group group) {
    String s = describe(group);
    String regionRef = attribute(V2017_07_15, s, "regionRef", group.regionRef());
    String caption = attribute(V2013_07_15, s, "caption", group.caption());
    var type = attribute(V2017_07_15, s, "type", group.type());
    Boolean continuation = attribute(V2017_07_15, s, "continuation", group.continuation());
    String custom = attribute(V2017_07_15, s, "custom", group.custom());
    String comments = attribute(V2017_07_15, s, "comments", group.comments());
    List<UserAttribute> userAttributes = userDefined(s, group.userAttributes());
    List<LabelGroup> labels = labels(s, group.labels());
    List<GroupMember> members =
        group.members().stream()
            .map(
                m ->
                    switch (m) {
                      case RegionRef ref -> ref;
                      case Group child -> (GroupMember) group(child);
                    })
            .toList();
    return switch (group) {
      case OrderedGroup g ->
          g.with(
              b ->
                  b.regionRef(regionRef)
                      .caption(caption)
                      .type(type)
                      .continuation(continuation)
                      .custom(custom)
                      .comments(comments)
                      .userAttributes(userAttributes)
                      .labels(labels)
                      .members(members));
      case UnorderedGroup g ->
          g.with(
              b ->
                  b.regionRef(regionRef)
                      .caption(caption)
                      .type(type)
                      .continuation(continuation)
                      .custom(custom)
                      .comments(comments)
                      .userAttributes(userAttributes)
                      .labels(labels)
                      .members(members));
    };
  }

  private List<Relation> relations(Page page) {
    Set<String> used = new HashSet<>();
    for (Relation relation : page.relations()) {
      if (relation.id() != null) {
        used.add(relation.id());
      }
    }
    int next = 1;
    List<Relation> result = new ArrayList<>();
    for (Relation relation : page.relations()) {
      String s = describe(relation);
      Relation fitted =
          relation.with(
              b ->
                  b.id(attribute(V2018_07_15, s, "id", relation.id()))
                      .labels(labels(s, relation.labels())));
      if (fitted.id() == null && !lacks(V2018_07_15)) {
        String id;
        do {
          id = "relation_" + next++;
        } while (used.contains(id) || page.find(id).isPresent() || id.equals(page.pcGtsId()));
        used.add(id);
        added(s, "added ID '" + id + "', required in " + target);
        fitted = fitted.withId(id);
      }
      if (fitted.type() == null && lacks(V2018_07_15)) {
        added(s, "added type 'link', required in " + target);
        fitted = fitted.withType(RelationType.LINK);
      }
      result.add(fitted);
    }
    return result;
  }

  // ---- Regions

  /**
   * The regions, fitted. Up to 2010 regions nest only in frames, so the regions nested in other
   * regions are moved next to them.
   */
  private List<Region> regions(List<Region> regions, boolean nested) {
    List<Region> result = new ArrayList<>(regions.size());
    for (Region original : regions) {
      Region region = region(original, nested);
      if (lacks(V2013_07_15) && !(region instanceof FrameRegion) && !region.regions().isEmpty()) {
        int n = region.regions().size();
        changed(
            describe(region),
            "moved "
                + n
                + (n == 1 ? " nested region" : " nested regions")
                + " next to it, "
                + target
                + " nests regions only in frames");
        result.add(withCommon(region, common(region, List.of())));
        result.addAll(region.regions());
      } else {
        result.add(region);
      }
    }
    return result;
  }

  /** The common parts of the region, as they are, with other nested regions. */
  private static Common common(Region region, List<Region> regions) {
    return new Common(
        region.custom(),
        region.comments(),
        region.continuation(),
        mirrored(region),
        region.alternativeImages(),
        region.coords(),
        region.userAttributes(),
        region.labels(),
        region.tableCellRole(),
        regions);
  }

  private Region region(Region original, boolean nested) {
    Region region = attributes(kind(original, nested));
    String s = describe(region);
    Common common =
        new Common(
            attribute(V2013_07_15, s, "custom", region.custom()),
            attribute(V2013_07_15, s, "comments", region.comments()),
            attribute(V2017_07_15, s, "continuation", region.continuation()),
            attribute(V2024_07_15, s, "mirrored", mirrored(region)),
            alternativeImages(
                s, elements(V2018_07_15, s, "AlternativeImage", region.alternativeImages())),
            coords(region.coords(), s),
            userDefined(s, region.userAttributes()),
            labels(s, region.labels()),
            roles(s, element(V2017_07_15, s, "Roles", region.tableCellRole())),
            regions(region.regions(), true));
    return switch (withCommon(region, common)) {
      case TextRegion r ->
          r.with(
              b ->
                  b.textLines(r.textLines().stream().map(this::textLine).toList())
                      .textEquivs(textEquivs(s, r.textEquivs()))
                      .textStyle(textStyle(s, r.textStyle())));
      case TableRegion r -> r.withGrid(grid(s, element(V2018_07_15, s, "Grid", r.grid())));
      case Region r -> r;
    };
  }

  /**
   * The region as a type the target has: form regions (from 2024), map regions (from 2018, nested
   * in other regions from 2024) and custom regions (from 2018) are replaced.
   */
  private Region kind(Region region, boolean nested) {
    return switch (region) {
      case FormRegion r when lacks(V2018_07_15) -> unknown(r);
      case FormRegion r when lacks(V2024_07_15) -> custom(r, "form", "");
      case MapRegion r when lacks(V2018_07_15) -> unknown(r);
      case MapRegion r when nested && lacks(V2024_07_15) ->
          custom(r, "map", " inside other regions");
      case CustomRegion r when lacks(V2018_07_15) -> unknown(r);
      case AdvertRegion r when lacks(V2013_07_15) -> unknown(r);
      case ChemRegion r when lacks(V2013_07_15) -> unknown(r);
      case MusicRegion r when lacks(V2013_07_15) -> unknown(r);
      case GraphicRegion r when lacks(V2013_07_15) && r.type() == GraphicsType.FRAME -> frame(r);
      case FrameRegion r when !lacks(V2013_07_15) -> graphic(r);
      default -> region;
    };
  }

  private Region custom(Region region, String type, String where) {
    String s = describe(region);
    String name = region.getClass().getSimpleName();
    changed(
        s,
        "changed to CustomRegion of type '"
            + type
            + "', "
            + name
            + " is not supported"
            + where
            + " in "
            + target);
    lost(region, "CustomRegion");
    return CustomRegionBuilder.builder()
        .id(region.id())
        .custom(region.custom())
        .comments(region.comments())
        .continuation(region.continuation())
        .type(type)
        .alternativeImages(region.alternativeImages())
        .coords(region.coords())
        .userAttributes(region.userAttributes())
        .labels(region.labels())
        .tableCellRole(region.tableCellRole())
        .regions(region.regions())
        .build();
  }

  private Region frame(GraphicRegion region) {
    String s = describe(region);
    changed(
        s, "changed to FrameRegion, GraphicRegion of type 'frame' is not supported in " + target);
    lost(region, "FrameRegion");
    return FrameRegionBuilder.builder()
        .id(region.id())
        .custom(region.custom())
        .comments(region.comments())
        .continuation(region.continuation())
        .alternativeImages(region.alternativeImages())
        .coords(region.coords())
        .userAttributes(region.userAttributes())
        .labels(region.labels())
        .tableCellRole(region.tableCellRole())
        .regions(region.regions())
        .build();
  }

  private Region graphic(FrameRegion region) {
    String s = describe(region);
    changed(
        s, "changed to GraphicRegion of type 'frame', FrameRegion is not supported in " + target);
    lost(region, "GraphicRegion");
    return GraphicRegionBuilder.builder()
        .id(region.id())
        .custom(region.custom())
        .comments(region.comments())
        .continuation(region.continuation())
        .type(GraphicsType.FRAME)
        .alternativeImages(region.alternativeImages())
        .coords(region.coords())
        .userAttributes(region.userAttributes())
        .labels(region.labels())
        .tableCellRole(region.tableCellRole())
        .regions(region.regions())
        .build();
  }

  private Region unknown(Region region) {
    String s = describe(region);
    String name = region.getClass().getSimpleName();
    changed(s, "changed to UnknownRegion, " + name + " is not supported in " + target);
    lost(region, "UnknownRegion");
    return UnknownRegionBuilder.builder()
        .id(region.id())
        .custom(region.custom())
        .comments(region.comments())
        .continuation(region.continuation())
        .alternativeImages(region.alternativeImages())
        .coords(region.coords())
        .userAttributes(region.userAttributes())
        .labels(region.labels())
        .tableCellRole(region.tableCellRole())
        .regions(region.regions())
        .build();
  }

  /** Reports the attributes of a region that its replacement doesn't have. */
  private void lost(Region region, String replacement) {
    Map<String, @Nullable Object> attributes = new LinkedHashMap<>();
    switch (region) {
      case FormRegion r -> {
        attributes.put("orientation", r.orientation());
        attributes.put("mirrored", r.mirrored());
        attributes.put("bgColour", r.bgColour());
      }
      case MapRegion r -> {
        attributes.put("orientation", r.orientation());
        attributes.put("mirrored", r.mirrored());
      }
      case CustomRegion r -> attributes.put("type", r.type());
      case AdvertRegion r -> {
        attributes.put("orientation", r.orientation());
        attributes.put("mirrored", r.mirrored());
        attributes.put("bgColour", r.bgColour());
      }
      case ChemRegion r -> {
        attributes.put("orientation", r.orientation());
        attributes.put("mirrored", r.mirrored());
        attributes.put("bgColour", r.bgColour());
      }
      case MusicRegion r -> {
        attributes.put("orientation", r.orientation());
        attributes.put("mirrored", r.mirrored());
        attributes.put("bgColour", r.bgColour());
      }
      case GraphicRegion r -> {
        attributes.put("orientation", r.orientation());
        attributes.put("mirrored", r.mirrored());
        attributes.put("numColours", r.numColours());
        attributes.put("embText", r.embText());
      }
      case FrameRegion r -> {
        attributes.put("bgColour", r.bgColour());
        attributes.put("borderPresent", r.borderPresent());
      }
      default -> {}
    }
    attributes.forEach(
        (name, value) -> {
          if (value != null) {
            messages.add(
                new ConversionMessage(
                    Kind.REMOVED,
                    describe(region)
                        + ": removed attribute "
                        + name
                        + ", which "
                        + replacement
                        + " does not have"));
          }
        });
  }

  /** The attributes of the region's type, fitted. */
  private Region attributes(Region region) {
    String s = describe(region);
    boolean otherDepth = !lacks(V2013_07_15);
    return switch (region) {
      case TextRegion r -> textRegionAttributes(s, r);
      case ImageRegion r ->
          r.with(
              b ->
                  b.colourDepth(
                          value(
                              s,
                              "colourDepth",
                              r.colourDepth(),
                              otherDepth || r.colourDepth() != ColourDepth.OTHER))
                      .bgColour(colour(s, "bgColour", r.bgColour())));
      case LineDrawingRegion r ->
          r.with(
              b ->
                  b.penColour(colour(s, "penColour", r.penColour()))
                      .bgColour(colour(s, "bgColour", r.bgColour())));
      case GraphicRegion r ->
          r.withType(
              value(
                  s,
                  "type",
                  r.type(),
                  !lacks(V2013_07_15)
                      || (r.type() != GraphicsType.BARCODE
                          && r.type() != GraphicsType.DECORATION)));
      case TableRegion r ->
          r.with(
              b ->
                  b.lineColour(colour(s, "lineColour", r.lineColour()))
                      .bgColour(colour(s, "bgColour", r.bgColour())));
      case ChartRegion r -> r.withBgColour(colour(s, "bgColour", r.bgColour()));
      case SeparatorRegion r -> r.withColour(colour(s, "colour", r.colour()));
      case MathsRegion r -> r.withBgColour(colour(s, "bgColour", r.bgColour()));
      case FrameRegion r -> r.withBgColour(colour(s, "bgColour", r.bgColour()));
      default -> region;
    };
  }

  private TextRegion textRegionAttributes(String s, TextRegion r) {
    TextType type = r.type();
    if (type != null && lacks(V2013_07_15) && TEXT_TYPES_2013.contains(type)
        || type != null && lacks(V2010_03_19) && TEXT_TYPES_2010_03_19.contains(type)) {
      type = value(s, "type", type, false);
    } else if (type == TextType.LIST_LABEL && lacks(V2017_07_15)) {
      changed(
          s, "changed type 'list-label' to 'other', 'list-label' is not supported in " + target);
      type = TextType.OTHER;
    }
    TextType fittedType = type;
    return r.with(
        b ->
            b.type(fittedType)
                .textLineOrder(attribute(V2016_07_15, s, "textLineOrder", r.textLineOrder()))
                .align(attribute(V2013_07_15, s, "align", r.align()))
                .primaryLanguage(language(s, "primaryLanguage", r.primaryLanguage()))
                .secondaryLanguage(language(s, "secondaryLanguage", r.secondaryLanguage()))
                .customLanguages(attribute(V2024_07_15, s, "customLanguages", r.customLanguages()))
                .primaryScript(script(V2009_03_16, s, "primaryScript", r.primaryScript()))
                .secondaryScript(script(V2009_03_16, s, "secondaryScript", r.secondaryScript()))
                .customScripts(attribute(V2024_07_15, s, "customScripts", r.customScripts()))
                .production(attribute(V2013_07_15, s, "production", r.production())));
  }

  private @Nullable TableCellRole roles(String s, @Nullable TableCellRole role) {
    if (role == null) {
      return null;
    }
    return role.withHeader(attribute(V2018_07_15, s + ", TableCellRole", "header", role.header()));
  }

  private @Nullable Grid grid(String s, @Nullable Grid grid) {
    if (grid == null) {
      return null;
    }
    return grid.withAddPoints(elements(V2024_07_15, s + ", Grid", "AddPoints", grid.addPoints()));
  }

  /** What all regions have, fitted to the target. */
  private record Common(
      @Nullable String custom,
      @Nullable String comments,
      @Nullable Boolean continuation,
      @Nullable Mirrored mirrored,
      List<AlternativeImage> alternativeImages,
      Polygon coords,
      List<UserAttribute> userAttributes,
      List<LabelGroup> labels,
      @Nullable TableCellRole tableCellRole,
      List<Region> regions) {}

  private static @Nullable Mirrored mirrored(Region region) {
    return switch (region) {
      case TextRegion r -> r.mirrored();
      case ImageRegion r -> r.mirrored();
      case LineDrawingRegion r -> r.mirrored();
      case GraphicRegion r -> r.mirrored();
      case TableRegion r -> r.mirrored();
      case ChartRegion r -> r.mirrored();
      case MapRegion r -> r.mirrored();
      case MathsRegion r -> r.mirrored();
      case ChemRegion r -> r.mirrored();
      case MusicRegion r -> r.mirrored();
      case AdvertRegion r -> r.mirrored();
      case FormRegion r -> r.mirrored();
      case FrameRegion r -> null;
      case SeparatorRegion r -> null;
      case NoiseRegion r -> null;
      case UnknownRegion r -> null;
      case CustomRegion r -> null;
    };
  }

  private static Region withCommon(Region region, Common c) {
    return switch (region) {
      case TextRegion r ->
          r.with(
              b ->
                  b.custom(c.custom())
                      .comments(c.comments())
                      .continuation(c.continuation())
                      .mirrored(c.mirrored())
                      .alternativeImages(c.alternativeImages())
                      .coords(c.coords())
                      .userAttributes(c.userAttributes())
                      .labels(c.labels())
                      .tableCellRole(c.tableCellRole())
                      .regions(c.regions()));
      case ImageRegion r ->
          r.with(
              b ->
                  b.custom(c.custom())
                      .comments(c.comments())
                      .continuation(c.continuation())
                      .mirrored(c.mirrored())
                      .alternativeImages(c.alternativeImages())
                      .coords(c.coords())
                      .userAttributes(c.userAttributes())
                      .labels(c.labels())
                      .tableCellRole(c.tableCellRole())
                      .regions(c.regions()));
      case LineDrawingRegion r ->
          r.with(
              b ->
                  b.custom(c.custom())
                      .comments(c.comments())
                      .continuation(c.continuation())
                      .mirrored(c.mirrored())
                      .alternativeImages(c.alternativeImages())
                      .coords(c.coords())
                      .userAttributes(c.userAttributes())
                      .labels(c.labels())
                      .tableCellRole(c.tableCellRole())
                      .regions(c.regions()));
      case GraphicRegion r ->
          r.with(
              b ->
                  b.custom(c.custom())
                      .comments(c.comments())
                      .continuation(c.continuation())
                      .mirrored(c.mirrored())
                      .alternativeImages(c.alternativeImages())
                      .coords(c.coords())
                      .userAttributes(c.userAttributes())
                      .labels(c.labels())
                      .tableCellRole(c.tableCellRole())
                      .regions(c.regions()));
      case TableRegion r ->
          r.with(
              b ->
                  b.custom(c.custom())
                      .comments(c.comments())
                      .continuation(c.continuation())
                      .mirrored(c.mirrored())
                      .alternativeImages(c.alternativeImages())
                      .coords(c.coords())
                      .userAttributes(c.userAttributes())
                      .labels(c.labels())
                      .tableCellRole(c.tableCellRole())
                      .regions(c.regions()));
      case ChartRegion r ->
          r.with(
              b ->
                  b.custom(c.custom())
                      .comments(c.comments())
                      .continuation(c.continuation())
                      .mirrored(c.mirrored())
                      .alternativeImages(c.alternativeImages())
                      .coords(c.coords())
                      .userAttributes(c.userAttributes())
                      .labels(c.labels())
                      .tableCellRole(c.tableCellRole())
                      .regions(c.regions()));
      case MapRegion r ->
          r.with(
              b ->
                  b.custom(c.custom())
                      .comments(c.comments())
                      .continuation(c.continuation())
                      .mirrored(c.mirrored())
                      .alternativeImages(c.alternativeImages())
                      .coords(c.coords())
                      .userAttributes(c.userAttributes())
                      .labels(c.labels())
                      .tableCellRole(c.tableCellRole())
                      .regions(c.regions()));
      case MathsRegion r ->
          r.with(
              b ->
                  b.custom(c.custom())
                      .comments(c.comments())
                      .continuation(c.continuation())
                      .mirrored(c.mirrored())
                      .alternativeImages(c.alternativeImages())
                      .coords(c.coords())
                      .userAttributes(c.userAttributes())
                      .labels(c.labels())
                      .tableCellRole(c.tableCellRole())
                      .regions(c.regions()));
      case ChemRegion r ->
          r.with(
              b ->
                  b.custom(c.custom())
                      .comments(c.comments())
                      .continuation(c.continuation())
                      .mirrored(c.mirrored())
                      .alternativeImages(c.alternativeImages())
                      .coords(c.coords())
                      .userAttributes(c.userAttributes())
                      .labels(c.labels())
                      .tableCellRole(c.tableCellRole())
                      .regions(c.regions()));
      case MusicRegion r ->
          r.with(
              b ->
                  b.custom(c.custom())
                      .comments(c.comments())
                      .continuation(c.continuation())
                      .mirrored(c.mirrored())
                      .alternativeImages(c.alternativeImages())
                      .coords(c.coords())
                      .userAttributes(c.userAttributes())
                      .labels(c.labels())
                      .tableCellRole(c.tableCellRole())
                      .regions(c.regions()));
      case AdvertRegion r ->
          r.with(
              b ->
                  b.custom(c.custom())
                      .comments(c.comments())
                      .continuation(c.continuation())
                      .mirrored(c.mirrored())
                      .alternativeImages(c.alternativeImages())
                      .coords(c.coords())
                      .userAttributes(c.userAttributes())
                      .labels(c.labels())
                      .tableCellRole(c.tableCellRole())
                      .regions(c.regions()));
      case FormRegion r ->
          r.with(
              b ->
                  b.custom(c.custom())
                      .comments(c.comments())
                      .continuation(c.continuation())
                      .mirrored(c.mirrored())
                      .alternativeImages(c.alternativeImages())
                      .coords(c.coords())
                      .userAttributes(c.userAttributes())
                      .labels(c.labels())
                      .tableCellRole(c.tableCellRole())
                      .regions(c.regions()));
      case FrameRegion r ->
          r.with(
              b ->
                  b.custom(c.custom())
                      .comments(c.comments())
                      .continuation(c.continuation())
                      .alternativeImages(c.alternativeImages())
                      .coords(c.coords())
                      .userAttributes(c.userAttributes())
                      .labels(c.labels())
                      .tableCellRole(c.tableCellRole())
                      .regions(c.regions()));
      case SeparatorRegion r ->
          r.with(
              b ->
                  b.custom(c.custom())
                      .comments(c.comments())
                      .continuation(c.continuation())
                      .alternativeImages(c.alternativeImages())
                      .coords(c.coords())
                      .userAttributes(c.userAttributes())
                      .labels(c.labels())
                      .tableCellRole(c.tableCellRole())
                      .regions(c.regions()));
      case NoiseRegion r ->
          r.with(
              b ->
                  b.custom(c.custom())
                      .comments(c.comments())
                      .continuation(c.continuation())
                      .alternativeImages(c.alternativeImages())
                      .coords(c.coords())
                      .userAttributes(c.userAttributes())
                      .labels(c.labels())
                      .tableCellRole(c.tableCellRole())
                      .regions(c.regions()));
      case UnknownRegion r ->
          r.with(
              b ->
                  b.custom(c.custom())
                      .comments(c.comments())
                      .continuation(c.continuation())
                      .alternativeImages(c.alternativeImages())
                      .coords(c.coords())
                      .userAttributes(c.userAttributes())
                      .labels(c.labels())
                      .tableCellRole(c.tableCellRole())
                      .regions(c.regions()));
      case CustomRegion r ->
          r.with(
              b ->
                  b.custom(c.custom())
                      .comments(c.comments())
                      .continuation(c.continuation())
                      .alternativeImages(c.alternativeImages())
                      .coords(c.coords())
                      .userAttributes(c.userAttributes())
                      .labels(c.labels())
                      .tableCellRole(c.tableCellRole())
                      .regions(c.regions()));
    };
  }

  // ---- Text lines, words and glyphs

  private TextLine textLine(TextLine line) {
    String s = describe(line);
    return line.with(
        b ->
            b.orientation(attribute(V2024_07_15, s, "orientation", line.orientation()))
                .mirrored(attribute(V2024_07_15, s, "mirrored", line.mirrored()))
                .secondaryLanguage(
                    attribute(V2024_07_15, s, "secondaryLanguage", line.secondaryLanguage()))
                .customLanguages(
                    attribute(V2024_07_15, s, "customLanguages", line.customLanguages()))
                .primaryScript(script(V2016_07_15, s, "primaryScript", line.primaryScript()))
                .secondaryScript(script(V2016_07_15, s, "secondaryScript", line.secondaryScript()))
                .customScripts(attribute(V2024_07_15, s, "customScripts", line.customScripts()))
                .readingDirection(
                    attribute(V2016_07_15, s, "readingDirection", line.readingDirection()))
                .primaryLanguage(
                    attribute(V2013_07_15, s, "primaryLanguage", line.primaryLanguage()))
                .production(attribute(V2013_07_15, s, "production", line.production()))
                .custom(attribute(V2013_07_15, s, "custom", line.custom()))
                .comments(attribute(V2013_07_15, s, "comments", line.comments()))
                .index(attribute(V2018_07_15, s, "index", line.index()))
                .alternativeImages(
                    alternativeImages(
                        s, elements(V2018_07_15, s, "AlternativeImage", line.alternativeImages())))
                .coords(coords(line.coords(), s))
                .ascentLine(
                    polygon(
                        element(V2024_07_15, s, "AscentLine", line.ascentLine()), s, "AscentLine"))
                .meanLine(
                    polygon(element(V2024_07_15, s, "MeanLine", line.meanLine()), s, "MeanLine"))
                .baseline(
                    polygon(element(V2013_07_15, s, "Baseline", line.baseline()), s, "Baseline"))
                .descentLine(
                    polygon(
                        element(V2024_07_15, s, "DescentLine", line.descentLine()),
                        s,
                        "DescentLine"))
                .words(line.words().stream().map(this::word).toList())
                .textEquivs(textEquivs(s, line.textEquivs()))
                .textStyle(textStyle(s, element(V2013_07_15, s, "TextStyle", line.textStyle())))
                .userAttributes(userDefined(s, line.userAttributes()))
                .labels(labels(s, line.labels())));
  }

  private Word word(Word word) {
    String s = describe(word);
    return word.with(
        b ->
            b.orientation(attribute(V2024_07_15, s, "orientation", word.orientation()))
                .mirrored(attribute(V2024_07_15, s, "mirrored", word.mirrored()))
                .customLanguages(
                    attribute(V2024_07_15, s, "customLanguages", word.customLanguages()))
                .primaryScript(script(V2016_07_15, s, "primaryScript", word.primaryScript()))
                .secondaryScript(script(V2016_07_15, s, "secondaryScript", word.secondaryScript()))
                .customScripts(attribute(V2024_07_15, s, "customScripts", word.customScripts()))
                .language(attribute(V2013_07_15, s, "language", word.language()))
                .readingDirection(
                    attribute(V2016_07_15, s, "readingDirection", word.readingDirection()))
                .production(attribute(V2013_07_15, s, "production", word.production()))
                .custom(attribute(V2013_07_15, s, "custom", word.custom()))
                .comments(attribute(V2013_07_15, s, "comments", word.comments()))
                .alternativeImages(
                    alternativeImages(
                        s, elements(V2018_07_15, s, "AlternativeImage", word.alternativeImages())))
                .coords(coords(word.coords(), s))
                .glyphs(word.glyphs().stream().map(this::glyph).toList())
                .textEquivs(textEquivs(s, word.textEquivs()))
                .textStyle(textStyle(s, element(V2013_07_15, s, "TextStyle", word.textStyle())))
                .userAttributes(userDefined(s, word.userAttributes()))
                .labels(labels(s, word.labels())));
  }

  private Glyph glyph(Glyph glyph) {
    String s = describe(glyph);
    return glyph.with(
        b ->
            b.orientation(attribute(V2024_07_15, s, "orientation", glyph.orientation()))
                .mirrored(attribute(V2024_07_15, s, "mirrored", glyph.mirrored()))
                .script(script(V2016_07_15, s, "script", glyph.script()))
                .customScript(attribute(V2024_07_15, s, "customScript", glyph.customScript()))
                .production(attribute(V2013_07_15, s, "production", glyph.production()))
                .custom(attribute(V2013_07_15, s, "custom", glyph.custom()))
                .comments(attribute(V2013_07_15, s, "comments", glyph.comments()))
                .alternativeImages(
                    alternativeImages(
                        s, elements(V2018_07_15, s, "AlternativeImage", glyph.alternativeImages())))
                .coords(coords(glyph.coords(), s))
                .graphemes(glyph.graphemes().stream().map(g -> grapheme(s, g)).toList())
                .textEquivs(textEquivs(s, glyph.textEquivs()))
                .textStyle(textStyle(s, element(V2013_07_15, s, "TextStyle", glyph.textStyle())))
                .userAttributes(userDefined(s, glyph.userAttributes()))
                .labels(labels(s, glyph.labels())));
  }

  /** Graphemes exist from 2017 (older targets lose them first); only their coordinates change. */
  private GraphemeElement grapheme(String glyph, GraphemeElement grapheme) {
    String s = glyph + ", " + grapheme.getClass().getSimpleName() + " '" + grapheme.id() + "'";
    return switch (grapheme) {
      case Grapheme g -> g.withCoords(coords(g.coords(), s));
      case NonPrintingChar c -> c;
      case GraphemeGroup g ->
          g.withGraphemes(g.graphemes().stream().map(m -> grapheme(glyph, m)).toList());
    };
  }
}
