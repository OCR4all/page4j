package com.maxnth.page4j.formats;

import com.maxnth.page4j.formats.internal.FineReaderReader;
import com.maxnth.page4j.pagexml.PageReadException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads ABBYY FineReader XML (FineReader 10 and the similar earlier versions) into the PAGE model.
 *
 * <p>Blocks become regions by their {@code blockType}; text and table blocks are replaced by their
 * paragraphs, which become text regions around their lines. A line's text is its characters. Only
 * the first page is read.
 *
 * <p>The document is read leniently, since there is no bundled schema; what can't be mapped to the
 * PAGE model is reported in the result's {@link ReadResult#diagnostics() diagnostics}. External
 * entities and DTDs are never loaded. Instances are immutable and thread-safe.
 */
public final class FineReaderXml {

  private static final FineReaderXml INSTANCE = new FineReaderXml();

  private FineReaderXml() {}

  /** Returns the shared instance. */
  public static FineReaderXml create() {
    return INSTANCE;
  }

  /**
   * Reads a page from a file.
   *
   * @throws PageReadException if the document is not well-formed XML or not FineReader XML
   * @throws IOException if the file can't be read
   */
  public ReadResult read(Path path) throws IOException {
    return read(Files.readAllBytes(path));
  }

  /**
   * Reads a page from a stream, which is read to the end and closed.
   *
   * @throws PageReadException if the document is not well-formed XML or not FineReader XML
   * @throws IOException if the stream can't be read
   */
  public ReadResult read(InputStream in) throws IOException {
    try (in) {
      return read(in.readAllBytes());
    }
  }

  /**
   * Reads a page from a document in memory.
   *
   * @throws PageReadException if the document is not well-formed XML or not FineReader XML
   */
  public ReadResult read(byte[] xml) throws PageReadException {
    return FineReaderReader.read(xml);
  }
}
