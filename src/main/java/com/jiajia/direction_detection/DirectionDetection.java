package com.jiajia.direction_detection;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.jiajia.common_object.Direction;
import com.jiajia.common_object.RotatedBox;
import com.jiajia.utils.common.CollectionUtil;
import com.jiajia.utils.common.ImageUtils;
import com.jiajia.utils.cv.MatNdarray;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DirectionDetection {
    private List<String> labels = Arrays.asList("No_Rotate", "Rotate");
    private DirectionModel directionModel;

    public DirectionDetection(DirectionModel directionModel) {
        this.directionModel = directionModel;
    }

    public RotatedBox detect(Mat imgMat) throws OrtException {
        int height = imgMat.height();
        int width = imgMat.width();
        Direction direction = null;
        String angle;
        RotatedBox rotatedBox = null;
        Mat processedMat = imgMat;
        if (height * 1.0 / width > 1.5) {
            processedMat = MatNdarray.rotateMat(imgMat);
            Map<String, OnnxTensor> inputMap = this.processInput(processedMat);
            try {
                float[] predictions = this.inference(inputMap);
                direction = this.processOutput(predictions);
                boolean rotate = false;
                if (direction.getName().equalsIgnoreCase("Rotate")) {
                    angle = "90";
                    rotate = true;
                } else {
                    angle = "270";
                }
                rotatedBox = new RotatedBox(new float[]{0, 0, width, height}, rotate, angle, direction.getProb());
            } finally {
                for (OnnxTensor tensor : inputMap.values()) {
                    tensor.close();
                }
            }
        } else {
            Map<String, OnnxTensor> inputMap = this.processInput(processedMat);
            try {
                float[] predictions = this.inference(inputMap);
                direction = this.processOutput(predictions);
                boolean rotate = false;
                if (direction.getName().equalsIgnoreCase("No_Rotate")) {
                    angle = "0";
                } else {
                    angle = "180";
                    rotate = true;
                }
                rotatedBox = new RotatedBox(new float[]{0, 0, width, height}, rotate, angle, direction.getProb());
            } finally {
                for (OnnxTensor tensor : inputMap.values()) {
                    tensor.close();
                }
            }
        }
        if (processedMat != imgMat) {
            processedMat.release();
        }
        return rotatedBox;
    }

    private Map<String, OnnxTensor> processInput(Mat img) throws OrtException {
        int imgC = 3;
        int imgH = 48;
        int imgW = 192;
        int rawImgHeight = img.height();
        int rawImgWidth = img.width();
        int resizeW = 0;
        float ratio = (float) rawImgWidth / (float) rawImgHeight;
        if (Math.ceil(imgH * ratio) > imgW) {
            resizeW = imgW;
        } else {
            resizeW = (int) (Math.ceil(imgH * ratio));
        }

        Mat inputImg = new Mat();
        Imgproc.resize(img, inputImg, new Size(resizeW, imgH));

        Mat maskMat = Mat.zeros(imgH, imgW, CvType.CV_8UC3);

        byte[] data = new byte[inputImg.channels() * inputImg.width()];
        for(int row = 0; row < inputImg.height(); row++){
            inputImg.get(row,0, data);
            maskMat.put(row,0, data);
        }
        // resize 后的图像已复制进 maskMat，释放以避免内存泄漏
        inputImg.release();

        Imgproc.cvtColor(maskMat, maskMat, Imgproc.COLOR_BGR2RGB);

        maskMat.convertTo(maskMat, CvType.CV_32FC3, 1./255);

        Map<String, OnnxTensor> inputMap = CollectionUtil.newHashMap();
        float[] whc = new float[(int) (imgH * imgW * imgC)];
        maskMat.get(0, 0, whc);
        float[] chw = ImageUtils.whc2cwh(whc);

        FloatBuffer inputBuffer= FloatBuffer.wrap(chw);
        OnnxTensor inputTensor = OnnxTensor.createTensor(this.directionModel.getEnv(), inputBuffer, new long[]{1, imgC, imgH, imgW});

        inputMap.put(this.directionModel.getInputName(), inputTensor);
        maskMat.release();
        return inputMap;
    }

    private float[] inference(Map<String, OnnxTensor> inputMap) throws OrtException {
        try (OrtSession.Result result = this.directionModel.getSession().run(inputMap)) {
            float[] predicitons = ((float[][])result.get(0).getValue())[0];
            return predicitons;
        }
    }

    private Direction processOutput(float[] predictions) {
        int maxIndex = 0;
        if(predictions[1] > predictions[0]) {
            maxIndex = 1;
        }
        return new Direction(this.labels.get(maxIndex), Float.valueOf(predictions[maxIndex]));
    }

}
