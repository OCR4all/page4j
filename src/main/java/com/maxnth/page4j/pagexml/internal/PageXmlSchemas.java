package com.maxnth.page4j.pagexml.internal;

import com.maxnth.page4j.pagexml.PageReadException;
import com.maxnth.page4j.pagexml.PageXmlVersion;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.jspecify.annotations.Nullable;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/** The bundled PAGE schemas, loaded once per version, and validation against them. */
public final class PageXmlSchemas {

  private static final String LOCATION = "/com/maxnth/page4j/pagexml/schemas/";
  private static final int MAX_ERRORS = 20;
  private static final Map<PageXmlVersion, Schema> SCHEMAS = new EnumMap<>(PageXmlVersion.class);

  private PageXmlSchemas() {}

  /** Opens the XSD of the given version. */
  static InputStream open(PageXmlVersion version) {
    InputStream in = PageXmlSchemas.class.getResourceAsStream(resource(version));
    if (in == null) {
      throw new IllegalStateException("Missing schema " + resource(version));
    }
    return in;
  }

  private static String resource(PageXmlVersion version) {
    return LOCATION + version.date() + "_pagecontent.xsd";
  }

  /** The schema of the given version. */
  public static synchronized Schema schema(PageXmlVersion version) {
    Schema schema = SCHEMAS.get(version);
    if (schema == null) {
      String resource = resource(version);
      try (InputStream in = open(version)) {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        schema = factory.newSchema(new StreamSource(in, resource));
      } catch (IOException | SAXException e) {
        throw new IllegalStateException("Cannot load schema " + resource, e);
      }
      SCHEMAS.put(version, schema);
    }
    return schema;
  }

  /**
   * Validates a document against the schema of the given version. External entities and DTDs are
   * not resolved.
   *
   * @throws PageReadException with every error found (up to a limit) if the document is invalid
   */
  public static void validate(byte[] xml, PageXmlVersion version) throws PageReadException {
    Validator validator = schema(version).newValidator();
    Errors errors = new Errors();
    validator.setErrorHandler(errors);
    try {
      validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
      var reader = SecureXmlParsers.newSAXParser(true).getXMLReader();
      validator.validate(new SAXSource(reader, new InputSource(new ByteArrayInputStream(xml))));
    } catch (SAXParseException e) {
      errors.add(e); // fatal, already recorded unless the limit was hit
    } catch (SAXException | ParserConfigurationException e) {
      throw new PageReadException(-1, -1, "Cannot validate: " + e.getMessage(), e);
    } catch (IOException e) {
      throw new PageReadException(-1, -1, "Cannot read: " + e.getMessage(), e);
    }
    if (!errors.messages.isEmpty()) {
      SAXParseException first = errors.first;
      throw new PageReadException(
          first == null ? -1 : first.getLineNumber(),
          first == null ? -1 : first.getColumnNumber(),
          errors.messages,
          first);
    }
  }

  private static final class Errors implements ErrorHandler {
    final List<String> messages = new ArrayList<>();
    @Nullable SAXParseException first;
    private @Nullable SAXParseException last;

    void add(SAXParseException e) {
      if (e == last || messages.size() >= MAX_ERRORS) {
        return;
      }
      last = e;
      if (first == null) {
        first = e;
      }
      messages.add(
          PageReadException.position(e.getLineNumber(), e.getColumnNumber()) + e.getMessage());
    }

    @Override
    public void warning(SAXParseException e) {}

    @Override
    public void error(SAXParseException e) {
      add(e);
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXParseException {
      add(e);
      throw e;
    }
  }
}
