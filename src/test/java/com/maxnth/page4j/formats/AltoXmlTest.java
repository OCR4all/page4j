package com.maxnth.page4j.formats;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.maxnth.page4j.geometry.Point;
import com.maxnth.page4j.geometry.Polygon;
import com.maxnth.page4j.geometry.Rect;
import com.maxnth.page4j.model.AlternativeImage;
import com.maxnth.page4j.model.GlyphBuilder;
import com.maxnth.page4j.model.GraphicRegion;
import com.maxnth.page4j.model.GraphicRegionBuilder;
import com.maxnth.page4j.model.ImageRegion;
import com.maxnth.page4j.model.ImageRegionBuilder;
import com.maxnth.page4j.model.Label;
import com.maxnth.page4j.model.LabelGroup;
import com.maxnth.page4j.model.LabelGroupBuilder;
import com.maxnth.page4j.model.MetadataBuilder;
import com.maxnth.page4j.model.MetadataItem;
import com.maxnth.page4j.model.MetadataItemBuilder;
import com.maxnth.page4j.model.OrderedGroup;
import com.maxnth.page4j.model.OrderedGroupBuilder;
import com.maxnth.page4j.model.Page;
import com.maxnth.page4j.model.ReadingOrderBuilder;
import com.maxnth.page4j.model.RegionRef;
import com.maxnth.page4j.model.SeparatorRegion;
import com.maxnth.page4j.model.SeparatorRegionBuilder;
import com.maxnth.page4j.model.TableRegion;
import com.maxnth.page4j.model.TableRegionBuilder;
import com.maxnth.page4j.model.TextEquiv;
import com.maxnth.page4j.model.TextEquivBuilder;
import com.maxnth.page4j.model.TextLine;
import com.maxnth.page4j.model.TextLineBuilder;
import com.maxnth.page4j.model.TextRegion;
import com.maxnth.page4j.model.TextRegionBuilder;
import com.maxnth.page4j.model.TextStyle;
import com.maxnth.page4j.model.TextStyleBuilder;
import com.maxnth.page4j.model.UnorderedGroup;
import com.maxnth.page4j.model.Word;
import com.maxnth.page4j.model.WordBuilder;
import com.maxnth.page4j.model.value.Align;
import com.maxnth.page4j.model.value.Language;
import com.maxnth.page4j.model.value.MetadataItemType;
import com.maxnth.page4j.model.value.PageDateTime;
import com.maxnth.page4j.model.value.PageType;
import com.maxnth.page4j.model.value.TextType;
import com.maxnth.page4j.pagexml.PageReadException;
import com.maxnth.page4j.pagexml.PageXml;
import com.maxnth.page4j.pagexml.PageXmlVersion;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Element;

class AltoXmlTest {

  private static final AltoXml ALTO = AltoXml.create();
  private static final PageDateTime TIME = PageDateTime.parse("2020-01-02T03:04:05");

  @TempDir Path directory;

  private static Polygon box(int left, int top, int right, int bottom) {
    return new Rect(left, top, right, bottom).toPolygon();
  }

  private static TextEquiv text(String unicode) {
    return TextEquivBuilder.builder().unicode(unicode).build();
  }

  private static Page.Builder page() {
    return Page.builder()
        .metadata(MetadataBuilder.builder().creator("test").created(TIME).lastChange(TIME).build())
        .imageFilename("page.png")
        .imageWidth(1000)
        .imageHeight(800);
  }

  private static AltoReadResult read(String xml) throws PageReadException {
    return ALTO.read(xml.getBytes(StandardCharsets.UTF_8));
  }

  /** A page with everything ALTO can hold. */
  private static Page richPage() {
    TextStyle style =
        TextStyleBuilder.builder()
            .fontFamily("Arial")
            .serif(false)
            .fontSize(10.0)
            .textColourRgb(255)
            .bold(true)
            .build();
    LabelGroup labels =
        LabelGroupBuilder.builder()
            .externalModel("http://example.com/model")
            .addLabels(new Label("person", "entity", "a name"))
            .build();
    Word hello =
        WordBuilder.builder()
            .id("w1")
            .coords(box(110, 110, 300, 150))
            .language(Language.ENGLISH)
            .labels(List.of(labels))
            .addTextEquivs(TextEquivBuilder.builder().conf(0.9).unicode("Hello").build())
            .addGlyphs(
                GlyphBuilder.builder()
                    .id("g1")
                    .coords(box(110, 110, 150, 150))
                    .addTextEquivs(
                        TextEquivBuilder.builder().index(1).conf(0.8).unicode("H").build())
                    .addTextEquivs(
                        TextEquivBuilder.builder().index(2).conf(0.1).unicode("K").build())
                    .build())
            .build();
    Word world =
        WordBuilder.builder()
            .id("w2")
            .coords(box(320, 110, 500, 150))
            .textStyle(style)
            .addTextEquivs(TextEquivBuilder.builder().index(1).unicode("world").build())
            .addTextEquivs(TextEquivBuilder.builder().index(2).unicode("word").build())
            .build();
    TextLine line =
        TextLineBuilder.builder()
            .id("l1")
            .coords(box(100, 100, 600, 160))
            .primaryLanguage(Language.ENGLISH)
            .words(List.of(hello, world))
            .addTextEquivs(text("Hello world"))
            .build();
    MetadataItem processing =
        MetadataItemBuilder.builder()
            .type(MetadataItemType.PROCESSING_STEP)
            .name("engine")
            .value("recognised the text")
            .date(TIME)
            .addLabels(
                LabelGroupBuilder.builder()
                    .addLabels(new Label("contentGeneration", "processingCategory", null))
                    .addLabels(new Label("1.0", "softwareVersion", null))
                    .build())
            .build();
    return page()
        .pcGtsId("page1")
        .metadata(
            MetadataBuilder.builder()
                .creator("test")
                .created(TIME)
                .lastChange(TIME)
                .addMetadataItems(processing)
                .build())
        .type(PageType.TITLE)
        .conf(0.75)
        .printSpace(box(50, 50, 949, 749))
        .regions(
            List.of(
                TextRegionBuilder.builder()
                    .id("r1")
                    .coords(
                        Polygon.of(new Point(100, 100), new Point(600, 90), new Point(610, 200)))
                    .orientation(5.0)
                    .type(TextType.HEADING)
                    .primaryLanguage(Language.GERMAN)
                    .align(Align.CENTRE)
                    .leading(12)
                    .textStyle(style)
                    .labels(List.of(labels))
                    .textLines(List.of(line))
                    .addTextEquivs(text("Hello world"))
                    .build(),
                ImageRegionBuilder.builder()
                    .id("i1")
                    .coords(box(100, 300, 400, 500))
                    .addAlternativeImages(new AlternativeImage("image.png", null, null))
                    .build(),
                SeparatorRegionBuilder.builder()
                    .id("s1")
                    .coords(box(500, 300, 510, 700))
                    .orientation(-90.0)
                    .build(),
                TableRegionBuilder.builder().id("t1").coords(box(600, 300, 900, 500)).build(),
                GraphicRegionBuilder.builder()
                    .id("c1")
                    .coords(box(100, 550, 400, 700))
                    .addRegions(
                        ImageRegionBuilder.builder()
                            .id("i2")
                            .coords(box(120, 560, 380, 690))
                            .build())
                    .build()))
        .readingOrder(
            ReadingOrderBuilder.builder()
                .group(
                    OrderedGroupBuilder.builder()
                        .id("ro")
                        .addMembers(new RegionRef("r1"))
                        .addMembers(new RegionRef("i1"))
                        .addMembers(new RegionRef("t1"))
                        .build())
                .build())
        .build();
  }

  @Test
  void writtenPagesReadBackAndWriteTheSame() throws Exception {
    Page original = richPage();
    byte[] written = ALTO.write(original);
    AltoReadResult read = ALTO.read(written);
    assertEquals(List.of(), read.diagnostics(), new String(written, StandardCharsets.UTF_8));
    assertArrayEquals(written, ALTO.write(read.page()));

    Page page = read.page();
    assertEquals(MeasurementUnit.PIXEL, read.measurementUnit());
    assertEquals("page1", page.pcGtsId());
    assertEquals("page.png", page.imageFilename());
    assertEquals(1000, page.imageWidth());
    assertEquals(800, page.imageHeight());
    assertEquals(PageType.TITLE, page.type());
    assertEquals(0.75, page.conf());
    assertEquals(original.printSpace(), page.printSpace());

    TextRegion expected = original.find("r1", TextRegion.class).orElseThrow();
    TextRegion region = page.find("r1", TextRegion.class).orElseThrow();
    assertEquals(expected.coords(), region.coords());
    assertEquals(5.0, region.orientation());
    assertEquals(TextType.HEADING, region.type());
    assertEquals(Language.GERMAN, region.primaryLanguage());
    assertEquals(Align.CENTRE, region.align());
    assertEquals(12, region.leading());
    assertEquals(expected.textStyle(), region.textStyle());
    assertEquals(expected.labels(), region.labels());
    assertEquals(expected.textEquivs(), region.textEquivs());

    TextLine line = region.textLines().getFirst();
    TextLine expectedLine = expected.textLines().getFirst();
    assertEquals(expectedLine.coords(), line.coords());
    assertEquals(Language.ENGLISH, line.primaryLanguage());
    assertEquals(expectedLine.textEquivs(), line.textEquivs());
    for (int i = 0; i < 2; i++) {
      Word word = line.words().get(i);
      Word expectedWord = expectedLine.words().get(i);
      assertEquals(expectedWord.id(), word.id());
      assertEquals(expectedWord.coords(), word.coords());
      assertEquals(expectedWord.textEquivs(), word.textEquivs());
      assertEquals(expectedWord.language(), word.language());
      assertEquals(expectedWord.labels(), word.labels());
      assertEquals(expectedWord.textStyle(), word.textStyle());
    }
    assertEquals(
        expectedLine.words().getFirst().glyphs().getFirst().textEquivs(),
        line.words().getFirst().glyphs().getFirst().textEquivs());

    assertEquals(
        List.of(new AlternativeImage("image.png", null, null)),
        page.find("i1", ImageRegion.class).orElseThrow().alternativeImages());
    assertEquals(-90.0, page.find("s1", SeparatorRegion.class).orElseThrow().orientation());
    assertTrue(page.find("t1", TableRegion.class).isPresent());
    GraphicRegion composed = page.find("c1", GraphicRegion.class).orElseThrow();
    assertInstanceOf(ImageRegion.class, composed.regions().getFirst());

    OrderedGroup order = assertInstanceOf(OrderedGroup.class, page.readingOrder().group());
    assertEquals(
        List.of(new RegionRef("r1"), new RegionRef("i1"), new RegionRef("t1")), order.members());

    MetadataItem item = page.metadata().metadataItems().getFirst();
    assertEquals(original.metadata().metadataItems().getFirst(), item);
    assertEquals("engine", page.metadata().creator());
  }

  @Test
  void readPagesAreValidPage() throws Exception {
    Page page = ALTO.read(ALTO.write(richPage())).page();
    PageXml.create().write(page, PageXmlVersion.LATEST);
  }

  @Test
  void theGoldenPageGoesThroughAltoAndBackAsValidPage() throws Exception {
    Page golden;
    try (var in = AltoXmlTest.class.getResourceAsStream("/golden/2024-07-15.xml")) {
      golden = PageXml.create().read(in).page();
    }
    AltoReadResult read = ALTO.read(ALTO.write(golden));
    assertEquals(List.of(), read.diagnostics());
    assertEquals(
        golden.allRegions().stream().map(r -> r.id()).toList(),
        read.page().allRegions().stream().map(r -> r.id()).toList());
    assertEquals(
        golden.textLines().stream().map(l -> l.id()).toList(),
        read.page().textLines().stream().map(l -> l.id()).toList());
    PageXml.create().write(read.page(), PageXmlVersion.LATEST);
  }

  @Test
  void writesPathsAndStreams() throws Exception {
    Page page = richPage();
    Path file = directory.resolve("alto.xml");
    ALTO.write(page, file);
    assertArrayEquals(ALTO.write(page), Files.readAllBytes(file));
    assertEquals(
        ALTO.read(file).page().regions(), ALTO.read(Files.newInputStream(file)).page().regions());
  }

  @Test
  void aLineWithoutWordsGetsOneStringWithItsText() throws Exception {
    Page page =
        page()
            .regions(
                List.of(
                    TextRegionBuilder.builder()
                        .id("r1")
                        .coords(box(0, 0, 99, 99))
                        .addTextEquivs(text("first\nsecond"))
                        .addTextLines(
                            TextLineBuilder.builder().id("l1").coords(box(0, 0, 99, 9)).build())
                        .addTextLines(
                            TextLineBuilder.builder()
                                .id("l2")
                                .coords(box(0, 10, 99, 19))
                                .addTextEquivs(text("own text"))
                                .build())
                        .build()))
            .build();
    TextRegion region =
        ALTO.read(ALTO.write(page)).page().find("r1", TextRegion.class).orElseThrow();
    assertEquals("first", region.textLines().get(0).textEquivs().getFirst().unicode());
    assertEquals("own text", region.textLines().get(1).textEquivs().getFirst().unicode());
    assertEquals(box(0, 0, 99, 9), region.textLines().get(0).words().getFirst().coords());
  }

  @Test
  void textIsPropagatedToWordsAndGlyphsWithoutText() throws Exception {
    Page page =
        page()
            .regions(
                List.of(
                    TextRegionBuilder.builder()
                        .id("r1")
                        .coords(box(0, 0, 99, 99))
                        .addTextLines(
                            TextLineBuilder.builder()
                                .id("l1")
                                .coords(box(0, 0, 99, 9))
                                .addTextEquivs(text("ab cd"))
                                .addWords(
                                    WordBuilder.builder()
                                        .id("w2")
                                        .coords(box(50, 0, 99, 9))
                                        .build())
                                .addWords(
                                    WordBuilder.builder()
                                        .id("w1")
                                        .coords(box(0, 0, 49, 9))
                                        .addGlyphs(
                                            GlyphBuilder.builder()
                                                .id("g1")
                                                .coords(box(0, 0, 9, 9))
                                                .build())
                                        .addGlyphs(
                                            GlyphBuilder.builder()
                                                .id("g2")
                                                .coords(box(10, 0, 19, 9))
                                                .build())
                                        .build())
                                .build())
                        .build()))
            .build();
    Page read = ALTO.read(ALTO.write(page)).page();
    assertEquals("ab", read.find("w1", Word.class).orElseThrow().textEquivs().getFirst().unicode());
    assertEquals("cd", read.find("w2", Word.class).orElseThrow().textEquivs().getFirst().unicode());
    assertEquals(
        "b",
        read.find("g2", com.maxnth.page4j.model.Glyph.class)
            .orElseThrow()
            .textEquivs()
            .getFirst()
            .unicode());
  }

  @Test
  void marginsSurroundTheRegionsWhenThereIsNoPrintSpace() throws Exception {
    Page page =
        page()
            .imageWidth(100)
            .imageHeight(100)
            .regions(
                List.of(TextRegionBuilder.builder().id("r1").coords(box(20, 30, 79, 69)).build()))
            .build();
    var factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    var doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(ALTO.write(page)));

    assertEquals(new Rect(0, 0, 99, 29), margin(doc, "TopMargin"));
    assertEquals(new Rect(0, 70, 99, 99), margin(doc, "BottomMargin"));
    assertEquals(new Rect(0, 30, 19, 69), margin(doc, "LeftMargin"));
    assertEquals(new Rect(80, 30, 99, 69), margin(doc, "RightMargin"));
  }

  @Test
  void marginsAreEmptyAtTheEdgeWhereTheContentReachesIt() throws Exception {
    // No regions: the whole page is content. A region reaching past the image is clipped.
    for (Page page :
        List.of(
            page().imageWidth(100).imageHeight(100).build(),
            page()
                .imageWidth(100)
                .imageHeight(100)
                .regions(
                    List.of(
                        TextRegionBuilder.builder().id("r1").coords(box(-5, 0, 120, 99)).build()))
                .build())) {
      var factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      var doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(ALTO.write(page)));
      assertEquals(List.of(0, 0, 100, 0), position(doc, "TopMargin"));
      assertEquals(List.of(0, 100, 100, 0), position(doc, "BottomMargin"));
      assertEquals(List.of(0, 0, 0, 100), position(doc, "LeftMargin"));
      assertEquals(List.of(100, 0, 0, 100), position(doc, "RightMargin"));
    }
  }

  private static List<Integer> position(org.w3c.dom.Document doc, String name) {
    var node = (Element) doc.getElementsByTagNameNS("*", name).item(0);
    return List.of("HPOS", "VPOS", "WIDTH", "HEIGHT").stream()
        .map(a -> Integer.parseInt(node.getAttribute(a)))
        .toList();
  }

  private static Rect margin(org.w3c.dom.Document doc, String name) {
    var node = (Element) doc.getElementsByTagNameNS("*", name).item(0);
    int left = Integer.parseInt(node.getAttribute("HPOS"));
    int top = Integer.parseInt(node.getAttribute("VPOS"));
    int width = Integer.parseInt(node.getAttribute("WIDTH"));
    int height = Integer.parseInt(node.getAttribute("HEIGHT"));
    return new Rect(left, top, left + width - 1, top + height - 1);
  }

  // ---- Reading ALTO from elsewhere

  private static final String ALTO_2 =
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <alto xmlns="http://www.loc.gov/standards/alto/ns-v2#"
            xmlns:xlink="http://www.w3.org/1999/xlink">
        <Description>
          <MeasurementUnit>pixel</MeasurementUnit>
          <sourceImageInformation><fileName>scan.tif</fileName></sourceImageInformation>
          <OCRProcessing ID="OCR_0">
            <ocrProcessingStep>
              <processingDateTime>2019-05-06T07:08:09</processingDateTime>
              <processingSoftware>
                <softwareCreator>Someone</softwareCreator>
                <softwareName>Engine</softwareName>
              </processingSoftware>
            </ocrProcessingStep>
          </OCRProcessing>
        </Description>
        <Styles>
          <TextStyle ID="TS1" FONTFAMILY="Times" FONTSIZE="9" FONTSTYLE="italics"/>
          <ParagraphStyle ID="PS1" ALIGN="Block" LINESPACE="11.6" FIRSTLINE="3"/>
        </Styles>
        <Tags>
          <NamedEntityTag ID="NE1" TYPE="person" LABEL="Goethe"/>
        </Tags>
        <Layout>
          <Page ID="P1" PHYSICAL_IMG_NR="3" WIDTH="2000" HEIGHT="3000" PC="0.8">
            <TopMargin ID="TM" HPOS="0" VPOS="0" WIDTH="2000" HEIGHT="100">
              <TextBlock ID="header" HPOS="10" VPOS="10" WIDTH="500" HEIGHT="50" IDNEXT="TB1">
                <TextLine HPOS="10" VPOS="10" WIDTH="500" HEIGHT="50">
                  <String CONTENT="Page" HPOS="10" VPOS="10" WIDTH="100" HEIGHT="50"/>
                </TextLine>
              </TextBlock>
            </TopMargin>
            <PrintSpace ID="PS" HPOS="100" VPOS="100" WIDTH="1800" HEIGHT="2800">
              <TextBlock ID="TB1" HPOS="100" VPOS="100" WIDTH="1000" HEIGHT="200"
                         STYLEREFS="TS1 PS1" LANG="deu" ROTATION="350" CS="false">
                <TextLine ID="TL1" HPOS="100" VPOS="100" WIDTH="1000" HEIGHT="60" BASELINE="150">
                  <String ID="S1" CONTENT="Wan" HPOS="100" VPOS="100" WIDTH="200" HEIGHT="60"
                          WC="0.95" CC="0 0 1" TAGREFS="NE1" STYLE="bold"/>
                  <SP WIDTH="20" HPOS="300" VPOS="100"/>
                  <String ID="S2" CONTENT="dern" HPOS="320" VPOS="100" WIDTH="200" HEIGHT="60"/>
                  <HYP CONTENT="¬" HPOS="520" VPOS="100" WIDTH="20"/>
                </TextLine>
              </TextBlock>
              <ComposedBlock ID="CB1" HPOS="100" VPOS="400" WIDTH="800" HEIGHT="400">
                <Illustration ID="IL1" TYPE="photo" HPOS="100" VPOS="400" WIDTH="800" HEIGHT="400"/>
              </ComposedBlock>
              <GraphicalElement ID="GE1" HPOS="100" VPOS="900" WIDTH="800" HEIGHT="5"/>
              <Unexpected/>
            </PrintSpace>
          </Page>
          <Page ID="P2" WIDTH="2000" HEIGHT="3000"/>
        </Layout>
      </alto>
      """;

  @Test
  void readsAnAlto2Document() throws Exception {
    AltoReadResult read = read(ALTO_2);
    Page page = read.page();
    assertEquals("P1", page.pcGtsId());
    assertEquals("scan.tif", page.imageFilename());
    assertEquals(2000, page.imageWidth());
    assertEquals(0.8, page.conf());
    assertEquals("Physical image number: 3", page.metadata().comments());
    assertEquals("Engine", page.metadata().creator());
    MetadataItem item = page.metadata().metadataItems().getFirst();
    assertEquals(PageDateTime.parse("2019-05-06T07:08:09"), item.date());
    assertEquals(
        List.of(
            new Label("contentGeneration", "processingCategory", null),
            new Label("Someone", "softwareCreator", null)),
        item.labels().getFirst().labels());

    assertEquals(
        List.of("header", "TB1", "IL1", "GE1"), page.regions().stream().map(r -> r.id()).toList());
    TextRegion region = page.find("TB1", TextRegion.class).orElseThrow();
    assertEquals(-10.0, region.orientation());
    assertEquals(Language.GERMAN, region.primaryLanguage());
    assertEquals(Align.JUSTIFY, region.align());
    assertEquals(12, region.leading());
    assertEquals("Times", region.textStyle().fontFamily());
    assertEquals(Boolean.TRUE, region.textStyle().italic());
    assertEquals("Wan dern¬", region.textEquivs().getFirst().unicode());

    TextLine line = region.textLines().getFirst();
    assertEquals(Polygon.of(new Point(100, 150), new Point(1099, 150)), line.baseline());
    Word wan = line.words().getFirst();
    assertEquals(0.95, wan.textEquivs().getFirst().conf());
    assertEquals(Boolean.TRUE, wan.textStyle().bold());
    assertNull(wan.textStyle().italic(), "the block's style is not copied down");
    assertEquals(List.of(new Label("Goethe", "person", null)), wan.labels().getFirst().labels());
    Word dern = line.words().get(1);
    assertEquals("dern¬", dern.textEquivs().getFirst().unicode());
    assertEquals(539, dern.coords().boundingBox().right(), "extended to the hyphen");

    assertInstanceOf(ImageRegion.class, page.find("IL1").orElseThrow());
    OrderedGroup order = assertInstanceOf(OrderedGroup.class, page.readingOrder().group());
    assertEquals(List.of(new RegionRef("header"), new RegionRef("TB1")), order.members());

    assertEquals(
        List.of(
            "IGNORED ParagraphStyle: attribute FIRSTLINE",
            "IGNORED TextBlock: attribute CS",
            "IGNORED String: attribute CC",
            "CHANGED ComposedBlock: replaced by its blocks, the model has no composed blocks",
            "CHANGED Illustration: TYPE 'photo' is not a PAGE region type, read as ImageRegion",
            "IGNORED PrintSpace: element Unexpected",
            "IGNORED Layout: page ignored, only the first page is read"),
        read.diagnostics().stream().map(d -> d.kind() + " " + d.message()).toList());
    PageXml.create().write(page, PageXmlVersion.LATEST);
  }

  @Test
  void diagnosticsHaveThePositionOfTheFirstOccurrenceAndACount() throws Exception {
    AltoReadResult read =
        read(
            """
            <alto xmlns="http://www.loc.gov/standards/alto/ns-v4#">
              <Layout><Page WIDTH="10" HEIGHT="10"><PrintSpace>
                <TextBlock ID="b1" CS="1"/>
                <TextBlock ID="b2" CS="1" HPOS="x"/>
              </PrintSpace></Page></Layout>
            </alto>
            """);
    ReadDiagnostic cs = read.diagnostics().getFirst();
    assertEquals(ReadDiagnostic.Kind.IGNORED, cs.kind());
    assertEquals(3, cs.line());
    assertEquals("TextBlock: attribute CS (2 times)", cs.message());
    assertEquals("3:" + cs.column() + ": IGNORED TextBlock: attribute CS (2 times)", cs.toString());
    assertEquals("TextBlock: HPOS 'x' is not a number", read.diagnostics().get(1).message());
  }

  @Test
  void generatesMissingAndReplacesInvalidOrDuplicateIds() throws Exception {
    AltoReadResult read =
        read(
            """
            <alto xmlns="http://www.loc.gov/standards/alto/ns-v4#">
              <Layout><Page WIDTH="10" HEIGHT="10"><PrintSpace>
                <TextBlock ID="r1"><TextLine><String CONTENT="a"/></TextLine></TextBlock>
                <TextBlock ID="r1"/>
                <TextBlock ID="1st"/>
                <TextBlock/>
              </PrintSpace></Page></Layout>
            </alto>
            """);
    assertEquals(
        List.of("r1", "r2", "r3", "r4"), read.page().regions().stream().map(r -> r.id()).toList());
    assertEquals("l1", read.page().textLines().getFirst().id());
    assertEquals("w1", read.page().words().getFirst().id());
    assertEquals(
        List.of(
            "TextBlock: ID 'r1' is used twice, replaced by 'r2'",
            "TextBlock: ID '1st' is not a valid ID, replaced by 'r3'"),
        read.diagnostics().stream().map(ReadDiagnostic::message).toList());
  }

  @Test
  void idNextChainsBecomeOrderedGroupsInAnUnorderedRoot() throws Exception {
    AltoReadResult read =
        read(
            """
            <alto xmlns="http://www.loc.gov/standards/alto/ns-v4#">
              <Layout><Page WIDTH="10" HEIGHT="10"><PrintSpace>
                <TextBlock ID="a" IDNEXT="b"/>
                <TextBlock ID="b"/>
                <TextBlock ID="c" IDNEXT="d"/>
                <TextBlock ID="d" IDNEXT="c"/>
                <TextBlock ID="e" IDNEXT="missing"/>
              </PrintSpace></Page></Layout>
            </alto>
            """);
    UnorderedGroup root =
        assertInstanceOf(UnorderedGroup.class, read.page().readingOrder().group());
    assertEquals(
        List.of(List.of("a", "b"), List.of("c", "d")),
        root.members().stream()
            .map(
                m ->
                    ((OrderedGroup) m)
                        .members().stream().map(r -> ((RegionRef) r).regionRef()).toList())
            .toList());
    assertEquals(
        "TextBlock: IDNEXT 'missing' refers to no block", read.diagnostics().getFirst().message());
  }

  @Test
  void composedBlocksOfAPageRegionTypeHoldNestedRegions() throws Exception {
    Page page =
        read("""
                <alto xmlns="http://www.loc.gov/standards/alto/ns-v4#">
                  <Layout><Page WIDTH="10" HEIGHT="10"><PrintSpace>
                    <ComposedBlock ID="c" TYPE="table">
                      <TextBlock ID="t"/>
                    </ComposedBlock>
                  </PrintSpace></Page></Layout>
                </alto>
                """)
            .page();
    TableRegion table = page.find("c", TableRegion.class).orElseThrow();
    assertEquals("t", table.regions().getFirst().id());
  }

  @Test
  void scalesPhysicalUnitsToTheImage() throws Exception {
    String xml =
        """
        <alto xmlns="http://www.loc.gov/standards/alto/ns-v4#">
          <Description><MeasurementUnit>mm10</MeasurementUnit></Description>
          <Layout><Page WIDTH="2100" HEIGHT="2970"><PrintSpace>
            <TextBlock ID="b" HPOS="254" VPOS="508" WIDTH="254" HEIGHT="254"/>
          </PrintSpace></Page></Layout>
        </alto>
        """;
    AltoReadResult unscaled = read(xml);
    assertEquals(MeasurementUnit.MM10, unscaled.measurementUnit());
    assertEquals(
        new Rect(254, 508, 507, 761), unscaled.page().regions().getFirst().coords().boundingBox());

    byte[] bytes = xml.getBytes(StandardCharsets.UTF_8);
    // The page size differs from the image size and holds all objects: the ratio is the scale.
    AltoReadResult byRatio = ALTO.read(bytes, new SourceImage(1050, 1485, 300, 300));
    assertEquals(1050, byRatio.page().imageWidth());
    assertEquals(
        new Rect(127, 254, 254, 381), byRatio.page().regions().getFirst().coords().boundingBox());

    // Same size: the resolution is the scale (300 dpi, 254 mm10 per inch).
    AltoReadResult byResolution = ALTO.read(bytes, new SourceImage(2100, 2970, 300, 300));
    assertEquals(
        new Rect(300, 600, 599, 899),
        byResolution.page().regions().getFirst().coords().boundingBox());
  }

  @Test
  void rotationIsTheOrientation() throws Exception {
    Page page =
        read("""
                <alto xmlns="http://www.loc.gov/standards/alto/ns-v4#">
                  <Layout><Page WIDTH="10" HEIGHT="10"><PrintSpace>
                    <TextBlock ID="a" ROTATION="90"/>
                    <Illustration ID="b" ROTATION="270"/>
                  </PrintSpace></Page></Layout>
                </alto>
                """)
            .page();
    assertEquals(90.0, page.find("a", TextRegion.class).orElseThrow().orientation());
    assertEquals(-90.0, page.find("b", ImageRegion.class).orElseThrow().orientation());
  }

  @Test
  void ellipseShapeUsesCentreAndLengths() throws Exception {
    Rect box = outline("<Ellipse HPOS=\"100\" VPOS=\"80\" HLENGTH=\"60\" VLENGTH=\"40\"/>");
    assertNear(new Rect(70, 60, 130, 100), box);
  }

  @Test
  void circleShapeUsesCentreAndRadius() throws Exception {
    Rect box = outline("<Circle HPOS=\"50\" VPOS=\"60\" RADIUS=\"10\"/>");
    assertNear(new Rect(40, 50, 60, 70), box);
  }

  @Test
  void polygonShapeAcceptsBothPointForms() throws Exception {
    assertEquals(new Rect(1, 2, 30, 40), outline("<Polygon POINTS=\"1,2 30,2 30,40\"/>"));
    assertEquals(new Rect(1, 2, 30, 40), outline("<Polygon POINTS=\"1 2 30 2 30 40\"/>"));
  }

  @Test
  void incompleteShapeKeepsTheBlockOutline() throws Exception {
    assertEquals(new Rect(0, 0, 199, 199), outline("<Circle HPOS=\"50\" VPOS=\"60\"/>"));
    assertEquals(new Rect(0, 0, 199, 199), outline("<Ellipse HPOS=\"50\" VPOS=\"60\"/>"));
    assertEquals(new Rect(0, 0, 199, 199), outline("<Polygon POINTS=\"1,2 3,4\"/>"));
  }

  private static Rect outline(String shape) throws Exception {
    String alto =
        """
        <alto xmlns="http://www.loc.gov/standards/alto/ns-v4#">
          <Layout>
            <Page ID="p1" WIDTH="300" HEIGHT="300">
              <PrintSpace HPOS="0" VPOS="0" WIDTH="300" HEIGHT="300">
                <Illustration ID="i1" HPOS="0" VPOS="0" WIDTH="200" HEIGHT="200">
                  <Shape>%s</Shape>
                </Illustration>
              </PrintSpace>
            </Page>
          </Layout>
        </alto>
        """
            .formatted(shape);
    return read(alto)
        .page()
        .find("i1")
        .map(o -> ((ImageRegion) o).coords())
        .orElseThrow()
        .boundingBox();
  }

  private static void assertNear(Rect expected, Rect actual) {
    assertEquals(expected.left(), actual.left(), 1, actual::toString);
    assertEquals(expected.top(), actual.top(), 1, actual::toString);
    assertEquals(expected.right(), actual.right(), 1, actual::toString);
    assertEquals(expected.bottom(), actual.bottom(), 1, actual::toString);
  }

  @Test
  void failsOnOtherDocumentsAndDoesNotResolveEntities() throws Exception {
    assertThrows(PageReadException.class, () -> read("<PcGts/>"));
    assertThrows(PageReadException.class, () -> read("<alto>"));
    Path secret = directory.resolve("secret.txt");
    Files.writeString(secret, "secret");
    var e =
        assertThrows(
            PageReadException.class,
            () ->
                read(
                    "<!DOCTYPE alto [<!ENTITY x SYSTEM \""
                        + secret.toUri()
                        + "\">]><alto><Description><MeasurementUnit>&x;</MeasurementUnit>"
                        + "</Description></alto>"));
    assertFalse(e.getMessage().contains("secret"), e.getMessage());
  }
}
