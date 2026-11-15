package com.maxnth.page4j.pagexml;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import com.maxnth.page4j.pagexml.internal.PageXmlReader;
import com.maxnth.page4j.pagexml.internal.PageXmlWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Stream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Same-version round trips of test documents with the new reader and writer ({@code
 * pagexml.internal}): read, write in the version that was read, and compare the output with the
 * input.
 *
 * <p>Differences the library still produces are listed in {@value #KNOWN_DIFFERENCES}. The list is
 * a ratchet: a difference that is not listed is a regression, and a listed difference that no
 * longer occurs must be removed, so every fix shows up in the diff of that file.
 */
final class RoundTrip {

  static final String KNOWN_DIFFERENCES = "fidelity/known-differences.txt";

  /** Documents whose same-version round trip is checked, besides the fixtures. */
  static final List<String> CORPUS = List.of("golden/source.xml", "document.xml");

  /** Line kinds produced by {@link #differences}. */
  static final String[] DIFFERENCE_KINDS = {
    "MISSING_", "EXTRA_", "CHANGED_", "READ_FAILED", "WRITE_FAILED"
  };

  /** Line kinds produced by {@link #droppedContent}. */
  static final String[] DROPPED_KINDS = {"DROPPED_", "READ_FAILED", "WRITE_FAILED"};

  private static final String LENIENT_PREFIX = "fidelity/lenient/";
  private static final String PAGE_NAMESPACE_PREFIX = "http://schema.primaresearch.org/PAGE/";

  private RoundTrip() {}

  /**
   * All documents to check: the fixtures (except the invalid ones under {@code fidelity/lenient/},
   * which must fail to read), the schema coverage documents and {@link #CORPUS}.
   */
  static Stream<String> documents() throws Exception {
    return Stream.of(xmlFiles("fidelity"), xmlFiles("coverage"), CORPUS)
        .flatMap(List::stream)
        .filter(document -> !document.startsWith(LENIENT_PREFIX));
  }

  private static List<String> xmlFiles(String folder) throws Exception {
    Path root = resource(folder);
    try (Stream<Path> files = Files.walk(root)) {
      return files
          .filter(file -> file.toString().endsWith(".xml"))
          .map(file -> folder + "/" + root.relativize(file).toString().replace('\\', '/'))
          .sorted()
          .toList();
    }
  }

  /** The output of a round trip, or the line that describes why it failed. */
  record Result(Path source, Path output, String failure) {}

  static Result run(String document, Path directory) throws Exception {
    Path source = resource(document);
    PageXmlReadResult read;
    try {
      read = PageXmlReader.read(Files.readAllBytes(source));
    } catch (IOException | RuntimeException e) {
      return new Result(source, null, "READ_FAILED " + describe(e));
    }
    Path output = directory.resolve(source.getFileName());
    try {
      Files.write(output, PageXmlWriter.write(read.page(), read.version()));
    } catch (IOException | RuntimeException e) {
      return new Result(source, null, "WRITE_FAILED " + describe(e));
    }
    return new Result(source, output, null);
  }

  /** Every difference between the document and its round trip. */
  static List<String> differences(String document, Path directory) throws Exception {
    Result result = run(document, directory);
    if (result.failure() != null) return List.of(result.failure());
    return XmlDiff.compare(result.source(), result.output()).stream()
        .map(XmlDiff.Difference::toString)
        .toList();
  }

  /**
   * Elements and attributes of the document that occur less often in its round trip, wherever they
   * are. This is coarser than {@link #differences}, but holds for any document, also where the
   * output is reordered or reformatted.
   */
  static List<String> droppedContent(String document, Path directory) throws Exception {
    Result result = run(document, directory);
    if (result.failure() != null) return List.of(result.failure());
    Map<String, Integer> source = counts(result.source());
    Map<String, Integer> output = counts(result.output());
    var dropped = new ArrayList<String>();
    for (var entry : source.entrySet()) {
      int written = output.getOrDefault(entry.getKey(), 0);
      if (written < entry.getValue())
        dropped.add(entry.getKey() + " " + entry.getValue() + " -> " + written);
    }
    return dropped;
  }

  /** How often each element and attribute occurs in the document. */
  private static Map<String, Integer> counts(Path document) throws IOException {
    var counts = new LinkedHashMap<String, Integer>();
    try (InputStream in = Files.newInputStream(document)) {
      XMLStreamReader xml = XMLInputFactory.newFactory().createXMLStreamReader(in);
      while (xml.hasNext()) {
        if (xml.next() != XMLStreamConstants.START_ELEMENT) continue;
        String element = name(xml.getNamespaceURI(), xml.getPrefix(), xml.getLocalName());
        counts.merge("DROPPED_ELEMENT " + element, 1, Integer::sum);
        for (int i = 0; i < xml.getAttributeCount(); i++) {
          String attribute =
              name(
                  xml.getAttributeNamespace(i),
                  xml.getAttributePrefix(i),
                  xml.getAttributeLocalName(i));
          counts.merge("DROPPED_ATTRIBUTE " + element + "/@" + attribute, 1, Integer::sum);
        }
      }
      xml.close();
    } catch (XMLStreamException e) {
      throw new IOException(e);
    }
    return counts;
  }

  private static String name(String namespace, String prefix, String localName) {
    if (namespace == null || namespace.isEmpty() || namespace.startsWith(PAGE_NAMESPACE_PREFIX))
      return localName;
    if (prefix != null && !prefix.isEmpty()) return prefix + ":" + localName;
    return "{" + namespace + "}" + localName;
  }

  /**
   * Fails unless the actual lines are exactly the known differences of the document whose kind
   * starts with one of the given prefixes.
   */
  static void assertKnown(String document, List<String> actual, String... kinds) {
    var known = new TreeSet<String>();
    for (String line : knownDifferences().getOrDefault(document, List.of())) {
      for (String kind : kinds) {
        if (line.startsWith(kind)) known.add(line);
      }
    }
    var occurring = new TreeSet<>(actual);

    var regressions = new TreeSet<>(occurring);
    regressions.removeAll(known);
    var fixed = new TreeSet<>(known);
    fixed.removeAll(occurring);
    if (regressions.isEmpty() && fixed.isEmpty()) return;

    var message = new StringBuilder(document).append(" round trip does not match ");
    message.append(KNOWN_DIFFERENCES).append('.');
    if (!regressions.isEmpty()) {
      message.append("\n\nNew differences (regressions, or a new fixture to list):");
      regressions.forEach(line -> message.append("\n").append(document).append(" | ").append(line));
    }
    if (!fixed.isEmpty()) {
      message.append("\n\nNo longer occurring (fixed; remove these lines):");
      fixed.forEach(line -> message.append("\n").append(document).append(" | ").append(line));
    }
    fail(message.toString());
  }

  /** Reads {@value #KNOWN_DIFFERENCES}: lines of {@code <document> | <difference>}. */
  static Map<String, List<String>> knownDifferences() {
    try {
      var result = new LinkedHashMap<String, List<String>>();
      for (String line : Files.readAllLines(resource(KNOWN_DIFFERENCES), StandardCharsets.UTF_8)) {
        if (line.isBlank() || line.startsWith("#")) continue;
        int separator = line.indexOf(" | ");
        if (separator < 0) throw new IllegalStateException("Malformed line: " + line);
        result
            .computeIfAbsent(line.substring(0, separator), key -> new ArrayList<>())
            .add(line.substring(separator + 3));
      }
      return result;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }

  static Path resource(String name) throws URISyntaxException {
    var url = RoundTrip.class.getResource("/" + name);
    assertNotNull(url, "Missing test resource " + name);
    return Path.of(url.toURI());
  }

  /** The exception and its causes by class, plus the innermost message without file paths. */
  private static String describe(Throwable e) {
    var classes = new ArrayList<String>();
    Throwable innermost = e;
    for (Throwable t = e; t != null; t = t.getCause()) {
      classes.add(t.getClass().getSimpleName());
      innermost = t;
    }
    String message = innermost.getMessage() == null ? "" : innermost.getMessage();
    message = message.replaceAll("(file:)?/\\S+/", "").replace('\n', ' ');
    return String.join(" <- ", classes) + ": " + message;
  }
}
