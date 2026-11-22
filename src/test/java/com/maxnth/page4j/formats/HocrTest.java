package com.maxnth.page4j.formats;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.maxnth.page4j.geometry.Point;
import com.maxnth.page4j.geometry.Polygon;
import com.maxnth.page4j.geometry.Rect;
import com.maxnth.page4j.model.ChemRegion;
import com.maxnth.page4j.model.ImageRegion;
import com.maxnth.page4j.model.LineDrawingRegion;
import com.maxnth.page4j.model.MathsRegion;
import com.maxnth.page4j.model.Page;
import com.maxnth.page4j.model.SeparatorRegion;
import com.maxnth.page4j.model.TableRegion;
import com.maxnth.page4j.model.TextLine;
import com.maxnth.page4j.model.TextLineBuilder;
import com.maxnth.page4j.model.TextRegion;
import com.maxnth.page4j.model.TextRegionBuilder;
import com.maxnth.page4j.model.Word;
import com.maxnth.page4j.model.value.Language;
import com.maxnth.page4j.model.value.ReadingDirection;
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

class HocrTest {

  private static final Hocr HOCR = Hocr.create();

  @TempDir Path directory;

  private static ReadResult read(String html) throws PageReadException {
    return HOCR.read(html.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void readsHocrWithoutSchemaValidation() throws Exception {
    Path file = directory.resolve("page.hocr");
    Files.writeString(
        file,
        """
        <html xmlns="http://www.w3.org/1999/xhtml">
          <head><meta name="ocr-system" content="test"/></head>
          <body>
            <div class="ocr_page" title="image &quot;page.png&quot;; bbox 0 0 200 100">
              <p class="ocr_par" id="par_1" title="bbox 10 10 190 40">
                <span class="ocr_line" id="line_1" title="bbox 10 10 190 40">
                  <span class="ocrx_word" id="word_1" title="bbox 10 10 90 40">Hello</span>
                </span>
              </p>
            </div>
          </body>
        </html>
        """);

    Page page = HOCR.read(file).page();

    assertEquals(200, page.imageWidth());
    assertEquals(100, page.imageHeight());
    assertEquals("page.png", page.imageFilename());
    assertEquals(1, page.regions().size());
    TextRegion region = assertInstanceOf(TextRegion.class, page.regions().getFirst());
    assertEquals(1, region.textLines().size());
    assertEquals("test", page.metadata().creator());
  }

  private static final String TESSERACT =
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
          "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
      <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
       <head>
        <title></title>
        <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>
        <meta name='ocr-system' content='tesseract 5.3.0'/>
        <meta name='ocr-capabilities' content='ocr_page ocr_carea ocr_par ocr_line ocrx_word'/>
       </head>
       <body>
        <div class='ocr_page' id='page_1' title='image "/scans/p1.png"; bbox 0 0 1000 800; ppageno 0; scan_res 300 300'>
         <div class='ocr_carea' id='block_1_1' title="bbox 100 100 900 300">
          <p class='ocr_par' id='par_1_1' lang='deu' dir='ltr' title="bbox 100 100 900 300">
           <span class='ocr_line' id='line_1_1' title="bbox 100 100 900 150; baseline 0.01 -10; x_size 40; x_descenders 8; x_ascenders 10">
            <span class='ocrx_word' id='word_1_1' title='bbox 100 100 300 150; x_wconf 96'>Guten</span>
            <span class='ocrx_word' id='word_1_2' title='bbox 320 100 500 150; x_wconf 91'><strong>Tag</strong></span>
           </span>
           <span class='ocr_header' id='line_1_2' title="bbox 100 200 900 250; textangle 90">Kopf  zeile</span>
          </p>
         </div>
         <div class='ocr_carea' id='block_1_2' title="bbox 100 400 900 450">
          <span class='ocr_line' id='line_1_3' title="bbox 100 400 900 450">
           <span class='ocrx_word' id='word_1_3' title='bbox 100 400 300 450' lang='xx'>direkt</span>
          </span>
         </div>
         <div class='ocr_photo' id='photo_1' title="bbox 100 500 400 700"></div>
         <div class='ocr_section'>
          <p class='ocr_par' id='par_1_9' title="bbox 0 0 10 10"></p>
         </div>
        </div>
       </body>
      </html>
      """;

  @Test
  void readsTesseractOutput() throws Exception {
    ReadResult read = read(TESSERACT);
    Page page = read.page();
    assertEquals("page_1", page.pcGtsId());
    assertEquals("p1.png", page.imageFilename());
    assertEquals(1000, page.imageWidth());
    assertEquals(300.0, page.imageXResolution());
    assertEquals("tesseract 5.3.0", page.metadata().creator());
    assertNull(page.primaryLanguage());

    assertEquals(
        List.of("par_1_1", "block_1_2", "photo_1", "par_1_9"),
        page.regions().stream().map(r -> r.id()).toList());
    TextRegion paragraph = page.find("par_1_1", TextRegion.class).orElseThrow();
    assertEquals(Language.GERMAN, paragraph.primaryLanguage());
    assertEquals(ReadingDirection.LEFT_TO_RIGHT, paragraph.readingDirection());
    assertEquals("Guten Tag\nKopf zeile", paragraph.textEquivs().getFirst().unicode());

    TextLine line = paragraph.textLines().getFirst();
    assertEquals(Polygon.of(new Point(100, 140), new Point(900, 148)), line.baseline());
    Word tag = line.words().get(1);
    assertEquals("Tag", tag.textEquivs().getFirst().unicode());
    assertEquals(0.91, tag.textEquivs().getFirst().conf());
    assertEquals(new Rect(320, 100, 500, 150), tag.coords().boundingBox());
    assertEquals(90.0, paragraph.textLines().get(1).orientation());

    assertEquals(
        "direkt",
        page.find("block_1_2", TextRegion.class).orElseThrow().textEquivs().getFirst().unicode());
    assertInstanceOf(ImageRegion.class, page.find("photo_1").orElseThrow());

    assertEquals(
        List.of(
            "IGNORED ocr_line: title property x_size",
            "IGNORED ocr_line: title property x_descenders",
            "IGNORED ocr_line: title property x_ascenders",
            "CHANGED lang 'xx' is not a known language code, read as other",
            "IGNORED hOCR class ocr_section ignored, its content is read"),
        read.diagnostics().stream().map(d -> d.kind() + " " + d.message()).toList());
    PageXml.create().write(page, PageXmlVersion.LATEST);
  }

  @Test
  void readsTablesDrawingsMathsAndChemistryAsRegions() throws Exception {
    ReadResult read =
        read(
            """
            <html xmlns="http://www.w3.org/1999/xhtml">
              <body>
                <div class="ocr_page" lang="deu" title="bbox 0 0 200 100">
                  <div class="ocr_table" id="t1" title="bbox 0 0 100 50">
                    <div class="ocr_carea" id="c1" title="bbox 0 0 50 25">
                      <span class="ocr_line" id="l1" title="bbox 0 0 50 25">cell</span>
                    </div>
                  </div>
                  <div class="ocr_linedrawing" id="d1" title="bbox 100 0 150 50"></div>
                  <div class="ocr_math" id="m1" title="bbox 150 0 200 50"></div>
                  <div class="ocr_chem" id="h1" title="bbox 0 50 50 100"></div>
                  <div class="ocr_table" id="t2"></div>
                </div>
              </body>
            </html>
            """);

    Page page = read.page();
    assertEquals(Language.GERMAN, page.primaryLanguage());
    assertEquals(
        List.of("t1", "c1", "d1", "m1", "h1"), page.regions().stream().map(r -> r.id()).toList());
    assertInstanceOf(TableRegion.class, page.regions().getFirst());
    assertInstanceOf(TextRegion.class, page.regions().get(1));
    assertInstanceOf(LineDrawingRegion.class, page.regions().get(2));
    assertInstanceOf(MathsRegion.class, page.regions().get(3));
    assertInstanceOf(ChemRegion.class, page.regions().get(4));
    assertEquals(
        List.of("IGNORED ocr_table without bbox ignored, its content is read"),
        read.diagnostics().stream().map(d -> d.kind() + " " + d.message()).toList());
  }

  @Test
  void writesALineWhoseWordsDontSpellItsTextAsItsText() throws Exception {
    byte[] written = HOCR.write(WriterTestPages.page("engine", "Hello World!"));

    TextLine first =
        HOCR.read(written).page().all(TextLine.class).stream().findFirst().orElseThrow();
    assertEquals("Hello World!", first.textEquivs().getFirst().unicode());
    assertEquals(List.of(), first.words());
  }

  @Test
  void writesImageNamesWithQuotesAndSemicolonsThatReadBack() throws Exception {
    byte[] written = HOCR.write(WriterTestPages.page("engine", "Hello Wörld", "scan;1 \"a\".png"));

    ReadResult read = HOCR.read(written);
    assertEquals("scan;1 \"a\".png", read.page().imageFilename());
    assertEquals(1000, read.page().imageWidth());
    assertEquals(List.of(), read.diagnostics());
  }

  @Test
  void readsWindowsImagePaths() throws Exception {
    Page page =
        read("""
                <html xmlns="http://www.w3.org/1999/xhtml"><body>
                  <div class="ocr_page" title='image "C:\\scans\\p1.png"; bbox 0 0 10 10'></div>
                </body></html>
                """)
            .page();

    assertEquals("p1.png", page.imageFilename());
  }

  @Test
  void refusesTextXmlCannotHold() {
    Page page =
        WriterTestPages.page()
            .withReadingOrder(null)
            .withRegions(
                List.of(
                    TextRegionBuilder.builder()
                        .id("r1")
                        .coords(WriterTestPages.box(0, 0, 100, 50))
                        .addTextLines(
                            TextLineBuilder.builder()
                                .id("l1")
                                .coords(WriterTestPages.box(0, 0, 100, 50))
                                .addTextEquivs(WriterTestPages.text("a\u0001b", null))
                                .build())
                        .build()));

    assertEquals(
        "Cannot write hOCR: TextLine 'l1' contains the character U+0001, which XML can't hold",
        assertThrows(IllegalArgumentException.class, () -> HOCR.write(page)).getMessage());
    assertEquals(
        "Cannot write FineReader XML: TextLine 'l1' contains the character U+0001, which XML"
            + " can't hold",
        assertThrows(IllegalArgumentException.class, () -> FineReaderXml.create().write(page))
            .getMessage());
    assertEquals(
        "Cannot write ALTO: String attribute CONTENT contains the character U+0001, which XML"
            + " can't hold",
        assertThrows(IllegalArgumentException.class, () -> AltoXml.create().write(page))
            .getMessage());
  }

  @Test
  void leavesOutABlankCreator() throws Exception {
    String written =
        new String(HOCR.write(WriterTestPages.page(" ", "Hello Wörld")), StandardCharsets.UTF_8);

    assertFalse(written.contains("ocr-system"), written);
  }

  @Test
  void failsOnOtherDocuments() {
    assertThrows(PageReadException.class, () -> read("<alto/>"));
    assertThrows(PageReadException.class, () -> read("<html><br></html>"));
  }

  @Test
  void writtenPagesReadBackAndWriteTheSame() throws Exception {
    byte[] written = HOCR.write(WriterTestPages.page());
    ReadResult read = HOCR.read(written);
    assertEquals(List.of(), read.diagnostics(), new String(written, StandardCharsets.UTF_8));
    assertArrayEquals(written, HOCR.write(read.page()));

    Page page = read.page();
    assertEquals("page1", page.pcGtsId());
    assertEquals("engine", page.metadata().creator());
    assertEquals("page.png", page.imageFilename());
    assertEquals(1000, page.imageWidth());
    assertEquals(800, page.imageHeight());
    assertEquals(300.0, page.imageXResolution());
    assertEquals(Language.GERMAN, page.primaryLanguage());
    assertEquals(
        List.of("i1", "r1", "s1"), page.regions().stream().map(region -> region.id()).toList());
    assertInstanceOf(ImageRegion.class, page.regions().getFirst());
    assertInstanceOf(SeparatorRegion.class, page.regions().get(2));

    TextRegion region = page.find("r1", TextRegion.class).orElseThrow();
    assertEquals(new Rect(100, 100, 900, 300), region.coords().boundingBox());
    assertEquals(ReadingDirection.LEFT_TO_RIGHT, region.readingDirection());

    TextLine first = region.textLines().getFirst();
    assertEquals("l1", first.id());
    assertEquals(new Rect(100, 100, 500, 150), first.coords().boundingBox());
    assertEquals(Polygon.of(new Point(100, 140), new Point(500, 140)), first.baseline());
    assertEquals("Hello Wörld", first.textEquivs().getFirst().unicode());
    Word hello = first.words().getFirst();
    assertEquals("w1", hello.id());
    assertEquals(new Rect(100, 100, 249, 150), hello.coords().boundingBox());
    assertEquals(0.9, hello.textEquivs().getFirst().conf());
    assertEquals(0.75, first.words().get(1).textEquivs().getFirst().conf());

    TextLine second = region.textLines().get(1);
    assertEquals(Language.ENGLISH, second.primaryLanguage());
    assertEquals("second & <line>", second.textEquivs().getFirst().unicode());

    PageXml.create().write(page, PageXmlVersion.LATEST);
  }

  @Test
  void theGoldenPageGoesThroughHocrAndBackAsValidPage() throws Exception {
    Page golden;
    try (var in = HocrTest.class.getResourceAsStream("/golden/2024-07-15.xml")) {
      golden = PageXml.create().read(in).page();
    }
    ReadResult read = HOCR.read(HOCR.write(golden));
    assertEquals(
        golden.textLines().stream().map(line -> line.id()).toList(),
        read.page().textLines().stream().map(line -> line.id()).toList());
    assertEquals(
        golden.all(TextRegion.class).stream().map(region -> region.id()).sorted().toList(),
        read.page().all(TextRegion.class).stream().map(region -> region.id()).sorted().toList());
    PageXml.create().write(read.page(), PageXmlVersion.LATEST);
  }

  @Test
  void writesPathsAndStreams() throws Exception {
    Page page = WriterTestPages.page();
    Path file = directory.resolve("written.hocr");
    HOCR.write(page, file);
    assertArrayEquals(HOCR.write(page), Files.readAllBytes(file));
    var stream = new ByteArrayOutputStream();
    HOCR.write(page, stream);
    assertArrayEquals(HOCR.write(page), stream.toByteArray());
  }
}
