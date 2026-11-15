package com.maxnth.page4j.convert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.maxnth.page4j.convert.ConversionMessage.Kind;
import com.maxnth.page4j.geometry.Point;
import com.maxnth.page4j.geometry.Polygon;
import com.maxnth.page4j.model.CustomRegion;
import com.maxnth.page4j.model.FormRegionBuilder;
import com.maxnth.page4j.model.FrameRegion;
import com.maxnth.page4j.model.FrameRegionBuilder;
import com.maxnth.page4j.model.GlyphBuilder;
import com.maxnth.page4j.model.GraphemeBuilder;
import com.maxnth.page4j.model.GraphicRegion;
import com.maxnth.page4j.model.MapRegion;
import com.maxnth.page4j.model.MapRegionBuilder;
import com.maxnth.page4j.model.Metadata;
import com.maxnth.page4j.model.MetadataBuilder;
import com.maxnth.page4j.model.Page;
import com.maxnth.page4j.model.Region;
import com.maxnth.page4j.model.Relation;
import com.maxnth.page4j.model.RelationBuilder;
import com.maxnth.page4j.model.TextEquiv;
import com.maxnth.page4j.model.TextEquivBuilder;
import com.maxnth.page4j.model.TextLineBuilder;
import com.maxnth.page4j.model.TextRegion;
import com.maxnth.page4j.model.TextRegionBuilder;
import com.maxnth.page4j.model.TextStyleBuilder;
import com.maxnth.page4j.model.UnknownRegion;
import com.maxnth.page4j.model.WordBuilder;
import com.maxnth.page4j.model.value.Colour;
import com.maxnth.page4j.model.value.GraphicsType;
import com.maxnth.page4j.model.value.Language;
import com.maxnth.page4j.model.value.Mirrored;
import com.maxnth.page4j.model.value.PageDateTime;
import com.maxnth.page4j.model.value.RelationType;
import com.maxnth.page4j.model.value.Script;
import com.maxnth.page4j.model.value.TextType;
import com.maxnth.page4j.pagexml.PageXmlReadResult;
import com.maxnth.page4j.pagexml.PageXmlVersion;
import com.maxnth.page4j.pagexml.internal.PageXmlReader;
import com.maxnth.page4j.pagexml.internal.PageXmlWriter;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PageConverterTest {

  private static final Polygon BOX =
      Polygon.of(new Point(0, 0), new Point(10, 0), new Point(10, 10), new Point(0, 10));

  private static List<PageXmlVersion> versions() {
    return Arrays.asList(PageXmlVersion.values());
  }

  static Stream<Arguments> versionPairs() {
    return versions().stream().flatMap(s -> versions().stream().map(t -> Arguments.of(s, t)));
  }

  private static PageXmlReadResult coverage(PageXmlVersion version) throws Exception {
    String name = "/coverage/" + version.date() + ".xml";
    try (InputStream in = PageConverterTest.class.getResourceAsStream(name)) {
      return PageXmlReader.read(in.readAllBytes());
    }
  }

  private static Metadata metadata() {
    PageDateTime time = PageDateTime.parse("2024-01-02T03:04:05");
    return MetadataBuilder.builder().creator("test").created(time).lastChange(time).build();
  }

  private static Page page(Region... regions) {
    return Page.builder()
        .metadata(metadata())
        .imageFilename("a.png")
        .imageWidth(10)
        .imageHeight(20)
        .regions(List.of(regions))
        .build();
  }

  private static TextRegion region(String id) {
    return TextRegionBuilder.builder().id(id).coords(BOX).build();
  }

  private static List<String> messages(Conversion conversion) {
    return conversion.messages().stream().map(ConversionMessage::message).toList();
  }

  // ---- Every version to every version

  /**
   * The coverage documents use every element and attribute of their version, so this proves the
   * converter fits anything a version can hold to every other version: the strict writer accepts
   * the result.
   */
  @ParameterizedTest(name = "{0} -> {1}")
  @MethodSource("versionPairs")
  void fitsEveryVersionToEveryVersion(PageXmlVersion source, PageXmlVersion target)
      throws Exception {
    Page page = coverage(source).page();
    Conversion conversion = PageConverter.convert(page, target);
    PageXmlWriter.write(conversion.page(), target);

    if (source == target) {
      assertEquals(List.of(), conversion.messages());
      assertEquals(page, conversion.page());
    } else if (source.compareTo(target) < 0) {
      // Upgrades lose nothing, except what a 2009/2010 frame has beyond a graphic region.
      List<String> removed =
          conversion.messages().stream()
              .filter(m -> m.kind() == Kind.REMOVED)
              .map(ConversionMessage::message)
              .filter(
                  m ->
                      !m.matches(
                          "FrameRegion '\\w+': removed attribute (bgColour|borderPresent),.*"))
              .toList();
      assertEquals(List.of(), removed);
    } else if (source.compareTo(PageXmlVersion.V2013_07_15) >= 0) {
      // Each version from 2013 has content the older ones lack.
      assertTrue(conversion.lossy());
    }
  }

  @Test
  void upgradingFrom2013MapsScriptsAndAddsRelationIds() throws Exception {
    Conversion conversion =
        PageConverter.convert(coverage(PageXmlVersion.V2013_07_15).page(), PageXmlVersion.LATEST);
    assertEquals(
        List.of(
            "Relation 'tex12' -> 'tex12': added ID 'relation_1', required in PAGE 2024-07-15",
            "TextRegion 'tex12': changed primaryScript 'Arabic' to 'Arab - Arabic', 'Arabic' is"
                + " not supported in PAGE 2024-07-15",
            "TextRegion 'tex12': changed secondaryScript 'Arabic' to 'Arab - Arabic', 'Arabic' is"
                + " not supported in PAGE 2024-07-15"),
        messages(conversion).stream().distinct().limit(3).toList());
  }

  // ---- Rules

  @Test
  void removesWhatTheTargetLacks() {
    TextRegion region =
        region("r1")
            .withMirrored(Mirrored.HORIZONTALLY)
            .withCoords(BOX.withConfidence(0.5))
            .withTextLines(
                List.of(
                    TextLineBuilder.builder()
                        .id("l1")
                        .coords(BOX)
                        .ascentLine(BOX)
                        .orientation(1.0)
                        .build()));
    Conversion conversion = PageConverter.convert(page(region), PageXmlVersion.V2019_07_15);
    assertEquals(
        List.of(
            "TextRegion 'r1': removed attribute mirrored, not supported in PAGE 2019-07-15",
            "TextLine 'l1': removed attribute orientation, not supported in PAGE 2019-07-15",
            "TextLine 'l1': removed element AscentLine, not supported in PAGE 2019-07-15"),
        messages(conversion));
    TextRegion converted = (TextRegion) conversion.page().regions().getFirst();
    assertNull(converted.mirrored());
    assertEquals(0.5, converted.coords().confidence());
    assertNull(converted.textLines().getFirst().ascentLine());

    Conversion to2017 = PageConverter.convert(page(region), PageXmlVersion.V2017_07_15);
    assertTrue(
        messages(to2017)
            .contains(
                "TextRegion 'r1', Coords: removed attribute conf, not supported in PAGE"
                    + " 2017-07-15"));
  }

  @Test
  void replacesFormRegionsAndNestedMapRegions() {
    MapRegion nestedMap = MapRegionBuilder.builder().id("m1").coords(BOX).build();
    Page page =
        page(
            FormRegionBuilder.builder()
                .id("f1")
                .coords(BOX)
                .bgColour(Colour.WHITE)
                .regions(List.of(nestedMap))
                .build());

    Conversion to2019 = PageConverter.convert(page, PageXmlVersion.V2019_07_15);
    assertEquals(
        List.of(
            "FormRegion 'f1': changed to CustomRegion of type 'form', FormRegion is not supported"
                + " in PAGE 2019-07-15",
            "FormRegion 'f1': removed attribute bgColour, which CustomRegion does not have",
            "MapRegion 'm1': changed to CustomRegion of type 'map', MapRegion is not supported"
                + " inside other regions in PAGE 2019-07-15"),
        messages(to2019));
    CustomRegion form = (CustomRegion) to2019.page().regions().getFirst();
    assertEquals("form", form.type());
    assertEquals("map", ((CustomRegion) form.regions().getFirst()).type());

    Page topLevelMap = page(nestedMap);
    assertEquals(
        List.of(),
        PageConverter.convert(topLevelMap, PageXmlVersion.V2019_07_15).messages(),
        "MapRegion is allowed on the page from 2018");

    Conversion to2017 = PageConverter.convert(page, PageXmlVersion.V2017_07_15);
    Region unknown = to2017.page().regions().getFirst();
    assertInstanceOf(UnknownRegion.class, unknown);
    assertInstanceOf(UnknownRegion.class, unknown.regions().getFirst());
  }

  @Test
  void mapsScriptsBetween2013NamesAndIso15924() {
    TextRegion iso =
        region("r1")
            .withPrimaryScript(Script.fromXml("Cyrl - Cyrillic"))
            .withSecondaryScript(Script.fromXml("Copt - Coptic"));
    Conversion to2013 = PageConverter.convert(page(iso), PageXmlVersion.V2013_07_15);
    TextRegion old = (TextRegion) to2013.page().regions().getFirst();
    assertEquals(Script.fromXml("Cyrillic"), old.primaryScript());
    assertEquals(Script.OTHER, old.secondaryScript());

    Conversion back = PageConverter.convert(to2013.page(), PageXmlVersion.V2016_07_15);
    TextRegion upgraded = (TextRegion) back.page().regions().getFirst();
    assertEquals(Script.fromXml("Cyrl - Cyrillic"), upgraded.primaryScript());
    assertEquals(Script.OTHER, upgraded.secondaryScript());
    assertEquals(Kind.CHANGED, back.messages().getFirst().kind());
  }

  @Test
  void resetsListLabelsBefore2017() {
    Page page = page(region("r1").withType(TextType.LIST_LABEL));
    Conversion conversion = PageConverter.convert(page, PageXmlVersion.V2016_07_15);
    assertEquals(TextType.OTHER, ((TextRegion) conversion.page().regions().getFirst()).type());
    assertEquals(
        List.of(
            "TextRegion 'r1': changed type 'list-label' to 'other', 'list-label' is not supported"
                + " in PAGE 2016-07-15"),
        messages(conversion));
  }

  @Test
  void givesRelationsIdsFrom2018AndTypesBefore() {
    Relation untyped =
        RelationBuilder.builder().sourceRegionRef("r1").targetRegionRef("relation_1").build();
    Page page = page(region("r1"), region("relation_1")).withRelations(List.of(untyped));

    Conversion to2018 = PageConverter.convert(page, PageXmlVersion.V2018_07_15);
    assertEquals("relation_2", to2018.page().relations().getFirst().id());

    Conversion to2017 = PageConverter.convert(to2018.page(), PageXmlVersion.V2017_07_15);
    Relation old = to2017.page().relations().getFirst();
    assertNull(old.id());
    assertEquals(RelationType.LINK, old.type());
    assertEquals(
        List.of(
            "Relation 'relation_2': removed attribute id, not supported in PAGE 2017-07-15",
            "Relation 'relation_2': added type 'link', required in PAGE 2017-07-15"),
        messages(to2017));
  }

  @Test
  void keepsTheTextEquivWithTheLowestIndexFor2013() {
    TextEquiv second = TextEquivBuilder.builder().index(2).unicode("b").build();
    TextEquiv first = TextEquivBuilder.builder().index(1).unicode("a").build();
    Page page = page(region("r1").withTextEquivs(List.of(second, first)));
    Conversion conversion = PageConverter.convert(page, PageXmlVersion.V2013_07_15);
    assertEquals(
        List.of(TextEquivBuilder.builder().unicode("a").build()),
        ((TextRegion) conversion.page().regions().getFirst()).textEquivs());
    assertEquals(
        List.of(
            "TextRegion 'r1': removed 1 of 2 TextEquiv elements, PAGE 2013-07-15 allows one",
            "TextRegion 'r1', TextEquiv: removed attribute index, not supported in PAGE"
                + " 2013-07-15"),
        messages(conversion));
  }

  @Test
  void removesGraphemesWithTheRelationsToThem() {
    var glyph =
        GlyphBuilder.builder()
            .id("g1")
            .coords(BOX)
            .addGraphemes(GraphemeBuilder.builder().id("gr1").index(0).coords(BOX).build())
            .build();
    var line =
        TextLineBuilder.builder()
            .id("l1")
            .coords(BOX)
            .addWords(WordBuilder.builder().id("w1").coords(BOX).addGlyphs(glyph).build())
            .build();
    Relation relation =
        RelationBuilder.builder()
            .type(RelationType.LINK)
            .sourceRegionRef("gr1")
            .targetRegionRef("r1")
            .build();
    Page page = page(region("r1").withTextLines(List.of(line))).withRelations(List.of(relation));

    Conversion conversion = PageConverter.convert(page, PageXmlVersion.V2016_07_15);
    assertEquals(
        List.of(
            "Glyph 'g1': removed element Graphemes, not supported in PAGE 2016-07-15",
            "Relation 'gr1' -> 'r1': removed, it refers to a removed grapheme"),
        messages(conversion));
    assertTrue(conversion.page().find("gr1").isEmpty());
    assertEquals(List.of(), conversion.page().relations());
  }

  // ---- 2009 and 2010

  @Test
  void mapsFramesAndGraphicRegionsOfTypeFrameOntoEachOther() {
    Region frame =
        FrameRegionBuilder.builder()
            .id("f1")
            .coords(BOX)
            .bgColour(Colour.WHITE)
            .regions(List.of(region("r1")))
            .build();

    Conversion to2013 = PageConverter.convert(page(frame), PageXmlVersion.V2013_07_15);
    GraphicRegion graphic = (GraphicRegion) to2013.page().regions().getFirst();
    assertEquals(GraphicsType.FRAME, graphic.type());
    assertEquals(List.of(region("r1")), graphic.regions());
    assertEquals(
        List.of(
            "FrameRegion 'f1': changed to GraphicRegion of type 'frame', FrameRegion is not"
                + " supported in PAGE 2013-07-15",
            "FrameRegion 'f1': removed attribute bgColour, which GraphicRegion does not have"),
        messages(to2013));

    Conversion back = PageConverter.convert(to2013.page(), PageXmlVersion.V2010_03_19);
    FrameRegion again = (FrameRegion) back.page().regions().getFirst();
    assertEquals(List.of(region("r1")), again.regions());
  }

  @Test
  void movesRegionsNestedOutsideFramesNextToTheirParent() {
    TextRegion inner = region("r2");
    TextRegion outer = region("r1").withRegions(List.of(inner.withRegions(List.of(region("r3")))));
    Conversion conversion = PageConverter.convert(page(outer), PageXmlVersion.V2010_03_19);
    assertEquals(
        List.of("r1", "r2", "r3"), conversion.page().regions().stream().map(Region::id).toList());
    assertTrue(conversion.page().regions().stream().allMatch(r -> r.regions().isEmpty()));
    assertEquals(
        List.of(
            "TextRegion 'r2': moved 1 nested region next to it, PAGE 2010-03-19 nests regions only"
                + " in frames",
            "TextRegion 'r1': moved 2 nested regions next to it, PAGE 2010-03-19 nests regions only"
                + " in frames"),
        messages(conversion));
  }

  @Test
  void removesValuesThatLegacyVersionsLack() {
    TextRegion region =
        region("r1")
            .withType(TextType.FOOTNOTE)
            .withPrimaryLanguage(Language.fromXml("Yiddish"))
            .withTextStyle(
                TextStyleBuilder.builder()
                    .textColour(Colour.OTHER)
                    .bold(true)
                    .fontSize(9.0)
                    .build())
            .withTextEquivs(List.of(TextEquivBuilder.builder().conf(0.9).unicode("a").build()));

    Conversion to2010 = PageConverter.convert(page(region), PageXmlVersion.V2010_03_19);
    TextRegion converted = (TextRegion) to2010.page().regions().getFirst();
    assertEquals(TextType.FOOTNOTE, converted.type(), "footnote exists from 2010-03-19");
    assertNull(converted.primaryLanguage());
    assertEquals(TextStyleBuilder.builder().fontSize(9.0).build(), converted.textStyle());
    assertEquals(
        List.of(TextEquivBuilder.builder().plainText("a").unicode("a").build()),
        converted.textEquivs());
    assertEquals(
        List.of(
            "TextRegion 'r1': removed primaryLanguage 'Yiddish', not supported in PAGE 2010-03-19",
            "TextRegion 'r1', TextEquiv: removed attribute conf, not supported in PAGE 2010-03-19",
            "TextRegion 'r1', TextEquiv: added PlainText, a copy of Unicode, required in PAGE"
                + " 2010-03-19",
            "TextRegion 'r1', TextStyle: removed textColour 'other', not supported in PAGE"
                + " 2010-03-19",
            "TextRegion 'r1', TextStyle: removed attribute bold, not supported in PAGE 2010-03-19"),
        messages(to2010));

    Conversion to2009 = PageConverter.convert(page(region), PageXmlVersion.V2009_03_16);
    assertNull(((TextRegion) to2009.page().regions().getFirst()).type());
  }

  @Test
  void givesA2009PageWithoutRegionsAnEmptyUnknownRegion() throws Exception {
    Conversion conversion = PageConverter.convert(page(), PageXmlVersion.V2009_03_16);
    Region added = conversion.page().regions().getFirst();
    assertInstanceOf(UnknownRegion.class, added);
    assertEquals(
        List.of(
            "Page: added UnknownRegion 'region_1' without coordinates, PAGE 2009-03-16 requires"
                + " one"),
        messages(conversion));
    PageXmlWriter.write(conversion.page(), PageXmlVersion.V2009_03_16);

    assertEquals(List.of(), PageConverter.convert(page(), PageXmlVersion.V2010_01_12).messages());
  }

  @Test
  void repeatsALonePointFrom2013() {
    Region region = region("r1").withCoords(Polygon.of(new Point(3, 4)));
    Conversion conversion = PageConverter.convert(page(region), PageXmlVersion.V2013_07_15);
    assertEquals(
        List.of(new Point(3, 4), new Point(3, 4)),
        conversion.page().regions().getFirst().coords().points());
    assertEquals(
        List.of(
            "TextRegion 'r1', Coords: repeated the point, '3,4 3,4', PAGE 2013-07-15 requires at"
                + " least two"),
        messages(conversion));
  }
}
