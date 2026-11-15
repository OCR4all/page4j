package com.maxnth.page4j.geometry;

/**
 * Immutable point with floating-point coordinates.
 *
 * @param x Horizontal position
 * @param y Vertical position
 */
public record PointD(double x, double y) {

  /** Returns the Euclidean distance to the point ({@code xOther}, {@code yOther}). */
  public double distanceTo(double xOther, double yOther) {
    return Math.hypot(xOther - x, yOther - y);
  }

  /** Returns the Euclidean distance to the given point. */
  public double distanceTo(PointD other) {
    return distanceTo(other.x, other.y);
  }

  /**
   * Returns the point on the line segment from {@code a} to {@code b} that is closest to this
   * point.
   */
  public PointD closestPointOnSegment(PointD a, PointD b) {
    double xDiff = b.x - a.x;
    double yDiff = b.y - a.y;
    double lengthSquared = xDiff * xDiff + yDiff * yDiff;
    if (lengthSquared == 0.0) return a;
    double u = ((x - a.x) * xDiff + (y - a.y) * yDiff) / lengthSquared;
    if (u <= 0.0) return a;
    if (u >= 1.0) return b;
    return new PointD(a.x + u * xDiff, a.y + u * yDiff);
  }

  /** Returns the distance to the line segment from {@code a} to {@code b}. */
  public double distanceToSegment(PointD a, PointD b) {
    return distanceTo(closestPointOnSegment(a, b));
  }
}
