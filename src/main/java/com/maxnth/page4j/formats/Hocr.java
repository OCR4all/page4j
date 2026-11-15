package com.maxnth.page4j.formats;

import com.maxnth.page4j.formats.internal.HocrReader;
import com.maxnth.page4j.pagexml.PageReadException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads hOCR into the PAGE model. The document must be well-formed XML (XHTML), as hOCR from
 * Tesseract is.
 *
 * <p>Paragraphs ({@code ocr_par}), and areas ({@code ocr_carea}) with lines of their own, become
 * text regions; {@code ocr_line}, {@code ocr_textfloat}, {@code ocr_header} and {@code ocr_caption}
 * lines; {@code ocrx_word} words; {@code ocr_photo} and {@code ocr_image} image regions; {@code
 * ocr_separator} separators. {@code meta} elements become the metadata comments, {@code ocr-system}
 * the creator. Only the first page is read.
 *
 * <p>The document is read leniently, since there is no bundled schema; what can't be mapped to the
 * PAGE model is reported in the result's {@link ReadResult#diagnostics() diagnostics}. External
 * entities and DTDs are never loaded. Instances are immutable and thread-safe.
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
}
