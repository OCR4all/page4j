/**
 * Reading ALTO, ABBYY FineReader and hOCR into the PAGE model, writing ALTO, and listing the page
 * files of a METS document.
 *
 * <p>These formats have no bundled schema, so they are read leniently: what a reader can't map is
 * reported as a {@link com.maxnth.page4j.formats.ReadDiagnostic}, never dropped silently.
 */
@NullMarked
package com.maxnth.page4j.formats;

import org.jspecify.annotations.NullMarked;
