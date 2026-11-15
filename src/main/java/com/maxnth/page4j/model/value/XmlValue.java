package com.maxnth.page4j.model.value;

/** A value with a fixed representation in PAGE XML, such as an enumeration constant. */
public interface XmlValue {

  /** Returns the value as written in PAGE XML. */
  String xmlValue();
}
