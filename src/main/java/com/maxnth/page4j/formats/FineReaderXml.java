package com.maxnth.page4j.formats;

import com.maxnth.page4j.formats.internal.FineReaderReader;
import com.maxnth.page4j.formats.internal.FineReaderWriter;
import com.maxnth.page4j.model.Page;
import com.maxnth.page4j.pagexml.PageReadException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads ABBYY FineReader XML (FineReader 10 and the similar earlier versions) into the PAGE model
 * and writes the PAGE model as FineReader 10 XML.
 *
 * <p>Blocks become regions by their {@code blockType}; text and table blocks are replaced by their
 * paragraphs, which become text regions around their lines. A line's text is its characters. Only
 * the first page is read.
 *
 * <p>The document is read leniently, since there is no bundled schema; what can't be mapped to the
 * PAGE model is reported in the result's {@link ReadResult#diagnostics() diagnostics}.
 *
 * <p>Writing produces one block per region, in reading order and then in document order, with
 * nested regions as blocks of their own: text regions as {@code Text} blocks with one paragraph
 * around their lines, tables as {@code Table}, separators as {@code Separator}, barcodes as {@code
 * Barcode} and all other regions as {@code Picture}. Outlines become bounding boxes and baselines
 * their mean height. A line's words become characters, with the boxes of their glyphs or else their
 * box split evenly, and spaces between them; a line without words, or whose words lack a box or
 * don't spell its text, is written as plain text. Languages, alignment, confidences, the page size
 * and resolution are written; the rest is not. Where lines or words have no text, the text of their
 * region or line is split over them. Region IDs are not kept. The output is not validated.
 *
 * <p>External entities and DTDs are never loaded. Instances are immutable and thread-safe.
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

  /**
   * Writes a page as FineReader 10 XML.
   *
   * @throws IllegalArgumentException if the page holds text XML can't hold, such as control
   *     characters
   */
  public byte[] write(Page page) {
    return FineReaderWriter.write(page);
  }

  /**
   * Writes a page as FineReader 10 XML to a stream, which is not closed.
   *
   * @throws IOException if the stream can't be written
   * @throws IllegalArgumentException if the page holds text XML can't hold
   */
  public void write(Page page, OutputStream out) throws IOException {
    out.write(write(page));
  }

  /**
   * Writes a page as FineReader 10 XML to a file, replacing it if it exists.
   *
   * @throws IOException if the file can't be written
   * @throws IllegalArgumentException if the page holds text XML can't hold
   */
  public void write(Page page, Path path) throws IOException {
    Files.write(path, write(page));
  }
}
