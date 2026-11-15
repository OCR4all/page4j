package com.maxnth.page4j.model;

import com.maxnth.page4j.model.value.GroupType;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A reading order group. The members of an {@link OrderedGroup} are in reading order; when written,
 * they get their position as {@code index}.
 */
public sealed interface Group extends GroupMember, ContentObject
    permits OrderedGroup, UnorderedGroup {

  /** The region this group stands for, if any. */
  @Nullable String regionRef();

  @Nullable String caption();

  @Nullable GroupType type();

  @Nullable Boolean continuation();

  @Nullable String custom();

  @Nullable String comments();

  List<UserAttribute> userAttributes();

  List<LabelGroup> labels();

  List<GroupMember> members();
}
