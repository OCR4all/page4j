package com.maxnth.page4j.pagexml.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.maxnth.page4j.geometry.Point;
import com.maxnth.page4j.model.FrameRegion;
import com.maxnth.page4j.model.Glyph;
import com.maxnth.page4j.model.Grapheme;
import com.maxnth.page4j.model.GraphemeGroup;
import com.maxnth.page4j.model.Group;
import com.maxnth.page4j.model.NonPrintingChar;
import com.maxnth.page4j.model.OrderedGroup;
import com.maxnth.page4j.model.Page;
import com.maxnth.page4j.model.Region;
import com.maxnth.page4j.model.RegionRef;
import com.maxnth.page4j.model.Relation;
import com.maxnth.page4j.model.TextLine;
import com.maxnth.page4j.model.TextRegion;
import com.maxnth.page4j.model.TextStyle;
import com.maxnth.page4j.model.UnorderedGroup;
import com.maxnth.page4j.model.value.Colour;
import com.maxnth.page4j.model.value.TextType;
import com.maxnth.page4j.pagexml.PageReadException;
import com.maxnth.page4j.pagexml.PageXmlReadResult;
import com.maxnth.page4j.pagexml.PageXmlVersion;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class PageXmlReaderTest {

  private static final List<String> COVERAGE_VERSIONS =
      List.of(
          "2009-03-16",
          "2010-01-12",
          "2010-03-19",
          "2013-07-15",
          "2016-07-15",
          "2017-07-15",
          "2018-07-15",
          "2019-07-15",
          "2024-07-15");

  static List<String> coverageVersions() {
    return COVERAGE_VERSIONS;
  }

  private static byte[] resource(String name) {
    try (InputStream in = PageXmlReaderTest.class.getResourceAsStream("/" + name)) {
      if (in == null) {
        throw new IllegalArgumentException("Missing test resource " + name);
      }
      return in.readAllBytes();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static Page read(String name) throws PageReadException {
    return PageXmlReader.read(resource(name)).page();
  }

  private static byte[] page2019(String body) {
    return ("""
        <?xml version="1.0" encoding="UTF-8"?>
        %s
        <PcGts xmlns="http://schema.primaresearch.org/PAGE/gts/pagecontent/2019-07-15">
          <Metadata><Creator/><Created>2020-01-01T00:00:00</Created>\
        <LastChange>2020-01-01T00:00:00</LastChange></Metadata>
          <Page imageFilename="a.png" imageWidth="10" imageHeight="10">%s</Page>
        </PcGts>
        """)
        .formatted("", body)
        .getBytes(StandardCharsets.UTF_8);
  }

  // ---- Coverage: every element and attribute of every version

  @ParameterizedTest
  @MethodSource("coverageVersions")
  void readsEveryElementAndAttributeOfTheSchema(String version) throws Exception {
    // The reader fails on any element it doesn't know and any attribute it doesn't read.
    PageXmlReadResult result = PageXmlReader.read(resource("coverage/" + version + ".xml"));

    assertEquals(version, result.version().date());
    Page page = result.page();
    Set<String> regionTypes =
        page.allRegions().stream()
            .map(r -> r.getClass().getSimpleName())
            .collect(Collectors.toSet());
    assertTrue(regionTypes.containsAll(Set.of("TextRegion", "TableRegion", "NoiseRegion")));
    assertFalse(page.textLines().isEmpty());
    assertFalse(page.glyphs().isEmpty());
    if (result.version().compareTo(PageXmlVersion.V2013_07_15) >= 0) {
      assertEquals(1, page.relations().size());
      Relation relation = page.relations().getFirst();
      assertEquals(relation.sourceRegionRef(), relation.targetRegionRef());
    } else {
      assertTrue(regionTypes.contains("FrameRegion"));
    }
  }

  @Test
  void coverageDocumentOf2024HasAllRegionTypesAndGraphemes() throws Exception {
    Page page = read("coverage/2024-07-15.xml");
    Set<Class<?>> types =
        page.allRegions().stream().map(Object::getClass).collect(Collectors.toSet());
    assertEquals(16, types.size());
    Glyph glyph = page.glyphs().getFirst();
    assertInstanceOf(Grapheme.class, glyph.graphemes().get(0));
    assertInstanceOf(NonPrintingChar.class, glyph.graphemes().get(1));
    GraphemeGroup group = assertInstanceOf(GraphemeGroup.class, glyph.graphemes().get(2));
    assertEquals(2, group.graphemes().size());
    assertEquals(
        Set.of(OrderedGroup.class, UnorderedGroup.class),
        page.all(Group.class).stream().map(Object::getClass).collect(Collectors.toSet()));
  }

  // ---- Real documents

  static Stream<String> documents() throws Exception {
    Path root = Path.of(PageXmlReaderTest.class.getResource("/fidelity").toURI()).getParent();
    try (Stream<Path> files = Files.walk(root)) {
      return files
          .filter(p -> p.toString().endsWith(".xml"))
          .map(p -> root.relativize(p).toString().replace('\\', '/'))
          .filter(p -> !p.startsWith("fidelity/lenient/") && !p.startsWith("coverage/"))
          .sorted()
          .toList()
          .stream();
    }
  }

  @ParameterizedTest
  @MethodSource("documents")
  void readsTheTestDocuments(String name) throws Exception {
    Page page = read(name);
    assertTrue(page.imageWidth() > 0);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "fidelity/lenient/custom-region-type.xml",
        "fidelity/lenient/duplicate-id.xml",
        "fidelity/lenient/unknown-content.xml"
      })
  void rejectsInvalidDocumentsWithTheirPosition(String name) {
    PageReadException e = assertThrows(PageReadException.class, () -> read(name));
    assertTrue(e.line() > 0, e.getMessage());
    assertTrue(e.errors().getFirst().startsWith(e.line() + ":"), e.errors().getFirst());
  }

  // ---- Semantics

  @Test
  void keepsTimestampsAsWritten() throws Exception {
    Page page = read("fidelity/timestamps.xml");
    assertEquals("2020-01-02T03:04:05+02:00", page.metadata().created().xmlValue());
    assertEquals("2020-01-02T03:04:05", page.metadata().lastChange().xmlValue());
  }

  @Test
  void keepsEmptyAndAbsentApart() throws Exception {
    Page page = read("fidelity/empty-plain-text.xml");
    var equiv = ((TextRegion) page.regions().getFirst()).textEquivs().getFirst();
    assertEquals("", equiv.plainText());
    assertNull(read("fidelity/metadata-without-comments.xml").metadata().comments());
  }

  @Test
  void ordersIndexedMembersByIndex() throws Exception {
    Group root = read("fidelity/reading-order-index.xml").readingOrder().group();
    List<?> members = root.members();
    assertEquals(new RegionRef("r1"), members.get(0));
    assertEquals(new RegionRef("r2"), members.get(1));
    assertEquals("ro2", ((Group) members.get(2)).id());
  }

  @Test
  void keepsGraphemesInTheirGroup() throws Exception {
    Glyph glyph = read("fidelity/grapheme-group.xml").glyphs().getFirst();
    GraphemeGroup group = (GraphemeGroup) glyph.graphemes().get(1);
    assertEquals("gr3", group.graphemes().getFirst().id());
    assertEquals("ü", group.textEquivs().getFirst().unicode());
  }

  @Test
  void keepsRepeatedRelationsAndLabels() throws Exception {
    assertEquals(2, read("fidelity/relations-same-pair.xml").relations().size());
    Page labels = read("fidelity/labels-repeated.xml");
    var groups =
        labels.allRegions().stream().mapToInt(r -> r.labels().size()).max().orElse(0)
            + labels.labels().size();
    assertTrue(groups >= 2);
  }

  @Test
  void readsRelationsOfOldVersions() throws Exception {
    Relation relation = read("coverage/2013-07-15.xml").relations().getFirst();
    assertNull(relation.id());
    assertEquals("link", relation.type().xmlValue());
  }

  @Test
  void readsTypedValues() throws Exception {
    Page page =
        PageXmlReader.read(
                page2019(
                    """
        <TextRegion id="r1" type="heading" orientation="-1.5E1" indented="1">
          <Coords points="1,2 3,4 5,6"/>
          <TextLine id="l1"><Coords points="0,0 1,1"/><Baseline points="0,1 1,1"/></TextLine>
        </TextRegion>
        """))
            .page();
    TextRegion region = (TextRegion) page.regions().getFirst();
    assertEquals(TextType.HEADING, region.type());
    assertEquals(-15.0, region.orientation());
    assertEquals(Boolean.TRUE, region.indented());
    assertEquals(3, region.coords().size());
    TextLine line = region.textLines().getFirst();
    assertEquals(2, line.baseline().size());
    assertNull(line.index());
  }

  // ---- Failures

  @Test
  void rejectsDocumentsThatAreNotPage() {
    var notXml = "not xml".getBytes(StandardCharsets.UTF_8);
    assertTrue(
        assertThrows(PageReadException.class, () -> PageXmlReader.read(notXml))
            .getMessage()
            .contains("Not well-formed"));
    var other = "<root xmlns=\"urn:x\"/>".getBytes(StandardCharsets.UTF_8);
    assertTrue(
        assertThrows(PageReadException.class, () -> PageXmlReader.read(other))
            .getMessage()
            .contains("Not a PAGE document"));
    var unknown =
        "<PcGts xmlns=\"http://schema.primaresearch.org/PAGE/gts/pagecontent/2099-01-01\"/>"
            .getBytes(StandardCharsets.UTF_8);
    assertTrue(
        assertThrows(PageReadException.class, () -> PageXmlReader.read(unknown))
            .getMessage()
            .contains("Unknown PAGE namespace"));
  }

  @Test
  void readsTheShapeOf2009And2010() throws Exception {
    Page page = read("coverage/2009-03-16.xml");
    TextRegion text = page.all(TextRegion.class).getFirst();
    assertEquals(List.of(new Point(1, 1)), text.coords().points());
    // The style attributes of a text region become its text style.
    TextStyle style = text.textStyle();
    assertEquals(Colour.BLACK, style.textColour());
    assertEquals(Colour.BLACK, style.bgColour());
    assertEquals(true, style.reverseVideo());
    assertEquals(0.5, style.fontSize());
    assertEquals(1, style.kerning());
    assertNull(style.bold());

    FrameRegion frame = page.all(FrameRegion.class).getFirst();
    assertEquals(true, frame.borderPresent());
    assertFalse(frame.regions().isEmpty());
  }

  @Test
  void wrapsALegacyReadingOrderWithSeveralMembersInARootGroup() throws Exception {
    // 2009-03-16 and 2010-01-12 allow several groups and references directly in the reading order.
    Group root = read("coverage/2010-01-12.xml").readingOrder().group();
    UnorderedGroup wrapper = assertInstanceOf(UnorderedGroup.class, root);
    assertEquals("readingOrder", wrapper.id());
    assertEquals(
        List.of("RegionRef", "OrderedGroup", "UnorderedGroup"),
        wrapper.members().stream().map(m -> m.getClass().getSimpleName()).toList());

    Group single = read("coverage/2010-03-19.xml").readingOrder().group();
    assertInstanceOf(OrderedGroup.class, single);
  }

  @Test
  void reportsEverySchemaViolation() {
    byte[] xml =
        page2019(
            """
            <TextRegion id="r1" type="nope"><Coords points="1,2 3,4"/></TextRegion>
            <TextRegion id="r1"><Coords points="x"/></TextRegion>
            """);
    var e = assertThrows(PageReadException.class, () -> PageXmlReader.read(xml));
    assertTrue(e.errors().size() >= 3, e.errors().toString());
    assertTrue(e.getMessage().contains("more"), e.getMessage());
  }

  // ---- Untrusted XML

  @Test
  void doesNotExpandExternalEntities(@TempDir Path directory) throws Exception {
    Path secret = Files.writeString(directory.resolve("secret.txt"), "SECRET-CANARY");
    String xml =
        new String(
                page2019(
                    "<TextRegion id=\"r1\"><Coords points=\"1,2 3,4\"/>"
                        + "<TextEquiv><Unicode>&xxe;</Unicode></TextEquiv></TextRegion>"),
                StandardCharsets.UTF_8)
            .replace(
                "\n<PcGts",
                "<!DOCTYPE PcGts [<!ENTITY xxe SYSTEM \"" + secret.toUri() + "\">]>\n<PcGts");
    var e =
        assertThrows(
            PageReadException.class,
            () -> PageXmlReader.read(xml.getBytes(StandardCharsets.UTF_8)));
    assertFalse(e.getMessage().contains("SECRET-CANARY"));
  }

  @Test
  void stopsExponentialEntityExpansion() {
    StringBuilder entities = new StringBuilder("<!ENTITY lol0 \"lol\">");
    for (int level = 1; level <= 10; level++) {
      entities.append(
          "<!ENTITY lol%d \"%s\">".formatted(level, ("&lol" + (level - 1) + ";").repeat(10)));
    }
    String xml =
        new String(
                page2019(
                    "<TextRegion id=\"r1\" comments=\"&lol10;\"><Coords points=\"1,2 3,4\"/>"
                        + "</TextRegion>"),
                StandardCharsets.UTF_8)
            .replace("\n<PcGts", "<!DOCTYPE PcGts [" + entities + "]>\n<PcGts");
    assertTimeoutPreemptively(
        Duration.ofSeconds(10),
        () ->
            assertThrows(
                PageReadException.class,
                () -> PageXmlReader.read(xml.getBytes(StandardCharsets.UTF_8))));
  }

  @Test
  void acceptsAPlainDoctype() throws Exception {
    String xml =
        new String(page2019(""), StandardCharsets.UTF_8)
            .replace("\n<PcGts", "<!DOCTYPE PcGts>\n<PcGts");
    Page page = PageXmlReader.read(xml.getBytes(StandardCharsets.UTF_8)).page();
    assertEquals(List.of(), page.regions());
  }

  @Test
  void doesNotLoadExternalDtds(@TempDir Path directory) throws Exception {
    // If the reader tried to load the DTD, the missing file would make the read fail.
    String dtd = directory.resolve("missing.dtd").toUri().toString();
    String xml =
        new String(page2019(""), StandardCharsets.UTF_8)
            .replace("\n<PcGts", "<!DOCTYPE PcGts SYSTEM \"" + dtd + "\">\n<PcGts");
    Page page = PageXmlReader.read(xml.getBytes(StandardCharsets.UTF_8)).page();
    assertEquals(List.of(), page.regions());
  }

  @Test
  void detectsTheVersion() throws Exception {
    for (String version : COVERAGE_VERSIONS) {
      assertEquals(
          version, PageXmlReader.detectVersion(resource("coverage/" + version + ".xml")).date());
    }
    assertEquals(PageXmlVersion.V2024_07_15, PageXmlVersion.LATEST);
  }

  @Test
  void regionsKeepTheirNestedRegions() throws Exception {
    Region first = read("coverage/2024-07-15.xml").regions().getFirst();
    assertEquals(16, first.regions().size());
  }
}
