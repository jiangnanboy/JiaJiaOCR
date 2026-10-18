package com.jiajia.core;

import com.jiajia.common_object.Box;
import com.jiajia.common_object.CancellationToken;
import com.jiajia.common_object.Text;
import com.jiajia.common_object.DetectionResult;
import com.jiajia.common_object.TableResult;
import com.jiajia.utils.common.CollectionUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;

import java.util.List;

/**
 * @author jiangnanboy
 * @date 2025/11/24 17:27
 */
public class TableDetRec {
    private TableDetection tabDetection;
    private TableRecognition tabRecognition;
    GeneralOCR generalOCR;

    public TableDetRec(TableDetection tableDetection, TableRecognition tableRecognition, GeneralOCR generalOCR) {
        if (tableDetection == null || tableRecognition == null || generalOCR == null) {
            throw new IllegalArgumentException("TableDetRec dependencies cannot be null. tableDetection=" + tableDetection + ", tableRecognition=" + tableRecognition + ", generalOCR=" + generalOCR);
        }
        this.tabDetection = tableDetection;
        this.tabRecognition = tableRecognition;
        this.generalOCR = generalOCR;
    }


    public List<TableResult> run(String imgPath) throws Exception {
        return this.run(imgPath, null);
    }

    public List<TableResult> run(String imgPath, CancellationToken cancellationToken) throws Exception {
        Mat img = Imgcodecs.imread(imgPath);
        try {
            return this.run(img, cancellationToken);
        } finally {
            if (img != null && !img.empty()) {
                img.release();
            }
        }
    }

    public List<TableResult> run(Mat img) throws Exception {
        return this.run(img, null);
    }

    public List<TableResult> run(Mat img, CancellationToken cancellationToken) throws Exception {
        List<TableResult> tableResultList = CollectionUtil.newArrayList();
        List<DetectionResult> detectionResultList = tabDetection.run(img);
        List<Mat> cutTableList = CollectionUtil.newArrayList();
        try {
            for(DetectionResult result : detectionResultList) {
                if (cancellationToken != null) {
                    cancellationToken.throwIfCancelled();
                }
                Rect tableRect = result.getRect();
                Mat cutMat = img.submat(tableRect);
                cutTableList.add(cutMat);
            }

            for(Mat mat : cutTableList) {
                if (cancellationToken != null) {
                    cancellationToken.throwIfCancelled();
                }
                List<Pair<Text, Box>> ocrResultList = generalOCR.run(mat, true, cancellationToken);
                TableResult tableResult = tabRecognition.run(mat, ocrResultList, cancellationToken);
                tableResultList.add(tableResult);
            }
            return tableResultList;
        } finally {
            for (Mat mat : cutTableList) {
                if (mat != null && !mat.empty()) {
                    mat.release();
                }
            }
        }
    }

}
