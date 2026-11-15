package com.maxnth.page4j.pagexml;

import java.io.IOException;
import java.io.Serial;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Thrown when a document can't be read: it is not well-formed, not a supported PAGE version, or not
 * valid against the schema of its version; for the other formats ({@code
 * com.maxnth.page4j.formats}), when it is not well-formed or not of the expected format.
 */
public final class PageReadException extends IOException {

  @Serial private static final long serialVersionUID = 1L;

  private final int line;
  private final int column;
  private final transient List<String> errors;

  /**
   * @param line the line of the (first) error, or -1 if unknown
   * @param column the column of the (first) error, or -1 if unknown
   * @param errors all errors, each with its position; the first is used as the message
   */
  public PageReadException(int line, int column, List<String> errors, @Nullable Throwable cause) {
    super(message(errors), cause);
    this.line = line;
    this.column = column;
    this.errors = List.copyOf(errors);
  }

  public PageReadException(int line, int column, String error, @Nullable Throwable cause) {
    this(line, column, List.of(position(line, column) + error), cause);
  }

  /** The line of the first error, or -1 if unknown. */
  public int line() {
    return line;
  }

  /** The column of the first error, or -1 if unknown. */
  public int column() {
    return column;
  }

  /** All errors, for example every schema violation, each starting with its position. */
  public List<String> errors() {
    return errors;
  }

  /** Formats a position as a message prefix, for example {@code "12:5: "}. */
  public static String position(int line, int column) {
    return line < 0 ? "" : column < 0 ? line + ": " : line + ":" + column + ": ";
  }

  private static String message(List<String> errors) {
    if (errors.isEmpty()) {
      return "Cannot read document";
    }
    return errors.size() == 1
        ? errors.getFirst()
        : errors.getFirst() + " (and " + (errors.size() - 1) + " more)";
  }
}
