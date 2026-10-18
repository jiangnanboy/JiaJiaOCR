package com.jiajia.text_recognition.convnext;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.jiajia.common_object.CancellationToken;
import com.jiajia.common_object.Text;
import com.jiajia.utils.common.CollectionUtil;
import com.jiajia.utils.common.ImageUtils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.nio.FloatBuffer;
import java.util.List;
import java.util.Map;

/**
 * @author jiangnanboy
 * @date 2024/7/5 21:28
 */
public class ConvRecognition {
    private ConvRegModel regModel = null;

    public ConvRecognition(ConvRegModel regModel) {
        this.regModel = regModel;
    }

    /**
     * @param imgMat
     * @return
     * @throws OrtException
     */
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
            float[][] predictions = inference(inputMap);
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

    /**
     * @param img
     * @return
     * @throws OrtException
     */
    private Map<String, OnnxTensor> prepareInput(Mat img) throws OrtException {
        int rawImgHeight = img.height();
        int rawImgWidth = img.width();
        float curRatio = rawImgWidth / (float)rawImgHeight;
        int maskHeight = 32;
        int maskWidth = 804;
        int curTargetHeight;
        int curTargetWidth;
        if(curRatio > (maskWidth / (float)maskHeight)) {
            curTargetHeight = maskHeight;
            curTargetWidth = maskWidth;
        } else {
            curTargetHeight = maskHeight;
            curTargetWidth = (int) (maskHeight * curRatio);
        }

        Mat inputImg = new Mat();
        Imgproc.resize(img, inputImg, new Size(curTargetWidth, curTargetHeight));
        //(32, 804, 3)
        Mat maskMat = Mat.zeros(maskHeight, maskWidth, CvType.CV_8UC3);
        //创建一行像素点的缓存数组
        byte[] data = new byte[inputImg.channels() * inputImg.width()];

        for(int row = 0; row < inputImg.height(); row++){
            inputImg.get(row,0, data);//当col为0的时候表示获取一行
            //写入
            maskMat.put(row,0, data);
        }
        // resize 后的图像已复制进 maskMat，释放以避免内存泄漏
        inputImg.release();

        //(32, 300, 3)
        Mat chunkMat1 = Mat.zeros(maskHeight, 300, CvType.CV_8UC3);
        Mat chunkMat2 = Mat.zeros(maskHeight, 300, CvType.CV_8UC3);
        Mat chunkMat3 = Mat.zeros(maskHeight, 300, CvType.CV_8UC3);
        List<Mat> chunkMatList = CollectionUtil.newArrayList();
        for(int i = 0; i<3; i++) {
            int left = (300 - 48) * i;
            chunkMatList.add(maskMat.colRange(left, left + 300));
        }
        byte[] data1 = new byte[maskMat.channels() * 300];

        for(int row=0; row < chunkMatList.get(0).height(); row++) {
            chunkMatList.get(0).get(row,0, data1);
            chunkMat1.put(row, 0, data1);
        }
        for(int row=0; row < chunkMatList.get(1).height(); row++) {
            chunkMatList.get(1).get(row,0, data1);
            chunkMat2.put(row, 0 ,data1);
        }
        for(int row=0; row < chunkMatList.get(2).height(); row++) {
            chunkMatList.get(2).get(row,0, data1);
            chunkMat3.put(row, 0 ,data1);
        }
        // 释放 colRange 产生的子矩阵视图
        for (Mat chunkView : chunkMatList) {
            chunkView.release();
        }

        chunkMat1.convertTo(chunkMat1, CvType.CV_32FC3, 1./255);
        chunkMat2.convertTo(chunkMat2, CvType.CV_32FC3, 1./255);
        chunkMat3.convertTo(chunkMat3, CvType.CV_32FC3, 1./255);

//        Imgproc.cvtColor(chunkMat1, chunkMat1, Imgproc.COLOR_BGR2RGB);
//        Imgproc.cvtColor(chunkMat2, chunkMat2, Imgproc.COLOR_BGR2RGB);
//        Imgproc.cvtColor(chunkMat3, chunkMat3, Imgproc.COLOR_BGR2RGB);

        Map<String, OnnxTensor> inputMap = CollectionUtil.newHashMap();
        float[] chw = new float[maskHeight * 3 * 300 * 3];

        float[] whc1 = new float[maskHeight * 300 * 3];
        chunkMat1.get(0, 0, whc1);
        float[] chw1 = ImageUtils.whc2cwh(whc1);

        float[] whc2 = new float[maskHeight * 300 * 3];
        chunkMat2.get(0, 0, whc2);
        float[] chw2 = ImageUtils.whc2cwh(whc2);

        float[] whc3 = new float[maskHeight * 300 * 3];
        chunkMat3.get(0, 0, whc3);
        float[] chw3 = ImageUtils.whc2cwh(whc3);

        int whcIndexTotal;
        for(int whcIndex=0; whcIndex < chw1.length; whcIndex++) {
            chw[whcIndex] = chw1[whcIndex];
        }
        whcIndexTotal = chw1.length;
        for(int whcIndex=0; whcIndex < chw2.length; whcIndex++) {
            chw[whcIndexTotal + whcIndex] = chw2[whcIndex];
        }
        whcIndexTotal += chw2.length;
        for(int whcIndex=0; whcIndex < chw3.length; whcIndex++) {
            chw[whcIndexTotal + whcIndex] = chw3[whcIndex];
        }
        FloatBuffer inputBuffer= FloatBuffer.wrap(chw);
        OnnxTensor inputTensor = OnnxTensor.createTensor(this.regModel.getEnv(), inputBuffer, new long[]{3, 3, maskHeight, 300});
        inputMap.put(this.regModel.getInputName(), inputTensor);
        maskMat.release();
        chunkMat1.release();
        chunkMat2.release();
        chunkMat3.release();
        return inputMap;
    }

    /**
     * @param inputMap
     * @return
     * @throws OrtException
     */
    private float[][] inference(Map<String, OnnxTensor> inputMap) throws OrtException {
        //output_name: NodeArg(name='result', type='tensor(float)', shape=[1, 160, 7644])
        try (OrtSession.Result result = this.regModel.getSession().run(inputMap)) {
            float[][] predicitons = ((float[][][])result.get(0).getValue())[0]; // [160, 7644]
            return predicitons;
        }
    }

    /**
     * @param predictions
     * @return
     */
    private Text processOutput(float[][] predictions) {
        long lastP = 0L;
        StringBuilder sb = new StringBuilder();
        for (float[] row : predictions) {
            long index = argmax(row);
            if ((index != lastP) && (index != 0L)) {
                sb.append(this.regModel.getLabelMapping().getOrDefault(index, " "));
            }
            lastP = index;
        }
        Text text = new Text(sb.toString());
        return text;
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
