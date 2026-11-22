package com.maxnth.page4j.formats;

import com.maxnth.page4j.formats.internal.HocrReader;
import com.maxnth.page4j.formats.internal.HocrWriter;
import com.maxnth.page4j.model.Page;
import com.maxnth.page4j.pagexml.PageReadException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads hOCR into the PAGE model and writes the PAGE model as hOCR. Read documents must be
 * well-formed XML (XHTML), as hOCR from Tesseract is.
 *
 * <p>Paragraphs ({@code ocr_par}), and areas ({@code ocr_carea}) with lines of their own, become
 * text regions; {@code ocr_line}, {@code ocr_textfloat}, {@code ocr_header} and {@code ocr_caption}
 * lines; {@code ocrx_word} words; {@code ocr_photo} and {@code ocr_image} image regions; {@code
 * ocr_separator} separators; {@code ocr_table}, {@code ocr_linedrawing}, {@code ocr_math} and
 * {@code ocr_chem} table, line drawing, maths and chemistry regions, whose content is read as well.
 * The language of the {@code ocr_page} becomes the page's; the {@code lang} of the {@code html}
 * element is not read, since Tesseract always writes {@code en}. {@code meta} elements become the
 * metadata comments, {@code ocr-system} the creator. Only the first page is read.
 *
 * <p>The document is read leniently, since there is no bundled schema; what can't be mapped to the
 * PAGE model is reported in the result's {@link ReadResult#diagnostics() diagnostics}.
 *
 * <p>Writing produces XHTML with one {@code ocr_page}. Regions follow in reading order and then in
 * document order, nested regions on their own: text regions as an {@code ocr_carea} around an
 * {@code ocr_par} with the region's ID, lines as {@code ocr_line} and words as {@code ocrx_word},
 * other regions as {@code ocr_photo}, {@code ocr_separator}, {@code ocr_table}, {@code ocr_math},
 * {@code ocr_chem}, {@code ocr_linedrawing} or {@code ocr_float}. Outlines become bounding boxes;
 * baselines, text angles, languages, reading directions and word confidences are written, the rest
 * is not. A line whose words lack a box or don't spell its text is written as its text alone. Where
 * lines or words have no text, the text of their region or line is split over them. The output is
 * not validated.
 *
 * <p>External entities and DTDs are never loaded. Instances are immutable and thread-safe.
 */
public final class Hocr {

  private static final Hocr INSTANCE = new Hocr();

  private Hocr() {}

  /** Returns the shared instance. */
  public static Hocr create() {
    return INSTANCE;
  }

  /**
   * Reads a page from a file.
   *
   * @throws PageReadException if the document is not well-formed XML or not HTML
   * @throws IOException if the file can't be read
   */
  public ReadResult read(Path path) throws IOException {
    return read(Files.readAllBytes(path));
  }

  /**
   * Reads a page from a stream, which is read to the end and closed.
   *
   * @throws PageReadException if the document is not well-formed XML or not HTML
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
   * @throws PageReadException if the document is not well-formed XML or not HTML
   */
  public ReadResult read(byte[] xml) throws PageReadException {
    return HocrReader.read(xml);
  }

  /**
   * Writes a page as hOCR.
   *
   * @throws IllegalArgumentException if the page holds text XML can't hold, such as control
   *     characters
   */
  public byte[] write(Page page) {
    return HocrWriter.write(page);
  }

  /**
   * Writes a page as hOCR to a stream, which is not closed.
   *
   * @throws IOException if the stream can't be written
   * @throws IllegalArgumentException if the page holds text XML can't hold
   */
  public void write(Page page, OutputStream out) throws IOException {
    out.write(write(page));
  }

  /**
   * Writes a page as hOCR to a file, replacing it if it exists.
   *
   * @throws IOException if the file can't be written
   * @throws IllegalArgumentException if the page holds text XML can't hold
   */
  public void write(Page page, Path path) throws IOException {
    Files.write(path, write(page));
  }
}
