package com.maxnth.page4j.formats.internal;

import com.maxnth.page4j.model.Group;
import com.maxnth.page4j.model.GroupMember;
import com.maxnth.page4j.model.OrderedGroup;
import com.maxnth.page4j.model.Page;
import com.maxnth.page4j.model.ReadingOrder;
import com.maxnth.page4j.model.Region;
import com.maxnth.page4j.model.RegionRef;
import com.maxnth.page4j.model.UnorderedGroup;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class RegionOrder {

  private RegionOrder() {}

  static List<Region> flattened(Page page) {
    Set<String> ordered = new LinkedHashSet<>();
    ReadingOrder order = page.readingOrder();
    if (order != null) {
      collect(order.group(), ordered);
    }
    List<Region> regions = new ArrayList<>();
    for (String id : ordered) {
      page.find(id, Region.class).ifPresent(regions::add);
    }
    for (Region region : page.allRegions()) {
      if (!ordered.contains(region.id())) {
        regions.add(region);
      }
    }
    return regions;
  }

  private static void collect(Group group, Set<String> into) {
    List<GroupMember> members =
        switch (group) {
          case OrderedGroup g -> g.members();
          case UnorderedGroup g -> g.members();
        };
    for (GroupMember member : members) {
      switch (member) {
        case Group child -> collect(child, into);
        case RegionRef ref -> into.add(ref.regionRef());
      }
    }
  }
}
