package com.maxnth.page4j.pagexml;

import com.maxnth.page4j.model.Page;
import java.util.Objects;

/** A page read from PAGE XML, with the version it was written in. */
public record PageXmlReadResult(Page page, PageXmlVersion version) {
  public PageXmlReadResult {
    Objects.requireNonNull(page, "page");
    Objects.requireNonNull(version, "version");
  }
}
