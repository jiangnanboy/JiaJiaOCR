package com.jiajia.table_recognition;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class HtmlTableParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(HtmlTableParser.class);

    public static String parseHtmlTable(String htmlStr) {
        try {
            // 解析HTML字符串
            Document doc = Jsoup.parse(htmlStr);
            Element table = doc.select("table").first();
            Elements rows = table.select("tr");

            // 计算行数和列数
            int rowsCount = rows.size();
            int columnsCount = 0;

            // 找出最大列数
            for (Element row : rows) {
                int currentCols = row.select("td, th").size();
                if (currentCols > columnsCount) {
                    columnsCount = currentCols;
                }
            }

            // 创建单元格数组
            JSONArray cells = new JSONArray();

            // 处理每个单元格
            for (int i = 0; i < rowsCount; i++) {
                Element row = rows.get(i);
                Elements cellsInRow = row.select("td, th");

                for (int j = 0; j < cellsInRow.size(); j++) {
                    Element cell = cellsInRow.get(j);
                    String text = cell.text();

                    // 创建单元格对象
                    JSONObject cellObj = new JSONObject();
                    cellObj.put("start_row", i);
                    cellObj.put("start_column", j);
                    cellObj.put("end_row", i);
                    cellObj.put("end_column", j);
                    cellObj.put("text", text);

                    cells.add(cellObj);
                }
            }

            // 创建结果JSON
            JSONObject result = new JSONObject();
            JSONObject data = new JSONObject();

            data.put("rows", rowsCount);
            data.put("columns", columnsCount);
            data.put("cells", cells);

            result.put("data", data);
//            result.put("code", 200);
//            result.put("message", "success");

            return result.toJSONString(); // 格式化输出，缩进2个空格
        } catch (Exception e) {
            LOGGER.error("Failed to parse HTML table", e);
            // 错误处理
            JSONObject errorResult = new JSONObject();
//            errorResult.put("code", 500);
//            errorResult.put("message", "解析失败: " + e.getMessage());
            return errorResult.toJSONString();
        }
    }


    public static JSONObject parseHtmlTable2(String htmlStr) {
        try {
            // 解析HTML字符串
            Document doc = Jsoup.parse(htmlStr);
            Element table = doc.select("table").first();
            Elements rows = table.select("tr");

            // 计算行数和列数
            int rowsCount = rows.size();
            int columnsCount = 0;

            // 找出最大列数
            for (Element row : rows) {
                int currentCols = row.select("td, th").size();
                if (currentCols > columnsCount) {
                    columnsCount = currentCols;
                }
            }

            // 创建单元格数组
            JSONArray cells = new JSONArray();

            // 处理每个单元格
            for (int i = 0; i < rowsCount; i++) {
                Element row = rows.get(i);
                Elements cellsInRow = row.select("td, th");

                for (int j = 0; j < cellsInRow.size(); j++) {
                    Element cell = cellsInRow.get(j);
                    String text = cell.text();

                    // 创建单元格对象
                    JSONObject cellObj = new JSONObject();
                    cellObj.put("start_row", i);
                    cellObj.put("start_column", j);
                    cellObj.put("end_row", i);
                    cellObj.put("end_column", j);
                    cellObj.put("text", text);

                    cells.add(cellObj);
                }
            }

            // 创建结果JSON
            JSONObject result = new JSONObject();
            JSONObject data = new JSONObject();

            data.put("rows", rowsCount);
            data.put("columns", columnsCount);
            data.put("cells", cells);

            result.put("data", data);
//            result.put("code", 200);
//            result.put("message", "success");

            return result; // 格式化输出，缩进2个空格
        } catch (Exception e) {
            LOGGER.error("Failed to parse HTML table", e);
            // 错误处理
            JSONObject errorResult = new JSONObject();
//            errorResult.put("code", 500);
//            errorResult.put("message", "解析失败: " + e.getMessage());
            return errorResult;
        }
    }

}