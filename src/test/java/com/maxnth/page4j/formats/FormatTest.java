package com.maxnth.page4j.formats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.maxnth.page4j.formats.internal.Languages;
import com.maxnth.page4j.model.value.Language;
import com.maxnth.page4j.pagexml.PageReadException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class FormatTest {

  private static Format detect(String xml) throws PageReadException {
    return Format.detect(xml.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void detectsTheFormatByTheRootElement() throws Exception {
    assertEquals(
        Format.PAGE_XML,
        detect("<PcGts xmlns='http://schema.primaresearch.org/PAGE/gts/pagecontent/2019-07-15'/>"));
    assertEquals(Format.ALTO, detect("<alto xmlns='http://www.loc.gov/standards/alto/ns-v3#'/>"));
    assertEquals(
        Format.FINE_READER,
        detect(
            "<document xmlns='http://www.abbyy.com/FineReader_xml/FineReader10-schema-v1.xml'/>"));
    assertEquals(Format.HOCR, detect("<html/>"));
    assertEquals(Format.METS, detect("<mets:mets xmlns:mets='http://www.loc.gov/METS/'/>"));
    var e = assertThrows(PageReadException.class, () -> detect("<document/>"));
    assertEquals("Unknown document format: the root element is document", e.getMessage());
    assertThrows(PageReadException.class, () -> detect("not xml"));
  }

  @Test
  void mapsLanguageCodes() {
    assertEquals(Language.GERMAN, Languages.fromCode("de"));
    assertEquals(Language.GERMAN, Languages.fromCode("deu"));
    assertEquals(Language.GERMAN, Languages.fromCode("ger"));
    assertEquals(Language.ENGLISH, Languages.fromCode("en-GB"));
    assertEquals(Language.GERMAN, Languages.fromCode("deu_frak"));
    assertEquals(null, Languages.fromCode("xx"));
    assertEquals("en", Languages.toCode(Language.ENGLISH));
    assertEquals(null, Languages.toCode(Language.OTHER));
  }
}
