package com.maxnth.page4j.geometry;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PolygonTest {
  private static final Polygon SQUARE = new Rect(0, 0, 10, 10).toPolygon();

  @Test
  void buildsPointsInOrder() {
    Polygon polygon = Polygon.builder().add(0, 0).add(10, 0).add(new Point(10, 10)).build();
    assertEquals(List.of(new Point(0, 0), new Point(10, 0), new Point(10, 10)), polygon.points());
    assertEquals(3, polygon.size());
    assertNull(polygon.confidence());
  }

  @Test
  void rectangleTracesCornersClockwise() {
    assertEquals(
        List.of(new Point(0, 0), new Point(10, 0), new Point(10, 10), new Point(0, 10)),
        SQUARE.points());
  }

  @Test
  void computesBoundingBox() {
    Polygon polygon =
        Polygon.of(new Point(5, 5), new Point(15, 5), new Point(15, 20), new Point(5, 20));
    assertEquals(new Rect(5, 5, 15, 20), polygon.boundingBox());
    assertEquals(11, polygon.boundingBox().width());
    assertEquals(16, polygon.boundingBox().height());
    assertFalse(Polygon.EMPTY.boundingBox().contains(0, 0));
  }

  @Test
  void containsUsesEvenOddRule() {
    assertTrue(SQUARE.contains(5, 5));
    assertFalse(SQUARE.contains(15, 15));
    assertFalse(Polygon.EMPTY.contains(0, 0));
  }

  @Test
  void isImmutable() {
    var points = new ArrayList<>(List.of(new Point(0, 0), new Point(1, 1)));
    Polygon polygon = new Polygon(points);
    points.add(new Point(2, 2));
    assertEquals(2, polygon.size());
    assertThrows(UnsupportedOperationException.class, () -> polygon.points().add(new Point(3, 3)));
    assertThrows(NullPointerException.class, () -> Polygon.of(new Point(0, 0), null));
  }

  @Test
  void hasValueSemanticsIncludingConfidence() {
    assertEquals(new Rect(0, 0, 10, 10).toPolygon(), SQUARE);
    assertNotEquals(SQUARE, SQUARE.withConfidence(0.5));
    assertEquals(0.5, SQUARE.withConfidence(0.5).confidence());
  }

  @Test
  void derivesModifiedCopies() {
    Polygon moved = SQUARE.withConfidence(0.8).withPoint(1, new Point(20, 0));
    assertEquals(new Point(20, 0), moved.points().get(1));
    assertEquals(new Point(10, 0), SQUARE.points().get(1));
    assertEquals(0.8, moved.confidence());

    Polygon scaled = moved.map(p -> new Point(p.x() * 2, p.y() * 2));
    assertEquals(new Rect(0, 0, 40, 20), scaled.boundingBox());
    assertEquals(0.8, scaled.confidence());
  }

  @Test
  void calculatesPerimeter() {
    assertEquals(40.0, SQUARE.perimeter(), 1e-9);
    assertEquals(0.0, Polygon.of(new Point(1, 1)).perimeter());
  }
}
