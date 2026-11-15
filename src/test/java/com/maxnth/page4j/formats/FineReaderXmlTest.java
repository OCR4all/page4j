package com.maxnth.page4j.formats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.maxnth.page4j.geometry.Point;
import com.maxnth.page4j.geometry.Polygon;
import com.maxnth.page4j.geometry.Rect;
import com.maxnth.page4j.model.GraphicRegion;
import com.maxnth.page4j.model.ImageRegion;
import com.maxnth.page4j.model.Page;
import com.maxnth.page4j.model.TextLine;
import com.maxnth.page4j.model.TextRegion;
import com.maxnth.page4j.model.UnknownRegion;
import com.maxnth.page4j.model.value.Align;
import com.maxnth.page4j.model.value.GraphicsType;
import com.maxnth.page4j.model.value.Language;
import com.maxnth.page4j.pagexml.PageReadException;
import com.maxnth.page4j.pagexml.PageXml;
import com.maxnth.page4j.pagexml.PageXmlVersion;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class FineReaderXmlTest {

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
            "formatting: attribute lang (2 times)",
            "charParams: attribute l (3 times)",
            "charParams: attribute t (3 times)",
            "charParams: attribute r (3 times)",
            "charParams: attribute b (3 times)",
            "charParams: attribute charConfidence",
            "block: blockType 'Checkmark' read as UnknownRegion",
            "document: page ignored, only the first page is read"),
        messages);
    PageXml.create().write(page, PageXmlVersion.LATEST);
  }

  @Test
  void failsOnOtherDocuments() {
    assertThrows(PageReadException.class, () -> FineReaderXml.create().read("<alto/>".getBytes()));
  }
}
