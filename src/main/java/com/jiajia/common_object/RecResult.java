package com.jiajia.common_object;

import lombok.Data;
import org.opencv.core.Point;

import java.util.Arrays;

@Data
public class RecResult {
    private Point[] dtBoxes;
    private final String text;
    private final float confidence;

    public RecResult(Point[] dtBoxes, String text, float confidence) {
        this.dtBoxes = dtBoxes;
        this.text = text;
        this.confidence = confidence;
    }

    @Override
    public String toString() {
        return "RecResult{" +
                "dtBoxes=" + Arrays.toString(dtBoxes) +
                ", text='" + text + '\'' +
                ", confidence=" + confidence +
                '}';
    }
}
