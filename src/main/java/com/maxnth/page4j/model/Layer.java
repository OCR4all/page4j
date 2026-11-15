package com.maxnth.page4j.model;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** A layer of regions with a z-index ({@code Layer}). */
@PageRecord
public record Layer(String id, int zIndex, @Nullable String caption, List<String> regionRefs)
    implements ContentObject, LayerBuilder.With {
  public Layer {
    Objects.requireNonNull(id, "id");
    regionRefs = List.copyOf(regionRefs);
  }
}
