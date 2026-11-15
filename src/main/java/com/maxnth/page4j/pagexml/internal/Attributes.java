package com.maxnth.page4j.pagexml.internal;

import com.maxnth.page4j.geometry.Point;
import com.maxnth.page4j.geometry.Polygon;
import com.maxnth.page4j.model.value.PageDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamReader;
import org.jspecify.annotations.Nullable;

/**
 * The attributes of one element, parsed into model values. Records which attributes were read, so
 * {@link #done()} can fail if the reader missed one; content must never be dropped silently.
 */
final class Attributes {

  private final String element;
  private final int line;
  private final int column;
  private final Map<String, String> values = new LinkedHashMap<>();
  private final Set<String> read = new HashSet<>();

  private Attributes(XMLStreamReader reader) {
    element = reader.getLocalName();
    Location location = reader.getLocation();
    line = location.getLineNumber();
    column = location.getColumnNumber();
    for (int i = 0; i < reader.getAttributeCount(); i++) {
      String namespace = reader.getAttributeNamespace(i);
      // xsi:schemaLocation and the like belong to the XML, not the page.
      if (namespace == null || namespace.isEmpty()) {
        values.put(reader.getAttributeLocalName(i), reader.getAttributeValue(i));
      }
    }
  }

  /** The attributes of the element at the reader's position (a start tag). */
  static Attributes of(XMLStreamReader reader) {
    return new Attributes(reader);
  }

  String element() {
    return element;
  }

  /** An error at this element's position. */
  ReadError error(String message) {
    return new ReadError(line, column, message);
  }

  @Nullable String string(String name) {
    read.add(name);
    return values.get(name);
  }

  String requiredString(String name) {
    String value = string(name);
    if (value == null) {
      throw error(element + " has no '" + name + "' attribute");
    }
    return value;
  }

  /** An {@code xsd:float} or {@code xsd:double}. */
  @Nullable Double decimal(String name) {
    return parse(name, Attributes::parseDecimal);
  }

  /** An {@code xsd:int} or {@code xsd:integer}. */
  @Nullable Integer integer(String name) {
    return parse(name, v -> Integer.valueOf(v.strip()));
  }

  int requiredInteger(String name) {
    requiredString(name);
    Integer value = integer(name);
    return value == null ? 0 : value;
  }

  @Nullable Boolean bool(String name) {
    return parse(name, Attributes::parseBoolean);
  }

  @Nullable PageDateTime dateTime(String name) {
    return parse(name, v -> PageDateTime.parse(v.strip()));
  }

  /** A value of an enumeration, parsed with its {@code fromXml}. */
  <T> @Nullable T value(String name, Function<String, T> fromXml) {
    return parse(name, fromXml);
  }

  /** A required {@code points} attribute, with the optional {@code conf} attribute. */
  Polygon polygon() {
    return new Polygon(points(requiredString("points")), decimal("conf"));
  }

  /** Fails if the element has an attribute that was not read. */
  void done() {
    if (!read.containsAll(values.keySet())) {
      List<String> unread = new ArrayList<>(values.keySet());
      unread.removeAll(read);
      throw error(element + " has attributes the reader does not support: " + unread);
    }
  }

  private <T> @Nullable T parse(String name, Function<String, T> parser) {
    String value = string(name);
    if (value == null) {
      return null;
    }
    try {
      return parser.apply(value);
    } catch (RuntimeException e) {
      throw error("Invalid value '" + value + "' of " + element + "/@" + name);
    }
  }

  static List<Point> points(String text) {
    String[] pairs = text.strip().split("\\s+");
    List<Point> points = new ArrayList<>(pairs.length);
    for (String pair : pairs) {
      int comma = pair.indexOf(',');
      if (comma < 0) {
        throw new IllegalArgumentException("Not a point: " + pair);
      }
      points.add(
          new Point(
              Integer.parseInt(pair.substring(0, comma)),
              Integer.parseInt(pair.substring(comma + 1))));
    }
    return points;
  }

  private static Double parseDecimal(String value) {
    return switch (value.strip()) {
      case "INF", "+INF" -> Double.POSITIVE_INFINITY;
      case "-INF" -> Double.NEGATIVE_INFINITY;
      case "NaN" -> Double.NaN;
      default -> Double.valueOf(value.strip());
    };
  }

  private static Boolean parseBoolean(String value) {
    return switch (value.strip()) {
      case "true", "1" -> true;
      case "false", "0" -> false;
      default -> throw new IllegalArgumentException("Not a boolean: " + value);
    };
  }
}
