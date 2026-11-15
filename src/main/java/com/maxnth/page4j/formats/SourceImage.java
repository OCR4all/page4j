package com.maxnth.page4j.formats;

/**
 * The image an ALTO document describes, used to scale coordinates given in a physical unit to
 * pixels.
 *
 * @param width width in pixels
 * @param height height in pixels
 * @param xResolution horizontal resolution in pixels per inch
 * @param yResolution vertical resolution in pixels per inch
 */
public record SourceImage(int width, int height, double xResolution, double yResolution) {

  public SourceImage {
    if (width <= 0 || height <= 0) {
      throw new IllegalArgumentException("Image size must be positive: " + width + "x" + height);
    }
  }
}
