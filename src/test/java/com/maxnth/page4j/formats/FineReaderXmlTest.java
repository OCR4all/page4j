package com.maxnth.page4j.formats;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.maxnth.page4j.geometry.Point;
import com.maxnth.page4j.geometry.Polygon;
import com.maxnth.page4j.geometry.Rect;
import com.maxnth.page4j.model.GraphicRegion;
import com.maxnth.page4j.model.ImageRegion;
import com.maxnth.page4j.model.Page;
import com.maxnth.page4j.model.SeparatorRegion;
import com.maxnth.page4j.model.TextLine;
import com.maxnth.page4j.model.TextRegion;
import com.maxnth.page4j.model.UnknownRegion;
import com.maxnth.page4j.model.Word;
import com.maxnth.page4j.model.value.Align;
import com.maxnth.page4j.model.value.GraphicsType;
import com.maxnth.page4j.model.value.Language;
import com.maxnth.page4j.pagexml.PageReadException;
import com.maxnth.page4j.pagexml.PageXml;
import com.maxnth.page4j.pagexml.PageXmlVersion;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FineReaderXmlTest {

  private static final FineReaderXml FINE_READER = FineReaderXml.create();

  private static final String DOCUMENT =
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <document xmlns="http://www.abbyy.com/FineReader_xml/FineReader10-schema-v1.xml"
                version="1.0" producer="FineReader 10" pagesCount="2" mainLanguage="German">
        <page width="1000" height="800" resolution="300" originalCoords="1">
          <block blockType="Text" l="100" t="100" r="900" b="300">
            <region><rect l="100" t="100" r="900" b="300"/></region>
            <text>
              <par align="Justified" lineSpacing="1">
                <line baseline="140" l="100" t="100" r="500" b="150">
                  <formatting lang="GermanStandard">
                    <charParams l="100" t="100" r="120" b="150" charConfidence="90">A</charParams>
                    <charParams l="120" t="100" r="140" b="150"> </charParams>
                    <charParams l="140" t="100" r="160" b="150">b</charParams>
                  </formatting>
                </line>
                <line baseline="240" l="100" t="200" r="900" b="250">
                  <formatting lang="GermanStandard">plain text</formatting>
                </line>
              </par>
              <par/>
            </text>
          </block>
          <block blockType="Picture" l="0" t="0" r="0" b="0">
            <region>
              <rect l="100" t="400" r="500" b="500"/>
              <rect l="100" t="500" r="300" b="600"/>
            </region>
          </block>
          <block blockType="Barcode"><region><rect l="1" t="1" r="9" b="9"/></region></block>
          <block blockType="Checkmark"><region><rect l="1" t="1" r="9" b="9"/></region></block>
        </page>
        <page width="1000" height="800"/>
      </document>
      """;

  @Test
  void readsAFineReader10Document() throws Exception {
    ReadResult read = FineReaderXml.create().read(DOCUMENT.getBytes(StandardCharsets.UTF_8));
    Page page = read.page();
    assertEquals("FineReader 10", page.metadata().creator());
    assertEquals("Original coords: true", page.metadata().comments());
    assertEquals(1000, page.imageWidth());
    assertEquals(300.0, page.imageXResolution());
    assertEquals(Language.GERMAN, page.primaryLanguage());
    assertEquals(4, page.regions().size());

    TextRegion paragraph = assertInstanceOf(TextRegion.class, page.regions().getFirst());
    assertEquals(new Rect(100, 100, 900, 250), paragraph.coords().boundingBox());
    assertEquals(Align.JUSTIFY, paragraph.align());
    assertEquals("A b\nplain text", paragraph.textEquivs().getFirst().unicode());
    TextLine first = paragraph.textLines().getFirst();
    assertEquals(Polygon.of(new Point(100, 140), new Point(500, 140)), first.baseline());
    assertEquals(Language.GERMAN, first.primaryLanguage());
    assertEquals(2, first.words().size());
    Word a = first.words().getFirst();
    assertEquals(new Rect(100, 100, 120, 150), a.coords().boundingBox());
    assertEquals("A", a.textEquivs().getFirst().unicode());
    assertEquals(0.9, a.textEquivs().getFirst().conf());
    Word b = first.words().get(1);
    assertEquals(new Rect(140, 100, 160, 150), b.coords().boundingBox());
    assertNull(b.textEquivs().getFirst().conf());
    assertEquals(List.of(), paragraph.textLines().get(1).words());

    ImageRegion picture = assertInstanceOf(ImageRegion.class, page.regions().get(1));
    assertEquals(
        Polygon.of(
            new Point(500, 400),
            new Point(500, 500),
            new Point(300, 500),
            new Point(300, 600),
            new Point(100, 600),
            new Point(100, 500),
            new Point(100, 500),
            new Point(100, 400)),
        picture.coords());
    assertEquals(
        GraphicsType.BARCODE, assertInstanceOf(GraphicRegion.class, page.regions().get(2)).type());
    assertInstanceOf(UnknownRegion.class, page.regions().get(3));

    List<String> messages = read.diagnostics().stream().map(ReadDiagnostic::message).toList();
    assertEquals(
        List.of(
            "par: attribute lineSpacing",
            "block: blockType 'Checkmark' read as UnknownRegion",
            "document: page ignored, only the first page is read"),
        messages);
    PageXml.create().write(page, PageXmlVersion.LATEST);
  }

  @Test
  void buildsWordsAcrossSpacesWithoutABox() throws Exception {
    String document =
        """
        <document xmlns="http://www.abbyy.com/FineReader_xml/FineReader10-schema-v1.xml">
          <page width="100" height="50">
            <block blockType="Text" l="0" t="0" r="100" b="50">
              <text><par><line baseline="40" l="0" t="0" r="100" b="50"><formatting>
                <charParams l="0" t="0" r="9" b="50" wordStart="1">a</charParams>
                <charParams wordStart="0"> </charParams>
                <charParams l="20" t="0" r="29" b="50" wordStart="1">b</charParams>
              </formatting></line></par></text>
            </block>
          </page>
        </document>
        """;

    TextLine line =
        FINE_READER
            .read(document.getBytes(StandardCharsets.UTF_8))
            .page()
            .all(TextLine.class)
            .getFirst();
    assertEquals("a b", line.textEquivs().getFirst().unicode());
    assertEquals(
        List.of("a", "b"),
        line.words().stream().map(w -> w.textEquivs().getFirst().unicode()).toList());
  }

  @Test
  void writtenPagesReadBack() throws Exception {
    byte[] written = FINE_READER.write(WriterTestPages.page());
    ReadResult read = FINE_READER.read(written);
    assertEquals(List.of(), read.diagnostics(), new String(written, StandardCharsets.UTF_8));

    Page page = read.page();
    assertEquals("engine", page.metadata().creator());
    assertEquals(1000, page.imageWidth());
    assertEquals(800, page.imageHeight());
    assertEquals(300.0, page.imageXResolution());
    assertEquals(Language.GERMAN, page.primaryLanguage());
    assertEquals(3, page.regions().size());
    assertEquals(
        new Rect(100, 400, 500, 600),
        assertInstanceOf(ImageRegion.class, page.regions().getFirst()).coords().boundingBox());
    assertEquals(
        new Rect(0, 650, 999, 660),
        assertInstanceOf(SeparatorRegion.class, page.regions().get(2)).coords().boundingBox());

    TextRegion region = assertInstanceOf(TextRegion.class, page.regions().get(1));
    assertEquals(Align.CENTRE, region.align());
    assertEquals("Hello Wörld\nsecond & <line>", region.textEquivs().getFirst().unicode());

    TextLine first = region.textLines().getFirst();
    assertEquals(new Rect(100, 100, 500, 150), first.coords().boundingBox());
    assertEquals(Polygon.of(new Point(100, 140), new Point(500, 140)), first.baseline());
    assertEquals(Language.GERMAN, first.primaryLanguage());
    assertEquals(
        List.of("Hello", "Wörld"),
        first.words().stream().map(w -> w.textEquivs().getFirst().unicode()).toList());
    assertEquals(new Rect(100, 100, 249, 150), first.words().getFirst().coords().boundingBox());
    assertEquals(new Rect(300, 100, 500, 150), first.words().get(1).coords().boundingBox());
    assertEquals(0.9, first.words().getFirst().textEquivs().getFirst().conf());
    assertEquals(0.75, first.words().get(1).textEquivs().getFirst().conf());

    TextLine second = region.textLines().get(1);
    assertEquals(Language.ENGLISH, second.primaryLanguage());
    assertEquals("second & <line>", second.textEquivs().getFirst().unicode());
    assertEquals(List.of(), second.words());

    PageXml.create().write(page, PageXmlVersion.LATEST);
  }

  @Test
  void writesALineWhoseWordsDontSpellItsTextAsPlainText() throws Exception {
    byte[] written = FINE_READER.write(WriterTestPages.page("engine", "Hello World!"));

    TextLine first =
        FINE_READER.read(written).page().all(TextLine.class).stream().findFirst().orElseThrow();
    assertEquals("Hello World!", first.textEquivs().getFirst().unicode());
    assertEquals(List.of(), first.words());
  }

  @Test
  void theGoldenPageGoesThroughFineReaderAndBackAsValidPage() throws Exception {
    Page golden;
    try (var in = FineReaderXmlTest.class.getResourceAsStream("/golden/2024-07-15.xml")) {
      golden = PageXml.create().read(in).page();
    }
    ReadResult read = FINE_READER.read(FINE_READER.write(golden));
    assertEquals(List.of(), read.diagnostics());
    assertEquals(golden.textLines().size(), read.page().textLines().size());
    PageXml.create().write(read.page(), PageXmlVersion.LATEST);
  }

  @Test
  void writesPathsAndStreams(@TempDir Path directory) throws Exception {
    Page page = WriterTestPages.page();
    Path file = directory.resolve("page.xml");
    FINE_READER.write(page, file);
    assertArrayEquals(FINE_READER.write(page), Files.readAllBytes(file));
    var stream = new ByteArrayOutputStream();
    FINE_READER.write(page, stream);
    assertArrayEquals(FINE_READER.write(page), stream.toByteArray());
  }

  @Test
  void failsOnOtherDocuments() {
    assertThrows(PageReadException.class, () -> FineReaderXml.create().read("<alto/>".getBytes()));
  }
}
