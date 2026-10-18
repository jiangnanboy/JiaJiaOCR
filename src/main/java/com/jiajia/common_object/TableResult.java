package com.jiajia.common_object;

import com.alibaba.fastjson2.JSON;
import com.jiajia.table_recognition.entity.TableResultV2;

import java.util.Arrays;
import java.util.List;

public class TableResult {

    private String html;

    private List<float[]> cellBoxes;

    private double elapse;

    private transient ParsedTableData parsedTableData;

    public TableResult(String html, List<float[]> cellBoxes) {
        this.html = html;
        this.cellBoxes = cellBoxes;
    }

    public String getHtml() {
        return html;
    }

    public void setHtmlStr(String html) {
        this.html = html;
        this.parsedTableData = null;
    }

    public List<float[]> getCellBoxes() {
        return cellBoxes;
    }

    public void setCellBoxes(List<float[]> cellBoxes) {
        this.cellBoxes = cellBoxes;
        this.parsedTableData = null;
    }

    public double getElapse() {
        return elapse;
    }

    public void setElapse(double elapse) {
        this.elapse = elapse;
    }

    /**
     * 获取解析后的结构化表格数据
     * @return ParsedTableData 结构化表格数据
     */
    public ParsedTableData getParsedTableData() {
        if (this.parsedTableData != null) {
            return this.parsedTableData;
        }
        TableResultV2 tableResultV2 = new TableResultV2(this.html, this.cellBoxes);
        this.parsedTableData = tableResultV2.parseToStructuredData();
        return this.parsedTableData;
    }

    /**
     * 获取JSON格式的表格数据
     * @return JSON字符串，包含行列数、单元格内容、位置及坐标
     */
    public String getTableJson() {
        ParsedTableData data = getParsedTableData();
        return JSON.toJSONString(data);
    }

    @Override
    public String toString() {
        return "TableResult{" +
                "html='" + html + '\'' +
                ", cellBoxes=" + (cellBoxes != null ? Arrays.deepToString(cellBoxes.toArray()) : "null") +
                '}';
    }
}