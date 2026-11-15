package com.maxnth.page4j.formats;

import com.maxnth.page4j.pagexml.PageReadException;
import java.util.Objects;

/**
 * Something a reader left out or changed while mapping a document to the PAGE model. A read with
 * diagnostics still succeeds; the page then differs from the document as described.
 *
 * @param kind what happened
 * @param line line of the first occurrence in the document (1-based), or -1 if unknown
 * @param column column of the first occurrence (1-based), or -1 if unknown
 * @param message what was left out or changed, and how often if more than once
 */
public record ReadDiagnostic(Kind kind, int line, int column, String message) {

  /** What a reader did. */
  public enum Kind {
    /** Content that has no place in the model, or that could not be read, was left out. */
    IGNORED,
    /** A value was replaced, for example an invalid ID or an unknown language code. */
    CHANGED
  }

  public ReadDiagnostic {
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(message, "message");
  }

  /** For example {@code "12:5: IGNORED String: attribute CC (3 times)"}. */
  @Override
  public String toString() {
    return PageReadException.position(line, column) + kind + " " + message;
  }
}
