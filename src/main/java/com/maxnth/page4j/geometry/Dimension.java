package com.maxnth.page4j.geometry;

/**
 * Immutable width and height in pixels.
 *
 * @param width Width
 * @param height Height
 */
public record Dimension(int width, int height) {

  /** A dimension of zero width and height. */
  public static final Dimension ZERO = new Dimension(0, 0);
}
