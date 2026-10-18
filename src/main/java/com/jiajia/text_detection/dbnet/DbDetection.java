package com.jiajia.text_detection.dbnet;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.jiajia.common_object.Box;
import com.jiajia.common_object.Boxes;
import com.jiajia.common_object.CancellationToken;
import com.jiajia.common_onnx.Model;
import com.jiajia.utils.common.CollectionUtil;
import com.jiajia.utils.common.ImageUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.Map;

public class DbDetection {
    private Boxes boxes = null;
    private float thresh = 0.5f;

    private DbDetModel detModel = null;

    /** 预处理结果：输入张量 + 缩放比例（用于输出坐标反算） */
    private static final class PreparedInput {
        final Map<String, OnnxTensor> inputMap;
        final float ratioH;
        final float ratioW;

        PreparedInput(Map<String, OnnxTensor> inputMap, float ratioH, float ratioW) {
            this.inputMap = inputMap;
            this.ratioH = ratioH;
            this.ratioW = ratioW;
        }
    }

    public Boxes getBoxes() {
        return boxes;
    }

    public void setBoxes(Boxes boxes) {
        this.boxes = boxes;
    }

    public float getThresh() {
        return thresh;
    }

    public void setThresh(float thresh) {
        this.thresh = thresh;
    }

    public DbDetection(DbDetModel detModel) {
        this.detModel = detModel;
    }

    public DbDetection(DbDetModel detModel, float thresh) {
        this.detModel = detModel;
        this.thresh = thresh;
    }

    public Boxes detect(Mat imgMat) throws OrtException {
        return this.detect(imgMat, null);
    }

    public Boxes detect(Mat imgMat, CancellationToken cancellationToken) throws OrtException {
        if (cancellationToken != null) {
            cancellationToken.throwIfCancelled();
        }
        PreparedInput prepared = this.prepareInput(imgMat);
        Map<String, OnnxTensor> inputMap = prepared.inputMap;
        try {
            if (cancellationToken != null) {
                cancellationToken.throwIfCancelled();
            }
            float[][] predictions = inference(inputMap);
            if (cancellationToken != null) {
                cancellationToken.throwIfCancelled();
            }
            Boxes detBoxes = this.processOutput(predictions, prepared.ratioH, prepared.ratioW, cancellationToken);
            return detBoxes;
        } finally {
            for (OnnxTensor tensor : inputMap.values()) {
                tensor.close();
            }
        }
    }

    private float[][] inference(Map<String, OnnxTensor> inputMap) throws OrtException {
        try (OrtSession.Result result = this.detModel.getSession().run(inputMap)) {
            float[][] predicitons = ((float[][][][])result.get(0).getValue())[0][0];
            return predicitons;
        }
    }

    private PreparedInput prepareInput(Mat img) throws OrtException {
        int rawImgHeight = img.height();
        int rawImgWidth = img.width();
        Mat inputImg = new Mat();

        int limitSideLen = 960;
        float ratio = 1.0f;

        if (Math.max(rawImgHeight, rawImgWidth) > limitSideLen) {
            if (rawImgHeight > rawImgWidth) {
                ratio = (float) limitSideLen / (float) rawImgHeight;
            } else {
                ratio = (float) limitSideLen / (float) rawImgWidth;
            }
        }

        int resizeH = (int) (rawImgHeight * ratio);
        int resizeW = (int) (rawImgWidth * ratio);

        resizeH = Math.round((float) resizeH / 32f) * 32;
        resizeW = Math.round((float) resizeW / 32f) * 32;

        float ratioH = resizeH / (float) rawImgHeight;
        float ratioW = resizeW / (float) rawImgWidth;

        Imgproc.resize(img, inputImg, new Size(resizeW, resizeH));
        Imgproc.cvtColor(inputImg, inputImg, Imgproc.COLOR_BGR2RGB);
        inputImg.convertTo(inputImg, CvType.CV_32FC3, 1./255);
        Core.subtract(inputImg, new Scalar(0.485, 0.456, 0.406), inputImg);
        Core.divide(inputImg, new Scalar(0.229, 0.224, 0.225), inputImg);

        Map<String, OnnxTensor> inputMap = CollectionUtil.newHashMap();

        float[] whc = new float[resizeH * resizeW * 3];
        inputImg.get(0, 0, whc);
        float[] chw = ImageUtils.whc2cwh(whc);

        FloatBuffer inputBuffer= FloatBuffer.wrap(chw);
        OnnxTensor inputTensor = OnnxTensor.createTensor(this.detModel.getEnv(), inputBuffer, new long[]{1, 3, resizeH, resizeW});

        inputMap.put(this.detModel.getInputName(), inputTensor);
        inputImg.release();
        return new PreparedInput(inputMap, ratioH, ratioW);
    }

    private Boxes processOutput(float[][] predictions, float ratioH, float ratioW, CancellationToken cancellationToken) {
        int height = predictions.length;
        int width = predictions[0].length;

        Mat predMat = new Mat(height, width, CvType.CV_32F);
        // 将二维数组展平后一次性写入，避免逐元素 JNI 调用
        float[] flat = new float[height * width];
        for (int i = 0; i < height; i++) {
            System.arraycopy(predictions[i], 0, flat, i * width, width);
        }
        predMat.put(0, 0, flat);

        Mat binary = new Mat();
        Core.compare(predMat, new Scalar(this.thresh), binary, Core.CMP_GT);
        binary.convertTo(binary, CvType.CV_8UC1);

        Pair<List<float[][][]>, float[]> pair = DBUtils.boxesFromBitmap(predictions, binary, 0.5f);

        Boxes textBoxes = null;
        if (pair != null && pair.getLeft() != null && pair.getLeft().size() > 0) {
            List<float[][][]> boxes = pair.getLeft();
            float[] scoreList = pair.getRight();

            List<Box> boxList = CollectionUtil.newArrayList();
            for (int i = 0; i < boxes.size(); i++) {
                if (cancellationToken != null) {
                    cancellationToken.throwIfCancelled();
                }
                float[][][] box = boxes.get(i);
                float[] scoreBox = new float[8];
                for (int p = 0; p < 4; p++) {
                    scoreBox[p * 2] = box[0][p][0] / ratioW;
                    scoreBox[p * 2 + 1] = box[0][p][1] / ratioH;
                }
                float score = scoreList[i];
                Box tBox = new Box(scoreBox, score);
                boxList.add(tBox);
            }
            textBoxes = new Boxes(boxList);
        }

        predMat.release();
        binary.release();
        return textBoxes;
    }
}
