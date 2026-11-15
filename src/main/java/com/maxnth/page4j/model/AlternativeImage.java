package com.maxnth.page4j.model;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** Another image of the object, for example a binarised one ({@code AlternativeImage}). */
@PageRecord
public record AlternativeImage(String filename, @Nullable String comments, @Nullable Double conf)
    implements AlternativeImageBuilder.With {
  public AlternativeImage {
    Objects.requireNonNull(filename, "filename");
  }
}
