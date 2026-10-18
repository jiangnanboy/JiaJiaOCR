package com.jiajia.table_recognition.entity;

import com.jiajia.common_object.ParsedTableCell;
import com.jiajia.common_object.ParsedTableData;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 表格识别结果类（融合解析能力）
 * @author jiangnanboy
 * @date 2025/12/1 14:08
 */
public class TableResultV2 {

    // 原有属性
    private String html;
    private List<float[]> cellBoxes;
    private double elapse;

    // 解析后的结构化数据（可选缓存）
    private ParsedTableData parsedData;

    // 原有构造方法
    public TableResultV2(String html, List<float[]> cellBoxes) {
        this.html = html;
        this.cellBoxes = cellBoxes;
    }

    // ====================== 原有Getter/Setter ======================
    public String getHtml() {
        return html;
    }

    public void setHtmlStr(String html) {
        this.html = html;
        this.parsedData = null; // 重置缓存
    }

    public List<float[]> getCellBoxes() {
        return cellBoxes;
    }

    public void setCellBoxes(List<float[]> cellBoxes) {
        this.cellBoxes = cellBoxes;
        this.parsedData = null; // 重置缓存
    }

    public double getElapse() {
        return elapse;
    }

    public void setElapse(double elapse) {
        this.elapse = elapse;
    }

    @Override
    public String toString() {
        return "TableResult{" +
                "html='" + html + '\'' +
                ", cellBoxes=" + (cellBoxes != null ? Arrays.deepToString(cellBoxes.toArray()) : "null") +
                ", elapse=" + elapse +
                '}';
    }

    // ====================== 新增核心解析方法 ======================
    /**
     * 解析当前TableResult为结构化表格数据（带缓存，避免重复解析）
     * @return 结构化表格数据
     */
    public ParsedTableData parseToStructuredData() {
        // 缓存命中：已解析过且原始数据未变更，直接返回
        if (this.parsedData != null) {
            return this.parsedData;
        }

        ParsedTableData parsedTable = new ParsedTableData();
        List<ParsedTableCell> cells = new ArrayList<>();

        // 1. 空值校验
        if (this.html == null || this.html.trim().isEmpty()) {
            parsedTable.setRows(0);
            parsedTable.setColumns(0);
            parsedTable.setCells(cells);
            this.parsedData = parsedTable;
            return parsedTable;
        }

        // 2. Jsoup解析HTML表格
        Document doc = Jsoup.parse(this.html);
        Element table = doc.selectFirst("table");
        if (table == null) { // 无表格节点
            parsedTable.setRows(0);
            parsedTable.setColumns(0);
            parsedTable.setCells(cells);
            this.parsedData = parsedTable;
            return parsedTable;
        }

        // 3. 获取所有行，计算总行数
        Elements trs = table.select("tr");
        int totalRows = trs.size();
        parsedTable.setRows(totalRows);

        // 4. 初始化跨行跨列占用标记，计算最大列数
        int maxColumns = 0;
        List<int[]> rowColOccupied = new ArrayList<>();
        for (int i = 0; i < totalRows; i++) {
            rowColOccupied.add(new int[0]);
        }

        // 第一步：遍历计算最大列数，标记单元格占用
        for (int rowIdx = 0; rowIdx < trs.size(); rowIdx++) {
            Element tr = trs.get(rowIdx);
            Elements tds = tr.select("td");
            int currentCol = 0;
            int[] occupied = rowColOccupied.get(rowIdx);

            for (Element td : tds) {
                // 跳过已被rowspan占用的列
                while (currentCol < occupied.length && occupied[currentCol] > 0) {
                    currentCol++;
                }

                // 获取合并属性（默认1）
                int colSpan = getAttributeValue(td, "colspan");
                int rowSpan = getAttributeValue(td, "rowspan");

                // 更新最大列数
                if (currentCol + colSpan > maxColumns) {
                    maxColumns = currentCol + colSpan;
                }

                // 标记跨行跨列的占用状态
                markOccupied(rowColOccupied, rowIdx, currentCol, rowSpan, colSpan, maxColumns);
                currentCol += colSpan;
            }
        }
        parsedTable.setColumns(maxColumns);

        // 重置占用标记，重新遍历构建单元格
        rowColOccupied.clear();
        for (int i = 0; i < totalRows; i++) {
            rowColOccupied.add(new int[maxColumns]);
        }
        int cellBoxIndex = 0; // 坐标索引与td遍历顺序一一对应

        // 第二步：遍历构建每个单元格对象
        for (int rowIdx = 0; rowIdx < trs.size(); rowIdx++) {
            Element tr = trs.get(rowIdx);
            Elements tds = tr.select("td");
            int currentCol = 0;
            int[] occupied = rowColOccupied.get(rowIdx);

            for (Element td : tds) {
                // 跳过已被占用的列
                while (currentCol < occupied.length && occupied[currentCol] > 0) {
                    currentCol++;
                }

                // 基础属性解析
                String cellText = td.text().trim();
                int colSpan = getAttributeValue(td, "colspan");
                int rowSpan = getAttributeValue(td, "rowspan");

                // 计算起始/结束行列（从0开始）
                int startRow = rowIdx;
                int startCol = currentCol;
                int endRow = rowIdx + rowSpan - 1;
                int endCol = currentCol + colSpan - 1;

                // 坐标转换（float[] → List<Double>）
                List<Double> position = new ArrayList<>();
                if (this.cellBoxes != null && cellBoxIndex < this.cellBoxes.size()) {
                    float[] floatBox = this.cellBoxes.get(cellBoxIndex);
                    for (float f : floatBox) {
                        position.add((double) f);
                    }
                }

                // 构建单元格对象
                ParsedTableCell cell = new ParsedTableCell();
                cell.setStart_row(startRow);
                cell.setStart_column(startCol);
                cell.setEnd_row(endRow);
                cell.setEnd_column(endCol);
                cell.setText(cellText);
                cell.setPosition(position);
                cells.add(cell);

                // 标记当前单元格的占用状态
                markOccupied(rowColOccupied, startRow, startCol, rowSpan, colSpan, maxColumns);

                currentCol += colSpan;
                cellBoxIndex++;
            }
        }

        parsedTable.setCells(cells);
        this.parsedData = parsedTable; // 缓存解析结果
        return parsedTable;
    }

    private int getAttributeValue(Element td, String attrName) {
        if (td.hasAttr(attrName)) {
            try {
                return Integer.parseInt(td.attr(attrName));
            } catch (NumberFormatException e) {
                return 1;
            }
        }
        return 1;
    }

    /**
     * 标记单元格跨行跨列的占用状态
     */
    private void markOccupied(List<int[]> rowColOccupied, int startRow, int startCol,
                              int rowSpan, int colSpan, int maxColumns) {
        for (int r = startRow; r < startRow + rowSpan; r++) {
            // 行索引超出时扩容
            if (r >= rowColOccupied.size()) {
                rowColOccupied.add(new int[maxColumns]);
            }
            int[] rowOcc = rowColOccupied.get(r);
            // 列数组扩容到maxColumns
            if (rowOcc.length < maxColumns) {
                rowOcc = Arrays.copyOf(rowOcc, maxColumns);
                rowColOccupied.set(r, rowOcc);
            }
            // 标记当前列的剩余占用行数
            for (int c = startCol; c < startCol + colSpan; c++) {
                rowOcc[c] = rowSpan - (r - startRow);
            }
        }
    }

    // ====================== 测试方法 ======================
    public static void main(String[] args) {
        // 1. 模拟原始数据
        String html = "<html><body><table><tr><td>序号</td><td>客户名称</td><td>销售额</td><td>占年度销售总额比 例（%）</td><td>是否与上市公司存 在关联关系</td></tr><tr><td>1</td><td>客户A</td><td>10,911.73</td><td>21.00</td><td>否</td></tr><tr><td>2</td><td>客户B</td><td>5,174.20</td><td>9.96</td><td>否</td></tr><tr><td>3</td><td>客户C</td><td>4,219.01</td><td>8.12</td><td>香</td></tr><tr><td>4</td><td>客户D</td><td>2,689.81</td><td>5.18</td><td>否</td></tr><tr><td>5</td><td>客户E</td><td>2,154.45</td><td>4.15</td><td>否</td></tr><tr><tr><td>合计</td><td>/</td><td>25,149.20</td><td>48.41</td><td>—</td></tr></table></body></html>";

        // 2. 模拟cellBoxes数据
        List<float[]> cellBoxes = new ArrayList<>();
        cellBoxes.add(new float[]{19.272882f, 20.029133f, 466.04025f, 20.567871f, 466.0143f, 324.43735f, 16.072155f, 322.05023f});
        cellBoxes.add(new float[]{426.35144f, 17.083534f, 1638.1757f, 17.660158f, 1632.0723f, 278.8377f, 423.09964f, 277.10437f});
        cellBoxes.add(new float[]{1588.9072f, 18.524559f, 2700.2002f, 19.08466f, 2710.107f, 309.46066f, 1589.4532f, 307.66132f});

        // 3. 构建TableResult实例
        TableResultV2 tableResult = new TableResultV2(html, cellBoxes);

        // 4. 直接调用解析方法（一体化调用）
        ParsedTableData parsedData = tableResult.parseToStructuredData();

        // 5. 输出结果（可结合Jackson序列化为JSON）
        System.out.println("表格总行数：" + parsedData.getRows());
        System.out.println("表格总列数：" + parsedData.getColumns());
        System.out.println("第一个单元格文本：" + parsedData.getCells().get(0).getText());
        System.out.println("第一个单元格坐标：" + parsedData.getCells().get(0).getPosition());
    }
}