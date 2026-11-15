package com.maxnth.page4j.pagexml.internal;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;

/**
 * Creates SAX parsers that are safe for untrusted XML.
 *
 * <p>External entities and external DTDs are never resolved, so documents cannot read local files
 * or reach network hosts (XXE). Secure processing stays enabled, which bounds entity expansion.
 * DOCTYPE declarations remain allowed because formats such as hOCR declare an XHTML DTD.
 */
final class SecureXmlParsers {

  private static final String EXTERNAL_GENERAL_ENTITIES =
      "http://xml.org/sax/features/external-general-entities";
  private static final String EXTERNAL_PARAMETER_ENTITIES =
      "http://xml.org/sax/features/external-parameter-entities";
  private static final String LOAD_EXTERNAL_DTD =
      "http://apache.org/xml/features/nonvalidating/load-external-dtd";

  private SecureXmlParsers() {}

  /** Returns a non-validating SAX parser that never resolves external resources. */
  static SAXParser newSAXParser(boolean namespaceAware)
      throws ParserConfigurationException, SAXException {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setNamespaceAware(namespaceAware);
    factory.setValidating(false);
    factory.setXIncludeAware(false);
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    factory.setFeature(EXTERNAL_GENERAL_ENTITIES, false);
    factory.setFeature(EXTERNAL_PARAMETER_ENTITIES, false);
    factory.setFeature(LOAD_EXTERNAL_DTD, false);

    SAXParser parser = factory.newSAXParser();
    parser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    parser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    return parser;
  }
}
