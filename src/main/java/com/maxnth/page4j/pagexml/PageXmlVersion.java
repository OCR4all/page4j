package com.maxnth.page4j.pagexml;

import java.util.Optional;

/** A version of the PAGE XML schema, named after its date. */
public enum PageXmlVersion {
  V2009_03_16("2009-03-16"),
  V2010_01_12("2010-01-12"),
  V2010_03_19("2010-03-19"),
  V2013_07_15("2013-07-15"),
  V2016_07_15("2016-07-15"),
  V2017_07_15("2017-07-15"),
  V2018_07_15("2018-07-15"),
  V2019_07_15("2019-07-15"),
  V2024_07_15("2024-07-15");

  /** The newest version. */
  public static final PageXmlVersion LATEST = V2024_07_15;

  private static final String NAMESPACE_PREFIX =
      "http://schema.primaresearch.org/PAGE/gts/pagecontent/";

  private final String date;

  PageXmlVersion(String date) {
    this.date = date;
  }

  /** The date of the version, for example {@code 2019-07-15}. */
  public String date() {
    return date;
  }

  /** The namespace of the version's elements. */
  public String namespace() {
    return NAMESPACE_PREFIX + date;
  }

  /** The location of the version's schema, as written to {@code xsi:schemaLocation}. */
  public String schemaLocation() {
    return NAMESPACE_PREFIX + date + "/pagecontent.xsd";
  }

  /** The version with the given namespace. */
  public static Optional<PageXmlVersion> fromNamespace(String namespace) {
    for (PageXmlVersion version : values()) {
      if (version.namespace().equals(namespace)) {
        return Optional.of(version);
      }
    }
    return Optional.empty();
  }

  @Override
  public String toString() {
    return "PAGE " + date;
  }
}
