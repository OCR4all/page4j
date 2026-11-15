package com.maxnth.page4j.geometry;

/**
 * Immutable axis-aligned rectangle with inclusive pixel bounds.
 *
 * @param left Leftmost x coordinate
 * @param top Topmost y coordinate
 * @param right Rightmost x coordinate (inclusive)
 * @param bottom Bottommost y coordinate (inclusive)
 */
public record Rect(int left, int top, int right, int bottom) {

  /** Returns whether the point lies within the rectangle, including its border. */
  public boolean contains(int x, int y) {
    return x >= left && x <= right && y >= top && y <= bottom;
  }

  /** Returns the width in pixels, counting both border columns. */
  public int width() {
    return right - left + 1;
  }

  /** Returns the height in pixels, counting both border rows. */
  public int height() {
    return bottom - top + 1;
  }

  /** Returns the center point, rounded towards the top left. */
  public Point center() {
    return new Point((left + right) / 2, (top + bottom) / 2);
  }

  /** Returns the rectangle as a four-point polygon, clockwise from the top left corner. */
  public Polygon toPolygon() {
    return Polygon.of(
        new Point(left, top),
        new Point(right, top),
        new Point(right, bottom),
        new Point(left, bottom));
  }
}
