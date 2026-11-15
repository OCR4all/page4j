package com.maxnth.page4j.pagexml.internal;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.maxnth.page4j.geometry.Point;
import com.maxnth.page4j.geometry.Polygon;
import com.maxnth.page4j.model.FormRegionBuilder;
import com.maxnth.page4j.model.Metadata;
import com.maxnth.page4j.model.MetadataBuilder;
import com.maxnth.page4j.model.Page;
import com.maxnth.page4j.model.Relation;
import com.maxnth.page4j.model.RelationBuilder;
import com.maxnth.page4j.model.TextEquivBuilder;
import com.maxnth.page4j.model.TextRegion;
import com.maxnth.page4j.model.TextRegionBuilder;
import com.maxnth.page4j.model.TextStyleBuilder;
import com.maxnth.page4j.model.value.Colour;
import com.maxnth.page4j.model.value.Mirrored;
import com.maxnth.page4j.model.value.PageDateTime;
import com.maxnth.page4j.model.value.TextType;
import com.maxnth.page4j.pagexml.PageWriteException;
import com.maxnth.page4j.pagexml.PageXmlVersion;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PageXmlWriterTest {

  private static final Polygon BOX =
      Polygon.of(new Point(0, 0), new Point(10, 0), new Point(10, 10), new Point(0, 10));

  private static Metadata metadata() {
    PageDateTime time = PageDateTime.parse("2024-01-02T03:04:05");
    return MetadataBuilder.builder().creator("test").created(time).lastChange(time).build();
  }

  private static Page page(TextRegion... regions) {
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

  private static String write(Page page, PageXmlVersion version) throws PageWriteException {
    return new String(PageXmlWriter.write(page, version), StandardCharsets.UTF_8);
  }

  private static List<String> problems(Page page, PageXmlVersion version) {
    return assertThrows(PageWriteException.class, () -> PageXmlWriter.write(page, version))
        .problems();
  }

  @Test
  void writesAttributesInSchemaOrderWithIndentation() throws Exception {
    TextRegion region =
        region("r1").withType(TextType.HEADING).withOrientation(0.0).withCustom("c");
    String xml = write(page(region), PageXmlVersion.V2019_07_15);
    assertEquals(
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <PcGts xmlns="http://schema.primaresearch.org/PAGE/gts/pagecontent/2019-07-15" \
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" \
        xsi:schemaLocation="http://schema.primaresearch.org/PAGE/gts/pagecontent/2019-07-15 \
        http://schema.primaresearch.org/PAGE/gts/pagecontent/2019-07-15/pagecontent.xsd">
          <Metadata>
            <Creator>test</Creator>
            <Created>2024-01-02T03:04:05</Created>
            <LastChange>2024-01-02T03:04:05</LastChange>
          </Metadata>
          <Page imageFilename="a.png" imageWidth="10" imageHeight="20">
            <TextRegion id="r1" custom="c" orientation="0.0" type="heading">
              <Coords points="0,0 10,0 10,10 0,10"/>
            </TextRegion>
          </Page>
        </PcGts>
        """,
        xml);
  }

  @Test
  void writesEmptyTextAsAnElementAndAbsentTextNotAtAll() throws Exception {
    TextRegion region =
        region("r1")
            .withTextEquivs(
                List.of(
                    TextEquivBuilder.builder().plainText("").unicode("").build(),
                    TextEquivBuilder.builder().unicode(" a  b ").build()));
    String xml = write(page(region), PageXmlVersion.V2019_07_15);
    assertTrue(xml.contains("<PlainText></PlainText>"), xml);
    assertTrue(xml.contains("<Unicode> a  b </Unicode>"), xml);
    assertEquals(1, xml.split("<PlainText>", -1).length - 1);
  }

  @ParameterizedTest
  @CsvSource({
    "0.0, 0.0",
    "0.5, 0.5",
    "300.0, 300.0",
    "-1.25, -1.25",
    "1.0E7, 10000000.0",
    "1.0E-4, 0.0001",
    "1.5E-7, 0.00000015"
  })
  void formatsDoublesWithoutExponent(double value, String expected) {
    assertEquals(expected, PageXmlWriter.formatDouble(value));
  }

  @Test
  void formatsSpecialDoublesAsXsd() {
    assertEquals("INF", PageXmlWriter.formatDouble(Double.POSITIVE_INFINITY));
    assertEquals("-INF", PageXmlWriter.formatDouble(Double.NEGATIVE_INFINITY));
    assertEquals("NaN", PageXmlWriter.formatDouble(Double.NaN));
  }

  // ---- Strictness

  @Test
  void rejectsContentTheVersionDoesNotDefine() {
    TextRegion region =
        region("r1")
            .withMirrored(Mirrored.HORIZONTALLY)
            .withRegions(List.of(FormRegionBuilder.builder().id("f1").coords(BOX).build()));
    assertEquals(
        List.of(
            "TextRegion 'r1': attribute mirrored is not supported in PAGE 2019-07-15",
            "FormRegion 'f1' is not supported in PAGE 2019-07-15"),
        problems(page(region), PageXmlVersion.V2019_07_15));
  }

  @Test
  void rejectsValuesTheVersionDoesNotDefine() {
    Page page = page(region("r1").withType(TextType.LIST_LABEL));
    assertEquals(
        List.of("TextRegion 'r1': type 'list-label' is not supported in PAGE 2013-07-15"),
        problems(page, PageXmlVersion.V2013_07_15));
  }

  @Test
  void rejectsMoreElementsThanTheVersionAllows() {
    TextRegion region =
        region("r1")
            .withTextEquivs(
                List.of(
                    TextEquivBuilder.builder().unicode("a").build(),
                    TextEquivBuilder.builder().unicode("b").build(),
                    TextEquivBuilder.builder().unicode("c").build()));
    assertEquals(
        List.of("TextRegion 'r1': PAGE 2013-07-15 allows at most 1 TextEquiv"),
        problems(page(region), PageXmlVersion.V2013_07_15));
  }

  @Test
  void keepsLineBreaksTabsAndCarriageReturns() throws Exception {
    // A reader normalizes raw line breaks and tabs in attributes to spaces, and carriage returns
    // in text to line feeds, so they must be written as character references.
    TextRegion region =
        region("r1")
            .withComments("a\nb\tc\rd")
            .withCustom("x\r\ny")
            .withTextEquivs(List.of(TextEquivBuilder.builder().unicode("l1\r\nl2\rl3\n").build()));
    Page page = page(region);
    String xml = write(page, PageXmlVersion.V2019_07_15);
    assertTrue(xml.contains("custom=\"x&#13;&#10;y\" comments=\"a&#10;b&#9;c&#13;d\""), xml);
    assertTrue(xml.contains("<Unicode>l1&#13;\nl2&#13;l3\n</Unicode>"), xml);
    assertEquals(page, PageXmlReader.read(xml.getBytes(StandardCharsets.UTF_8)).page());
  }

  @Test
  void rejectsCharactersXmlCannotHold() {
    TextRegion region =
        region("r1")
            .withComments("a\uD800b")
            .withTextEquivs(List.of(TextEquivBuilder.builder().unicode("form\ffeed").build()));
    assertEquals(
        List.of(
            "TextRegion 'r1': attribute comments contains the character U+D800, which XML can't"
                + " hold",
            "TextRegion 'r1': Unicode contains the character U+000C, which XML can't hold"),
        problems(page(region), PageXmlVersion.V2019_07_15));
  }

  @Test
  void writesRelationsInTheShapeOfTheVersion() throws Exception {
    Relation untyped =
        RelationBuilder.builder().id("rel").sourceRegionRef("r1").targetRegionRef("r2").build();
    Page page = page(region("r1"), region("r2")).withRelations(List.of(untyped));

    String xml2018 = write(page, PageXmlVersion.V2018_07_15);
    assertTrue(xml2018.contains("<SourceRegionRef regionRef=\"r1\"/>"), xml2018);

    // Up to 2017 the type is required and relations have no ID.
    assertEquals(
        List.of(
            "Relation 'rel': attribute id is not supported in PAGE 2017-07-15",
            "Relation 'rel': attribute type is required in PAGE 2017-07-15"),
        problems(page, PageXmlVersion.V2017_07_15));
    Relation old = untyped.withId(null).withType(com.maxnth.page4j.model.value.RelationType.LINK);
    String xml2017 = write(page.withRelations(List.of(old)), PageXmlVersion.V2017_07_15);
    assertTrue(
        xml2017.contains("<RegionRef regionRef=\"r1\"/>\n        <RegionRef regionRef=\"r2\"/>"),
        xml2017);
  }

  @Test
  void upgradingNeedsTheConverterWhereVersionsDiffer() throws Exception {
    // 2013 relations have no ID and 2013 script names are not 2016+ ISO 15924 values.
    Page page = PageXmlReader.read(resource("coverage/2013-07-15.xml")).page();
    List<String> problems = problems(page, PageXmlVersion.V2024_07_15);
    assertEquals("Relation: attribute id is required in PAGE 2024-07-15", problems.getFirst());
    assertTrue(
        problems.contains(
            "TextRegion 'tex12': primaryScript 'Arabic' is not supported in PAGE 2024-07-15"),
        problems.toString());
  }

  @Test
  void validatesWhatItWrote() {
    TextRegion region = region("r1").withCoords(Polygon.of(new Point(1, 1)));
    List<String> problems = problems(page(region), PageXmlVersion.V2019_07_15);
    assertEquals(
        "The written document is not valid against the schema of PAGE 2019-07-15",
        problems.getFirst());
    assertTrue(problems.size() > 1);
  }

  @Test
  void writesNothingWhenItFails() {
    var target = new ByteArrayOutputStream();
    Page page = page(region("r1").withMirrored(Mirrored.HORIZONTALLY));
    assertThrows(
        PageWriteException.class,
        () -> PageXmlWriter.write(page, PageXmlVersion.V2019_07_15, target));
    assertArrayEquals(new byte[0], target.toByteArray());
  }

  @Test
  void writesTheShapeOf2010() throws Exception {
    TextRegion region =
        region("r1")
            .withTextStyle(
                TextStyleBuilder.builder()
                    .textColour(Colour.RED)
                    .fontSize(12.0)
                    .kerning(1)
                    .build());
    String xml = write(page(region), PageXmlVersion.V2010_03_19);
    assertTrue(
        xml.contains(
            "<TextRegion id=\"r1\" textColour=\"red\" fontSize=\"12.0\" kerning=\"1\">\n"
                + "      <Coords>\n"
                + "        <Point x=\"0\" y=\"0\"/>"),
        xml);
    assertFalse(xml.contains("TextStyle"), xml);

    TextRegion bold = region.withTextStyle(region.textStyle().withBold(true));
    assertEquals(
        List.of("TextRegion 'r1': TextStyle attribute bold is not supported in PAGE 2010-03-19"),
        problems(page(bold), PageXmlVersion.V2010_03_19));
  }

  private static byte[] resource(String name) throws Exception {
    try (InputStream in = PageXmlWriterTest.class.getResourceAsStream("/" + name)) {
      return in.readAllBytes();
    }
  }
}
