package com.maxnth.page4j.model;

import com.maxnth.page4j.model.value.MetadataItemType;
import com.maxnth.page4j.model.value.PageDateTime;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** A metadata entry, such as a processing step ({@code MetadataItem}). */
@PageRecord
public record MetadataItem(
    @Nullable MetadataItemType type,
    @Nullable String name,
    String value,
    @Nullable PageDateTime date,
    List<LabelGroup> labels)
    implements MetadataItemBuilder.With {
  public MetadataItem {
    Objects.requireNonNull(value, "value");
    labels = List.copyOf(labels);
  }
}
