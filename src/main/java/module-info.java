/**
 * Reading, writing and converting PAGE XML documents, with an immutable page model.
 *
 * <ul>
 *   <li>{@link com.maxnth.page4j.model}: the page model, the superset of all PAGE versions; values
 *       of the schema's simple types are in {@link com.maxnth.page4j.model.value}.
 *   <li>{@link com.maxnth.page4j.pagexml.PageXml}: reads and writes PAGE XML 2009-03-16 to
 *       2024-07-15, validating against the bundled schemas.
 *   <li>{@link com.maxnth.page4j.convert.PageConverter}: fits a page to another PAGE version and
 *       reports what changed.
 *   <li>{@link com.maxnth.page4j.formats}: reads ALTO, ABBYY FineReader and hOCR, writes ALTO, and
 *       lists the files of a METS document.
 *   <li>{@link com.maxnth.page4j.geometry}: points, polygons and rectangles.
 * </ul>
 */
module com.maxnth.page4j {
  requires transitive java.xml;
  requires transitive org.jspecify;
  requires static io.soabase.recordbuilder.core;
  // The generated builders carry javax.annotation.processing.Generated (source retention).
  requires static transitive java.compiler;

  exports com.maxnth.page4j.convert;
  exports com.maxnth.page4j.formats;
  exports com.maxnth.page4j.geometry;
  exports com.maxnth.page4j.model;
  exports com.maxnth.page4j.model.value;
  exports com.maxnth.page4j.pagexml;
}
