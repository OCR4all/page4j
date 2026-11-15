package com.maxnth.page4j.formats;

import com.maxnth.page4j.model.Page;
import java.util.List;
import java.util.Objects;

/**
 * A page read from ALTO, with what the reader left out or changed.
 *
 * @param page the page
 * @param measurementUnit the unit of the document's coordinates; the page's coordinates are in this
 *     unit unless the document was read with its {@link SourceImage}, which scales them to pixels
 * @param diagnostics what the reader left out or changed, in document order
 */
public record AltoReadResult(
    Page page, MeasurementUnit measurementUnit, List<ReadDiagnostic> diagnostics) {

  public AltoReadResult {
    Objects.requireNonNull(page, "page");
    Objects.requireNonNull(measurementUnit, "measurementUnit");
    diagnostics = List.copyOf(diagnostics);
  }
}
