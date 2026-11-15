package com.maxnth.page4j.model;

import org.jspecify.annotations.Nullable;

/** The position of a region within a table ({@code TableCellRole}). */
@PageRecord
public record TableCellRole(
    int rowIndex,
    int columnIndex,
    @Nullable Integer rowSpan,
    @Nullable Integer colSpan,
    @Nullable Boolean header)
    implements TableCellRoleBuilder.With {
  public TableCellRole {}
}
