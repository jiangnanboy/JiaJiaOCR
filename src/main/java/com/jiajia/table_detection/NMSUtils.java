package com.jiajia.table_detection;

import org.opencv.core.Rect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class NMSUtils {
    /**
     * 非极大值抑制实现
     * @param boxes 边界框列表
     * @param scores 置信度分数列表
     * @param confThreshold 置信度阈值
     * @param nmsThreshold NMS阈值
     * @return 保留的边界框索引
     */
    public static List<Integer> nms(List<Rect> boxes, List<Float> scores, float confThreshold, float nmsThreshold) {
        // 过滤低于置信度阈值的框
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < scores.size(); i++) {
            if (scores.get(i) >= confThreshold) {
                indices.add(i);
            }
        }

        // 按置信度排序
        Collections.sort(indices, new Comparator<Integer>() {
            @Override
            public int compare(Integer i1, Integer i2) {
                return Float.compare(scores.get(i2), scores.get(i1));
            }
        });

        List<Integer> keepIndices = new ArrayList<>();
        boolean[] isSuppressed = new boolean[indices.size()];

        for (int i = 0; i < indices.size(); i++) {
            if (isSuppressed[i]) continue;

            keepIndices.add(indices.get(i));

            // 计算与其他框的IoU并抑制重叠的框
            for (int j = i + 1; j < indices.size(); j++) {
                if (isSuppressed[j]) continue;

                float iou = calculateIoU(boxes.get(indices.get(i)), boxes.get(indices.get(j)));
                if (iou >= nmsThreshold) {
                    isSuppressed[j] = true;
                }
            }
        }

        return keepIndices;
    }

    /**
     * 计算两个边界框的IoU
     */
    private static float calculateIoU(Rect box1, Rect box2) {
        int x1 = Math.max(box1.x, box2.x);
        int y1 = Math.max(box1.y, box2.y);
        int x2 = Math.min(box1.x + box1.width, box2.x + box2.width);
        int y2 = Math.min(box1.y + box1.height, box2.y + box2.height);

        if (x1 >= x2 || y1 >= y2) return 0.0f;

        float intersection = (x2 - x1) * (y2 - y1);
        float union = (float) (box1.area() + box2.area() - intersection);

        return intersection / union;
    }
}