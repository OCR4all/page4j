package com.maxnth.page4j.model;

import com.maxnth.page4j.geometry.Polygon;
import java.util.Objects;

/** One row of grid points of a table ({@code GridPoints}). */
@PageRecord
public record GridPoints(int index, Polygon points) implements GridPointsBuilder.With {
  public GridPoints {
    Objects.requireNonNull(points, "points");
  }
}
