package com.jiajia.layout_analysis;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.jiajia.common_object.Layout;
import com.jiajia.utils.common.CollectionUtil;
import com.jiajia.utils.common.ImageUtils;
import com.jiajia.utils.cv.MatNdarray;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.nio.FloatBuffer;
import java.util.List;
import java.util.Map;

public class LayoutAnalysis {
    private float confThreshold;
    private float iouThreshold;
    private LayoutModel layoutModel;

    private int stride = 32;

    /** 预处理结果：输入张量 + padding 后的高宽（用于坐标反算） */
    private static final class PreparedInput {
        final Map<String, OnnxTensor> inputMap;
        final int newH;
        final int newW;

        PreparedInput(Map<String, OnnxTensor> inputMap, int newH, int newW) {
            this.inputMap = inputMap;
            this.newH = newH;
            this.newW = newW;
        }
    }

    public LayoutAnalysis(LayoutModel layoutModel) throws OrtException {
        this(layoutModel, 0.3f, 0.5f);
    }

    public LayoutAnalysis(LayoutModel layoutModel, float confThres, float iouThres) throws OrtException {
        this.layoutModel = layoutModel;
        this.confThreshold = confThres;
        this.iouThreshold = iouThres;
    }

    public List<Layout> detect(Mat img) throws OrtException {
        int rawImgHeight = img.height();
        int rawImgWidth = img.width();
        PreparedInput prepared = this.prepareInput(img, rawImgHeight, rawImgWidth);
        Map<String, OnnxTensor> inputMap = prepared.inputMap;
        try {
            float[][] predictions = inference(inputMap);
            List<Layout> detectionList = this.processOutput(predictions, rawImgHeight, rawImgWidth, prepared.newH, prepared.newW);
            return detectionList;
        } finally {
            for (OnnxTensor tensor : inputMap.values()) {
                tensor.close();
            }
        }
    }

    private float[][] inference(Map<String, OnnxTensor> inputMap) throws OrtException {
        try (OrtSession.Result result = this.layoutModel.getSession().run(inputMap)) {
            float[][] predicitons = ((float[][][])result.get(0).getValue())[0];
            return predicitons;
        }
    }

    private PreparedInput prepareInput(Mat img, int rawImgHeight, int rawImgWidth) throws OrtException {
        int targetH = 1024;
        int targetW = 1024;
        float r = (float) Math.min(targetH / (rawImgHeight * 1.0), targetW / (rawImgWidth * 1.0));
        int resizedH = Math.round(rawImgHeight * r);
        int resizedW = Math.round(rawImgWidth * r);

        Mat inputImg = new Mat();
        Imgproc.resize(img, inputImg, new Size(resizedW, resizedH), Imgproc.INTER_LINEAR);
        int padW = (targetW - resizedW) % this.stride;
        int padH = (targetH - resizedH) % this.stride;
        int top = padH/2;
        int bottom = padH - padH/2;
        int left = padW/2;
        int right = padW - padW/2;

        Core.copyMakeBorder(inputImg, inputImg, top, bottom, left, right, Core.BORDER_CONSTANT, new Scalar(114, 114, 114));

        int newH = inputImg.height();
        int newW = inputImg.width();

        float[] chw = MatNdarray.matToCHW(inputImg);

        Map<String, OnnxTensor> inputMap = CollectionUtil.newHashMap();

        FloatBuffer inputBuffer= FloatBuffer.wrap(chw);
        OnnxTensor inputTensor = OnnxTensor.createTensor(this.layoutModel.getEnv(), inputBuffer, new long[]{1, 3, newH, newW});

        inputMap.put(this.layoutModel.getInputName(), inputTensor);
        inputImg.release();
        return new PreparedInput(inputMap, newH, newW);
    }

    private List<Layout> processOutput(float[][] predictions, int rawImgHeight, int rawImgWidth, int newH, int newW) {
        List<Layout> detectionList = CollectionUtil.newArrayList();
        for(float[] bbox : predictions) {
            int label = (int) bbox[5];
            float conf = bbox[4];
            if(conf < this.confThreshold) {
                continue;
            }
            float[] new_bbox = new float[4];
            new_bbox[0] = bbox[0];
            new_bbox[1] = bbox[1];
            new_bbox[2] = bbox[2];
            new_bbox[3] = bbox[3];
            rescaleBoxes(new_bbox, rawImgHeight, rawImgWidth, newH, newW);
            detectionList.add(new Layout(this.layoutModel.getLabelMapping().getOrDefault(label, "unknown"), label, new_bbox, conf));
        }
        return detectionList;
    }

    public void rescaleBoxes(float[] bbox, int rawImgHeight, int rawImgWidth, int newH, int newW) {
        float gain = (float) Math.min(newH / (rawImgHeight * 1.0), newW / (rawImgWidth * 1.0));
        float padX = Math.round((newW - rawImgWidth * gain) / (2 * 1.0) - 0.1);
        float padY = Math.round((newH - rawImgHeight * gain) / (2* 1.0) - 0.1);
        bbox[0] -= padX;
        bbox[1] -= padY;
        bbox[2] -= padX;
        bbox[3] -= padY;

        bbox[0] /= (gain * 1.0);
        bbox[1] /= (gain * 1.0);
        bbox[2] /= (gain * 1.0);
        bbox[3] /= (gain * 1.0);
    }
}
