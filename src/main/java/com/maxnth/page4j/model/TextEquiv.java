package com.maxnth.page4j.model;

import com.maxnth.page4j.model.value.TextDataType;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** The text of an object, one of possibly several alternatives ({@code TextEquiv}). */
@PageRecord
public record TextEquiv(
    @Nullable Integer index,
    @Nullable Double conf,
    @Nullable TextDataType dataType,
    @Nullable String dataTypeDetails,
    @Nullable String comments,
    @Nullable String plainText,
    String unicode)
    implements TextEquivBuilder.With {
  public TextEquiv {
    Objects.requireNonNull(unicode, "unicode");
  }
}
