package com.maxnth.page4j.pagexml;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.maxnth.page4j.model.Page;
import com.maxnth.page4j.model.TextRegion;
import com.maxnth.page4j.model.value.Mirrored;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PageXmlTest {

  private static final PageXml XML = PageXml.create();

  @TempDir Path directory;

  private static byte[] golden2019() throws Exception {
    try (InputStream in = PageXmlTest.class.getResourceAsStream("/golden/2019-07-15.xml")) {
      return in.readAllBytes();
    }
  }

  @Test
  void isShared() {
    assertSame(XML, PageXml.create());
  }

  @Test
  void readsFromBytesStreamsAndFiles() throws Exception {
    byte[] bytes = golden2019();
    Path file = directory.resolve("page.xml");
    Files.write(file, bytes);

    PageXmlReadResult fromBytes = XML.read(bytes);
    assertEquals(PageXmlVersion.V2019_07_15, fromBytes.version());
    assertEquals(fromBytes, XML.read(file));

    var closed = new AtomicBoolean();
    var stream =
        new ByteArrayInputStream(bytes) {
          @Override
          public void close() {
            closed.set(true);
          }
        };
    assertEquals(fromBytes, XML.read(stream));
    assertTrue(closed.get(), "the stream is closed");
  }

  @Test
  void writesToBytesStreamsAndFiles() throws Exception {
    byte[] bytes = golden2019();
    Page page = XML.read(bytes).page();
    assertArrayEquals(bytes, XML.write(page, PageXmlVersion.V2019_07_15));

    var out = new ByteArrayOutputStream();
    XML.write(page, PageXmlVersion.V2019_07_15, out);
    assertArrayEquals(bytes, out.toByteArray());

    Path file = directory.resolve("out.xml");
    Files.writeString(file, "old content");
    XML.write(page, PageXmlVersion.V2019_07_15, file);
    assertArrayEquals(bytes, Files.readAllBytes(file));
  }

  @Test
  void leavesTheTargetUntouchedWhenThePageDoesNotFit() throws Exception {
    Page page = XML.read(golden2019()).page();
    TextRegion region = page.find("r1", TextRegion.class).orElseThrow();
    Page unfit = page.replace("r1", region.withMirrored(Mirrored.HORIZONTALLY));

    Path file = directory.resolve("out.xml");
    Files.writeString(file, "old content");
    var e =
        assertThrows(
            PageWriteException.class, () -> XML.write(unfit, PageXmlVersion.V2019_07_15, file));
    assertEquals(
        "TextRegion 'r1': attribute mirrored is not supported in PAGE 2019-07-15",
        e.problems().getFirst());
    assertEquals("old content", Files.readString(file));

    var out = new ByteArrayOutputStream();
    assertThrows(PageWriteException.class, () -> XML.write(unfit, PageXmlVersion.V2019_07_15, out));
    assertEquals(0, out.size());

    Path missing = directory.resolve("missing.xml");
    assertThrows(
        PageWriteException.class, () -> XML.write(unfit, PageXmlVersion.V2019_07_15, missing));
    assertFalse(Files.exists(missing));
  }

  @Test
  void detectsTheVersionWithoutValidating() throws Exception {
    byte[] invalid =
        """
        <PcGts xmlns="http://schema.primaresearch.org/PAGE/gts/pagecontent/2017-07-15">
          <Nonsense/>
        </PcGts>
        """
            .getBytes(StandardCharsets.UTF_8);
    assertEquals(PageXmlVersion.V2017_07_15, XML.version(invalid));
    assertThrows(PageReadException.class, () -> XML.read(invalid));
  }

  @Test
  void failsOnMissingFilesAndOtherDocuments() {
    assertThrows(NoSuchFileException.class, () -> XML.read(directory.resolve("nope.xml")));
    assertThrows(
        PageReadException.class,
        () -> XML.read("<alto xmlns='http://www.loc.gov/standards/alto/ns-v2#'/>".getBytes()));
  }

  @Test
  void doesNotResolveExternalEntities() throws Exception {
    Path secret = directory.resolve("secret.txt");
    Files.writeString(secret, "secret");
    String xml =
        new String(golden2019(), StandardCharsets.UTF_8)
            .replaceFirst(
                "<PcGts ",
                "<!DOCTYPE PcGts [<!ENTITY x SYSTEM \"" + secret.toUri() + "\">]>\n<PcGts ")
            .replace("<Creator>page4j</Creator>", "<Creator>&x;</Creator>");
    var e =
        assertThrows(PageReadException.class, () -> XML.read(xml.getBytes(StandardCharsets.UTF_8)));
    assertFalse(e.getMessage().contains("secret"), e.getMessage());
  }

  private static byte[] withDoctype(String doctype, String text) {
    return """
        <?xml version="1.0" encoding="UTF-8"?>
        %s
        <PcGts xmlns="http://schema.primaresearch.org/PAGE/gts/pagecontent/2019-07-15">
          <Metadata>
            <Creator>page4j</Creator>
            <Created>2026-01-01T00:00:00</Created>
            <LastChange>2026-01-01T00:00:00</LastChange>
          </Metadata>
          <Page imageFilename="page.png" imageWidth="10" imageHeight="10">
            <TextRegion id="r1">
              <Coords points="0,0 5,0 5,5 0,5"/>
              <TextEquiv><Unicode>%s</Unicode></TextEquiv>
            </TextRegion>
          </Page>
        </PcGts>
        """
        .formatted(doctype, text)
        .getBytes(StandardCharsets.UTF_8);
  }

  @Test
  void doesNotFetchExternalDtdsOrParameterEntities() throws Exception {
    AtomicInteger hits = new AtomicInteger();
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          hits.incrementAndGet();
          exchange.sendResponseHeaders(200, 0);
          exchange.close();
        });
    server.start();
    try {
      String dtd = "http://127.0.0.1:" + server.getAddress().getPort() + "/evil.dtd";
      for (String doctype :
          List.of(
              "<!DOCTYPE PcGts SYSTEM \"" + dtd + "\">",
              "<!DOCTYPE PcGts [<!ENTITY % remote SYSTEM \"" + dtd + "\"> %remote;]>")) {
        try {
          XML.read(withDoctype(doctype, "plain text"));
        } catch (PageReadException e) {
          // Failing is fine; fetching is not.
        }
      }
      assertEquals(0, hits.get());
    } finally {
      server.stop(0);
    }
  }

  @Test
  void stopsExponentialEntityExpansion() {
    StringBuilder entities = new StringBuilder("<!ENTITY lol0 \"lol\">");
    for (int level = 1; level <= 10; level++) {
      entities
          .append("<!ENTITY lol")
          .append(level)
          .append(" \"")
          .append(("&lol" + (level - 1) + ";").repeat(10))
          .append("\">");
    }
    byte[] xml = withDoctype("<!DOCTYPE PcGts [" + entities + "]>", "&lol10;");
    assertTimeoutPreemptively(
        Duration.ofSeconds(10), () -> assertThrows(PageReadException.class, () -> XML.read(xml)));
  }

  @Test
  void acceptsDocumentsWithAPlainDoctype() throws Exception {
    Page page = XML.read(withDoctype("<!DOCTYPE PcGts>", "plain text")).page();
    assertEquals(
        "plain text",
        page.find("r1", TextRegion.class).orElseThrow().textEquivs().getFirst().unicode());
  }

  @Test
  void readsConcurrently() throws Exception {
    byte[] bytes = golden2019();
    var start = new CountDownLatch(1);
    List<Future<PageXmlReadResult>> results = new ArrayList<>();
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      for (int i = 0; i < 32; i++) {
        results.add(
            executor.submit(
                () -> {
                  start.await();
                  return PageXml.create().read(bytes);
                }));
      }
      start.countDown();
      PageXmlReadResult expected = results.getFirst().get();
      for (var result : results) {
        assertEquals(expected, result.get());
      }
    }
  }

  @Test
  void readsAndWritesPathsOfANonDefaultFileSystem() throws Exception {
    Path archive = directory.resolve("pages.zip");
    try (var zip =
        FileSystems.newFileSystem(URI.create("jar:" + archive.toUri()), Map.of("create", "true"))) {
      Path input = zip.getPath("/input.xml");
      Files.write(input, golden2019());
      PageXmlReadResult read = XML.read(input);
      Path output = zip.getPath("/output.xml");
      XML.write(read.page(), read.version(), output);
      assertArrayEquals(golden2019(), Files.readAllBytes(output));
    }
  }
}
