package com.maxnth.page4j.model;

import java.util.List;

/** The grid of a table ({@code Grid}). */
@PageRecord
public record Grid(List<GridPoints> gridPoints, List<GridAddPoints> addPoints)
    implements GridBuilder.With {
  public Grid {
    gridPoints = List.copyOf(gridPoints);
    addPoints = List.copyOf(addPoints);
  }
}
