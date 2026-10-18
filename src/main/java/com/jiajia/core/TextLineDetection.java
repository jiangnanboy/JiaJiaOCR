package com.jiajia.core;

import ai.onnxruntime.OrtException;
import com.jiajia.common_object.Boxes;
import com.jiajia.text_detection.dbnet.DbDetModel;
import com.jiajia.text_detection.dbnet.DbDetection;
import com.jiajia.utils.common.ImageUtils;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

/**
 * @author jiangnanboy
 * @date 2025/11/25 16:57
 */
public class TextLineDetection {
    private DbDetection detection;
    private DbDetModel detModel;

    public TextLineDetection(byte[] detBytes) throws OrtException {
        detModel = new DbDetModel(detBytes);
        detection = new DbDetection(detModel);
    }

    public Boxes run(String imgPath) throws OrtException {
        Mat img = Imgcodecs.imread(imgPath);
        try {
            return this.run(img);
        } finally {
            if (img != null && !img.empty()) {
                img.release();
            }
        }
    }

    public Boxes run(Mat img) throws OrtException {
        Boxes detBoxes = detection.detect(img);
        return detBoxes;
    }

    public void close() {
        if (detModel != null) {
            detModel.closeModel();
        }
    }

}
