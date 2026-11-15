package com.maxnth.page4j.pagexml;

import java.io.IOException;
import java.io.Serial;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Thrown when a page can't be written in the requested version, for example because it holds
 * content the version doesn't define. Nothing is written in that case.
 */
public final class PageWriteException extends IOException {

  @Serial private static final long serialVersionUID = 1L;

  private final transient List<String> problems;

  public PageWriteException(List<String> problems, @Nullable Throwable cause) {
    super(message(problems), cause);
    this.problems = List.copyOf(problems);
  }

  /**
   * One message per problem, for example {@code TextRegion 'r1': attribute 'mirrored' is not
   * supported in PAGE 2019-07-15}.
   */
  public List<String> problems() {
    return problems;
  }

  private static String message(List<String> problems) {
    if (problems.isEmpty()) {
      return "Cannot write page";
    }
    return problems.size() == 1
        ? problems.getFirst()
        : problems.getFirst() + " (and " + (problems.size() - 1) + " more)";
  }
}
