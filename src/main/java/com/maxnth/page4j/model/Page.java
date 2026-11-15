package com.maxnth.page4j.model;

import com.maxnth.page4j.geometry.Polygon;
import com.maxnth.page4j.model.value.Language;
import com.maxnth.page4j.model.value.PageType;
import com.maxnth.page4j.model.value.ReadingDirection;
import com.maxnth.page4j.model.value.ResolutionUnit;
import com.maxnth.page4j.model.value.Script;
import com.maxnth.page4j.model.value.TextLineOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/**
 * A PAGE document: the page with its metadata ({@code PcGts} with its {@code Metadata} and {@code
 * Page}).
 *
 * <p>A page is immutable. Every ID in it is unique and every reference ({@code regionRef}, layer
 * and relation references) resolves to an ID of the page; the constructor throws {@link
 * InvalidPageException} otherwise. Objects can be looked up by ID, and {@link #replace} and {@link
 * #remove} return a changed copy of the page.
 */
public final class Page {

  private final @Nullable String pcGtsId;
  private final Metadata metadata;
  private final String imageFilename;
  private final int imageWidth;
  private final int imageHeight;
  private final @Nullable Double imageXResolution;
  private final @Nullable Double imageYResolution;
  private final @Nullable ResolutionUnit imageResolutionUnit;
  private final @Nullable String custom;
  private final @Nullable String comments;
  private final @Nullable Double orientation;
  private final @Nullable PageType type;
  private final @Nullable Language primaryLanguage;
  private final @Nullable Language secondaryLanguage;
  private final @Nullable String customLanguages;
  private final @Nullable Script primaryScript;
  private final @Nullable Script secondaryScript;
  private final @Nullable String customScripts;
  private final @Nullable ReadingDirection readingDirection;
  private final @Nullable TextLineOrder textLineOrder;
  private final @Nullable Double conf;
  private final List<AlternativeImage> alternativeImages;
  private final @Nullable Polygon border;
  private final @Nullable Polygon printSpace;
  private final @Nullable ReadingOrder readingOrder;
  private final List<Layer> layers;
  private final List<Relation> relations;
  private final @Nullable TextStyle textStyle;
  private final List<UserAttribute> userAttributes;
  private final List<LabelGroup> labels;
  private final List<Region> regions;

  /** Content objects by ID, in document order. */
  private final Map<String, ContentObject> objects;

  /** The parent of each content object that has one (nested regions, text objects, groups). */
  private final Map<String, ContentObject> parents;

  private Page(Builder b) {
    pcGtsId = b.pcGtsId;
    metadata = Objects.requireNonNull(b.metadata, "metadata");
    imageFilename = Objects.requireNonNull(b.imageFilename, "imageFilename");
    imageWidth = Objects.requireNonNull(b.imageWidth, "imageWidth");
    imageHeight = Objects.requireNonNull(b.imageHeight, "imageHeight");
    imageXResolution = b.imageXResolution;
    imageYResolution = b.imageYResolution;
    imageResolutionUnit = b.imageResolutionUnit;
    custom = b.custom;
    comments = b.comments;
    orientation = b.orientation;
    type = b.type;
    primaryLanguage = b.primaryLanguage;
    secondaryLanguage = b.secondaryLanguage;
    customLanguages = b.customLanguages;
    primaryScript = b.primaryScript;
    secondaryScript = b.secondaryScript;
    customScripts = b.customScripts;
    readingDirection = b.readingDirection;
    textLineOrder = b.textLineOrder;
    conf = b.conf;
    alternativeImages = List.copyOf(b.alternativeImages);
    border = b.border;
    printSpace = b.printSpace;
    readingOrder = b.readingOrder;
    layers = List.copyOf(b.layers);
    relations = List.copyOf(b.relations);
    textStyle = b.textStyle;
    userAttributes = List.copyOf(b.userAttributes);
    labels = List.copyOf(b.labels);
    regions = List.copyOf(b.regions);

    Index index = new Index();
    index.page(this);
    index.checkReferences(this);
    if (!index.problems.isEmpty()) {
      throw new InvalidPageException(index.problems);
    }
    objects = index.objects;
    parents = index.parents;
  }

  public static Builder builder() {
    return new Builder();
  }

  /** A builder holding the content of this page. */
  public Builder toBuilder() {
    return new Builder(this);
  }

  // ---- Lookup

  /** The content object with the given ID. */
  public Optional<ContentObject> find(String id) {
    return Optional.ofNullable(objects.get(id));
  }

  /** The content object with the given ID, if it is of the given type. */
  public <T extends ContentObject> Optional<T> find(String id, Class<T> type) {
    return find(id).filter(type::isInstance).map(type::cast);
  }

  /**
   * The object that contains the object with the given ID: the region of a nested region or line,
   * the line of a word, and so on, or the parent group of a reading order group.
   *
   * @return empty for top-level regions, top-level groups and layers, whose parent is the page
   * @throws NoSuchElementException if the page has no content object with this ID
   */
  public Optional<ContentObject> parentOf(String id) {
    if (!objects.containsKey(id)) {
      throw new NoSuchElementException("No content object with ID '" + id + "'");
    }
    return Optional.ofNullable(parents.get(id));
  }

  /** All content objects of the given type, in document order. */
  public <T extends ContentObject> List<T> all(Class<T> type) {
    return objects.values().stream().filter(type::isInstance).map(type::cast).toList();
  }

  /** All regions including nested ones, in document order. */
  public List<Region> allRegions() {
    return all(Region.class);
  }

  /** All text lines, in document order. */
  public List<TextLine> textLines() {
    return all(TextLine.class);
  }

  /** All words, in document order. */
  public List<Word> words() {
    return all(Word.class);
  }

  /** All glyphs, in document order. */
  public List<Glyph> glyphs() {
    return all(Glyph.class);
  }

  /** The relations whose source or target is the object with the given ID. */
  public List<Relation> relations(String id) {
    return relations.stream()
        .filter(r -> r.sourceRegionRef().equals(id) || r.targetRegionRef().equals(id))
        .toList();
  }

  // ---- Editing

  public Page withMetadata(Metadata metadata) {
    return toBuilder().metadata(metadata).build();
  }

  public Page withRegions(Collection<? extends Region> regions) {
    return toBuilder().regions(regions).build();
  }

  public Page withReadingOrder(@Nullable ReadingOrder readingOrder) {
    return toBuilder().readingOrder(readingOrder).build();
  }

  public Page withRelations(Collection<? extends Relation> relations) {
    return toBuilder().relations(relations).build();
  }

  public Page withLayers(Collection<? extends Layer> layers) {
    return toBuilder().layers(layers).build();
  }

  /**
   * Replaces the content object with the given ID, wherever it is in the page. The replacement must
   * fit the place: a region for a region, a text line for a text line, and so on.
   *
   * <p>References to the old ID are not changed; if the replacement has another ID, the caller has
   * to update them, or the new page is invalid.
   *
   * @throws NoSuchElementException if the page has no content object with this ID
   * @throws IllegalArgumentException if the replacement does not fit the place of the object
   * @throws InvalidPageException if the resulting page is invalid
   */
  public Page replace(String id, ContentObject replacement) {
    ContentObject old =
        find(id).orElseThrow(() -> new NoSuchElementException("No content object with ID '" + id));
    Class<?> slot = slotType(old);
    if (!slot.isInstance(replacement)) {
      throw new IllegalArgumentException(
          "Cannot replace " + describe(old) + " with " + describe(replacement));
    }
    return rewrite(o -> o.id().equals(id) ? replacement : o).build();
  }

  /**
   * Removes the content object or relation with the given ID, with everything it contains, and all
   * references to the removed objects: region references in the reading order, layer references and
   * relations. Groups and layers left empty are removed as well, and the reading order if its root
   * group is left empty.
   *
   * @throws NoSuchElementException if the page has no content object or relation with this ID
   */
  public Page remove(String id) {
    return removeAll(List.of(id));
  }

  /**
   * Removes the content objects and relations with the given IDs, like {@link #remove(String)}, in
   * one pass.
   *
   * @throws NoSuchElementException if the page has no content object or relation with one of the
   *     IDs
   */
  public Page removeAll(Collection<String> ids) {
    if (ids.isEmpty()) {
      return this;
    }
    Index removed = new Index();
    Set<String> relationIds = new HashSet<>();
    for (String id : ids) {
      ContentObject object = objects.get(id);
      if (object != null) {
        removed.object(object, null);
      } else if (relations.stream().anyMatch(r -> id.equals(r.id()))) {
        relationIds.add(id);
      } else {
        throw new NoSuchElementException("No content object or relation with ID '" + id + "'");
      }
    }
    Set<String> gone = removed.objects.keySet();

    Builder b = gone.isEmpty() ? toBuilder() : rewrite(o -> gone.contains(o.id()) ? null : o);
    ReadingOrder order = b.readingOrder;
    if (order != null) {
      Group root = prune(order.group(), gone);
      if (root != order.group()) {
        b.readingOrder(root == null ? null : order.withGroup(root));
      }
    }
    List<Layer> keptLayers = new ArrayList<>();
    for (Layer layer : b.layers) {
      List<String> refs = layer.regionRefs().stream().filter(r -> !gone.contains(r)).toList();
      if (!refs.isEmpty()) {
        keptLayers.add(
            refs.size() == layer.regionRefs().size() ? layer : layer.withRegionRefs(refs));
      }
    }
    b.layers(keptLayers);
    b.relations(
        b.relations.stream()
            .filter(r -> r.id() == null || !relationIds.contains(r.id()))
            .filter(r -> !gone.contains(r.sourceRegionRef()) && !gone.contains(r.targetRegionRef()))
            .toList());
    return b.build();
  }

  /** An edit of one content object: returns it to keep it, null to remove it, or a replacement. */
  private interface Edit {
    @Nullable ContentObject apply(ContentObject object);
  }

  private Builder rewrite(Edit edit) {
    Builder b = toBuilder();
    b.regions(map(regions, r -> region(r, edit)));
    if (readingOrder != null) {
      Group root = group(readingOrder.group(), edit);
      if (root != readingOrder.group()) {
        b.readingOrder(root == null ? null : readingOrder.withGroup(root));
      }
    }
    b.layers(map(layers, l -> fit(edit.apply(l), l, Layer.class)));
    return b;
  }

  private static <T> List<T> map(List<T> items, Function<T, @Nullable T> f) {
    List<T> result = new ArrayList<>(items.size());
    boolean changed = false;
    for (T item : items) {
      T mapped = f.apply(item);
      changed |= mapped != item;
      if (mapped != null) {
        result.add(mapped);
      }
    }
    return changed ? result : items;
  }

  private static <T extends ContentObject> @Nullable T fit(
      @Nullable ContentObject result, T original, Class<T> type) {
    if (result == null || result == original) {
      return result == null ? null : original;
    }
    if (!type.isInstance(result)) {
      throw new IllegalArgumentException(
          "Cannot replace " + describe(original) + " with " + describe(result));
    }
    return type.cast(result);
  }

  private static @Nullable Region region(Region region, Edit edit) {
    ContentObject result = edit.apply(region);
    if (result != region) {
      return fit(result, region, Region.class);
    }
    List<Region> nested = map(region.regions(), r -> region(r, edit));
    Region updated = nested == region.regions() ? region : withRegions(region, nested);
    if (updated instanceof TextRegion text) {
      List<TextLine> lines = map(text.textLines(), l -> textLine(l, edit));
      if (lines != text.textLines()) {
        updated = text.withTextLines(lines);
      }
    }
    return updated;
  }

  private static @Nullable TextLine textLine(TextLine line, Edit edit) {
    ContentObject result = edit.apply(line);
    if (result != line) {
      return fit(result, line, TextLine.class);
    }
    List<Word> words = map(line.words(), w -> word(w, edit));
    return words == line.words() ? line : line.withWords(words);
  }

  private static @Nullable Word word(Word word, Edit edit) {
    ContentObject result = edit.apply(word);
    if (result != word) {
      return fit(result, word, Word.class);
    }
    List<Glyph> glyphs = map(word.glyphs(), g -> glyph(g, edit));
    return glyphs == word.glyphs() ? word : word.withGlyphs(glyphs);
  }

  private static @Nullable Glyph glyph(Glyph glyph, Edit edit) {
    ContentObject result = edit.apply(glyph);
    if (result != glyph) {
      return fit(result, glyph, Glyph.class);
    }
    List<GraphemeElement> graphemes = map(glyph.graphemes(), g -> grapheme(g, edit));
    return graphemes == glyph.graphemes() ? glyph : glyph.withGraphemes(graphemes);
  }

  private static @Nullable GraphemeElement grapheme(GraphemeElement grapheme, Edit edit) {
    ContentObject result = edit.apply(grapheme);
    if (result != grapheme) {
      return fit(result, grapheme, GraphemeElement.class);
    }
    if (grapheme instanceof GraphemeGroup group) {
      List<GraphemeElement> members = map(group.graphemes(), g -> grapheme(g, edit));
      return members == group.graphemes() ? group : group.withGraphemes(members);
    }
    return grapheme;
  }

  private static @Nullable Group group(Group group, Edit edit) {
    ContentObject result = edit.apply(group);
    if (result != group) {
      return fit(result, group, Group.class);
    }
    List<GroupMember> members =
        map(group.members(), m -> m instanceof Group child ? group(child, edit) : m);
    return members == group.members() ? group : withMembers(group, members);
  }

  /** Removes references to the given IDs from a group; null if the group is left empty. */
  private static @Nullable Group prune(Group group, Set<String> ids) {
    List<GroupMember> members =
        map(
            group.members(),
            m ->
                switch (m) {
                  case RegionRef ref -> ids.contains(ref.regionRef()) ? null : ref;
                  case Group child -> prune(child, ids);
                });
    if (members.isEmpty()) {
      return null;
    }
    Group updated = members == group.members() ? group : withMembers(group, members);
    String ref = updated.regionRef();
    if (ref != null && ids.contains(ref)) {
      updated =
          switch (updated) {
            case OrderedGroup g -> g.withRegionRef(null);
            case UnorderedGroup g -> g.withRegionRef(null);
          };
    }
    return updated;
  }

  private static Group withMembers(Group group, List<GroupMember> members) {
    return switch (group) {
      case OrderedGroup g -> g.withMembers(members);
      case UnorderedGroup g -> g.withMembers(members);
    };
  }

  private static Region withRegions(Region region, List<Region> regions) {
    return switch (region) {
      case TextRegion r -> r.withRegions(regions);
      case ImageRegion r -> r.withRegions(regions);
      case LineDrawingRegion r -> r.withRegions(regions);
      case GraphicRegion r -> r.withRegions(regions);
      case TableRegion r -> r.withRegions(regions);
      case ChartRegion r -> r.withRegions(regions);
      case MapRegion r -> r.withRegions(regions);
      case SeparatorRegion r -> r.withRegions(regions);
      case MathsRegion r -> r.withRegions(regions);
      case ChemRegion r -> r.withRegions(regions);
      case MusicRegion r -> r.withRegions(regions);
      case AdvertRegion r -> r.withRegions(regions);
      case FormRegion r -> r.withRegions(regions);
      case FrameRegion r -> r.withRegions(regions);
      case NoiseRegion r -> r.withRegions(regions);
      case UnknownRegion r -> r.withRegions(regions);
      case CustomRegion r -> r.withRegions(regions);
    };
  }

  /** The type an object must have to take the place of the given one. */
  private static Class<? extends ContentObject> slotType(ContentObject object) {
    return switch (object) {
      case Region r -> Region.class;
      case TextLine l -> TextLine.class;
      case Word w -> Word.class;
      case Glyph g -> Glyph.class;
      case GraphemeElement g -> GraphemeElement.class;
      case Group g -> Group.class;
      case Layer l -> Layer.class;
    };
  }

  private static String describe(ContentObject object) {
    return object.getClass().getSimpleName() + " '" + object.id() + "'";
  }

  // ---- Index and validation

  /** Walks the page in document order, collecting content objects and checking IDs. */
  private static final class Index {
    final Map<String, ContentObject> objects = new LinkedHashMap<>();
    final Map<String, ContentObject> parents = new HashMap<>();
    final Set<String> ids = new HashSet<>();
    final List<String> problems = new ArrayList<>();

    void page(Page page) {
      if (page.pcGtsId != null) {
        id(page.pcGtsId);
      }
      if (page.readingOrder != null) {
        object(page.readingOrder.group(), null);
      }
      for (Layer layer : page.layers) {
        object(layer, null);
      }
      for (Relation relation : page.relations) {
        if (relation.id() != null) {
          id(relation.id());
        }
      }
      for (Region region : page.regions) {
        object(region, null);
      }
    }

    void object(ContentObject object, @Nullable ContentObject parent) {
      id(object.id());
      objects.putIfAbsent(object.id(), object);
      if (parent != null) {
        parents.putIfAbsent(object.id(), parent);
      }
      switch (object) {
        case Region region -> {
          region.regions().forEach(r -> object(r, region));
          if (region instanceof TextRegion text) {
            text.textLines().forEach(l -> object(l, text));
          }
        }
        case TextLine line -> line.words().forEach(w -> object(w, line));
        case Word word -> word.glyphs().forEach(g -> object(g, word));
        case Glyph glyph -> glyph.graphemes().forEach(g -> object(g, glyph));
        case GraphemeGroup group -> group.graphemes().forEach(g -> object(g, group));
        case Grapheme g -> {}
        case NonPrintingChar c -> {}
        case Group group -> {
          for (GroupMember member : group.members()) {
            if (member instanceof Group child) {
              object(child, group);
            }
          }
        }
        case Layer layer -> {}
      }
    }

    void id(String id) {
      if (!ids.add(id)) {
        problems.add("Duplicate ID '" + id + "'");
      }
    }

    void checkReferences(Page page) {
      for (ContentObject object : objects.values()) {
        if (object instanceof Group group) {
          reference(group.regionRef(), group);
          for (GroupMember member : group.members()) {
            if (member instanceof RegionRef ref) {
              reference(ref.regionRef(), group);
            }
          }
        } else if (object instanceof Layer layer) {
          layer.regionRefs().forEach(r -> reference(r, layer));
        }
      }
      for (Relation relation : page.relations) {
        String name = relation.id() == null ? "Relation" : "Relation '" + relation.id() + "'";
        reference(relation.sourceRegionRef(), name);
        reference(relation.targetRegionRef(), name);
      }
    }

    private void reference(@Nullable String id, ContentObject from) {
      reference(id, describe(from));
    }

    private void reference(@Nullable String id, String from) {
      if (id != null && !ids.contains(id)) {
        problems.add("Unresolved reference '" + id + "' in " + from);
      }
    }
  }

  // ---- Accessors

  /** The {@code pcGtsId} of the document. */
  public @Nullable String pcGtsId() {
    return pcGtsId;
  }

  public Metadata metadata() {
    return metadata;
  }

  public String imageFilename() {
    return imageFilename;
  }

  public int imageWidth() {
    return imageWidth;
  }

  public int imageHeight() {
    return imageHeight;
  }

  public @Nullable Double imageXResolution() {
    return imageXResolution;
  }

  public @Nullable Double imageYResolution() {
    return imageYResolution;
  }

  public @Nullable ResolutionUnit imageResolutionUnit() {
    return imageResolutionUnit;
  }

  public @Nullable String custom() {
    return custom;
  }

  public @Nullable String comments() {
    return comments;
  }

  public @Nullable Double orientation() {
    return orientation;
  }

  public @Nullable PageType type() {
    return type;
  }

  public @Nullable Language primaryLanguage() {
    return primaryLanguage;
  }

  public @Nullable Language secondaryLanguage() {
    return secondaryLanguage;
  }

  public @Nullable String customLanguages() {
    return customLanguages;
  }

  public @Nullable Script primaryScript() {
    return primaryScript;
  }

  public @Nullable Script secondaryScript() {
    return secondaryScript;
  }

  public @Nullable String customScripts() {
    return customScripts;
  }

  public @Nullable ReadingDirection readingDirection() {
    return readingDirection;
  }

  public @Nullable TextLineOrder textLineOrder() {
    return textLineOrder;
  }

  public @Nullable Double conf() {
    return conf;
  }

  public List<AlternativeImage> alternativeImages() {
    return alternativeImages;
  }

  public @Nullable Polygon border() {
    return border;
  }

  public @Nullable Polygon printSpace() {
    return printSpace;
  }

  public @Nullable ReadingOrder readingOrder() {
    return readingOrder;
  }

  public List<Layer> layers() {
    return layers;
  }

  public List<Relation> relations() {
    return relations;
  }

  public @Nullable TextStyle textStyle() {
    return textStyle;
  }

  public List<UserAttribute> userAttributes() {
    return userAttributes;
  }

  public List<LabelGroup> labels() {
    return labels;
  }

  /** The top-level regions. */
  public List<Region> regions() {
    return regions;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof Page p
        && Objects.equals(pcGtsId, p.pcGtsId)
        && metadata.equals(p.metadata)
        && imageFilename.equals(p.imageFilename)
        && imageWidth == p.imageWidth
        && imageHeight == p.imageHeight
        && Objects.equals(imageXResolution, p.imageXResolution)
        && Objects.equals(imageYResolution, p.imageYResolution)
        && imageResolutionUnit == p.imageResolutionUnit
        && Objects.equals(custom, p.custom)
        && Objects.equals(comments, p.comments)
        && Objects.equals(orientation, p.orientation)
        && type == p.type
        && primaryLanguage == p.primaryLanguage
        && secondaryLanguage == p.secondaryLanguage
        && Objects.equals(customLanguages, p.customLanguages)
        && primaryScript == p.primaryScript
        && secondaryScript == p.secondaryScript
        && Objects.equals(customScripts, p.customScripts)
        && readingDirection == p.readingDirection
        && textLineOrder == p.textLineOrder
        && Objects.equals(conf, p.conf)
        && alternativeImages.equals(p.alternativeImages)
        && Objects.equals(border, p.border)
        && Objects.equals(printSpace, p.printSpace)
        && Objects.equals(readingOrder, p.readingOrder)
        && layers.equals(p.layers)
        && relations.equals(p.relations)
        && Objects.equals(textStyle, p.textStyle)
        && userAttributes.equals(p.userAttributes)
        && labels.equals(p.labels)
        && regions.equals(p.regions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(pcGtsId, metadata, imageFilename, imageWidth, imageHeight, regions);
  }

  @Override
  public String toString() {
    return "Page[imageFilename="
        + imageFilename
        + ", regions="
        + regions.size()
        + ", objects="
        + objects.size()
        + "]";
  }

  // ---- Builder

  /** Builds a {@link Page}. {@code metadata}, {@code imageFilename} and the size are required. */
  public static final class Builder {
    private @Nullable String pcGtsId;
    private @Nullable Metadata metadata;
    private @Nullable String imageFilename;
    private @Nullable Integer imageWidth;
    private @Nullable Integer imageHeight;
    private @Nullable Double imageXResolution;
    private @Nullable Double imageYResolution;
    private @Nullable ResolutionUnit imageResolutionUnit;
    private @Nullable String custom;
    private @Nullable String comments;
    private @Nullable Double orientation;
    private @Nullable PageType type;
    private @Nullable Language primaryLanguage;
    private @Nullable Language secondaryLanguage;
    private @Nullable String customLanguages;
    private @Nullable Script primaryScript;
    private @Nullable Script secondaryScript;
    private @Nullable String customScripts;
    private @Nullable ReadingDirection readingDirection;
    private @Nullable TextLineOrder textLineOrder;
    private @Nullable Double conf;
    private List<AlternativeImage> alternativeImages = new ArrayList<>();
    private @Nullable Polygon border;
    private @Nullable Polygon printSpace;
    private @Nullable ReadingOrder readingOrder;
    private List<Layer> layers = new ArrayList<>();
    private List<Relation> relations = new ArrayList<>();
    private @Nullable TextStyle textStyle;
    private List<UserAttribute> userAttributes = new ArrayList<>();
    private List<LabelGroup> labels = new ArrayList<>();
    private List<Region> regions = new ArrayList<>();

    private Builder() {}

    private Builder(Page p) {
      pcGtsId = p.pcGtsId;
      metadata = p.metadata;
      imageFilename = p.imageFilename;
      imageWidth = p.imageWidth;
      imageHeight = p.imageHeight;
      imageXResolution = p.imageXResolution;
      imageYResolution = p.imageYResolution;
      imageResolutionUnit = p.imageResolutionUnit;
      custom = p.custom;
      comments = p.comments;
      orientation = p.orientation;
      type = p.type;
      primaryLanguage = p.primaryLanguage;
      secondaryLanguage = p.secondaryLanguage;
      customLanguages = p.customLanguages;
      primaryScript = p.primaryScript;
      secondaryScript = p.secondaryScript;
      customScripts = p.customScripts;
      readingDirection = p.readingDirection;
      textLineOrder = p.textLineOrder;
      conf = p.conf;
      alternativeImages = new ArrayList<>(p.alternativeImages);
      border = p.border;
      printSpace = p.printSpace;
      readingOrder = p.readingOrder;
      layers = new ArrayList<>(p.layers);
      relations = new ArrayList<>(p.relations);
      textStyle = p.textStyle;
      userAttributes = new ArrayList<>(p.userAttributes);
      labels = new ArrayList<>(p.labels);
      regions = new ArrayList<>(p.regions);
    }

    /**
     * Builds the page.
     *
     * @throws NullPointerException if a required value is missing
     * @throws InvalidPageException if IDs are duplicated or references don't resolve
     */
    public Page build() {
      return new Page(this);
    }

    public Builder pcGtsId(@Nullable String pcGtsId) {
      this.pcGtsId = pcGtsId;
      return this;
    }

    public Builder metadata(Metadata metadata) {
      this.metadata = metadata;
      return this;
    }

    public Builder imageFilename(String imageFilename) {
      this.imageFilename = imageFilename;
      return this;
    }

    public Builder imageWidth(int imageWidth) {
      this.imageWidth = imageWidth;
      return this;
    }

    public Builder imageHeight(int imageHeight) {
      this.imageHeight = imageHeight;
      return this;
    }

    public Builder imageXResolution(@Nullable Double imageXResolution) {
      this.imageXResolution = imageXResolution;
      return this;
    }

    public Builder imageYResolution(@Nullable Double imageYResolution) {
      this.imageYResolution = imageYResolution;
      return this;
    }

    public Builder imageResolutionUnit(@Nullable ResolutionUnit imageResolutionUnit) {
      this.imageResolutionUnit = imageResolutionUnit;
      return this;
    }

    public Builder custom(@Nullable String custom) {
      this.custom = custom;
      return this;
    }

    public Builder comments(@Nullable String comments) {
      this.comments = comments;
      return this;
    }

    public Builder orientation(@Nullable Double orientation) {
      this.orientation = orientation;
      return this;
    }

    public Builder type(@Nullable PageType type) {
      this.type = type;
      return this;
    }

    public Builder primaryLanguage(@Nullable Language primaryLanguage) {
      this.primaryLanguage = primaryLanguage;
      return this;
    }

    public Builder secondaryLanguage(@Nullable Language secondaryLanguage) {
      this.secondaryLanguage = secondaryLanguage;
      return this;
    }

    public Builder customLanguages(@Nullable String customLanguages) {
      this.customLanguages = customLanguages;
      return this;
    }

    public Builder primaryScript(@Nullable Script primaryScript) {
      this.primaryScript = primaryScript;
      return this;
    }

    public Builder secondaryScript(@Nullable Script secondaryScript) {
      this.secondaryScript = secondaryScript;
      return this;
    }

    public Builder customScripts(@Nullable String customScripts) {
      this.customScripts = customScripts;
      return this;
    }

    public Builder readingDirection(@Nullable ReadingDirection readingDirection) {
      this.readingDirection = readingDirection;
      return this;
    }

    public Builder textLineOrder(@Nullable TextLineOrder textLineOrder) {
      this.textLineOrder = textLineOrder;
      return this;
    }

    public Builder conf(@Nullable Double conf) {
      this.conf = conf;
      return this;
    }

    public Builder alternativeImages(Collection<? extends AlternativeImage> alternativeImages) {
      this.alternativeImages = new ArrayList<>(alternativeImages);
      return this;
    }

    public Builder addAlternativeImages(AlternativeImage alternativeImage) {
      alternativeImages.add(alternativeImage);
      return this;
    }

    public Builder border(@Nullable Polygon border) {
      this.border = border;
      return this;
    }

    public Builder printSpace(@Nullable Polygon printSpace) {
      this.printSpace = printSpace;
      return this;
    }

    public Builder readingOrder(@Nullable ReadingOrder readingOrder) {
      this.readingOrder = readingOrder;
      return this;
    }

    public Builder layers(Collection<? extends Layer> layers) {
      this.layers = new ArrayList<>(layers);
      return this;
    }

    public Builder addLayers(Layer layer) {
      layers.add(layer);
      return this;
    }

    public Builder relations(Collection<? extends Relation> relations) {
      this.relations = new ArrayList<>(relations);
      return this;
    }

    public Builder addRelations(Relation relation) {
      relations.add(relation);
      return this;
    }

    public Builder textStyle(@Nullable TextStyle textStyle) {
      this.textStyle = textStyle;
      return this;
    }

    public Builder userAttributes(Collection<? extends UserAttribute> userAttributes) {
      this.userAttributes = new ArrayList<>(userAttributes);
      return this;
    }

    public Builder addUserAttributes(UserAttribute userAttribute) {
      userAttributes.add(userAttribute);
      return this;
    }

    public Builder labels(Collection<? extends LabelGroup> labels) {
      this.labels = new ArrayList<>(labels);
      return this;
    }

    public Builder addLabels(LabelGroup labelGroup) {
      labels.add(labelGroup);
      return this;
    }

    public Builder regions(Collection<? extends Region> regions) {
      this.regions = new ArrayList<>(regions);
      return this;
    }

    public Builder addRegions(Region region) {
      regions.add(region);
      return this;
    }
  }
}
