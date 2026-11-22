package com.maxnth.page4j.formats;

import com.maxnth.page4j.formats.internal.AltoReader;
import com.maxnth.page4j.formats.internal.AltoWriter;
import com.maxnth.page4j.model.Page;
import com.maxnth.page4j.pagexml.PageReadException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads ALTO into the PAGE model and writes the PAGE model as ALTO.
 *
 * <p>Reading accepts any ALTO version and reads the first page: blocks become regions ({@code
 * TextBlock} text regions, {@code GraphicalElement} separators, {@code Illustration} image regions
 * or the PAGE region type its {@code TYPE} names), with lines, strings (words), glyphs, shapes,
 * {@code IDNEXT} chains as the reading order, styles, tags as labels and processing steps as
 * metadata items. {@code ROTATION} is the PAGE orientation. The document is read leniently, since
 * there is no bundled schema; what can't be mapped is reported in the result's {@link
 * AltoReadResult#diagnostics() diagnostics}.
 *
 * <p>Coordinates are in the document's {@link MeasurementUnit}. Read with the {@link SourceImage},
 * coordinates in {@code mm10} or {@code inch1200} are scaled to the image's pixels: by the ratio of
 * page and image size if all objects lie within the page, else by the image resolution.
 *
 * <p>Writing produces ALTO 4.1, in pixels. ALTO holds much less than PAGE: regions, lines, words
 * and glyphs with their outlines and text, languages, text styles with a font size, alignment and
 * leading, labels and text region types (as tags), ordered reading order groups (as {@code
 * IDNEXT}), processing steps and the page size, type and confidence are written; the rest is not.
 * Where words or glyphs have no text, the text of their line or region is split over them; a line
 * without words is written with one string for the whole line. The output is not validated.
 *
 * <p>External entities and DTDs are never loaded. Instances are immutable and thread-safe.
 */
public final class AltoXml {

  private static final AltoXml INSTANCE = new AltoXml();

  private AltoXml() {}

  /** Returns the shared instance. */
  public static AltoXml create() {
    return INSTANCE;
  }

  // ---- Reading

  /**
   * Reads a page from a file, with coordinates in the document's unit.
   *
   * @throws PageReadException if the document is not well-formed XML or not ALTO
   * @throws IOException if the file can't be read
   */
  public AltoReadResult read(Path path) throws IOException {
    return read(Files.readAllBytes(path));
  }

  /**
   * Reads a page from a file, with coordinates in pixels of the given image.
   *
   * @throws PageReadException if the document is not well-formed XML or not ALTO
   * @throws IOException if the file can't be read
   */
  public AltoReadResult read(Path path, SourceImage image) throws IOException {
    return read(Files.readAllBytes(path), image);
  }

  /**
   * Reads a page from a stream, which is read to the end and closed, with coordinates in the
   * document's unit.
   *
   * @throws PageReadException if the document is not well-formed XML or not ALTO
   * @throws IOException if the stream can't be read
   */
  public AltoReadResult read(InputStream in) throws IOException {
    try (in) {
      return read(in.readAllBytes());
    }
  }

  /**
   * Reads a page from a document in memory, with coordinates in the document's unit.
   *
   * @throws PageReadException if the document is not well-formed XML or not ALTO
   */
  public AltoReadResult read(byte[] xml) throws PageReadException {
    return AltoReader.read(xml, null);
  }

  /**
   * Reads a page from a document in memory, with coordinates in pixels of the given image.
   *
   * @throws PageReadException if the document is not well-formed XML or not ALTO
   */
  public AltoReadResult read(byte[] xml, SourceImage image) throws PageReadException {
    return AltoReader.read(xml, image);
  }

  // ---- Writing

  /**
   * Writes a page as ALTO 4.1.
   *
   * @throws IllegalArgumentException if the page holds text XML can't hold, such as control
   *     characters
   */
  public byte[] write(Page page) {
    return AltoWriter.write(page);
  }

  /**
   * Writes a page as ALTO 4.1 to a stream, which is not closed.
   *
   * @throws IOException if the stream can't be written
   * @throws IllegalArgumentException if the page holds text XML can't hold
   */
  public void write(Page page, OutputStream out) throws IOException {
    out.write(write(page));
  }

  /**
   * Writes a page as ALTO 4.1 to a file, replacing it if it exists.
   *
   * @throws IOException if the file can't be written
   * @throws IllegalArgumentException if the page holds text XML can't hold
   */
  public void write(Page page, Path path) throws IOException {
    Files.write(path, write(page));
  }
}
