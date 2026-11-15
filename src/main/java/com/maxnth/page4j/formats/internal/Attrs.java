package com.maxnth.page4j.formats.internal;

import com.maxnth.page4j.formats.ReadDiagnostic.Kind;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamReader;
import org.jspecify.annotations.Nullable;

/**
 * The attributes of one element, read leniently: a value that can't be parsed is reported and
 * treated as absent, and {@link #done()} reports the attributes nobody read.
 */
public final class Attrs {

  private final String element;
  private final int line;
  private final int column;
  private final Diagnostics diagnostics;
  private final Map<String, String> values = new LinkedHashMap<>();
  private final Set<String> read = new HashSet<>();

  public Attrs(XMLStreamReader reader, Diagnostics diagnostics) {
    this.element = reader.getLocalName();
    Location location = reader.getLocation();
    this.line = location.getLineNumber();
    this.column = location.getColumnNumber();
    this.diagnostics = diagnostics;
    for (int i = 0; i < reader.getAttributeCount(); i++) {
      String namespace = reader.getAttributeNamespace(i);
      if (XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI.equals(namespace)) {
        continue; // xsi:schemaLocation and the like belong to the XML, not the content
      }
      String prefix = reader.getAttributePrefix(i);
      String name = reader.getAttributeLocalName(i);
      values.put(
          prefix == null || prefix.isEmpty() ? name : prefix + ":" + name,
          reader.getAttributeValue(i));
    }
  }

  public String element() {
    return element;
  }

  public int line() {
    return line;
  }

  public int column() {
    return column;
  }

  public void ignored(String message) {
    diagnostics.add(Kind.IGNORED, line, column, message);
  }

  public void changed(String message) {
    diagnostics.add(Kind.CHANGED, line, column, message);
  }

  public @Nullable String string(String name) {
    read.add(name);
    return values.get(name);
  }

  /** A number; one that can't be parsed is reported and treated as absent. */
  public @Nullable Double decimal(String name) {
    String value = string(name);
    if (value == null) {
      return null;
    }
    try {
      double number = Double.parseDouble(value.strip());
      if (Double.isFinite(number)) {
        return number;
      }
    } catch (NumberFormatException e) {
      // reported below
    }
    ignored(element + ": " + name + " '" + value + "' is not a number");
    return null;
  }

  /** A number truncated to an integer, or 0 if absent or invalid. */
  public int coordinate(String name) {
    Double value = decimal(name);
    return value == null ? 0 : (int) value.doubleValue();
  }

  /** Reports the attributes that were not read. */
  public void done() {
    for (String name : values.keySet()) {
      if (!read.contains(name)) {
        ignored(element + ": attribute " + name);
      }
    }
  }
}
