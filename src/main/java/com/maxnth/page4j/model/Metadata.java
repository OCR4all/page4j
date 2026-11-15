package com.maxnth.page4j.model;

import com.maxnth.page4j.model.value.PageDateTime;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** The metadata of the document ({@code Metadata}). */
@PageRecord
public record Metadata(
    @Nullable String externalRef,
    String creator,
    PageDateTime created,
    PageDateTime lastChange,
    @Nullable String comments,
    List<UserAttribute> userAttributes,
    List<MetadataItem> metadataItems)
    implements MetadataBuilder.With {
  public Metadata {
    Objects.requireNonNull(creator, "creator");
    Objects.requireNonNull(created, "created");
    Objects.requireNonNull(lastChange, "lastChange");
    userAttributes = List.copyOf(userAttributes);
    metadataItems = List.copyOf(metadataItems);
  }
}
