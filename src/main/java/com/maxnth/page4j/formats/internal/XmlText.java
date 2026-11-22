package com.maxnth.page4j.formats.internal;

import java.util.Locale;

final class XmlText {

  private XmlText() {}

  static String checked(String format, String what, String text) {
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
        throw new IllegalArgumentException(
            "Cannot write "
                + format
                + ": "
                + what
                + " contains the character U+"
                + String.format(Locale.ROOT, "%04X", c)
                + ", which XML can't hold");
      }
      i += Character.charCount(c);
    }
    return text;
  }
}
