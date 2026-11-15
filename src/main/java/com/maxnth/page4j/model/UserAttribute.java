package com.maxnth.page4j.model;

import com.maxnth.page4j.model.value.UserAttributeType;
import org.jspecify.annotations.Nullable;

/** An attribute defined by the creator of the file ({@code UserAttribute}). */
@PageRecord
public record UserAttribute(
    @Nullable String name,
    @Nullable String description,
    @Nullable UserAttributeType type,
    @Nullable String value)
    implements UserAttributeBuilder.With {
  public UserAttribute {}
}
