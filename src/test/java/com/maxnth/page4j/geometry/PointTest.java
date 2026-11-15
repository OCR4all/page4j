package com.maxnth.page4j.geometry;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PointTest {

  @Test
  void hasValueSemantics() {
    assertEquals(new Point(3, 4), new Point(3, 4));
    assertEquals(new Point(3, 4).hashCode(), new Point(3, 4).hashCode());
    assertNotEquals(new Point(3, 4), new Point(4, 3));
  }

  @Test
  void calculatesDistanceToOtherPoint() {
    Point p = new Point(10, 20);
    assertEquals(20.0, p.distanceTo(30, 20), 1e-9);
    assertEquals(5.0, new Point(0, 0).distanceTo(new Point(3, 4)), 1e-9);
  }

  @Test
  void findsClosestPointOnSegment() {
    Point p = new Point(10, 20);
    Point a = new Point(0, 40);
    Point b = new Point(100, 40);
    assertEquals(new Point(10, 40), p.closestPointOnSegment(a, b));
    assertEquals(20.0, p.distanceToSegment(a, b), 1e-9);
  }

  @Test
  void clampsToSegmentEnds() {
    Point a = new Point(0, 0);
    Point b = new Point(10, 0);
    assertEquals(a, new Point(-5, 3).closestPointOnSegment(a, b));
    assertEquals(b, new Point(15, 3).closestPointOnSegment(a, b));
    assertEquals(a, new Point(5, 5).closestPointOnSegment(a, a));
  }
}
