package com.maxnth.page4j.model;

import com.maxnth.page4j.model.value.GroupType;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** A reading order group whose members are read in order ({@code OrderedGroup}). */
@PageRecord
public record OrderedGroup(
    String id,
    @Nullable String regionRef,
    @Nullable String caption,
    @Nullable GroupType type,
    @Nullable Boolean continuation,
    @Nullable String custom,
    @Nullable String comments,
    List<UserAttribute> userAttributes,
    List<LabelGroup> labels,
    List<GroupMember> members)
    implements Group, OrderedGroupBuilder.With {
  public OrderedGroup {
    Objects.requireNonNull(id, "id");
    userAttributes = List.copyOf(userAttributes);
    labels = List.copyOf(labels);
    members = List.copyOf(members);
  }
}
