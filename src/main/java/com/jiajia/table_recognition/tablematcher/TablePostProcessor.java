package com.jiajia.table_recognition.tablematcher;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TablePostProcessor {

    /**
     * 处理由错误预测造成的孤立 span 情况
     * 例如原来预测 <td rowspan="2"></td> 被错误写成 <td></td> rowspan="2"></b></td> 等
     * @param theadPart thead 部分的字符串
     * @return 修正后的 thead 部分
     */
    public static String dealIsolateSpan(String theadPart) {
        // 1. 找出所有孤立 span 的字符串
        Pattern isolatePattern = Pattern.compile(
                "<td></td> rowspan=\"(\\d)+\" colspan=\"(\\d)+\"></b></td>|" +
                "<td></td> colspan=\"(\\d)+\" rowspan=\"(\\d)+\"></b></td>|" +
                "<td></td> rowspan=\"(\\d)+\"></b></td>|" +
                "<td></td> colspan=\"(\\d)+\"></b></td>"
        );
        Matcher isolateMatcher = isolatePattern.matcher(theadPart);

        // 使用循环将所有匹配的 group 收集到列表
        List<String> isolateList = new ArrayList<>();
        while (isolateMatcher.find()) {
            isolateList.add(isolateMatcher.group());
        }

        // 2. 为了找出孤立 span 的具体数值
        Pattern spanPattern = Pattern.compile(
                " rowspan=\"(\\d)+\" colspan=\"(\\d)+\"|" +
                " colspan=\"(\\d)+\" rowspan=\"(\\d)+\"|" +
                " rowspan=\"(\\d)+\"|" +
                " colspan=\"(\\d)+\""
        );

        List<String> correctedList = new ArrayList<>();
        for (String isolateItem : isolateList) {
            Matcher spanMatcher = spanPattern.matcher(isolateItem);
            if (spanMatcher.find()) {
                // 3. 将找到的 span 信息拼接回 <td ... ></td>
                String spanStrInIsolateItem = spanMatcher.group();
                String correctedItem = "<td" + spanStrInIsolateItem + "></td>";
                correctedList.add(correctedItem);
            } else {
                // 如果没匹配到，则填充 null 占位
                correctedList.add(null);
            }
        }

        // 4. 将匹配到的孤立 span 替换成正确的 td
        for (int i = 0; i < isolateList.size(); i++) {
            String isolateItem = isolateList.get(i);
            String correctedItem = correctedList.get(i);
            if (correctedItem != null) {
                theadPart = theadPart.replace(isolateItem, correctedItem);
            }
        }
        return theadPart;
    }

    /**
     * 处理 <td></td> 中重复出现 <b> 或 </b> 的情况
     * 确保每个 <td></td> 中只有一组 <b></b>
     * @param theadPart thead 部分的字符串
     * @return 修正后的 thead 部分
     */
    public static String dealDuplicateBb(String theadPart) {
        // 1. 找出所有 <td>...</td> 的内容
        Pattern tdPattern = Pattern.compile(
                "<td rowspan=\"(\\d)+\" colspan=\"(\\d)+\">(.+?)</td>|" +
                "<td colspan=\"(\\d)+\" rowspan=\"(\\d)+\">(.+?)</td>|" +
                "<td rowspan=\"(\\d)+\">(.+?)</td>|" +
                "<td colspan=\"(\\d)+\">(.+?)</td>|" +
                "<td>(.*?)</td>"
        );
        Matcher tdMatcher = tdPattern.matcher(theadPart);

        List<String> tdList = new ArrayList<>();
        while (tdMatcher.find()) {
            tdList.add(tdMatcher.group());
        }

        // 2. 判断每个 <td></td> 中是否有多个 <b> 或 </b>
        List<String> newTdList = new ArrayList<>();
        for (String tdItem : tdList) {
            // 统计 <b> 与 </b> 的出现次数
            int countOpenB = countOccurrences(tdItem, "<b>");
            int countCloseB = countOccurrences(tdItem, "</b>");

            if (countOpenB > 1 || countCloseB > 1) {
                // 多个 <b></b> 的情况
                // 1. 去除所有的 <b> 和 </b>
                String replaced = tdItem.replace("<b>", "").replace("</b>", "");
                // 2. 在 <td> 与 </td> 之间重新加上一组 <b></b>
                replaced = replaced.replace("<td>", "<td><b>").replace("</td>", "</b></td>");
                newTdList.add(replaced);
            } else {
                newTdList.add(tdItem);
            }
        }

        // 3. 将替换结果更新回 theadPart
        for (int i = 0; i < tdList.size(); i++) {
            theadPart = theadPart.replace(tdList.get(i), newTdList.get(i));
        }
        return theadPart;
    }

    /**
     * 在 <thead></thead> 中插入或修正 <b></b> 标签
     * @param resultToken 整个文本 token
     * @return 处理后结果
     */
    public static String dealBb(String resultToken) {
        // 1. 查找 <thead>...</thead> 结构
        Pattern theadPattern = Pattern.compile("<thead>(.*?)</thead>", Pattern.DOTALL);
        Matcher theadMatcher = theadPattern.matcher(resultToken);
        if (!theadMatcher.find()) {
            // 如果没有 thead 结构，则直接返回
            return resultToken;
        }
        // 获取匹配的 thead 部分
        String originTheadPart = theadMatcher.group();
        String theadPart = originTheadPart;

        // 2. 判断 <thead></thead> 中是否包含 rowspan 或 colspan
        Pattern spanPattern = Pattern.compile(
                "<td rowspan=\"(\\d)+\" colspan=\"(\\d)+\">|" +
                "<td colspan=\"(\\d)+\" rowspan=\"(\\d)+\">|" +
                "<td rowspan=\"(\\d)+\">|" +
                "<td colspan=\"(\\d)+\">"
        );
        Matcher spanMatcher = spanPattern.matcher(theadPart);
        List<String> spanList = new ArrayList<>();
        while (spanMatcher.find()) {
            spanList.add(spanMatcher.group());
        }
        boolean hasSpanInHead = !spanList.isEmpty();

        if (!hasSpanInHead) {
            // <thead></thead> 不包含 rowspan 或 colspan
            // 1. 将 <td> 转为 <td><b>, 并将 </td> 转为 </b></td>
            // 2. 避免重复的 <b><b> 和 </b></b>
            theadPart = theadPart.replace("<td>", "<td><b>")
                                 .replace("</td>", "</b></td>")
                                 .replace("<b><b>", "<b>")
                                 .replace("</b></b>", "</b>");
        } else {
            // <thead></thead> 包含 rowspan 或 colspan
            // 先处理带有 rowspan 或 colspan 的 <td> 标签，在其尾部插入 <b>
            List<String> replacedSpanList = new ArrayList<>();
            for (String sp : spanList) {
                // 将 ">" 替换为 "><b>"
                replacedSpanList.add(sp.replace(">", "><b>"));
            }
            // 逐一替换回 theadPart
            for (int i = 0; i < spanList.size(); i++) {
                theadPart = theadPart.replace(spanList.get(i), replacedSpanList.get(i));
            }

            // 然后统一把 </td> 替换为 </b></td>
            theadPart = theadPart.replace("</td>", "</b></td>");

            // 用正则去除可能重复的 <b> 或 </b>
            Pattern mbPattern = Pattern.compile("(<b>)+");
            theadPart = mbPattern.matcher(theadPart).replaceAll("<b>");
            Pattern mgbPattern = Pattern.compile("(</b>)+");
            theadPart = mgbPattern.matcher(theadPart).replaceAll("</b>");

            // 同时处理普通不带 span 的情况
            theadPart = theadPart.replace("<td>", "<td><b>").replace("<b><b>", "<b>");
        }

        // 将空白的 <td><b></b></td> 转回 <td></td>
        theadPart = theadPart.replace("<td><b></b></td>", "<td></td>");

        // 处理重复的 <b></b>
        theadPart = dealDuplicateBb(theadPart);

        // 修复孤立的 span token
        theadPart = dealIsolateSpan(theadPart);

        // 替换原始 thead 内容
        resultToken = resultToken.replace(originTheadPart, theadPart);
        return resultToken;
    }

    /**
     * 对空白占位 <eb>, <eb1>, <eb2>, ... 等进行替换
     * 最终替换为 <td> ... </td> 或带格式修饰的标签
     * @param masterToken 整个解析后文本
     * @return 替换后文本
     */
    public static String dealEbToken(String masterToken) {
        // 可根据需求继续扩展
        masterToken = masterToken.replace("<eb></eb>", "<td></td>");
        masterToken = masterToken.replace("<eb1></eb1>", "<td> </td>");
        masterToken = masterToken.replace("<eb2></eb2>", "<td><b> </b></td>");
        masterToken = masterToken.replace("<eb3></eb3>", "<td>\u2028\u2028</td>");
        masterToken = masterToken.replace("<eb4></eb4>", "<td><sup> </sup></td>");
        masterToken = masterToken.replace("<eb5></eb5>", "<td><b></b></td>");
        masterToken = masterToken.replace("<eb6></eb6>", "<td><i> </i></td>");
        masterToken = masterToken.replace("<eb7></eb7>", "<td><b><i></i></b></td>");
        masterToken = masterToken.replace("<eb8></eb8>", "<td><b><i> </i></b></td>");
        masterToken = masterToken.replace("<eb9></eb9>", "<td><i></i></td>");
        masterToken = masterToken.replace(
                "<eb10></eb10>", "<td><b> \u2028 \u2028 </b></td>"
        );
        return masterToken;
    }

    /**
     * 计算两矩形框的简单距离
     * @param box1 第一个矩形框 (x1, y1, x2, y2)
     * @param box2 第二个矩形框 (x3, y3, x4, y4)
     * @return 距离值
     */
    public static double distance(float[] box1, float[] box2) {
        // box1: x1, y1, x2, y2
        // box2: x3, y3, x4, y4
        float x1 = box1[0], y1 = box1[1], x2 = box1[2], y2 = box1[3];
        float x3 = box2[0], y3 = box2[1], x4 = box2[2], y4 = box2[3];

        double dis = Math.abs(x3 - x1) + Math.abs(y3 - y1)
                + Math.abs(x4 - x2) + Math.abs(y4 - y2);
        double dis2 = Math.abs(x3 - x1) + Math.abs(y3 - y1);
        double dis3 = Math.abs(x4 - x2) + Math.abs(y4 - y2);
        return dis + Math.min(dis2, dis3);
    }

    /**
     * 计算两个矩形框的 IoU (Intersection over Union)
     * @param rec1 矩形框1，格式 (y0, x0, y1, x1)
     * @param rec2 矩形框2，格式 (y0, x0, y1, x1)
     * @return IoU 值
     */
    public static double computeIou(float[] rec1, float[] rec2) {
        // 计算两个矩形框的面积
        // rec1: (y0, x0, y1, x1)
        // rec2: (y0, x0, y1, x1)
        double sRec1 = (rec1[2] - rec1[0]) * 1.0 * (rec1[3] - rec1[1]);
        double sRec2 = (rec2[2] - rec2[0]) * 1.0 * (rec2[3] - rec2[1]);

        // 计算面积之和
        double sumArea = sRec1 + sRec2;

        // 计算相交部分的边界
        double leftLine = Math.max(rec1[1], rec2[1]);
        double rightLine = Math.min(rec1[3], rec2[3]);
        double topLine = Math.max(rec1[0], rec2[0]);
        double bottomLine = Math.min(rec1[2], rec2[2]);

        // 判断是否有交集
        if (leftLine >= rightLine || topLine >= bottomLine) {
            return 0.0;
        } else {
            double intersect = (rightLine - leftLine) * (bottomLine - topLine);
            // IoU = 交集面积 / (总面积 - 交集面积)
            return intersect / (sumArea - intersect);
        }
    }

    /**
     * 辅助方法：统计字符串中子串出现的次数
     * @param source 原始字符串
     * @param target 需要统计的子串
     * @return 出现次数
     */
    private static int countOccurrences(String source, String target) {
        if (target.isEmpty()) {
            return 0;
        }
        int count = 0;
        int fromIndex = 0;
        while ((fromIndex = source.indexOf(target, fromIndex)) != -1) {
            count++;
            fromIndex += target.length();
        }
        return count;
    }
}
