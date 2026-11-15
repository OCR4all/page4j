package com.maxnth.page4j.model;

import org.jspecify.annotations.Nullable;

/** Additional points of a table grid ({@code AddPoints}). */
@PageRecord
public record GridAddPoints(int row, int col, @Nullable String left, @Nullable String top)
    implements GridAddPointsBuilder.With {
  public GridAddPoints {}
}
