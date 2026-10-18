package com.jiajia.table_recognition.tablematcher;

import com.jiajia.common_object.Pair;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 匹配 OCR 结果和检测到的表格单元格坐标，生成 HTML
 */
public class TableMatch {

    private boolean filterOcrResult;
    private boolean useMaster;

    public TableMatch() {
        this(true, false);
    }

    public TableMatch(boolean filterOcrResult, boolean useMaster) {
        this.filterOcrResult = filterOcrResult;
        this.useMaster = useMaster;
    }

    /**
     * 与 Python 中的 __call__ 方法对应
     *
     * @param predStructures 表格结构预测结果(HTML标签序列)，如 ["<table>", "<tr>", "<td></td>", ...]
     * @param predBBoxes     表格中每个单元格的定位框(可能是4点或8点)
     * @param dtBoxes        OCR 检测出的文本框(一般是4点或4维的矩形框)
     * @param recRes         OCR 识别结果, 每个元素为 String[]，其中 recRes[i][0] 存放文本
     * @return 返回拼接好的 HTML 字符串
     */
    public String call(List<String> predStructures, List<float[]> predBBoxes, List<float[]> dtBoxes, List<Pair<String, Float>> recRes) {

        // 1. 若需要过滤掉位于表格上方（或其他区域）的 OCR 结果，则先过滤
        List<float[]> filteredDtBoxes = dtBoxes;
        List<Pair<String, Float>> filteredRecRes = recRes;
        if (this.filterOcrResult) {
            Object[] filtered = filterOcrResult(predBBoxes, dtBoxes, recRes);
            // 分别取出新的 dtBoxes, recRes
            filteredDtBoxes = (List<float[]>) filtered[0];
            filteredRecRes = (List<Pair<String, Float>>) filtered[1];
        }

        // 2. 将 OCR 结果与表格预测框进行匹配
        // matchedIndex: key = predBBoxes 的索引 j， value = dtBoxes 索引列表
        Map<Integer, List<Integer>> matchedIndex = matchResult(filteredDtBoxes, predBBoxes);

        // 3. 根据匹配关系，将 OCR 内容插入到对应 <td> 标签中，返回最终 HTML
        Object[] getHtmlRes = getPredHtml(predStructures, matchedIndex, filteredRecRes);
        // getHtmlRes[0] = 拼接后的字符串, getHtmlRes[1] = 完整标签列表(可自行使用)
        String predHtml = (String) getHtmlRes[0];
        return predHtml;
    }

    /**
     * 匹配 OCR 检测框与表格单元格
     *
     * @param dtBoxes    OCR 检测到的文本框列表
     * @param predBBoxes 表格预测的单元格框列表
     * @return Map<Integer, List <Integer>>:
     * - key: 表格框(predBBoxes)的索引 j
     * - value: OCR 框(dtBoxes)的索引 i 列表
     */
    public Map<Integer, List<Integer>> matchResult(List<float[]> dtBoxes, List<float[]> predBBoxes) {
        Map<Integer, List<Integer>> matched = new LinkedHashMap<>();

        for (int i = 0; i < dtBoxes.size(); i++) {
            float[] gtBox = dtBoxes.get(i); // OCR 框
            // distances: 存放 (distanceVal, 1.0 - iou, predBoxIndex)
            // 用于排序选出最优的 predBoxIndex
            List<double[]> distances = new ArrayList<>();

            for (int j = 0; j < predBBoxes.size(); j++) {
                float[] predBox = predBBoxes.get(j);

                // 若 predBox 长度为 8，则转换为 [minX, minY, maxX, maxY]
                if (predBox.length == 8) {
                    predBox = normalizeBox(predBox);
                }

                // distance: l1 距离
                double distVal = TablePostProcessor.distance(gtBox, predBox);
                // iou: 交并比
                double iouVal = TablePostProcessor.computeIou(
                        convertBoxToYX(recBox(gtBox)),
                        convertBoxToYX(recBox(predBox))
                );

                // (distance, 1.0 - iou, j)
                double[] record = new double[]{distVal, (1.0 - iouVal), j};
                distances.add(record);
            }

            // 按 (1.0 - iou, distance) 排序
            // 对应 Python: sorted(distances, key=lambda item: (item[1], item[0]))
            // 先比 o1[1], o2[1], 若相等再比 o1[0], o2[0]
            distances.sort(Comparator.comparingDouble((double[] o) -> o[1]).thenComparingDouble(o -> o[0]));

            // 取最小值(排在第0)对应的 predBoxIndex
            double[] best = distances.get(0);
            int bestPredIdx = (int) best[2];

            // 将 dtBoxes[i] 归到 predBBoxes[bestPredIdx]
            if (!matched.containsKey(bestPredIdx)) {
                matched.put(bestPredIdx, new ArrayList<Integer>());
            }
            matched.get(bestPredIdx).add(i);
        }
        return matched;
    }

    /**
     * 将表格结构与匹配索引及 OCR 文本组合生成 HTML
     *
     * @param predStructures 预测的结构标签序列
     * @param matchedIndex   match_result 的结果(key=单元格索引, value=dtBox索引列表)
     * @param ocrContents    OCR 识别结果，每个元素是 String[]，其中 [0] 为识别文本
     * @return Object[] { String(拼接后的HTML), List<String>(处理后标签序列) }
     */
    public Object[] getPredHtml(List<String> predStructures,
                                Map<Integer, List<Integer>> matchedIndex,
                                List<Pair<String, Float>> ocrContents) {

        List<String> endHtml = new ArrayList<>();
        int tdIndex = 0; // 当前 <td> 的序号

        for (String tag : predStructures) {
            // 若不包含 "</td>", 则直接加入结果
            if (!tag.contains("</td>")) {
                endHtml.add(tag);
                continue;
            }

            // 如果是 "<td></td>"，则转成 "<td>"
            if ("<td></td>".equals(tag)) {
                endHtml.add("<td>");
            }

            // 若 matchedIndex 中包含当前 tdIndex，则拼接 OCR 文本
            if (matchedIndex.containsKey(tdIndex)) {
                // 判断是否需要 <b> ... </b>
                boolean bWith = false;
                int firstDtIdx = matchedIndex.get(tdIndex).get(0);
                if (ocrContents != null && firstDtIdx < ocrContents.size()) {
                    String firstContent = ocrContents.get(firstDtIdx).getLeft();
                    if (firstContent != null && firstContent.contains("<b>") &&
                            matchedIndex.get(tdIndex).size() > 1) {
                        bWith = true;
                        endHtml.add("<b>");
                    }
                }

                // 遍历同一个单元格中可能对应多个 dtBox
                List<Integer> dtIndices = matchedIndex.get(tdIndex);
                for (int idx = 0; idx < dtIndices.size(); idx++) {
                    int dtIndex = dtIndices.get(idx);
                    // 安全检查
                    if (dtIndex < 0 || dtIndex >= ocrContents.size()) {
                        continue;
                    }
                    Pair<String, Float> rec = ocrContents.get(dtIndex);
                    // rec[0] 存放文本
                    if (rec == null || rec.getLeft() == null) {
                        continue;
                    }
                    String content = rec.getLeft();

                    // 若此单元格对应多个 OCR 框，需要再做一些清理操作
                    if (dtIndices.size() > 1) {
                        // 去掉前后空格、<b>、</b> 等
                        content = cleanupContent(content);
                        if (content.isEmpty()) {
                            continue;
                        }
                        // 若不是最后一个 OCR，则在结尾加空格
                        if (idx != dtIndices.size() - 1 && !content.endsWith(" ")) {
                            content += " ";
                        }
                    }
                    // 将文本内容逐字符添加
                    endHtml.add(content);
                }

                if (bWith) {
                    endHtml.add("</b>");
                }
            }

            // 若原标签是 "<td></td>" 则要手动补上 "</td>"
            if ("<td></td>".equals(tag)) {
                endHtml.add("</td>");
            } else {
                endHtml.add(tag);
            }

            // 下一个 <td> 序号
            tdIndex++;
        }

        // 过滤不需要的标签: <thead>, </thead>, <tbody>, </tbody>
        List<String> filterElements = Arrays.asList("<thead>", "</thead>", "<tbody>", "</tbody>");
        List<String> filteredHtml = endHtml.stream()
                .filter(v -> !filterElements.contains(v))
                .collect(Collectors.toList());

        // 返回拼接结果和完整列表
        String joined = String.join("", filteredHtml);
        return new Object[]{joined, filteredHtml};
    }

    /**
     * 从预测结构中解码行列逻辑坐标
     *
     * @param predStructures HTML 标签序列
     * @return 每个 <td> 的逻辑坐标 [row_start, row_end, col_start, col_end]
     */
    public List<int[]> decodeLogicPoints(List<String> predStructures) {
        List<int[]> logicPoints = new ArrayList<>();
        int currentRow = 0;
        int currentCol = 0;

        // 记录已被占用的单元格
        Map<String, Boolean> occupiedCells = new HashMap<>();
        // 辅助函数
        java.util.function.BiFunction<Integer, Integer, Boolean> isOccupied =
                (r, c) -> occupiedCells.containsKey(r + "_" + c);
        java.util.function.Consumer<int[]> markOccupied = (arr) -> {
            // arr: [row, col, rowspan, colspan]
            int rStart = arr[0];
            int cStart = arr[1];
            int rowSpan = arr[2];
            int colSpan = arr[3];
            for (int rr = rStart; rr < rStart + rowSpan; rr++) {
                for (int cc = cStart; cc < cStart + colSpan; cc++) {
                    occupiedCells.put(rr + "_" + cc, true);
                }
            }
        };

        for (int i = 0; i < predStructures.size(); i++) {
            String token = predStructures.get(i);

            if ("<tr>".equals(token)) {
                currentCol = 0;
            } else if ("</tr>".equals(token)) {
                currentRow++;
            } else if (token.startsWith("<td")) {
                int colspan = 1;
                int rowspan = 1;

                // 若是 "<td></td>" 直接一次性结束
                if ("<td></td>".equals(token)) {
                    // 找下一个可用列
                    while (isOccupied.apply(currentRow, currentCol)) {
                        currentCol++;
                    }
                    // 记录逻辑坐标: [rowStart, rowEnd, colStart, colEnd]
                    // 相当于 rowSpan=1, colSpan=1
                    logicPoints.add(new int[]{currentRow, currentRow, currentCol, currentCol});
                    // 标记占用
                    markOccupied.accept(new int[]{currentRow, currentCol, 1, 1});
                    currentCol++;
                } else {
                    // 需要进一步解析后续 token
                    int j = i + 1;
                    // 提取 colspan, rowspan
                    while (j < predStructures.size() && !predStructures.get(j).startsWith(">")) {
                        String t = predStructures.get(j);
                        if (t.contains("colspan=")) {
                            // 假设形如 colspan="2" 或 colspan=2
                            colspan = parseIntAttr(t, "colspan=");
                        } else if (t.contains("rowspan=")) {
                            rowspan = parseIntAttr(t, "rowspan=");
                        }
                        j++;
                    }
                    // 此时 j 停在 '>' 或下一个标签
                    i = j - 1;

                    // 找下一个未被占用的格子
                    while (isOccupied.apply(currentRow, currentCol)) {
                        currentCol++;
                    }

                    int rStart = currentRow;
                    int rEnd = currentRow + rowspan - 1;
                    int cStart = currentCol;
                    int cEnd = currentCol + colspan - 1;

                    logicPoints.add(new int[]{rStart, rEnd, cStart, cEnd});

                    // 标记占用
                    // markOccupied 接收的 int[]{ row, col, rowSpan, colSpan }
                    markOccupied.accept(new int[]{rStart, cStart, rowspan, colspan});

                    currentCol += colspan;
                }
            }
        }
        return logicPoints;
    }

    /**
     * 过滤 OCR 结果：排除掉坐标低于表格中最小 y 的文本行
     *
     * @param predBBoxes 表格预测框集合
     * @param dtBoxes    OCR 框集合
     * @param recRes     OCR 文本
     * @return Object[]{ newDtBoxes, newRecRes }
     */
    private Object[] filterOcrResult(List<float[]> predBBoxes, List<float[]> dtBoxes, List<Pair<String, Float>> recRes) {
        // 1. 先找到 predBBoxes 中所有 y 坐标的最小值 minY
        //    对应 python: y1 = pred_bboxes[:, 1::2].min()
        //    这里我们遍历 each box, 取出 y 坐标中的最小值
        double minY = Double.MAX_VALUE;
        for (float[] box : predBBoxes) {
            float[] normalized = box.length == 8 ? normalizeBox(box) : box;
            // normalized: [x1, y1, x2, y2]
            // 取 y1, y2 中的较小值
            double tmp = Math.min(normalized[1], normalized[3]);
            if (tmp < minY) {
                minY = tmp;
            }
        }

        // 2. 筛选 dtBoxes 中满足 box[1::2].max() >= minY
        //    即 box 的最大 y >= minY
        //    python 里: if np.max(box[1::2]) < y1: continue
        List<float[]> newDtBoxes = new ArrayList<>();
        List<Pair<String, Float>> newRecRes = new ArrayList<>();
        for (int i = 0; i < dtBoxes.size(); i++) {
            float[] box = dtBoxes.get(i);
            // 可能是多边形, 也可能是 4 维
            float[] norm = (box.length == 8 ? normalizeBox(box) : box);
            // 取 y1,y2 中较大者
            double maxYBox = Math.max(norm[1], norm[3]);

            if (maxYBox < minY) {
                // 跳过
                continue;
            }
            newDtBoxes.add(box);
            newRecRes.add(recRes.get(i));
        }

        return new Object[]{newDtBoxes, newRecRes};
    }

    // =============== 以下是一些辅助方法 ===============

    /**
     * 若 box 为 8 长度 (x1, y1, x2, y2, x3, y3, x4, y4)，则取最小/最大 x,y 变为 [minX, minY, maxX, maxY]
     */
    private float[] normalizeBox(float[] box) {
        float minX = Math.min(Math.min(box[0], box[2]), Math.min(box[4], box[6]));
        float maxX = Math.max(Math.max(box[0], box[2]), Math.max(box[4], box[6]));
        float minY = Math.min(Math.min(box[1], box[3]), Math.min(box[5], box[7]));
        float maxY = Math.max(Math.max(box[1], box[3]), Math.max(box[5], box[7]));
        return new float[]{minX, minY, maxX, maxY};
    }

    /**
     * 将 box (x1, y1, x2, y2) 转成 (y0, x0, y1, x1)，
     * 用于与 TablePostProcessor.computeIou 的 (y0, x0, y1, x1) 格式对齐
     */
    private float[] recBox(float[] box) {
        // box: [x1, y1, x2, y2]
        // 这里假设 x1 < x2, y1 < y2
        return new float[]{box[0], box[1], box[2], box[3]};
    }

    /**
     * 将 (x1, y1, x2, y2) -> (y1, x1, y2, x2)
     * 方便 computeIou 需要的 (y0, x0, y1, x1) 格式
     */
    private float[] convertBoxToYX(float[] box) {
        // 这里 box: [x0, y0, x1, y1]
        return new float[]{box[1], box[0], box[3], box[2]};
    }

    /**
     * 解析 <td> 标签属性中 colspan=... 或 rowspan=... 数值
     */
    private int parseIntAttr(String token, String attrName) {
        // 例如 token = "colspan=\"2\"" 或 "colspan=2"
        int idx = token.indexOf(attrName);
        if (idx == -1) {
            return 1;
        }
        String sub = token.substring(idx + attrName.length());
        // 去掉可能的引号
        sub = sub.replaceAll("[\"']", "");
        // 去掉多余 > 或空格
        sub = sub.replaceAll("[^0-9]", "");
        try {
            return Integer.parseInt(sub);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    /**
     * 清理字符串中的前后空格、<b>、</b> 等标签
     */
    private String cleanupContent(String content) {
        if (content == null) {
            return "";
        }
        // 去掉首尾空格
        content = content.trim();
        // 去掉 <b> 与 </b>
        if (content.startsWith("<b>")) {
            content = content.substring(3);
        }
        if (content.endsWith("</b>")) {
            content = content.substring(0, content.length() - 4);
        }
        return content;
    }

}
