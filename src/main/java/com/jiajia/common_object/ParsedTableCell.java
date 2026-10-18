package com.jiajia.common_object;

import com.alibaba.fastjson2.annotation.JSONField;

import java.util.List;

/**
 * @author jiangnanboy
 * @date 2025/12/1 14:13
 */
public class ParsedTableCell {
    private int start_row;       // 起始行（从0开始）
    private int start_column;    // 起始列（从0开始）
    private int end_row;         // 结束行
    private int end_column;      // 结束列
    private String text;         // 单元格文本
    private List<Double> position; // 单元格坐标（8个数值）

    // Getter & Setter
    @JSONField(name = "row_start")
    public int getStart_row() { return start_row; }
    public void setStart_row(int start_row) { this.start_row = start_row; }

    @JSONField(name = "col_start")
    public int getStart_column() { return start_column; }
    public void setStart_column(int start_column) { this.start_column = start_column; }

    @JSONField(name = "row_end")
    public int getEnd_row() { return end_row; }
    public void setEnd_row(int end_row) { this.end_row = end_row; }

    @JSONField(name = "col_end")
    public int getEnd_column() { return end_column; }
    public void setEnd_column(int end_column) { this.end_column = end_column; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public List<Double> getPosition() { return position; }
    public void setPosition(List<Double> position) { this.position = position; }
}
