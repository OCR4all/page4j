package com.maxnth.page4j.geometry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;
import org.jspecify.annotations.Nullable;

/**
 * Immutable polygon, given as a path of points, with an optional confidence.
 *
 * <p>Build polygons with {@link #of(Point...)}, {@link Rect#toPolygon()} or {@link #builder()}.
 * Methods such as {@link #withConfidence(Double)} return a modified copy; store it with the owner's
 * {@code setCoords(...)}.
 *
 * @param points The points, in path order (unmodifiable, without {@code null} elements)
 * @param confidence Confidence in [0, 1], or {@code null} if not set
 */
public record Polygon(List<Point> points, @Nullable Double confidence) {

  /** A polygon without points. */
  public static final Polygon EMPTY = new Polygon(List.of(), null);

  /** Creates a polygon, copying the point list. */
  public Polygon {
    points = List.copyOf(points);
  }

  /** Creates a polygon without confidence. */
  public Polygon(List<Point> points) {
    this(points, null);
  }

  /** Creates a polygon from the given points, without confidence. */
  public static Polygon of(Point... points) {
    return new Polygon(List.of(points));
  }

  /** Returns a builder for adding points one by one. */
  public static Builder builder() {
    return new Builder();
  }

  /** Returns the number of points. */
  public int size() {
    return points.size();
  }

  /** Returns whether the polygon has no points. */
  public boolean isEmpty() {
    return points.isEmpty();
  }

  /**
   * Returns the smallest rectangle containing all points. An empty polygon yields a rectangle with
   * {@code left > right} and {@code top > bottom}, which contains no point.
   */
  public Rect boundingBox() {
    int left = Integer.MAX_VALUE, top = Integer.MAX_VALUE;
    int right = Integer.MIN_VALUE, bottom = Integer.MIN_VALUE;
    for (Point p : points) {
      left = Math.min(left, p.x());
      top = Math.min(top, p.y());
      right = Math.max(right, p.x());
      bottom = Math.max(bottom, p.y());
    }
    return new Rect(left, top, right, bottom);
  }

  /** Returns whether the point lies inside the polygon (even-odd rule). */
  public boolean contains(int x, int y) {
    if (!boundingBox().contains(x, y)) return false;

    int crossings = 0;
    int n = points.size();
    for (int i = 0; i < n; i++) {
      Point p1 = points.get(i);
      Point p2 = points.get((i + 1) % n);
      if ((p1.y() <= y && p2.y() > y) || (p1.y() > y && p2.y() <= y)) {
        double vt = (double) (y - p1.y()) / (p2.y() - p1.y());
        if (x < p1.x() + vt * (p2.x() - p1.x())) crossings++;
      }
    }
    return (crossings & 1) != 0;
  }

  /** Returns the length of the closed outline. */
  public double perimeter() {
    if (points.size() <= 1) return 0.0;
    double length = 0.0;
    for (int i = 0; i < points.size(); i++) {
      length += points.get(i).distanceTo(points.get((i + 1) % points.size()));
    }
    return length;
  }

  /** Returns a copy with the given confidence. */
  public Polygon withConfidence(@Nullable Double confidence) {
    return new Polygon(points, confidence);
  }

  /** Returns a copy with the point at {@code index} replaced. */
  public Polygon withPoint(int index, Point point) {
    var copy = new ArrayList<>(points);
    copy.set(index, Objects.requireNonNull(point));
    return new Polygon(copy, confidence);
  }

  /** Returns a copy with every point transformed by {@code mapper}, keeping the confidence. */
  public Polygon map(UnaryOperator<Point> mapper) {
    return new Polygon(points.stream().map(mapper).toList(), confidence);
  }

  /** Mutable builder for polygons. */
  public static final class Builder {
    private final List<Point> points = new ArrayList<>();
    private @Nullable Double confidence;

    private Builder() {}

    /** Appends a point. */
    public Builder add(int x, int y) {
      points.add(new Point(x, y));
      return this;
    }

    /** Appends a point. */
    public Builder add(Point point) {
      points.add(Objects.requireNonNull(point));
      return this;
    }

    /** Sets the confidence. */
    public Builder confidence(@Nullable Double confidence) {
      this.confidence = confidence;
      return this;
    }

    /** Returns the number of points added so far. */
    public int size() {
      return points.size();
    }

    /** Creates the polygon. The builder can be reused afterwards. */
    public Polygon build() {
      return new Polygon(points, confidence);
    }
  }
}
