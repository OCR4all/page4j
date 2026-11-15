package com.maxnth.page4j.pagexml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.maxnth.page4j.convert.Conversion;
import com.maxnth.page4j.convert.ConversionMessage;
import com.maxnth.page4j.convert.PageConverter;
import com.maxnth.page4j.model.FormRegion;
import com.maxnth.page4j.model.GridAddPoints;
import com.maxnth.page4j.model.Page;
import com.maxnth.page4j.model.TableRegion;
import com.maxnth.page4j.model.TextLine;
import com.maxnth.page4j.model.TextRegion;
import com.maxnth.page4j.model.value.PageDateTime;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Pins the XML written for every supported PAGE version, and the conversion messages, against
 * checked-in golden files.
 *
 * <p>{@code golden/source.xml} is a feature-rich 2024-07-15 document. For each version it is read,
 * converted to that version and written; the result must equal {@code golden/<version>.xml} and the
 * conversion messages {@code golden/<version>.messages.txt}. Each golden file must also survive a
 * read/write round trip unchanged.
 *
 * <p>Run with the environment variable {@code PAGE4J_UPDATE_GOLDENS=true} to regenerate the golden
 * files in the source tree after an intentional change, and review the diff. The same-version round
 * trip of {@code source.xml} itself is checked by the fidelity tests.
 */
class PageXmlGoldenTest {

  private static final PageXml XML = PageXml.create();
  private static final boolean UPDATE = "true".equals(System.getenv("PAGE4J_UPDATE_GOLDENS"));

  @ParameterizedTest
  @EnumSource(PageXmlVersion.class)
  void convertedSourceMatchesGolden(PageXmlVersion version) throws Exception {
    PageXmlReadResult source = XML.read(resource("source.xml"));
    assertEquals(PageXmlVersion.V2024_07_15, source.version());
    Conversion conversion = PageConverter.convert(source.page(), version);
    String xml = new String(XML.write(conversion.page(), version), StandardCharsets.UTF_8);
    String messages =
        conversion.messages().stream()
            .map(m -> m.kind() + " " + m.message() + "\n")
            .collect(Collectors.joining());

    if (UPDATE) {
      Path folder = Path.of("src/test/resources/golden");
      Files.writeString(folder.resolve(version.date() + ".xml"), xml, StandardCharsets.UTF_8);
      if (version != source.version()) {
        Files.writeString(
            folder.resolve(version.date() + ".messages.txt"), messages, StandardCharsets.UTF_8);
      }
    }
    assertEquals(text(version.date() + ".xml"), xml);
    if (version == source.version()) {
      assertEquals(List.of(), conversion.messages());
    } else {
      assertEquals(text(version.date() + ".messages.txt"), messages);
    }
  }

  @ParameterizedTest
  @EnumSource(PageXmlVersion.class)
  void goldenSurvivesReadWriteRoundTrip(PageXmlVersion version) throws Exception {
    PageXmlReadResult read = XML.read(resource(version.date() + ".xml"));
    assertEquals(version, read.version());
    assertEquals(
        text(version.date() + ".xml"),
        new String(XML.write(read.page(), version), StandardCharsets.UTF_8));
  }

  @ParameterizedTest
  @EnumSource(value = PageXmlVersion.class, mode = EnumSource.Mode.EXCLUDE, names = "V2024_07_15")
  void goldenUpgradesToLatestVersionWithoutLoss(PageXmlVersion version) throws Exception {
    Page page = XML.read(resource(version.date() + ".xml")).page();
    Conversion conversion = PageConverter.convert(page, PageXmlVersion.LATEST);
    assertFalse(
        conversion.lossy(),
        () -> conversion.messages().stream().map(ConversionMessage::message).toList().toString());

    PageXmlReadResult upgraded = XML.read(XML.write(conversion.page(), PageXmlVersion.LATEST));
    assertEquals(PageXmlVersion.LATEST, upgraded.version());
    assertEquals(page.allRegions().size(), upgraded.page().allRegions().size());
  }

  @Test
  void tableGridAndScriptLinesSurviveReading() throws Exception {
    Page page = XML.read(resource("2024-07-15.xml")).page();

    TableRegion table = page.find("r4", TableRegion.class).orElseThrow();
    assertNotNull(table.grid());
    assertEquals(2, table.grid().gridPoints().size());
    assertEquals(3, table.grid().gridPoints().getFirst().points().points().size());
    GridAddPoints addPoints = table.grid().addPoints().get(1);
    assertEquals(0, addPoints.row());
    assertEquals(1, addPoints.col());
    assertEquals("495,1000", addPoints.left());
    assertEquals("700,905", addPoints.top());

    TextLine line = page.find("r1", TextRegion.class).orElseThrow().textLines().getFirst();
    assertNotNull(line.ascentLine());
    assertEquals(2, line.ascentLine().points().size());
    assertEquals(0.5, line.ascentLine().confidence());
    assertEquals(2, line.meanLine().points().size());
    assertEquals(2, line.baseline().points().size());
    assertEquals(2, line.descentLine().points().size());

    assertEquals("a form", page.find("r19", FormRegion.class).orElseThrow().comments());
  }

  @Test
  void metadataTimestampsKeepTheirLexicalForm() throws Exception {
    Page page = XML.read(resource("2024-07-15.xml")).page();
    assertEquals(PageDateTime.parse("2020-01-02T03:04:05"), page.metadata().created());
    assertEquals("2021-06-07T08:09:10", page.metadata().lastChange().xmlValue());
  }

  private static byte[] resource(String name) throws IOException {
    try (InputStream in = PageXmlGoldenTest.class.getResourceAsStream("/golden/" + name)) {
      assertNotNull(in, "Missing golden resource " + name);
      return in.readAllBytes();
    }
  }

  private static String text(String name) throws IOException {
    return new String(resource(name), StandardCharsets.UTF_8);
  }
}
