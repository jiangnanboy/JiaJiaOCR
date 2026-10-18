package com.jiajia.common_object;

import org.opencv.core.Rect;

public class DetectionResult {
    private int classId;
    private String classLabel;
    private Rect rect;
    private float confidence;

    public DetectionResult(int classId, String classLabel, Rect rect, float confidence) {
        this.classId = classId;
        this.classLabel = classLabel;
        this.rect = rect;
        this.confidence = confidence;
    }

    public int getClassId() {
        return classId;
    }

    public void setClassId(int classId) {
        this.classId = classId;
    }

    public String getClassLabel() {
        return classLabel;
    }

    public void setClassLabel(String classLabel) {
        this.classLabel = classLabel;
    }

    public Rect getRect() {
        return rect;
    }

    public void setRect(Rect rect) {
        this.rect = rect;
    }

    public float getConfidence() {
        return confidence;
    }

    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }
}
