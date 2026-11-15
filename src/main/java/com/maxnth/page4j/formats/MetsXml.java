package com.maxnth.page4j.formats;

import com.maxnth.page4j.formats.internal.MetsReader;
import com.maxnth.page4j.pagexml.PageReadException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Lists the files of a METS document, for example the pages of a multi-page document. External
 * entities and DTDs are never loaded. Instances are immutable and thread-safe.
 */
public final class MetsXml {

  private static final MetsXml INSTANCE = new MetsXml();

  private MetsXml() {}

  /** Returns the shared instance. */
  public static MetsXml create() {
    return INSTANCE;
  }

  /**
   * The files a METS file refers to ({@code FLocat/@xlink:href}), in document order.
   *
   * @throws PageReadException if the document is not well-formed XML or not METS
   * @throws IOException if the file can't be read
   */
  public List<String> files(Path path) throws IOException {
    return files(Files.readAllBytes(path));
  }

  /**
   * The files a METS stream refers to; the stream is read to the end and closed.
   *
   * @throws PageReadException if the document is not well-formed XML or not METS
   * @throws IOException if the stream can't be read
   */
  public List<String> files(InputStream in) throws IOException {
    try (in) {
      return files(in.readAllBytes());
    }
  }

  /**
   * The files a METS document in memory refers to.
   *
   * @throws PageReadException if the document is not well-formed XML or not METS
   */
  public List<String> files(byte[] xml) throws PageReadException {
    return MetsReader.files(xml);
  }
}
