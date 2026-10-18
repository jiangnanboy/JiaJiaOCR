package com.jiajia.common_object;

import java.util.List;

/**
 * @author jiangnanboy
 * @date 2025/12/1 14:12
 */
public class ParsedTableData {
    private int rows;            // 表格总行数
    private int columns;         // 表格总列数
    private List<ParsedTableCell> cells; // 所有单元格列表

    // Getter & Setter
    public int getRows() { return rows; }
    public void setRows(int rows) { this.rows = rows; }
    public int getColumns() { return columns; }
    public void setColumns(int columns) { this.columns = columns; }
    public List<ParsedTableCell> getCells() { return cells; }
    public void setCells(List<ParsedTableCell> cells) { this.cells = cells; }
}
