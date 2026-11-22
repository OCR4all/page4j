package com.maxnth.page4j.formats;

import com.maxnth.page4j.geometry.Point;
import com.maxnth.page4j.geometry.Polygon;
import com.maxnth.page4j.geometry.Rect;
import com.maxnth.page4j.model.ImageRegionBuilder;
import com.maxnth.page4j.model.MetadataBuilder;
import com.maxnth.page4j.model.OrderedGroupBuilder;
import com.maxnth.page4j.model.Page;
import com.maxnth.page4j.model.ReadingOrderBuilder;
import com.maxnth.page4j.model.RegionRef;
import com.maxnth.page4j.model.SeparatorRegionBuilder;
import com.maxnth.page4j.model.TextEquiv;
import com.maxnth.page4j.model.TextEquivBuilder;
import com.maxnth.page4j.model.TextLineBuilder;
import com.maxnth.page4j.model.TextRegionBuilder;
import com.maxnth.page4j.model.WordBuilder;
import com.maxnth.page4j.model.value.Align;
import com.maxnth.page4j.model.value.Language;
import com.maxnth.page4j.model.value.PageDateTime;
import com.maxnth.page4j.model.value.ReadingDirection;
import com.maxnth.page4j.model.value.ResolutionUnit;
import java.util.List;

final class WriterTestPages {

  private static final PageDateTime TIME = PageDateTime.parse("2020-01-02T03:04:05");

  private WriterTestPages() {}

  static Polygon box(int left, int top, int right, int bottom) {
    return new Rect(left, top, right, bottom).toPolygon();
  }

  static TextEquiv text(String unicode, Double conf) {
    return TextEquivBuilder.builder().unicode(unicode).conf(conf).build();
  }

  static Page page() {
    return page("engine", "Hello Wörld");
  }

  static Page page(String creator, String firstLineText) {
    return page(creator, firstLineText, "page.png");
  }

  static Page page(String creator, String firstLineText, String imageFilename) {
    return Page.builder()
        .pcGtsId("page1")
        .metadata(MetadataBuilder.builder().creator(creator).created(TIME).lastChange(TIME).build())
        .imageFilename(imageFilename)
        .imageWidth(1000)
        .imageHeight(800)
        .imageXResolution(300.0)
        .imageYResolution(300.0)
        .imageResolutionUnit(ResolutionUnit.PPI)
        .primaryLanguage(Language.GERMAN)
        .readingOrder(
            ReadingOrderBuilder.builder()
                .group(
                    OrderedGroupBuilder.builder()
                        .id("ro")
                        .addMembers(new RegionRef("i1"))
                        .addMembers(new RegionRef("r1"))
                        .build())
                .build())
        .regions(
            List.of(
                TextRegionBuilder.builder()
                    .id("r1")
                    .coords(box(100, 100, 900, 300))
                    .align(Align.CENTRE)
                    .readingDirection(ReadingDirection.LEFT_TO_RIGHT)
                    .addTextLines(
                        TextLineBuilder.builder()
                            .id("l1")
                            .coords(box(100, 100, 500, 150))
                            .baseline(Polygon.of(new Point(100, 140), new Point(500, 140)))
                            .addWords(
                                WordBuilder.builder()
                                    .id("w1")
                                    .coords(box(100, 100, 249, 150))
                                    .addTextEquivs(text("Hello", 0.9))
                                    .build())
                            .addWords(
                                WordBuilder.builder()
                                    .id("w2")
                                    .coords(box(300, 100, 500, 150))
                                    .addTextEquivs(text("Wörld", 0.75))
                                    .build())
                            .addTextEquivs(text(firstLineText, null))
                            .build())
                    .addTextLines(
                        TextLineBuilder.builder()
                            .id("l2")
                            .coords(box(100, 200, 900, 250))
                            .primaryLanguage(Language.ENGLISH)
                            .addTextEquivs(text("second & <line>", null))
                            .build())
                    .build(),
                SeparatorRegionBuilder.builder().id("s1").coords(box(0, 650, 999, 660)).build(),
                ImageRegionBuilder.builder().id("i1").coords(box(100, 400, 500, 600)).build()))
        .build();
  }
}
