package com.maxnth.page4j.convert;

import java.util.Objects;

/**
 * One change made by {@link PageConverter}.
 *
 * @param kind what kind of change it was
 * @param message what was changed, naming the object, for example {@code TextLine 'l1': removed
 *     attribute orientation, not supported in PAGE 2019-07-15}
 */
public record ConversionMessage(Kind kind, String message) {

  /** The kinds of changes. */
  public enum Kind {
    /** Content the target version can't hold was removed. */
    REMOVED,
    /** A value or region type was replaced with one the target version allows. */
    CHANGED,
    /** Content the target version requires was added. */
    ADDED
  }

  public ConversionMessage {
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(message, "message");
  }

  @Override
  public String toString() {
    return message;
  }
}
