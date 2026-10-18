package com.jiajia.table_detection;

import ai.onnxruntime.OrtException;
import com.alibaba.fastjson2.JSON;
import com.jiajia.common_object.DetectionResult;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.util.List;

/**
 * @author jiangnanboy
 * @date 2025/6/11 16:15
 */
public class TableDetCore {
    public static void main(String...args) throws OrtException {
        nu.pattern.OpenCV.loadLocally();

        String modelPath = "E:\\idea_project\\ocr_project\\src\\main\\resources\\models\\anytable-det-yolo12s-imgsz960.onnx";
        TabDetection detector = new TabDetection(modelPath, 0.25f, 0.5f);
        String imgPath = "E:\\idea_project\\ocr_project\\src\\main\\resources\\test\\test13.jpeg";
        Mat mat = Imgcodecs.imread(imgPath);
        List<DetectionResult> detectionResultList = detector.detect(mat);
        System.out.println(JSON.toJSONString(detectionResultList));
        Mat resultMat = detector.drawResults(mat, detectionResultList);
        Imgcodecs.imwrite("table_md.jpg", resultMat);
    }

}
