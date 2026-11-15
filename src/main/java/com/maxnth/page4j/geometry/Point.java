package com.maxnth.page4j.geometry;

/**
 * Immutable point with integer pixel coordinates.
 *
 * @param x Horizontal position
 * @param y Vertical position
 */
public record Point(int x, int y) {

  /** Returns the Euclidean distance to the point ({@code xOther}, {@code yOther}). */
  public double distanceTo(int xOther, int yOther) {
    return Math.hypot(xOther - x, yOther - y);
  }

  /** Returns the Euclidean distance to the given point. */
  public double distanceTo(Point other) {
    return distanceTo(other.x, other.y);
  }

  /**
   * Returns the point on the line segment from {@code a} to {@code b} that is closest to this
   * point, rounded to integer coordinates.
   */
  public Point closestPointOnSegment(Point a, Point b) {
    int xDiff = b.x - a.x;
    int yDiff = b.y - a.y;
    double lengthSquared = (double) xDiff * xDiff + (double) yDiff * yDiff;
    if (lengthSquared == 0.0) return a;
    double u = ((double) (x - a.x) * xDiff + (double) (y - a.y) * yDiff) / lengthSquared;
    if (u <= 0.0) return a;
    if (u >= 1.0) return b;
    return new Point((int) Math.round(a.x + u * xDiff), (int) Math.round(a.y + u * yDiff));
  }

  /** Returns the distance to the line segment from {@code a} to {@code b}. */
  public double distanceToSegment(Point a, Point b) {
    return distanceTo(closestPointOnSegment(a, b));
  }
}
