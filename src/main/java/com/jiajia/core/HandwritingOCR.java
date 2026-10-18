package com.jiajia.core;

import ai.onnxruntime.OrtException;
import com.jiajia.common_object.Box;
import com.jiajia.common_object.Boxes;
import com.jiajia.common_object.CancellationToken;
import com.jiajia.common_object.RotatedBox;
import com.jiajia.common_object.Text;
import com.jiajia.direction_detection.DirectionDetection;
import com.jiajia.direction_detection.DirectionModel;
import com.jiajia.text_detection.dbnet.DbDetModel;
import com.jiajia.text_detection.dbnet.DbDetection;
import com.jiajia.text_recognition.convnext.ConvRecognition;
import com.jiajia.text_recognition.convnext.ConvRegModel;
import com.jiajia.utils.common.CollectionUtil;
import com.jiajia.utils.cv.OpenCVUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author jiangnanboy
 * @date 2025/11/24 16:39
 */
public class HandwritingOCR {
    private DbDetection detection;
    private DirectionDetection clsDetection;
    private ConvRecognition recognition;
    private DbDetModel detModel;
    private DirectionModel directionModel;
    private ConvRegModel regModel;

    public HandwritingOCR(byte[] clsBytes, byte[] detBytes, byte[] recBytes, byte[] vocabBytes) throws OrtException {

        detModel = new DbDetModel(detBytes);
        detection = new DbDetection(detModel);

        directionModel = new DirectionModel(clsBytes);
        clsDetection = new DirectionDetection(directionModel);

        regModel = new ConvRegModel(recBytes, vocabBytes);
        recognition = new ConvRecognition(regModel);
    }

    public List<Pair<Text, Box>> run(String imgPath) throws OrtException {
        return this.run(imgPath, false, null);
    }

    public List<Pair<Text, Box>> run(Mat img) throws OrtException {
        return this.run(img, false, null);
    }

    public List<Pair<Text, Box>> run(String imgPath, boolean clsBool) throws OrtException {
        return this.run(imgPath, clsBool, null);
    }

    public List<Pair<Text, Box>> run(Mat img, boolean clsBool) throws OrtException {
        return this.run(img, clsBool, null);
    }

    public List<Pair<Text, Box>> run(String imgPath, CancellationToken cancellationToken) throws OrtException {
        return this.run(imgPath, false, cancellationToken);
    }

    public List<Pair<Text, Box>> run(Mat img, CancellationToken cancellationToken) throws OrtException {
        return this.run(img, false, cancellationToken);
    }

    public List<Pair<Text, Box>> run(String imgPath, boolean clsBool, CancellationToken cancellationToken) throws OrtException {
        Mat img = Imgcodecs.imread(imgPath);
        try {
            return this.run(img, clsBool, cancellationToken);
        } finally {
            if (img != null && !img.empty()) {
                img.release();
            }
        }
    }

    public List<Pair<Text, Box>> run(Mat img, boolean clsBool, CancellationToken cancellationToken) throws OrtException {
        Boxes detBoxes = detection.detect(img, cancellationToken);
        List<Pair<Text, Box>> ocrResultList = null;
        List<Pair<Mat, Box>> subImgBoxList = CollectionUtil.newArrayList();
        try {
            if (Optional.ofNullable(detBoxes).isPresent()) {
                List<Box> boxList = detBoxes.getBoxes();
                for (Box box : boxList) {
                    if (cancellationToken != null) {
                        cancellationToken.throwIfCancelled();
                    }
                    Mat mat = OpenCVUtils.getRotateCropImage(img, box);
                    subImgBoxList.add(Pair.of(mat, box));
                }

                if (clsBool) {
                    for (Pair<Mat, Box> pair : subImgBoxList) {
                        if (cancellationToken != null) {
                            cancellationToken.throwIfCancelled();
                        }
                        Mat mat = pair.getKey();
                        RotatedBox rotatedBox = clsDetection.detect(mat);
                        if (Optional.ofNullable(rotatedBox).isPresent()) {
                            if (rotatedBox.getRotate()) {
                                if ("180".equals(rotatedBox.getText())) {
                                    Core.rotate(mat, mat, Core.ROTATE_180);
                                }
                            }
                        }
                    }
                }

                ocrResultList = CollectionUtil.newArrayList();
                for (Pair<Mat, Box> pair : subImgBoxList) {
                    if (cancellationToken != null) {
                        cancellationToken.throwIfCancelled();
                    }
                    Text text = recognition.recognize(pair.getKey(), cancellationToken);
                    ocrResultList.add(Pair.of(text, pair.getValue()));
                }
            }
            Collections.reverse(ocrResultList);
            return ocrResultList;
        } finally {
            for (Pair<Mat, Box> pair : subImgBoxList) {
                Mat mat = pair.getKey();
                if (mat != null && !mat.empty()) {
                    mat.release();
                }
            }
        }
    }

    public void close() {
        if (detModel != null) {
            detModel.closeModel();
        }
        if (directionModel != null) {
            directionModel.closeModel();
        }
        if (regModel != null) {
            regModel.closeModel();
        }
    }
}
