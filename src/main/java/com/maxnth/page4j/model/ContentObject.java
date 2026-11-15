package com.maxnth.page4j.model;

/** An object of the page that has an ID: a region, text object, grapheme, group or layer. */
public sealed interface ContentObject permits Region, TextObject, GraphemeElement, Group, Layer {

  /** The ID, unique within the document. */
  String id();
}
