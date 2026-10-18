package com.jiajia.core;

import com.jiajia.common_object.Box;
import com.jiajia.common_object.CancellationToken;
import com.jiajia.common_object.RecResult;
import com.jiajia.common_object.Text;
import com.jiajia.table_recognition.TabRecognition;
import com.jiajia.common_object.TableResult;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author jiangnanboy
 * @date 2025/11/24 16:40
 */
public class TableRecognition {
    private TabRecognition tableCore;

    public TableRecognition(byte[] tableRegBytes) {
        tableCore = new TabRecognition(tableRegBytes);
    }

    public TableResult run(String imgPath, List<Pair<Text, Box>> ocrResultList) throws Exception {
        return this.run(imgPath, ocrResultList, null);
    }

    public TableResult run(String imgPath, List<Pair<Text, Box>> ocrResultList, CancellationToken cancellationToken) throws Exception {
        Mat img = Imgcodecs.imread(imgPath);
        try {
            return this.run(img, ocrResultList, cancellationToken);
        } finally {
            if (img != null && !img.empty()) {
                img.release();
            }
        }
    }

    public TableResult run(Mat img, List<Pair<Text, Box>> ocrResultList) throws Exception {
        return this.run(img, ocrResultList, null);
    }

    public TableResult run(Mat img, List<Pair<Text, Box>> ocrResultList, CancellationToken cancellationToken) throws Exception {
        Collections.reverse(ocrResultList);
        List<RecResult> recResultList = ocrResultList.stream().map(pair -> {
            String text = pair.getKey().getText();
            float score = pair.getValue().getScore();
            float[] linePosition = pair.getValue().getLinePosition();
            Point[] dtBoxes = {
                    new Point(linePosition[0], linePosition[1]),
                    new Point(linePosition[2], linePosition[3]),
                    new Point(linePosition[4], linePosition[5]),
                    new Point(linePosition[6], linePosition[7])};
            return new RecResult(dtBoxes, text, score);
        }).collect(Collectors.toList());
        TableResult tableResult = tableCore.table(img, recResultList, cancellationToken);
        return tableResult;
    }

    public void close() {
        if (tableCore != null) {
            tableCore.close();
        }
    }

}
