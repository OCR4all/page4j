package com.maxnth.page4j.model;

import java.io.Serial;
import java.util.List;

/** Thrown when a page would have duplicate IDs or references that resolve to no object. */
public final class InvalidPageException extends IllegalArgumentException {

  @Serial private static final long serialVersionUID = 1L;

  private final transient List<String> problems;

  InvalidPageException(List<String> problems) {
    super("Invalid page: " + String.join("; ", problems));
    this.problems = List.copyOf(problems);
  }

  /** One message per problem, for example {@code Duplicate ID 'r1'}. */
  public List<String> problems() {
    return problems;
  }
}
