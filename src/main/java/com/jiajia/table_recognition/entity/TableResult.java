package com.jiajia.table_recognition.entity;

import java.util.Arrays;
import java.util.List;

public class TableResult {

    private String html;

    private List<float[]> cellBoxes;

    private double elapse;

    public TableResult(String html, List<float[]> cellBoxes) {
        this.html = html;
        this.cellBoxes = cellBoxes;
    }

    public String getHtml() {
        return html;
    }

    public void setHtmlStr(String html) {
        this.html = html;
    }

    public List<float[]> getCellBoxes() {
        return cellBoxes;
    }

    public void setCellBoxes(List<float[]> cellBoxes) {
        this.cellBoxes = cellBoxes;
    }


    @Override
    public String toString() {
        return "TableResult{" +
                "html='" + html + '\'' +
                ", cellBoxes=" + (cellBoxes != null ? Arrays.deepToString(cellBoxes.toArray()) : "null") +
                '}';
    }
}