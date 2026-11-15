package com.maxnth.page4j.formats.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Hands out the IDs of a page being read: the document's own where they are valid and unique,
 * generated ones otherwise, never one the document uses elsewhere.
 */
public final class Ids {

  private final Set<String> taken;
  private final Set<String> used = new HashSet<>();
  private final Map<String, Integer> next = new HashMap<>();

  /**
   * @param taken all IDs in the document, which generated IDs must avoid
   */
  public Ids(Set<String> taken) {
    this.taken = new HashSet<>(taken);
  }

  /**
   * The document's ID if it is a valid, unused {@code xsd:ID}; otherwise a generated one, and a
   * replaced ID is reported.
   */
  public String id(@Nullable String id, String prefix, Attrs at) {
    if (id == null) {
      return generate(prefix);
    }
    if (!isNcName(id)) {
      String generated = generate(prefix);
      at.changed(
          at.element() + ": ID '" + id + "' is not a valid ID, replaced by '" + generated + "'");
      return generated;
    }
    if (!used.add(id)) {
      String generated = generate(prefix);
      at.changed(at.element() + ": ID '" + id + "' is used twice, replaced by '" + generated + "'");
      return generated;
    }
    return id;
  }

  /** Marks an ID as used without checking it, for IDs that are not of a content object. */
  public boolean use(String id) {
    return isNcName(id) && used.add(id);
  }

  public String generate(String prefix) {
    while (true) {
      int n = next.merge(prefix, 1, Integer::sum);
      String id = prefix + n;
      if (!taken.contains(id) && used.add(id)) {
        return id;
      }
    }
  }

  /**
   * Whether a value is an {@code xsd:NCName}, as IDs must be (simplified to letters and digits).
   */
  public static boolean isNcName(String value) {
    if (value.isEmpty()) {
      return false;
    }
    int first = value.codePointAt(0);
    if (!Character.isLetter(first) && first != '_') {
      return false;
    }
    return value
        .codePoints()
        .allMatch(c -> Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.');
  }
}
