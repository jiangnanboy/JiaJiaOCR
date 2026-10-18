package com.jiajia.core;

import ai.onnxruntime.OrtException;
import com.jiajia.common_object.DetectionResult;
import com.jiajia.table_detection.TabDetection;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.util.List;

/**
 * @author jiangnanboy
 * @date 2025/11/24 16:42
 */
public class TableDetection {
    private TabDetection tabDetection;

    public TableDetection(byte[] tableDetBytes) throws OrtException {
        this(tableDetBytes, 0.5f, 0.5f);
    }

    public TableDetection(byte[] tableDetBytes, float confThreshold, float nmsThreshold) throws OrtException {
        tabDetection = new TabDetection(tableDetBytes, confThreshold, nmsThreshold);
    }

    public List<DetectionResult> run(String imgPath) throws OrtException {
        Mat img = Imgcodecs.imread(imgPath);
        try {
            return this.run(img);
        } finally {
            if (img != null && !img.empty()) {
                img.release();
            }
        }
    }

    public  List<DetectionResult> run(Mat img) throws OrtException {
        List<DetectionResult> detectionResultList = tabDetection.detect(img);
        return detectionResultList;
    }

    public void close() {
        if (tabDetection != null) {
            tabDetection.close();
        }
    }

}
