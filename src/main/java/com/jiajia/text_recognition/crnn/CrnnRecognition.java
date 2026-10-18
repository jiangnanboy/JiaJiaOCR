package com.jiajia.text_recognition.crnn;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.jiajia.common_object.CancellationToken;
import com.jiajia.common_object.Text;
import com.jiajia.utils.common.CollectionUtil;
import com.jiajia.utils.common.ImageUtils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.nio.FloatBuffer;
import java.util.Map;

public class CrnnRecognition {
    private CrnnRegModel regModel = null;
    public CrnnRecognition(CrnnRegModel regModel) {
        this.regModel = regModel;
    }

    public Text recognize(Mat imgMat) throws OrtException {
        return this.recognize(imgMat, null);
    }

    public Text recognize(Mat imgMat, CancellationToken cancellationToken) throws OrtException {
        if (cancellationToken != null) {
            cancellationToken.throwIfCancelled();
        }
        Map<String, OnnxTensor> inputMap = this.prepareInput(imgMat);
        try {
            if (cancellationToken != null) {
                cancellationToken.throwIfCancelled();
            }
            float[][] predictions = this.inference(inputMap);
            if (cancellationToken != null) {
                cancellationToken.throwIfCancelled();
            }
            Text text = this.processOutput(predictions);
            return text;
        } finally {
            for (OnnxTensor tensor : inputMap.values()) {
                tensor.close();
            }
        }
    }

    private Map<String, OnnxTensor> prepareInput(Mat img) throws OrtException {
        int rawImgHeight = img.height();
        int rawImgWidth = img.width();
        int maskHeight = 48;
        int maskWidth = 320;
        int curTargetHeight;
        int curTargetWidth;

        float maxWhRatio = (float) maskWidth / (float) maskHeight;

        float whRatio = (float) rawImgWidth / (float) rawImgHeight;

        maxWhRatio = Math.max(maxWhRatio, whRatio);
        maskWidth = (int)(maskHeight * maxWhRatio);

        if (Math.ceil(maskHeight * whRatio) > maskWidth) {
            curTargetWidth = maskWidth;
        } else {
            curTargetWidth = (int) (Math.ceil(maskHeight * whRatio));
        }
        curTargetHeight = maskHeight;

        Mat inputImg = new Mat();
        Imgproc.resize(img, inputImg, new Size(curTargetWidth, curTargetHeight));
        Mat maskMat = Mat.zeros(maskHeight, maskWidth, CvType.CV_8UC3);

        byte[] data = new byte[inputImg.channels() * inputImg.width()];
        for(int row = 0; row < inputImg.height(); row++){
            inputImg.get(row,0, data);
            maskMat.put(row,0, data);
        }
        inputImg.release();
        inputImg = maskMat.clone();

        Imgproc.cvtColor(inputImg, inputImg, Imgproc.COLOR_BGR2RGB);

        inputImg.convertTo(inputImg, CvType.CV_32FC3, 1./255);

        Map<String, OnnxTensor> inputMap = CollectionUtil.newHashMap();
        float[] whc = new float[(int) (maskHeight * maskWidth * 3)];
        inputImg.get(0, 0, whc);
        float[] chw = ImageUtils.whc2cwh(whc);

        FloatBuffer inputBuffer= FloatBuffer.wrap(chw);
        OnnxTensor inputTensor = OnnxTensor.createTensor(this.regModel.getEnv(), inputBuffer, new long[]{1, 3, maskHeight, maskWidth});

        inputMap.put(this.regModel.getInputName(), inputTensor);
        maskMat.release();
        inputImg.release();
        return inputMap;
    }

    private float[][] inference(Map<String, OnnxTensor> inputMap) throws OrtException {
        try (OrtSession.Result result = this.regModel.getSession().run(inputMap)) {
            float[][] predicitons = ((float[][][])result.get(0).getValue())[0];
            return predicitons;
        }
    }

    private Text processOutput(float[][] predictions) {
        long lastP = 0L;
        StringBuilder sb = new StringBuilder();
        for (float[] row : predictions) {
            long index = argmax(row);
            if((index != lastP) && (index != 0L)) {
                sb.append(this.regModel.getLabelMapping().getOrDefault(index, " "));
            }
            lastP = index;
        }
        return new Text(sb.toString());
    }
    
    private long argmax(float[] array) {
        long maxIndex = 0;
        float maxValue = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > maxValue) {
                maxValue = array[i];
                maxIndex = i;
            }
        }
        return maxIndex;
    }

}
