package com.maxnth.page4j.model.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PageDateTimeTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "2020-01-02T03:04:05",
        "2020-01-02T03:04:05Z",
        "2020-01-02T03:04:05+00:00",
        "2020-01-02T03:04:05.120-05:30",
        "2020-01-02T03:04:05.1234567891234",
        "12020-01-02T03:04:05",
        "-0044-03-15T12:00:00",
        "2020-01-02T24:00:00"
      })
  void keepsTheTextItWasParsedFrom(String text) {
    assertEquals(text, PageDateTime.parse(text).xmlValue());
    assertEquals(text, PageDateTime.parse(text).toString());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "2020-01-02",
        "2020-01-02 03:04:05",
        "2020-1-02T03:04:05",
        "2020-02-30T03:04:05",
        "2020-01-02T24:00:01",
        "2020-01-02T03:04:05+15:00",
        "2020-01-02T03:04:05.Z",
        "02020-01-02T03:04:05"
      })
  void rejectsInvalidText(String text) {
    assertThrows(IllegalArgumentException.class, () -> PageDateTime.parse(text));
  }

  @Test
  void exposesDateTimeAndOffset() {
    PageDateTime local = PageDateTime.parse("2020-01-02T03:04:05.5");
    assertEquals(LocalDateTime.of(2020, 1, 2, 3, 4, 5, 500_000_000), local.localDateTime());
    assertNull(local.offset());
    assertEquals(Instant.parse("2020-01-02T01:04:05.5Z"), local.toInstant(ZoneOffset.ofHours(2)));

    PageDateTime offset = PageDateTime.parse("2020-01-02T03:04:05-01:30");
    assertEquals(ZoneOffset.ofHoursMinutes(-1, -30), offset.offset());
    assertEquals(Instant.parse("2020-01-02T04:34:05Z"), offset.toInstant(ZoneOffset.UTC));

    assertEquals(
        LocalDateTime.of(2020, 1, 3, 0, 0),
        PageDateTime.parse("2020-01-02T24:00:00").localDateTime());
  }

  @Test
  void factoriesWriteTheMatchingForm() {
    assertEquals(
        "2020-01-02T03:04:05Z", PageDateTime.of(Instant.parse("2020-01-02T03:04:05Z")).xmlValue());
    assertEquals(
        "2020-01-02T03:04:05+02:00",
        PageDateTime.of(OffsetDateTime.parse("2020-01-02T03:04:05+02:00")).xmlValue());
    assertEquals(
        "2020-01-02T03:04:05", PageDateTime.of(LocalDateTime.of(2020, 1, 2, 3, 4, 5)).xmlValue());
  }

  @Test
  void equalityIsByText() {
    assertEquals(
        PageDateTime.parse("2020-01-02T03:04:05Z"), PageDateTime.parse("2020-01-02T03:04:05Z"));
    assertNotEquals(
        PageDateTime.parse("2020-01-02T03:04:05Z"),
        PageDateTime.parse("2020-01-02T03:04:05+00:00"));
  }
}
