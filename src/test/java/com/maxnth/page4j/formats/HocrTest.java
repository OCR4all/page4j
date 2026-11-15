package com.maxnth.page4j.formats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.maxnth.page4j.geometry.Point;
import com.maxnth.page4j.geometry.Polygon;
import com.maxnth.page4j.geometry.Rect;
import com.maxnth.page4j.model.ImageRegion;
import com.maxnth.page4j.model.Page;
import com.maxnth.page4j.model.TextLine;
import com.maxnth.page4j.model.TextRegion;
import com.maxnth.page4j.model.Word;
import com.maxnth.page4j.model.value.Language;
import com.maxnth.page4j.model.value.ReadingDirection;
import com.maxnth.page4j.pagexml.PageReadException;
import com.maxnth.page4j.pagexml.PageXml;
import com.maxnth.page4j.pagexml.PageXmlVersion;
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
  void failsOnOtherDocuments() {
    assertThrows(PageReadException.class, () -> read("<alto/>"));
    assertThrows(PageReadException.class, () -> read("<html><br></html>"));
  }
}
