package com.maxnth.page4j.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.maxnth.page4j.geometry.Point;
import com.maxnth.page4j.geometry.Polygon;
import com.maxnth.page4j.model.value.PageDateTime;
import com.maxnth.page4j.model.value.RelationType;
import com.maxnth.page4j.model.value.TextType;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PageTest {

  private static final Polygon BOX =
      Polygon.of(new Point(0, 0), new Point(10, 0), new Point(10, 10));

  private static Metadata metadata() {
    PageDateTime time = PageDateTime.parse("2024-01-02T03:04:05");
    return MetadataBuilder.builder().creator("test").created(time).lastChange(time).build();
  }

  private static TextLine line(String id, String... words) {
    var builder = TextLineBuilder.builder().id(id).coords(BOX);
    for (String word : words) {
      builder.addWords(WordBuilder.builder().id(word).coords(BOX).build());
    }
    return builder.build();
  }

  private static TextRegion text(String id, TextLine... lines) {
    return TextRegionBuilder.builder().id(id).coords(BOX).textLines(List.of(lines)).build();
  }

  private static OrderedGroup ordered(String id, GroupMember... members) {
    return OrderedGroupBuilder.builder().id(id).members(List.of(members)).build();
  }

  /** r1 (l1: w1 w2) containing nested r1a (l2: w3), image i1, reading order, layer, relation. */
  private static Page page() {
    TextRegion nested = text("r1a", line("l2", "w3"));
    TextRegion r1 = text("r1", line("l1", "w1", "w2")).withRegions(List.of(nested));
    ImageRegion i1 = ImageRegionBuilder.builder().id("i1").coords(BOX).build();
    return Page.builder()
        .metadata(metadata())
        .imageFilename("page.png")
        .imageWidth(100)
        .imageHeight(200)
        .readingOrder(
            new ReadingOrder(
                null,
                ordered(
                    "g0",
                    new RegionRef("r1"),
                    ordered("g1", new RegionRef("i1"), new RegionRef("r1a")))))
        .addLayers(LayerBuilder.builder().id("layer").zIndex(0).addRegionRefs("i1").build())
        .addRelations(
            RelationBuilder.builder()
                .id("rel")
                .type(RelationType.LINK)
                .sourceRegionRef("w1")
                .targetRegionRef("i1")
                .build())
        .addRegions(r1)
        .addRegions(i1)
        .build();
  }

  @Test
  void findsObjectsByIdAndType() {
    Page page = page();
    assertEquals("r1a", page.find("r1a").orElseThrow().id());
    assertTrue(page.find("r1a", TextRegion.class).isPresent());
    assertTrue(page.find("r1a", ImageRegion.class).isEmpty());
    assertTrue(page.find("nope").isEmpty());
    assertTrue(page.find("g1", Group.class).isPresent());
    assertTrue(page.find("layer", Layer.class).isPresent());
  }

  @Test
  void listsObjectsInDocumentOrder() {
    Page page = page();
    // Nested regions come before the text lines of their region, as in the XML.
    assertEquals(List.of("r1", "r1a", "i1"), page.allRegions().stream().map(Region::id).toList());
    assertEquals(List.of("l2", "l1"), page.textLines().stream().map(TextLine::id).toList());
    assertEquals(List.of("w3", "w1", "w2"), page.words().stream().map(Word::id).toList());
    assertEquals(List.of("g0", "g1"), page.all(Group.class).stream().map(Group::id).toList());
  }

  @Test
  void knowsTheParentOfEachObject() {
    Page page = page();
    assertEquals(Optional.empty(), page.parentOf("r1"));
    assertEquals("r1", page.parentOf("r1a").orElseThrow().id());
    assertEquals("l1", page.parentOf("w2").orElseThrow().id());
    assertEquals("g0", page.parentOf("g1").orElseThrow().id());
    assertThrows(NoSuchElementException.class, () -> page.parentOf("nope"));
  }

  @Test
  void findsRelationsOfAnObject() {
    Page page = page();
    assertEquals(List.of("rel"), page.relations("w1").stream().map(Relation::id).toList());
    assertEquals(List.of("rel"), page.relations("i1").stream().map(Relation::id).toList());
    assertEquals(List.of(), page.relations("r1"));
  }

  @Test
  void rejectsDuplicateIds() {
    var builder = page().toBuilder().addRegions(text("l1"));
    var e = assertThrows(InvalidPageException.class, builder::build);
    assertEquals(List.of("Duplicate ID 'l1'"), e.problems());
  }

  @Test
  void rejectsDuplicateIdsOfRelationsAndTheDocument() {
    var builder =
        page().toBuilder()
            .pcGtsId("r1")
            .addRelations(
                RelationBuilder.builder()
                    .id("rel")
                    .sourceRegionRef("r1")
                    .targetRegionRef("i1")
                    .build());
    var e = assertThrows(InvalidPageException.class, builder::build);
    assertEquals(List.of("Duplicate ID 'rel'", "Duplicate ID 'r1'"), e.problems());
  }

  @Test
  void rejectsUnresolvedReferences() {
    var builder =
        page().toBuilder()
            .readingOrder(new ReadingOrder(null, ordered("g", new RegionRef("missing1"))))
            .addLayers(LayerBuilder.builder().id("l").zIndex(1).addRegionRefs("missing2").build())
            .addRelations(
                RelationBuilder.builder()
                    .sourceRegionRef("r1")
                    .targetRegionRef("missing3")
                    .build());
    var e = assertThrows(InvalidPageException.class, builder::build);
    assertEquals(
        List.of(
            "Unresolved reference 'missing1' in OrderedGroup 'g'",
            "Unresolved reference 'missing2' in Layer 'l'",
            "Unresolved reference 'missing3' in Relation"),
        e.problems());
  }

  @Test
  void replaceRebuildsThePathToTheObject() {
    Page page = page();
    Word w3 = page.find("w3", Word.class).orElseThrow();
    Page changed = page.replace("w3", w3.withComments("changed"));

    assertEquals("changed", changed.find("w3", Word.class).orElseThrow().comments());
    assertNull(page.find("w3", Word.class).orElseThrow().comments());
    // Untouched branches are shared, not copied.
    assertSame(page.regions().get(1), changed.regions().get(1));
    assertSame(page.readingOrder(), changed.readingOrder());
  }

  @Test
  void replaceAcceptsAnotherTypeOfTheSameKind() {
    Page page = page();
    Region custom = CustomRegionBuilder.builder().id("i1").coords(BOX).type("stamp").build();
    Page changed = page.replace("i1", custom);
    assertTrue(changed.find("i1", CustomRegion.class).isPresent());

    TextRegion r1a = page.find("r1a", TextRegion.class).orElseThrow();
    Page retyped = page.replace("r1a", r1a.withType(TextType.HEADING));
    assertEquals(TextType.HEADING, retyped.find("r1a", TextRegion.class).orElseThrow().type());
  }

  @Test
  void replaceRejectsObjectsThatDontFit() {
    Page page = page();
    Word word = WordBuilder.builder().id("x").coords(BOX).build();
    assertThrows(IllegalArgumentException.class, () -> page.replace("l1", word));
    assertThrows(NoSuchElementException.class, () -> page.replace("nope", word));
    // Changing the ID leaves references to the old one dangling.
    Region renamed = page.find("i1", Region.class).orElseThrow();
    ImageRegion image = ((ImageRegion) renamed).withId("i2");
    assertThrows(InvalidPageException.class, () -> page.replace("i1", image));
  }

  @Test
  void removeDropsTheObjectWithItsContentAndReferences() {
    Page page = page().remove("r1");

    assertEquals(List.of("i1"), page.allRegions().stream().map(Region::id).toList());
    assertTrue(page.find("w1").isEmpty());
    // r1 and its nested r1a are gone from the reading order; g1 keeps i1.
    OrderedGroup root = (OrderedGroup) page.readingOrder().group();
    assertEquals(1, root.members().size());
    assertEquals(
        List.of(new RegionRef("i1")), ((OrderedGroup) root.members().getFirst()).members());
    // The relation from w1 is gone.
    assertEquals(List.of(), page.relations());
  }

  @Test
  void removeDropsGroupsLayersAndReadingOrderLeftEmpty() {
    Page page = page().remove("i1");
    assertEquals(List.of(), page.layers());
    assertEquals(List.of(), page.relations());

    Page noRegions = page.remove("r1");
    assertNull(noRegions.readingOrder());
  }

  @Test
  void removeClearsGroupRegionRefsToRemovedObjects() {
    Page page =
        page()
            .withReadingOrder(
                new ReadingOrder(
                    null,
                    OrderedGroupBuilder.builder()
                        .id("g")
                        .regionRef("r1a")
                        .addMembers(new RegionRef("i1"))
                        .build()));
    Group group = page.remove("r1a").readingOrder().group();
    assertNull(group.regionRef());
  }

  @Test
  void removeDropsRelationsAndGroupsById() {
    Page page = page();
    assertEquals(List.of(), page.remove("rel").relations());
    Page withoutGroup = page.remove("g1");
    assertEquals(List.of(new RegionRef("r1")), withoutGroup.readingOrder().group().members());
    assertThrows(NoSuchElementException.class, () -> page.remove("nope"));
  }

  @Test
  void removeAllRemovesSeveralObjectsAndRelationsAtOnce() {
    Page page = page().removeAll(List.of("w1", "r1a", "layer"));
    assertTrue(page.find("w1").isEmpty());
    assertTrue(page.find("w3").isEmpty());
    assertTrue(page.find("w2").isPresent());
    assertEquals(List.of(), page.layers());
    assertEquals(List.of(), page.relations());
    assertEquals(
        List.of(new RegionRef("r1"), ordered("g1", new RegionRef("i1"))),
        page.readingOrder().group().members());

    assertSame(page, page.removeAll(List.of()));
    assertEquals(List.of(), page().removeAll(List.of("rel")).relations());
    assertThrows(NoSuchElementException.class, () -> page().removeAll(List.of("w1", "nope")));
  }

  @Test
  void requiresTheMandatoryValues() {
    assertThrows(NullPointerException.class, () -> Page.builder().imageFilename("a").build());
  }
}
