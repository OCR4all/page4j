package com.maxnth.page4j.formats.internal;

import com.maxnth.page4j.pagexml.PageReadException;
import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.jspecify.annotations.Nullable;

/** StAX input for the lenient readers. External entities and DTDs are never loaded. */
public final class XmlInput {

  private static final XMLInputFactory FACTORY = newFactory();

  private XmlInput() {}

  /** The root element of a document. */
  public record Root(@Nullable String namespace, String name) {}

  /** Thrown inside the readers and turned into a {@link PageReadException}. */
  public static final class Failure extends RuntimeException {
    private static final long serialVersionUID = 1L;

    final int line;
    final int column;

    Failure(int line, int column, String message) {
      super(message);
      this.line = line;
      this.column = column;
    }

    public PageReadException toException() {
      return new PageReadException(line, column, String.valueOf(getMessage()), this);
    }
  }

  /** A reader, positioned at the start tag of the root element. */
  public static XMLStreamReader open(byte[] xml) throws PageReadException {
    try {
      XMLStreamReader reader = FACTORY.createXMLStreamReader(new ByteArrayInputStream(xml));
      while (reader.next() != XMLStreamConstants.START_ELEMENT) {
        // skip the prolog
      }
      return reader;
    } catch (XMLStreamException e) {
      throw notWellFormed(e);
    }
  }

  /** The root element of a document, which is otherwise not read. */
  public static Root root(byte[] xml) throws PageReadException {
    XMLStreamReader reader = open(xml);
    try {
      String namespace = reader.getNamespaceURI();
      return new Root(
          namespace == null || namespace.isEmpty() ? null : namespace, reader.getLocalName());
    } finally {
      close(reader);
    }
  }

  /** Fails unless the root element has the given name. */
  public static void requireRoot(XMLStreamReader reader, String name, String format) {
    if (!name.equals(reader.getLocalName())) {
      throw failure(reader, "Not " + format + ": the root element is " + reader.getName());
    }
  }

  public static PageReadException notWellFormed(XMLStreamException e) {
    Location location = e.getLocation();
    return new PageReadException(
        location == null ? -1 : location.getLineNumber(),
        location == null ? -1 : location.getColumnNumber(),
        "Not well-formed XML: " + e.getMessage(),
        e);
  }

  public static Failure failure(XMLStreamReader reader, String message) {
    Location location = reader.getLocation();
    return new Failure(location.getLineNumber(), location.getColumnNumber(), message);
  }

  public static void close(XMLStreamReader reader) {
    try {
      reader.close();
    } catch (XMLStreamException e) {
      // Reading is done; nothing to release beyond the in-memory stream.
    }
  }

  /** Moves to the next child start tag, or to the end tag of the current element (false). */
  public static boolean nextChild(XMLStreamReader reader) throws XMLStreamException {
    while (true) {
      switch (reader.next()) {
        case XMLStreamConstants.START_ELEMENT -> {
          return true;
        }
        case XMLStreamConstants.END_ELEMENT -> {
          return false;
        }
        default -> {
          // text, comments, processing instructions
        }
      }
    }
  }

  /** Skips the current element with everything in it, leaving the reader at its end tag. */
  public static void skip(XMLStreamReader reader) throws XMLStreamException {
    int depth = 1;
    while (depth > 0) {
      int event = reader.next();
      if (event == XMLStreamConstants.START_ELEMENT) {
        depth++;
      } else if (event == XMLStreamConstants.END_ELEMENT) {
        depth--;
      }
    }
  }

  /** All text in the current element and its descendants, leaving the reader at its end tag. */
  public static String text(XMLStreamReader reader) throws XMLStreamException {
    var text = new StringBuilder();
    int depth = 1;
    while (depth > 0) {
      switch (reader.next()) {
        case XMLStreamConstants.START_ELEMENT -> depth++;
        case XMLStreamConstants.END_ELEMENT -> depth--;
        case XMLStreamConstants.CHARACTERS, XMLStreamConstants.CDATA, XMLStreamConstants.SPACE ->
            text.append(reader.getText());
        default -> {
          // comments, processing instructions
        }
      }
    }
    return text.toString();
  }

  /** The values of an attribute (without namespace) on all elements of a document. */
  public static Set<String> attributeValues(byte[] xml, String attribute) throws PageReadException {
    XMLStreamReader reader = open(xml);
    Set<String> values = new HashSet<>();
    try {
      int event = XMLStreamConstants.START_ELEMENT;
      while (true) {
        if (event == XMLStreamConstants.START_ELEMENT) {
          String value = reader.getAttributeValue(XMLConstants.NULL_NS_URI, attribute);
          if (value != null) {
            values.add(value);
          }
        }
        if (!reader.hasNext()) {
          return values;
        }
        event = reader.next();
      }
    } catch (XMLStreamException e) {
      throw notWellFormed(e);
    } finally {
      close(reader);
    }
  }

  private static XMLInputFactory newFactory() {
    XMLInputFactory factory = XMLInputFactory.newFactory();
    factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
    factory.setProperty(XMLInputFactory.IS_COALESCING, true);
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    return factory;
  }
}
