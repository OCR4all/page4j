package com.maxnth.page4j.pagexml;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Reading a document and writing it in the same version must give back the same document.
 *
 * <p>The documents are the hand-written fixtures under {@code fidelity/} (one per known problem, so
 * their expected output is the input, not something the library produced) and the documents in
 * {@link RoundTrip#CORPUS}. What still differs is listed in {@value RoundTrip#KNOWN_DIFFERENCES}.
 */
class RoundTripFidelityTest {

  @TempDir Path directory;

  static Stream<String> documents() throws Exception {
    return RoundTrip.documents();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("documents")
  void sameVersionRoundTripKeepsTheDocument(String document) throws Exception {
    RoundTrip.assertKnown(
        document, RoundTrip.differences(document, directory), RoundTrip.DIFFERENCE_KINDS);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("documents")
  void sameVersionRoundTripDropsNothing(String document) throws Exception {
    RoundTrip.assertKnown(
        document, RoundTrip.droppedContent(document, directory), RoundTrip.DROPPED_KINDS);
  }

  @Test
  void knownDifferencesOnlyNameCheckedDocuments() throws Exception {
    var checked = new TreeSet<String>(RoundTrip.documents().toList());
    var listed = new TreeSet<>(RoundTrip.knownDifferences().keySet());
    listed.removeAll(checked);
    assertEquals(new TreeSet<String>(), listed, "Documents listed but not checked");
  }
}
