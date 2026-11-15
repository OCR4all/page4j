package com.maxnth.page4j.pagexml;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Compares two XML documents at the infoset level for round-trip tests.
 *
 * <p>Ignored: attribute order, namespace declarations, whitespace-only text between elements, and
 * the document order of siblings that carry an integer {@code index} attribute (they are compared
 * in index order, as PAGE defines their order by index). Everything else must match exactly,
 * including attribute values as written ({@code 0} differs from {@code 0.0}) and the difference
 * between a missing and an empty element.
 */
final class XmlDiff {

  enum Kind {
    MISSING_ELEMENT,
    EXTRA_ELEMENT,
    MISSING_ATTRIBUTE,
    EXTRA_ATTRIBUTE,
    CHANGED_ATTRIBUTE,
    CHANGED_TEXT
  }

  /** One difference; {@code expected} or {@code actual} is null where it does not apply. */
  record Difference(Kind kind, String path, String expected, String actual) {
    @Override
    public String toString() {
      return switch (kind) {
        case MISSING_ELEMENT, EXTRA_ELEMENT -> kind + " " + path;
        case MISSING_ATTRIBUTE -> kind + " " + path + " " + quote(expected);
        case EXTRA_ATTRIBUTE -> kind + " " + path + " " + quote(actual);
        case CHANGED_ATTRIBUTE, CHANGED_TEXT ->
            kind + " " + path + " " + quote(expected) + " -> " + quote(actual);
      };
    }

    private static String quote(String value) {
      return '"' + value.replace("\\", "\\\\").replace("\n", "\\n").replace("\"", "\\\"") + '"';
    }
  }

  private XmlDiff() {}

  static Document parse(Path file) throws Exception {
    var factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    factory.setExpandEntityReferences(false);
    return factory.newDocumentBuilder().parse(file.toFile());
  }

  static List<Difference> compare(Path expected, Path actual) throws Exception {
    return compare(parse(expected), parse(actual));
  }

  static List<Difference> compare(Document expected, Document actual) {
    var differences = new ArrayList<Difference>();
    Element e = expected.getDocumentElement();
    Element a = actual.getDocumentElement();
    if (!key(e).equals(key(a))) {
      differences.add(new Difference(Kind.MISSING_ELEMENT, "/" + step(e, 1), null, null));
      differences.add(new Difference(Kind.EXTRA_ELEMENT, "/" + step(a, 1), null, null));
    } else {
      compareElements(e, a, "/" + step(e, 1), differences);
    }
    return differences;
  }

  private static void compareElements(
      Element expected, Element actual, String path, List<Difference> differences) {
    compareAttributes(expected, actual, path, differences);

    List<Element> expectedChildren = indexOrdered(childElements(expected));
    List<Element> actualChildren = indexOrdered(childElements(actual));

    String expectedText = directText(expected);
    String actualText = directText(actual);
    boolean structural = !expectedChildren.isEmpty() || !actualChildren.isEmpty();
    if (!(structural && expectedText.isBlank() && actualText.isBlank())
        && !expectedText.equals(actualText)) {
      differences.add(new Difference(Kind.CHANGED_TEXT, path, expectedText, actualText));
    }

    alignChildren(expectedChildren, actualChildren, path, differences);
  }

  private static void compareAttributes(
      Element expected, Element actual, String path, List<Difference> differences) {
    Map<String, Attr> expectedAttributes = attributes(expected);
    Map<String, Attr> actualAttributes = attributes(actual);
    for (var entry : expectedAttributes.entrySet()) {
      Attr e = entry.getValue();
      Attr a = actualAttributes.get(entry.getKey());
      String attributePath = path + "/@" + e.getName();
      if (a == null) {
        differences.add(new Difference(Kind.MISSING_ATTRIBUTE, attributePath, e.getValue(), null));
      } else if (!e.getValue().equals(a.getValue())) {
        differences.add(
            new Difference(Kind.CHANGED_ATTRIBUTE, attributePath, e.getValue(), a.getValue()));
      }
    }
    for (var entry : actualAttributes.entrySet()) {
      if (!expectedAttributes.containsKey(entry.getKey())) {
        Attr a = entry.getValue();
        differences.add(
            new Difference(Kind.EXTRA_ATTRIBUTE, path + "/@" + a.getName(), null, a.getValue()));
      }
    }
  }

  /** Aligns the children by their longest common subsequence of keys. */
  private static void alignChildren(
      List<Element> expected, List<Element> actual, String path, List<Difference> differences) {
    int n = expected.size();
    int m = actual.size();
    int[][] lcs = new int[n + 1][m + 1];
    for (int i = n - 1; i >= 0; i--) {
      for (int j = m - 1; j >= 0; j--) {
        lcs[i][j] =
            key(expected.get(i)).equals(key(actual.get(j)))
                ? lcs[i + 1][j + 1] + 1
                : Math.max(lcs[i + 1][j], lcs[i][j + 1]);
      }
    }

    var expectedPositions = new Positions();
    var actualPositions = new Positions();
    int i = 0;
    int j = 0;
    while (i < n || j < m) {
      if (i < n && j < m && key(expected.get(i)).equals(key(actual.get(j)))) {
        Element e = expected.get(i++);
        Element a = actual.get(j++);
        actualPositions.next(a);
        compareElements(e, a, path + "/" + step(e, expectedPositions.next(e)), differences);
      } else if (j < m && (i == n || lcs[i][j + 1] >= lcs[i + 1][j])) {
        Element a = actual.get(j++);
        String step = step(a, actualPositions.next(a));
        differences.add(new Difference(Kind.EXTRA_ELEMENT, path + "/" + step, null, null));
      } else {
        Element e = expected.get(i++);
        String step = step(e, expectedPositions.next(e));
        differences.add(new Difference(Kind.MISSING_ELEMENT, path + "/" + step, null, null));
      }
    }
  }

  /** Counts the position of each element among its siblings with the same key. */
  private static final class Positions {
    private final Map<String, Integer> counts = new LinkedHashMap<>();

    int next(Element element) {
      return counts.merge(key(element), 1, Integer::sum);
    }
  }

  /** Elements are matched by name, and by id where they have one. */
  private static String key(Element element) {
    String name = "{" + Objects.toString(element.getNamespaceURI(), "") + "}" + localName(element);
    String id = element.getAttribute("id");
    return id.isEmpty() ? name : name + "#" + id;
  }

  private static String step(Element element, int position) {
    String id = element.getAttribute("id");
    return id.isEmpty()
        ? localName(element) + "[" + position + "]"
        : localName(element) + "[@id='" + id + "']";
  }

  private static String localName(Element element) {
    return element.getLocalName() != null ? element.getLocalName() : element.getNodeName();
  }

  private static Map<String, Attr> attributes(Element element) {
    var result = new LinkedHashMap<String, Attr>();
    NamedNodeMap map = element.getAttributes();
    for (int i = 0; i < map.getLength(); i++) {
      Attr attr = (Attr) map.item(i);
      if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(attr.getNamespaceURI())) continue;
      String localName = attr.getLocalName() != null ? attr.getLocalName() : attr.getName();
      result.put("{" + Objects.toString(attr.getNamespaceURI(), "") + "}" + localName, attr);
    }
    return result;
  }

  private static List<Element> childElements(Element parent) {
    var result = new ArrayList<Element>();
    for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child instanceof Element element) result.add(element);
    }
    return result;
  }

  private static String directText(Element element) {
    var text = new StringBuilder();
    for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child.getNodeType() == Node.TEXT_NODE || child.getNodeType() == Node.CDATA_SECTION_NODE)
        text.append(child.getNodeValue());
    }
    return text.toString();
  }

  /**
   * Puts the siblings with an integer {@code index} attribute in index order, keeping them in the
   * positions they occupy among the other siblings. The sort is stable.
   */
  private static List<Element> indexOrdered(List<Element> children) {
    var indexed = new ArrayList<Element>();
    for (Element child : children) {
      if (index(child) != null) indexed.add(child);
    }
    if (indexed.size() < 2) return children;
    indexed.sort(Comparator.comparing(XmlDiff::index));
    var result = new ArrayList<Element>(children.size());
    int next = 0;
    for (Element child : children) result.add(index(child) != null ? indexed.get(next++) : child);
    return result;
  }

  private static Integer index(Element element) {
    String value = element.getAttribute("index");
    if (value.isEmpty()) return null;
    try {
      return Integer.valueOf(value.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
