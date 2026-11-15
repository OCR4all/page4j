package com.maxnth.page4j.formats.internal;

import com.maxnth.page4j.formats.ReadDiagnostic;
import com.maxnth.page4j.formats.ReadDiagnostic.Kind;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Collects diagnostics; the same message is reported once, with its count and first position. They
 * are listed in document order, whenever during the element they were found.
 */
public final class Diagnostics {

  private static final class Entry {
    final Kind kind;
    final int line;
    final int column;
    final String message;
    int count = 1;

    Entry(Kind kind, int line, int column, String message) {
      this.kind = kind;
      this.line = line;
      this.column = column;
      this.message = message;
    }
  }

  private final Map<String, Entry> entries = new LinkedHashMap<>();

  public void add(Kind kind, int line, int column, String message) {
    Entry entry = entries.get(kind + " " + message);
    if (entry == null) {
      entries.put(kind + " " + message, new Entry(kind, line, column, message));
    } else {
      entry.count++;
    }
  }

  public List<ReadDiagnostic> list() {
    List<ReadDiagnostic> list = new ArrayList<>(entries.size());
    for (Entry e : entries.values()) {
      String message = e.count == 1 ? e.message : e.message + " (" + e.count + " times)";
      list.add(new ReadDiagnostic(e.kind, e.line, e.column, message));
    }
    list.sort(
        Comparator.comparingInt(ReadDiagnostic::line).thenComparingInt(ReadDiagnostic::column));
    return list;
  }
}
