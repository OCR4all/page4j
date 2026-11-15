package com.maxnth.page4j.model.value;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * An {@code xsd:dateTime} value, such as the {@code Created} and {@code LastChange} timestamps of
 * the page metadata.
 *
 * <p>Keeps the text it was parsed from, so a timestamp is written back exactly as it was read: with
 * or without an offset, with {@code Z} or {@code +00:00}, with its fraction digits. Two values are
 * equal when their text is equal; compare {@link #toInstant(ZoneOffset)} to compare points in time.
 */
public final class PageDateTime implements XmlValue {

  // xsd:dateTime: -?yyyy-mm-ddThh:mm:ss(.s+)?(Z|(+|-)hh:mm)?
  private static final Pattern LEXICAL =
      Pattern.compile(
          "(-?(?:[1-9]\\d{4,}|\\d{4}))-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2}):(\\d{2})(?:\\.(\\d+))?"
              + "(Z|[+-]\\d{2}:\\d{2})?");

  private final String lexical;
  private final LocalDateTime localDateTime;
  private final @Nullable ZoneOffset offset;

  private PageDateTime(String lexical, LocalDateTime localDateTime, @Nullable ZoneOffset offset) {
    this.lexical = lexical;
    this.localDateTime = localDateTime;
    this.offset = offset;
  }

  /**
   * Parses an {@code xsd:dateTime}.
   *
   * @throws IllegalArgumentException if the text is not a valid {@code xsd:dateTime}
   */
  public static PageDateTime parse(String text) {
    Matcher m = LEXICAL.matcher(text);
    if (!m.matches()) {
      throw new IllegalArgumentException("Not an xsd:dateTime: '" + text + "'");
    }
    try {
      int hour = Integer.parseInt(m.group(4));
      int minute = Integer.parseInt(m.group(5));
      int second = Integer.parseInt(m.group(6));
      String fraction = m.group(7);
      int nanos = fraction == null ? 0 : nanos(fraction);
      boolean endOfDay = hour == 24;
      if (endOfDay && (minute != 0 || second != 0 || nanos != 0)) {
        throw new DateTimeException("24:00:00 is the only time with hour 24");
      }
      LocalDateTime dateTime =
          LocalDateTime.of(
              Integer.parseInt(m.group(1)),
              Integer.parseInt(m.group(2)),
              Integer.parseInt(m.group(3)),
              endOfDay ? 0 : hour,
              minute,
              second,
              nanos);
      return new PageDateTime(text, endOfDay ? dateTime.plusDays(1) : dateTime, offset(m.group(8)));
    } catch (DateTimeException | NumberFormatException e) {
      throw new IllegalArgumentException("Not an xsd:dateTime: '" + text + "'", e);
    }
  }

  /** A UTC timestamp, written with {@code Z}, for example {@code 2024-01-02T03:04:05.123Z}. */
  public static PageDateTime of(Instant instant) {
    return parse(DateTimeFormatter.ISO_INSTANT.format(instant));
  }

  /** A timestamp with an offset, for example {@code 2024-01-02T03:04:05+02:00}. */
  public static PageDateTime of(OffsetDateTime dateTime) {
    return parse(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(dateTime));
  }

  /** A timestamp without an offset, for example {@code 2024-01-02T03:04:05}. */
  public static PageDateTime of(LocalDateTime dateTime) {
    return parse(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(dateTime));
  }

  /** The current time in UTC. */
  public static PageDateTime now() {
    return of(Instant.now());
  }

  /** The date and time without the offset. {@code 24:00:00} is midnight of the next day. */
  public LocalDateTime localDateTime() {
    return localDateTime;
  }

  /** The offset, or {@code null} if the timestamp has none. */
  public @Nullable ZoneOffset offset() {
    return offset;
  }

  /**
   * The point in time.
   *
   * @param assumedIfMissing the offset to use if the timestamp has none
   */
  public Instant toInstant(ZoneOffset assumedIfMissing) {
    return localDateTime.toInstant(offset != null ? offset : assumedIfMissing);
  }

  /** The text this value was parsed from. */
  @Override
  public String xmlValue() {
    return lexical;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof PageDateTime other && lexical.equals(other.lexical);
  }

  @Override
  public int hashCode() {
    return lexical.hashCode();
  }

  @Override
  public String toString() {
    return lexical;
  }

  private static int nanos(String fraction) {
    String digits = fraction.length() > 9 ? fraction.substring(0, 9) : fraction;
    return Integer.parseInt(digits + "0".repeat(9 - digits.length()));
  }

  private static @Nullable ZoneOffset offset(@Nullable String text) {
    if (text == null) {
      return null;
    }
    if (text.equals("Z")) {
      return ZoneOffset.UTC;
    }
    int sign = text.charAt(0) == '-' ? -1 : 1;
    int hours = Integer.parseInt(text.substring(1, 3));
    int minutes = Integer.parseInt(text.substring(4, 6));
    if (hours > 14 || minutes > 59 || (hours == 14 && minutes != 0)) {
      throw new DateTimeException("Offset out of range: " + text);
    }
    return ZoneOffset.ofHoursMinutes(sign * hours, sign * minutes);
  }
}
