package com.maxnth.page4j.pagexml.internal;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A minimal XML serializer for {@link PageXmlWriter}. Unlike the JDK's {@code XMLStreamWriter} it
 * writes tab, line feed and carriage return in attribute values, and carriage return in text, as
 * character references, so a reader gets them back instead of normalizing them to spaces or line
 * feeds. Callers must pass only characters XML can hold (see {@link #firstInvalid}).
 */
final class XmlOut {

  private final StringBuilder out = new StringBuilder(8192);
  private final Deque<String> open = new ArrayDeque<>();
  private boolean tagOpen;
  private boolean empty;

  /** The first code point XML 1.0 can't hold, or -1. A lone surrogate is returned as itself. */
  static int firstInvalid(String text) {
    for (int i = 0; i < text.length(); ) {
      int c = text.codePointAt(i);
      boolean valid =
          c == 0x9
              || c == 0xA
              || c == 0xD
              || (c >= 0x20 && c <= 0xD7FF)
              || (c >= 0xE000 && c <= 0xFFFD)
              || c >= 0x10000;
      if (!valid) {
        return c;
      }
      i += Character.charCount(c);
    }
    return -1;
  }

  void declaration() {
    out.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
  }

  void startElement(String name) {
    closeTag();
    out.append('<').append(name);
    open.push(name);
    tagOpen = true;
  }

  /** An element without content; attributes may follow. */
  void emptyElement(String name) {
    closeTag();
    out.append('<').append(name);
    tagOpen = true;
    empty = true;
  }

  /** An attribute of the element whose start tag was just written. */
  void attribute(String name, String value) {
    if (!tagOpen) {
      throw new IllegalStateException("No open start tag for attribute " + name);
    }
    out.append(' ').append(name).append("=\"");
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch (c) {
        case '&' -> out.append("&amp;");
        case '<' -> out.append("&lt;");
        case '>' -> out.append("&gt;");
        case '"' -> out.append("&quot;");
        case '\t' -> out.append("&#9;");
        case '\n' -> out.append("&#10;");
        case '\r' -> out.append("&#13;");
        default -> out.append(c);
      }
    }
    out.append('"');
  }

  void characters(String text) {
    closeTag();
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      switch (c) {
        case '&' -> out.append("&amp;");
        case '<' -> out.append("&lt;");
        case '>' -> out.append("&gt;");
        case '\r' -> out.append("&#13;");
        default -> out.append(c);
      }
    }
  }

  void endElement() {
    closeTag();
    out.append("</").append(open.pop()).append('>');
  }

  byte[] toBytes() {
    closeTag();
    if (!open.isEmpty()) {
      throw new IllegalStateException("Unclosed elements: " + open);
    }
    return out.toString().getBytes(StandardCharsets.UTF_8);
  }

  private void closeTag() {
    if (tagOpen) {
      out.append(empty ? "/>" : ">");
      tagOpen = false;
      empty = false;
    }
  }
}
