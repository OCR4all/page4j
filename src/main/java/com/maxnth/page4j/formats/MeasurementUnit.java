package com.maxnth.page4j.formats;

import java.util.Optional;

/** The unit of the coordinates of an ALTO document ({@code MeasurementUnit}). */
public enum MeasurementUnit {
  /** Pixels of the image. */
  PIXEL("pixel", 0),
  /** Tenths of a millimetre. */
  MM10("mm10", 254),
  /** 1200ths of an inch. */
  INCH1200("inch1200", 1200);

  private final String xmlValue;
  private final double perInch;

  MeasurementUnit(String xmlValue, double perInch) {
    this.xmlValue = xmlValue;
    this.perInch = perInch;
  }

  /** The value in ALTO, for example {@code mm10}. */
  public String xmlValue() {
    return xmlValue;
  }

  /** Units per inch, or 0 for pixels, whose size depends on the image. */
  public double perInch() {
    return perInch;
  }

  /** The unit with the given ALTO value. */
  public static Optional<MeasurementUnit> fromXml(String xmlValue) {
    for (MeasurementUnit unit : values()) {
      if (unit.xmlValue.equals(xmlValue.strip())) {
        return Optional.of(unit);
      }
    }
    return Optional.empty();
  }
}
