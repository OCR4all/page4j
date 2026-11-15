package com.maxnth.page4j.formats;

import com.maxnth.page4j.formats.internal.XmlInput;
import com.maxnth.page4j.pagexml.PageReadException;
import java.util.Locale;

/** The formats page4j reads, told apart by the root element of a document. */
public enum Format {
  /** PAGE XML, read with {@link com.maxnth.page4j.pagexml.PageXml}. */
  PAGE_XML,
  /** ALTO, read with {@link AltoXml}. */
  ALTO,
  /** ABBYY FineReader XML, read with {@link FineReaderXml}. */
  FINE_READER,
  /** hOCR, read with {@link Hocr}. */
  HOCR,
  /** METS, whose page files {@link MetsXml} lists. */
  METS;

  /**
   * The format of a document, by its root element: {@code PcGts}, {@code alto}, {@code document} in
   * an ABBYY namespace, {@code html} or {@code mets}. The document is not read further.
   *
   * @throws PageReadException if the document is not well-formed XML or of none of these formats
   */
  public static Format detect(byte[] xml) throws PageReadException {
    XmlInput.Root root = XmlInput.root(xml);
    String namespace = root.namespace() == null ? "" : root.namespace().toLowerCase(Locale.ROOT);
    switch (root.name()) {
      case "PcGts" -> {
        return PAGE_XML;
      }
      case "alto" -> {
        return ALTO;
      }
      case "document" -> {
        if (namespace.contains("abbyy")) {
          return FINE_READER;
        }
      }
      case "html" -> {
        return HOCR;
      }
      case "mets" -> {
        return METS;
      }
      default -> {
        // unknown
      }
    }
    throw new PageReadException(
        -1,
        -1,
        "Unknown document format: the root element is "
            + (root.namespace() == null ? "" : "{" + root.namespace() + "}")
            + root.name(),
        null);
  }
}
