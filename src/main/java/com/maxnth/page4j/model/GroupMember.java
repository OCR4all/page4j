package com.maxnth.page4j.model;

/** A member of a reading order group: a region reference or a nested group. */
public sealed interface GroupMember permits RegionRef, Group {}
