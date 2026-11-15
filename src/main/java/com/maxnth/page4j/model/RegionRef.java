package com.maxnth.page4j.model;

import java.util.Objects;

/**
 * A reference to a region in a reading order group ({@code RegionRef} or {@code RegionRefIndexed}).
 */
@PageRecord
public record RegionRef(String regionRef) implements GroupMember, RegionRefBuilder.With {
  public RegionRef {
    Objects.requireNonNull(regionRef, "regionRef");
  }
}
