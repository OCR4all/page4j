package com.maxnth.page4j.formats.internal;

import com.maxnth.page4j.pagexml.PageReadException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/** Lists the files a METS document refers to. */
public final class MetsReader {

  private static final String XLINK = "http://www.w3.org/1999/xlink";

  private MetsReader() {}

  /**
   * The {@code xlink:href} of every {@code FLocat}, in document order.
   *
   * @throws PageReadException if the document is not well-formed XML or not METS
   */
  public static List<String> files(byte[] xml) throws PageReadException {
    XMLStreamReader r = XmlInput.open(xml);
    try {
      XmlInput.requireRoot(r, "mets", "METS");
      List<String> files = new ArrayList<>();
      while (r.hasNext()) {
        if (r.next() == XMLStreamConstants.START_ELEMENT && "FLocat".equals(r.getLocalName())) {
          String href = r.getAttributeValue(XLINK, "href");
          if (href != null) {
            files.add(href);
          }
        }
      }
      return files;
    } catch (XMLStreamException e) {
      throw XmlInput.notWellFormed(e);
    } catch (XmlInput.Failure e) {
      throw e.toException();
    } finally {
      XmlInput.close(r);
    }
  }
}
