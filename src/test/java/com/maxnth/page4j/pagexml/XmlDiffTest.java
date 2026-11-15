package com.maxnth.page4j.pagexml;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

class XmlDiffTest {

  @Test
  void ignoresAttributeOrderNamespaceDeclarationsAndIndentation() throws Exception {
    assertEquals(
        List.of(),
        diff(
            "<a xmlns='urn:x'><b x='1' y='2'/></a>",
            "<p:a xmlns:p='urn:x'>\n  <p:b y='2' x='1'/>\n</p:a>"));
  }

  @Test
  void comparesAttributeValuesExactly() throws Exception {
    assertEquals(
        List.of("CHANGED_ATTRIBUTE /a[1]/b[1]/@orientation \"0\" -> \"0.0\""),
        diff("<a><b orientation='0'/></a>", "<a><b orientation='0.0'/></a>"));
  }

  @Test
  void reportsMissingAndExtraAttributes() throws Exception {
    assertEquals(
        List.of(
            "MISSING_ATTRIBUTE /a[1]/@x \"1\"", //
            "EXTRA_ATTRIBUTE /a[1]/@y \"2\""),
        diff("<a x='1'/>", "<a y='2'/>"));
  }

  @Test
  void distinguishesMissingFromEmptyElements() throws Exception {
    assertEquals(List.of("EXTRA_ELEMENT /a[1]/Comments[1]"), diff("<a/>", "<a><Comments/></a>"));
    assertEquals(List.of(), diff("<a><c></c></a>", "<a><c/></a>"));
  }

  @Test
  void keepsWhitespaceInsideTextElements() throws Exception {
    assertEquals(
        List.of("CHANGED_TEXT /a[1]/u[1] \" \" -> \"\""), diff("<a><u> </u></a>", "<a><u/></a>"));
  }

  @Test
  void matchesElementsByIdAndReportsMovedOnes() throws Exception {
    assertEquals(
        List.of("MISSING_ELEMENT /a[1]/g[@id='g1']/c[@id='c1']", "EXTRA_ELEMENT /a[1]/c[@id='c1']"),
        diff("<a><g id='g1'><c id='c1'/></g></a>", "<a><g id='g1'/><c id='c1'/></a>"));
  }

  @Test
  void comparesIndexedSiblingsInIndexOrder() throws Exception {
    assertEquals(
        List.of(),
        diff(
            "<g><n/><r index='1' ref='b'/><r index='0' ref='a'/></g>",
            "<g><n/><r index='0' ref='a'/><r index='1' ref='b'/></g>"));
    assertEquals(
        List.of(
            "CHANGED_ATTRIBUTE /g[1]/r[1]/@ref \"a\" -> \"b\"",
            "CHANGED_ATTRIBUTE /g[1]/r[2]/@ref \"b\" -> \"a\""),
        diff(
            "<g><r index='1' ref='b'/><r index='0' ref='a'/></g>",
            "<g><r index='0' ref='b'/><r index='1' ref='a'/></g>"));
  }

  private static List<String> diff(String expected, String actual) throws Exception {
    return XmlDiff.compare(parse(expected), parse(actual)).stream()
        .map(XmlDiff.Difference::toString)
        .toList();
  }

  private static Document parse(String xml) throws Exception {
    var factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    return factory
        .newDocumentBuilder()
        .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
  }
}
