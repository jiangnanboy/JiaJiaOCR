package com.jiajia.table_detection;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiajia.common_object.DetectionResult;
import com.jiajia.common_onnx.Model;
import com.jiajia.utils.common.CollectionUtil;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TabDetection extends Model {

    private static final Logger LOGGER = LoggerFactory.getLogger(TabDetection.class);

    private String modelPath;
    private String classerPath;
    private String[] classNames = new String[]{"table"};;
    private int classNum = classNames.length;

    private int inputHeight;
    private int inputWidth;

//    private OrtEnvironment env;
    private OrtSession session;

    private int boxNum;
    private float confThreshold;
    private float nmsThreshold;

    /**
     * 构造函数，初始化YOLO检测器
     * @param modelPath ONNX模型路径
     * @param confThreshold 置信度阈值
     * @param nmsThreshold NMS阈值
     */
    public TabDetection(String modelPath, float confThreshold, float nmsThreshold) throws OrtException {
        super(modelPath);
        this.modelPath = modelPath;
        this.confThreshold = confThreshold;
        this.nmsThreshold = nmsThreshold;
        
        // 设置默认参数
        this.inputHeight = 960;
        this.inputWidth = 960;
        this.boxNum = 18900;
        
        // 加载模型和类别标签
//        loadModelAndConfig();
    }

    public TabDetection(byte[] tableDetBytes, float confThreshold, float nmsThreshold) throws OrtException {
        super(tableDetBytes);
        this.confThreshold = confThreshold;
        this.nmsThreshold = nmsThreshold;

        // 设置默认参数
        this.inputHeight = 960;
        this.inputWidth = 960;
        this.boxNum = 18900;

        // 加载模型和类别标签
//        loadModelAndConfig();
    }

    /**
     * 加载模型和配置
     */
    private void loadModelAndConfig() {
        try {
            // 读取类别名称
//            classNames = Files.readAllLines(Paths.get(classerPath)).toArray(new String[0]);
            classNames = new String[]{"table"};
            classNum = classNames.length;

            // 初始化ONNX Runtime环境和会话
//            env = OrtEnvironment.getEnvironment();
//            OrtSession.SessionOptions options = new OrtSession.SessionOptions();
//            options.addCPU(false); // 设置为CPU运行
//            session = env.createSession(modelPath, options);
        } catch (Exception e) {
            throw new RuntimeException("模型加载失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 对图像进行目标检测
     * @param imagePath 图像路径
     * @return 检测结果列表
     */
    public List<DetectionResult> detect(String imagePath) {
        Mat image = Imgcodecs.imread(imagePath);
        if (image.empty()) {
            throw new IllegalArgumentException("无法读取图像: " + imagePath);
        }
        return detect(image);
    }
    
    /**
     * 对Mat对象进行目标检测
     * @param image 图像Mat对象
     * @return 检测结果列表
     */
    public List<DetectionResult> detect(Mat image) {
        if (image == null || image.empty()) {
            throw new IllegalArgumentException("输入图像为空");
        }
        
        Mat tempImage = null;
        Mat inputImg = null;
        
        try {
            // 图像预处理
            int height = image.height();
            int width = image.width();
            tempImage = new Mat();
            
            if (height > inputHeight || width > inputWidth) {
                float scale = Math.min((float)inputHeight / height, (float)inputWidth / width);
                Size newSize = new Size((int)(width * scale), (int)(height * scale));
                Imgproc.resize(image, tempImage, newSize);
            } else {
                tempImage = image.clone();
            }
            
            float ratioHeight = (float)height / tempImage.height();
            float ratioWidth = (float)width / tempImage.width();
            
            inputImg = new Mat();
            Core.copyMakeBorder(tempImage, inputImg, 0, inputHeight - tempImage.height(), 0, inputWidth - tempImage.width(), Core.BORDER_CONSTANT, new Scalar(0, 0, 0));

            // 准备输入张量
            float[] inputTensor = new float[1 * 3 * inputHeight * inputWidth];
            for (int y = 0; y < inputImg.height(); y++) {
                for (int x = 0; x < inputImg.width(); x++) {
                    double[] pixel = inputImg.get(y, x);
                    inputTensor[0 * inputHeight * inputWidth * 3 + 0 * inputHeight * inputWidth + y * inputWidth + x] = (float)pixel[0] / 255.0f;
                    inputTensor[0 * inputHeight * inputWidth * 3 + 1 * inputHeight * inputWidth + y * inputWidth + x] = (float)pixel[1] / 255.0f;
                    inputTensor[0 * inputHeight * inputWidth * 3 + 2 * inputHeight * inputWidth + y * inputWidth + x] = (float)pixel[2] / 255.0f;
                }
            }

            // 创建输入张量并执行推理
            long[] shape = {1, 3, inputHeight, inputWidth};
            long startTime = System.currentTimeMillis();
            try (OnnxTensor input = OnnxTensor.createTensor(this.getEnv(), FloatBuffer.wrap(inputTensor), shape)) {
                // 准备输入容器
                Map<String, OnnxTensor> inputs = new HashMap<>();
                inputs.put("images", input);

                try (OrtSession.Result results = this.getSession().run(inputs)) {
                long endTime = System.currentTimeMillis();
                LOGGER.info("推理耗时: {}ms", (endTime - startTime));

                // 获取输出
                OnnxTensor outputTensor = (OnnxTensor) results.get(0);
                float[] outputData = new float[boxNum * (classNum + 4)];
                FloatBuffer buffer = outputTensor.getFloatBuffer();
                buffer.get(outputData);

                // 转置数据
                float[] transposedData = transpose(outputData, 4 + classNum, boxNum);

                // 处理检测结果
                List<DetectionResult> detResults = new ArrayList<>();
                float[] confidenceInfo = new float[classNum];
                float[] rectData = new float[4];

                for (int i = 0; i < boxNum; i++) {
                    System.arraycopy(transposedData, i * (classNum + 4), rectData, 0, 4);
                    System.arraycopy(transposedData, i * (classNum + 4) + 4, confidenceInfo, 0, classNum);

                    // 找出最大置信度和对应的类别
                    float maxScore = confidenceInfo[0];
                    int maxIndex = 0;
                    for (int j = 1; j < classNum; j++) {
                        if (confidenceInfo[j] > maxScore) {
                            maxScore = confidenceInfo[j];
                            maxIndex = j;
                        }
                    }

                    // 只保留置信度高于阈值的检测结果
                    if (maxScore >= confThreshold) {
                        int centerX = (int)(rectData[0] * ratioWidth);
                        int centerY = (int)(rectData[1] * ratioHeight);
                        int widthDet = (int)(rectData[2] * ratioWidth);
                        int heightDet = (int)(rectData[3] * ratioHeight);

                        Rect rect = new Rect(
                            centerX - widthDet / 2,
                            centerY - heightDet / 2,
                            widthDet,
                            heightDet
                        );

                        detResults.add(new DetectionResult(
                            maxIndex,
                            classNames[maxIndex],
                            rect,
                            maxScore
                        ));
                    }
                }

                // 应用非极大值抑制(NMS)
                List<DetectionResult> result;
                if (!detResults.isEmpty()) {
                    result = applyNMS(detResults);
                } else {
                    result = new ArrayList<>();
                }
                
                if (tempImage != null) {
                    tempImage.release();
                }
                if (inputImg != null) {
                    inputImg.release();
                }
                return result;
                }
            }
        } catch (Exception e) {
            if (tempImage != null) {
                tempImage.release();
            }
            if (inputImg != null) {
                inputImg.release();
            }
            throw new RuntimeException("检测过程中发生错误: " + e.getMessage(), e);
        }
    }
    
    /**
     * 应用非极大值抑制(NMS)
     * @param detResults 检测结果列表
     * @return 经过NMS处理后的检测结果列表
     */
    private List<DetectionResult> applyNMS(List<DetectionResult> detResults) {
        // 创建OpenCV的Rect和scores数组
        Rect[] boxes = new Rect[detResults.size()];
        float[] scores = new float[detResults.size()];
        
        for (int i = 0; i < detResults.size(); i++) {
            boxes[i] = detResults.get(i).getRect();
            scores[i] = detResults.get(i).getConfidence();
        }
        
        MatOfRect boxesMat = new MatOfRect();
        MatOfFloat scoresMat = new MatOfFloat();
        MatOfInt indices = new MatOfInt();
        MatOfRect2d boxesMat2d = new MatOfRect2d();
        try {
            // 转换为OpenCV格式
            boxesMat.fromArray(boxes);
            scoresMat.fromArray(scores);

            // 将MatOfRect转换为MatOfRect2d
            Rect[] rects = boxesMat.toArray();
            Rect2d[] rects2d = new Rect2d[rects.length];
            for (int i = 0; i < rects.length; i++) {
                Rect r = rects[i];
                rects2d[i] = new Rect2d(r.x, r.y, r.width, r.height);
            }
            boxesMat2d.fromArray(rects2d);

            // 应用NMS
            org.opencv.dnn.Dnn.NMSBoxes(boxesMat2d, scoresMat, confThreshold, nmsThreshold, indices);

            // 收集NMS后的结果
            int[] indicesArray = indices.toArray();
            List<DetectionResult> filteredResults = CollectionUtil.newArrayList();
            for (int idx : indicesArray) {
                filteredResults.add(detResults.get(idx));
            }
            return filteredResults;
        } finally {
            boxesMat.release();
            scoresMat.release();
            indices.release();
            boxesMat2d.release();
        }
    }
    
    /**
     * 转置数组
     */
    private float[] transpose(float[] data, int rows, int cols) {
        float[] transposed = new float[data.length];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                transposed[j * rows + i] = data[i * cols + j];
            }
        }
        return transposed;
    }
    
    /**
     * 在图像上绘制检测结果
     * @param image 原始图像
     * @param results 检测结果
     * @return 绘制了检测框的图像
     */
    public Mat drawResults(Mat image, List<DetectionResult> results) {
        Mat resultImage = image.clone();
        for (DetectionResult result : results) {
            String label = result.getClassLabel() + ": " + String.format("%.2f", result.getConfidence());
            Imgproc.putText(resultImage, label, new Point(result.getRect().x, result.getRect().y - 10), 
                            Imgproc.FONT_HERSHEY_SIMPLEX, 1.0, new Scalar(0, 0, 255), 2);
            Imgproc.rectangle(resultImage, new Point(result.getRect().x, result.getRect().y), 
                             new Point(result.getRect().x + result.getRect().width, 
                                      result.getRect().y + result.getRect().height), 
                             new Scalar(0, 0, 255), 2);
        }
        return resultImage;
    }
    
    /**
     * 保存图像
     * @param image 图像
     * @param filePath 保存路径
     */
    public void saveImage(Mat image, String filePath) {
        Imgcodecs.imwrite(filePath, image);
    }
    
    /**
     * 释放资源
     */
    public void close() {
        if (session != null) {
            try {
                session.close();
            } catch (Exception e) {
                LOGGER.error("Failed to close TabDetection session", e);
            }
        }
        if (this.getEnv() != null) {
            try {
                this.getEnv().close();
            } catch (Exception e) {
                LOGGER.error("Failed to close TabDetection environment", e);
            }
        }
    }
}
