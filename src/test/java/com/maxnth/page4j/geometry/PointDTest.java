package com.maxnth.page4j.geometry;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PointDTest {

  @Test
  void hasValueSemantics() {
    assertEquals(new PointD(1.5, 2.5), new PointD(1.5, 2.5));
    assertNotEquals(new PointD(1.5, 2.5), new PointD(2.5, 1.5));
  }

  @Test
  void calculatesDistances() {
    assertEquals(5.0, new PointD(0, 0).distanceTo(new PointD(3, 4)), 1e-9);
    assertEquals(2.0, new PointD(1, 1).distanceTo(1, 3), 1e-9);
  }

  @Test
  void findsClosestPointOnSegmentWithoutRounding() {
    PointD a = new PointD(0, 0);
    PointD b = new PointD(3, 0);
    assertEquals(new PointD(1.25, 0), new PointD(1.25, 2).closestPointOnSegment(a, b));
    assertEquals(2.0, new PointD(1.25, 2).distanceToSegment(a, b), 1e-9);
    assertEquals(a, new PointD(-1, 1).closestPointOnSegment(a, b));
    assertEquals(b, new PointD(4, 1).closestPointOnSegment(a, b));
    assertEquals(a, new PointD(1, 1).closestPointOnSegment(a, a));
  }
}
