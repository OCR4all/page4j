package com.maxnth.page4j.formats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.maxnth.page4j.pagexml.PageReadException;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class MetsXmlTest {

  @Test
  void listsTheFilesInDocumentOrder() throws Exception {
    String mets =
        """
        <mets xmlns="http://www.loc.gov/METS/" xmlns:xlink="http://www.w3.org/1999/xlink">
          <fileSec>
            <fileGrp USE="PAGE">
              <file ID="f1"><FLocat LOCTYPE="URL" xlink:href="page1.xml"/></file>
              <file ID="f2"><FLocat LOCTYPE="URL" xlink:href="page2.xml"/></file>
            </fileGrp>
          </fileSec>
        </mets>
        """;
    assertEquals(
        List.of("page1.xml", "page2.xml"),
        MetsXml.create().files(mets.getBytes(StandardCharsets.UTF_8)));
    assertThrows(PageReadException.class, () -> MetsXml.create().files("<alto/>".getBytes()));
  }

  @Test
  void doesNotFetchExternalDtds() throws Exception {
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
      String mets =
          """
          <?xml version="1.0" encoding="UTF-8"?>
          <!DOCTYPE mets [<!ENTITY %% remote SYSTEM "%s"> %%remote;]>
          <mets xmlns="http://www.loc.gov/METS/" xmlns:xlink="http://www.w3.org/1999/xlink">
            <fileSec><fileGrp><file><FLocat xlink:href="page.xml"/></file></fileGrp></fileSec>
          </mets>
          """
              .formatted(dtd);
      assertEquals(
          List.of("page.xml"), MetsXml.create().files(mets.getBytes(StandardCharsets.UTF_8)));
      assertEquals(0, hits.get());
    } finally {
      server.stop(0);
    }
  }
}
