package com.maxnth.page4j.pagexml.internal;

import com.maxnth.page4j.pagexml.PageXmlVersion;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * What a PAGE version allows, read from its XSD: per complex type, the attributes (required or not,
 * with their enumeration values) and the child elements (with their maximum count). The writer
 * checks the page against these rules, so it can name the object that does not fit.
 */
final class SchemaRules {

  static final int UNBOUNDED = Integer.MAX_VALUE;
  private static final String XS = XMLConstants.W3C_XML_SCHEMA_NS_URI;
  private static final Map<PageXmlVersion, SchemaRules> RULES = new EnumMap<>(PageXmlVersion.class);

  record AttributeRule(boolean required, @Nullable Set<String> values) {}

  record ElementRule(String type, int maxOccurs) {}

  record TypeRules(Map<String, AttributeRule> attributes, Map<String, ElementRule> children) {}

  private final Map<String, TypeRules> types = new HashMap<>();
  private final Map<String, Element> complexTypes = new HashMap<>();
  private final Map<String, Element> simpleTypes = new HashMap<>();

  private SchemaRules(Document schema) {
    for (Node n = schema.getDocumentElement().getFirstChild(); n != null; n = n.getNextSibling()) {
      if (n instanceof Element e && XS.equals(e.getNamespaceURI())) {
        switch (e.getLocalName()) {
          case "complexType" -> complexTypes.put(e.getAttribute("name"), e);
          case "simpleType" -> simpleTypes.put(e.getAttribute("name"), e);
          default -> {}
        }
      }
    }
    for (String name : complexTypes.keySet()) {
      Map<String, AttributeRule> attributes = new LinkedHashMap<>();
      Map<String, ElementRule> children = new LinkedHashMap<>();
      collect(complexTypes.get(name), attributes, children);
      types.put(name, new TypeRules(attributes, children));
    }
  }

  static synchronized SchemaRules of(PageXmlVersion version) {
    SchemaRules rules = RULES.get(version);
    if (rules == null) {
      try (InputStream in = PageXmlSchemas.open(version)) {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        rules = new SchemaRules(factory.newDocumentBuilder().parse(in));
      } catch (Exception e) {
        throw new IllegalStateException("Cannot read the schema of " + version, e);
      }
      RULES.put(version, rules);
    }
    return rules;
  }

  /** The rules of a complex type, or null for a simple type (text content). */
  @Nullable TypeRules type(String name) {
    return types.get(name);
  }

  private void collect(
      Element type, Map<String, AttributeRule> attributes, Map<String, ElementRule> children) {
    Element content = type;
    Element extension = child(child(type, "complexContent"), "extension");
    if (extension != null) {
      String base = local(extension.getAttribute("base"));
      Element baseType = complexTypes.get(base);
      if (baseType == null) {
        throw new IllegalStateException("Unknown base type " + base);
      }
      collect(baseType, attributes, children);
      content = extension;
    }
    for (Node n = content.getFirstChild(); n != null; n = n.getNextSibling()) {
      if (n instanceof Element e && XS.equals(e.getNamespaceURI())) {
        switch (e.getLocalName()) {
          case "attribute" ->
              attributes.put(
                  e.getAttribute("name"),
                  new AttributeRule("required".equals(e.getAttribute("use")), values(e)));
          case "sequence", "choice" -> particles(e, 1, children);
          default -> {}
        }
      }
    }
  }

  private static void particles(Element group, int outerMax, Map<String, ElementRule> children) {
    int max = multiply(outerMax, maxOccurs(group));
    for (Node n = group.getFirstChild(); n != null; n = n.getNextSibling()) {
      if (n instanceof Element e && XS.equals(e.getNamespaceURI())) {
        switch (e.getLocalName()) {
          case "element" ->
              children.put(
                  e.getAttribute("name"),
                  new ElementRule(local(e.getAttribute("type")), multiply(max, maxOccurs(e))));
          case "sequence", "choice" -> particles(e, max, children);
          default -> {}
        }
      }
    }
  }

  /** The enumeration values of an attribute's type, or null if it is not an enumeration. */
  private @Nullable Set<String> values(Element attribute) {
    Element simple = child(attribute, "simpleType");
    if (simple == null) {
      simple = simpleTypes.get(local(attribute.getAttribute("type")));
    }
    Element restriction = child(simple, "restriction");
    if (restriction == null) {
      return null;
    }
    Set<String> values = new LinkedHashSet<>();
    for (Node n = restriction.getFirstChild(); n != null; n = n.getNextSibling()) {
      if (n instanceof Element e && "enumeration".equals(e.getLocalName())) {
        values.add(e.getAttribute("value"));
      }
    }
    return values.isEmpty() ? null : values;
  }

  private static @Nullable Element child(@Nullable Element parent, String localName) {
    if (parent == null) {
      return null;
    }
    for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
      if (n instanceof Element e
          && XS.equals(e.getNamespaceURI())
          && localName.equals(e.getLocalName())) {
        return e;
      }
    }
    return null;
  }

  private static int maxOccurs(Element particle) {
    String max = particle.getAttribute("maxOccurs");
    return max.isEmpty() ? 1 : max.equals("unbounded") ? UNBOUNDED : Integer.parseInt(max);
  }

  private static int multiply(int a, int b) {
    return a == UNBOUNDED || b == UNBOUNDED ? UNBOUNDED : a * b;
  }

  private static String local(String qname) {
    return qname.substring(qname.indexOf(':') + 1);
  }
}
