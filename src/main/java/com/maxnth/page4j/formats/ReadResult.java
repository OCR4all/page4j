package com.maxnth.page4j.formats;

import com.maxnth.page4j.model.Page;
import java.util.List;
import java.util.Objects;

/**
 * A page read from FineReader or hOCR, with what the reader left out or changed.
 *
 * @param page the page
 * @param diagnostics what the reader left out or changed, in document order
 */
public record ReadResult(Page page, List<ReadDiagnostic> diagnostics) {

  public ReadResult {
    Objects.requireNonNull(page, "page");
    diagnostics = List.copyOf(diagnostics);
  }
}
