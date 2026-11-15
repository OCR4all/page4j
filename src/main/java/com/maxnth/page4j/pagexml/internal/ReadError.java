package com.maxnth.page4j.pagexml.internal;

import com.maxnth.page4j.pagexml.PageReadException;
import java.io.Serial;

/** An error while building the model, turned into a {@link PageReadException} by the reader. */
final class ReadError extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  private final int line;
  private final int column;
  private final String error;

  ReadError(int line, int column, String error) {
    super(error);
    this.line = line;
    this.column = column;
    this.error = error;
  }

  PageReadException toException() {
    return new PageReadException(line, column, error, this);
  }
}
