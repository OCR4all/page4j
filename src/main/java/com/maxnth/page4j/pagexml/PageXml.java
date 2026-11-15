package com.maxnth.page4j.pagexml;

import com.maxnth.page4j.model.Page;
import com.maxnth.page4j.pagexml.internal.PageXmlReader;
import com.maxnth.page4j.pagexml.internal.PageXmlWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads and writes PAGE XML.
 *
 * <pre>{@code
 * PageXml xml = PageXml.create();
 * PageXmlReadResult read = xml.read(Path.of("page.xml"));
 * Page page = read.page().withRegions(...);
 * xml.write(page, read.version(), Path.of("page.xml"));
 * }</pre>
 *
 * <p>Reading validates the document against the bundled schema of its version before parsing it,
 * and fails on anything the schema doesn't allow. Writing is strict: the page must fit the target
 * version, and the result is validated before anything is written, so an invalid or partial
 * document is never produced. To write a page in another version than it was read in, fit it first
 * with {@link com.maxnth.page4j.convert.PageConverter}, which reports what it changes.
 *
 * <p>All PAGE versions are supported, 2009-03-16 to 2024-07-15. External entities and DTDs are
 * never loaded. Instances are immutable and thread-safe.
 */
public final class PageXml {

  private static final PageXml INSTANCE = new PageXml();

  private PageXml() {}

  /** Returns the shared instance. */
  public static PageXml create() {
    return INSTANCE;
  }

  // ---- Reading

  /**
   * Reads a page from a file.
   *
   * @throws PageReadException if the document is not valid PAGE XML of a supported version
   * @throws IOException if the file can't be read
   */
  public PageXmlReadResult read(Path path) throws IOException {
    return read(Files.readAllBytes(path));
  }

  /**
   * Reads a page from a stream, which is read to the end and closed.
   *
   * @throws PageReadException if the document is not valid PAGE XML of a supported version
   * @throws IOException if the stream can't be read
   */
  public PageXmlReadResult read(InputStream in) throws IOException {
    return PageXmlReader.read(in);
  }

  /**
   * Reads a page from a document in memory.
   *
   * @throws PageReadException if the document is not valid PAGE XML of a supported version
   */
  public PageXmlReadResult read(byte[] xml) throws PageReadException {
    return PageXmlReader.read(xml);
  }

  /**
   * The version of a document, given by the namespace of its root element. The document is not
   * validated.
   *
   * @throws PageReadException if the document is not PAGE XML or its version is unknown
   */
  public PageXmlVersion version(byte[] xml) throws PageReadException {
    return PageXmlReader.detectVersion(xml);
  }

  // ---- Writing

  /**
   * Writes a page in the given version.
   *
   * @throws PageWriteException if the page holds content the version can't express
   */
  public byte[] write(Page page, PageXmlVersion version) throws PageWriteException {
    return PageXmlWriter.write(page, version);
  }

  /**
   * Writes a page in the given version to a stream, which is not closed. Nothing is written if the
   * page can't be written.
   *
   * @throws PageWriteException if the page holds content the version can't express
   * @throws IOException if the stream can't be written
   */
  public void write(Page page, PageXmlVersion version, OutputStream out) throws IOException {
    out.write(write(page, version));
  }

  /**
   * Writes a page in the given version to a file, replacing it if it exists. The file is not
   * touched if the page can't be written.
   *
   * @throws PageWriteException if the page holds content the version can't express
   * @throws IOException if the file can't be written
   */
  public void write(Page page, PageXmlVersion version, Path path) throws IOException {
    Files.write(path, write(page, version));
  }
}
