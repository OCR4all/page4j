package com.maxnth.page4j.geometry;

import org.jspecify.annotations.Nullable;

public final class Geometry {
  private Geometry() {}

  /** Returns the intersection of the two lines, or {@code null} if they are parallel. */
  public static @Nullable Point lineIntersection(
      double ax1,
      double ay1,
      double ax2,
      double ay2,
      double bx1,
      double by1,
      double bx2,
      double by2) {
    double dividend = (bx2 - bx1) * (ay1 - by1) - (by2 - by1) * (ax1 - bx1);
    double divisor = (by2 - by1) * (ax2 - ax1) - (bx2 - bx1) * (ay2 - ay1);
    if (divisor == 0.0) return null;
    double ua = dividend / divisor;
    return new Point(
        (int) Math.round(ax1 + ua * (ax2 - ax1)), (int) Math.round(ay1 + ua * (ay2 - ay1)));
  }
}
