package com.jiajia.table_recognition;

import com.jiajia.common_object.CancellationToken;
import com.jiajia.common_object.OrtInferConfig;
import com.jiajia.common_object.Pair;
import com.jiajia.common_object.RecResult;
import com.jiajia.common_object.Triple;
import com.jiajia.common_object.TableResult;
import com.jiajia.table_recognition.entity.TableConfig;
import com.jiajia.table_recognition.entity.TableModelType;
import com.jiajia.table_recognition.tablematcher.TableMatch;
import com.jiajia.table_recognition.tablestructure.TableStructurer;
import com.jiajia.table_recognition.utils.LoadImage;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TabRecognition {

    private final TableModelType modelType;
    private final LoadImage loadImg;
    private final TableStructurer tableStructure;
    private final TableMatch tableMatcher;

    public TabRecognition(String modelPath) {
        this(new TableConfig(modelPath));
    }

    public TabRecognition(byte[] tableRegBytes) {
        this(new TableConfig(), tableRegBytes);
    }

    public TabRecognition(TableConfig config) {
        this.modelType = config.modelType;
        // 初始化 LoadImage
        this.loadImg = new LoadImage();

        OrtInferConfig inferConfig = new OrtInferConfig();
        inferConfig.setModelPath(config.modelPath);
        inferConfig.setUseCuda(config.useCuda);
        inferConfig.setDeviceId(config.deviceId);
        inferConfig.setUseArena(true);

        // 1. 初始化表格结构识别器
        this.tableStructure = new TableStructurer(inferConfig);

        // 2. 初始化表格匹配器
        this.tableMatcher = new TableMatch();
    }

    public TabRecognition(TableConfig config, byte[] tableRegBytes) {
        this.modelType = config.modelType;
        // 初始化 LoadImage
        this.loadImg = new LoadImage();

        OrtInferConfig inferConfig = new OrtInferConfig();
        inferConfig.setUseCuda(config.useCuda);
        inferConfig.setDeviceId(config.deviceId);
        inferConfig.setUseArena(true);

        // 1. 初始化表格结构识别器
        this.tableStructure = new TableStructurer(inferConfig, tableRegBytes);

        // 2. 初始化表格匹配器
        this.tableMatcher = new TableMatch();
    }

    public TableResult table(String imagePath, List<RecResult> ocrResult) throws Exception {
        return this.runImpl(imagePath, ocrResult, false, null);
    }

    public TableResult table(Path imagePath, List<RecResult> ocrResult) throws Exception {
        return this.runImpl(imagePath, ocrResult, false, null);
    }

    public TableResult table(byte[] imageData, List<RecResult> ocrResult) throws Exception {
        return this.runImpl(imageData, ocrResult, false, null);
    }

    public TableResult table(BufferedImage image, List<RecResult> ocrResult) throws Exception {
        return this.runImpl(image, ocrResult, false, null);
    }

    public TableResult table(Mat mat, List<RecResult> ocrResult) throws Exception {
        return this.runImpl(mat, ocrResult, false, null);
    }

    public TableResult table(String imagePath, List<RecResult> ocrResult, boolean returnLogicPoints) throws Exception {
        return this.runImpl(imagePath, ocrResult, returnLogicPoints, null);
    }

    public TableResult table(Path imagePath, List<RecResult> ocrResult, boolean returnLogicPoints) throws Exception {
        return this.runImpl(imagePath, ocrResult, returnLogicPoints, null);
    }

    public TableResult table(byte[] imageData, List<RecResult> ocrResult, boolean returnLogicPoints) throws Exception {
        return this.runImpl(imageData, ocrResult, returnLogicPoints, null);
    }

    public TableResult table(BufferedImage image, List<RecResult> ocrResult, boolean returnLogicPoints) throws Exception {
        return this.runImpl(image, ocrResult, returnLogicPoints, null);
    }

    public TableResult table(Mat mat, List<RecResult> ocrResult, boolean returnLogicPoints) throws Exception {
        return this.runImpl(mat, ocrResult, returnLogicPoints, null);
    }

    public TableResult table(String imagePath, List<RecResult> ocrResult, CancellationToken cancellationToken) throws Exception {
        return this.runImpl(imagePath, ocrResult, false, cancellationToken);
    }

    public TableResult table(Path imagePath, List<RecResult> ocrResult, CancellationToken cancellationToken) throws Exception {
        return this.runImpl(imagePath, ocrResult, false, cancellationToken);
    }

    public TableResult table(byte[] imageData, List<RecResult> ocrResult, CancellationToken cancellationToken) throws Exception {
        return this.runImpl(imageData, ocrResult, false, cancellationToken);
    }

    public TableResult table(BufferedImage image, List<RecResult> ocrResult, CancellationToken cancellationToken) throws Exception {
        return this.runImpl(image, ocrResult, false, cancellationToken);
    }

    public TableResult table(Mat mat, List<RecResult> ocrResult, CancellationToken cancellationToken) throws Exception {
        return this.runImpl(mat, ocrResult, false, cancellationToken);
    }

    public TableResult table(String imagePath, List<RecResult> ocrResult, boolean returnLogicPoints, CancellationToken cancellationToken) throws Exception {
        return this.runImpl(imagePath, ocrResult, returnLogicPoints, cancellationToken);
    }

    public TableResult table(Path imagePath, List<RecResult> ocrResult, boolean returnLogicPoints, CancellationToken cancellationToken) throws Exception {
        return this.runImpl(imagePath, ocrResult, returnLogicPoints, cancellationToken);
    }

    public TableResult table(byte[] imageData, List<RecResult> ocrResult, boolean returnLogicPoints, CancellationToken cancellationToken) throws Exception {
        return this.runImpl(imageData, ocrResult, returnLogicPoints, cancellationToken);
    }

    public TableResult table(BufferedImage image, List<RecResult> ocrResult, boolean returnLogicPoints, CancellationToken cancellationToken) throws Exception {
        return this.runImpl(image, ocrResult, returnLogicPoints, cancellationToken);
    }

    public TableResult table(Mat mat, List<RecResult> ocrResult, boolean returnLogicPoints, CancellationToken cancellationToken) throws Exception {
        return this.runImpl(mat, ocrResult, returnLogicPoints, cancellationToken);
    }

    private TableResult runImpl(Object imgContent, List<RecResult> ocrResult, boolean returnLogicPoints, CancellationToken cancellationToken) throws Exception {
        if (cancellationToken != null) {
            cancellationToken.throwIfCancelled();
        }

        // 1. 加载图像
        Mat img = this.loadImg.call(imgContent);

        try {
            if (cancellationToken != null) {
                cancellationToken.throwIfCancelled();
            }

            long startTime = System.currentTimeMillis();
            int h = img.rows();
            int w = img.cols();

            // 2. 解析 dt_boxes, rec_res
            Pair<List<float[]>, List<Pair<String, Float>>> boxAndRec = getBoxesRecs(ocrResult, h, w);
            List<float[]> dtBoxes = boxAndRec.getLeft();
            List<Pair<String, Float>> recRes = boxAndRec.getRight();

            if (cancellationToken != null) {
                cancellationToken.throwIfCancelled();
            }

            // 3. 表格结构推理: pred_structures, pred_bboxes, ...
            Triple<List<String>, List<float[]>, Double> structureRes = this.tableStructure.call(img);
            List<String> predStructures = structureRes.getLeft();
            List<float[]> predBBoxes = structureRes.getMiddle();

            if (cancellationToken != null) {
                cancellationToken.throwIfCancelled();
            }

            // 4、如果是 slanet-plus，需要缩放
            if (TableModelType.SLANET_PLUS.equals(this.modelType)) {
                predBBoxes = adaptSlanetPlus(img, predBBoxes);
            }

            if (cancellationToken != null) {
                cancellationToken.throwIfCancelled();
            }

            // 5. 调用表格匹配器, 组装最终 HTML
            String predHtml = this.tableMatcher.call(predStructures, predBBoxes, dtBoxes, recRes);
            double elapse = (System.currentTimeMillis() - startTime) / 1000.0;
            TableResult tableResult = new TableResult(predHtml, predBBoxes);
            tableResult.setElapse(elapse);
            // 注：当前 TableResult 未保存逻辑坐标，returnLogicPoints 参数暂未生效
            return tableResult;
        } finally {
            if (img != null && !img.empty()) {
                img.release();
            }
        }
    }

    /**
     * 处理OCR结果，获取边框和识别结果
     *
     * @param ocrResults OCR结果列表
     * @param h 图片高度
     * @param w 图片宽度
     * @return 包含边框和识别结果的BoxRecResult对象
     */
    public Pair<List<float[]>, List<Pair<String, Float>>> getBoxesRecs(List<RecResult> ocrResults, int h, int w) {
        List<float[]> rBoxes = new ArrayList<>();
        List<Pair<String, Float>> recRes = ocrResults.stream()
                .map(rec -> Pair.of(rec.getText(), rec.getConfidence()))
                .collect(Collectors.toList());

        for (RecResult result : ocrResults) {
            // 处理识别结果和分数

            // 处理边框坐标
            Point[] box = result.getDtBoxes();
            if (box == null || box.length == 0) {
                continue; // 如果边框为空，跳过
            }

            float xMin = Float.MAX_VALUE;
            float xMax = Float.MIN_VALUE;
            float yMin = Float.MAX_VALUE;
            float yMax = Float.MIN_VALUE;

            for (Point point : box) {
                float x = (float) point.x;
                float y = (float) point.y;
                if (x < xMin) xMin = x;
                if (x > xMax) xMax = x;
                if (y < yMin) yMin = y;
                if (y > yMax) yMax = y;
            }

            // 调整边界并确保在图片范围内
            xMin = Math.max(0, xMin - 1);
            xMax = Math.min(w, xMax + 1);
            yMin = Math.max(0, yMin - 1);
            yMax = Math.min(h, yMax + 1);

            rBoxes.add(new float[]{xMin, yMin, xMax, yMax});
        }
        return Pair.of(rBoxes, recRes);
    }

    /**
     * 适配 slanet-plus 输出 box 缩放
     *
     * @param img         图像对象
     * @param predBBoxes  原预测 box, shape [n, 8] or [n, 4] 需看项目情况
     * @return 适配后的 box
     */
    private List<float[]> adaptSlanetPlus(Mat img, List<float[]> predBBoxes) {
        int h = img.rows();
        int w = img.cols();

        int resized = 488;
        float ratio = Math.min((float) resized / h, (float) resized / w);

        float w_ratio = resized / (w * ratio);
        float h_ratio = resized / (h * ratio);

        for (int i = 0; i < predBBoxes.size(); i++) {
            for (int j = 0; j < predBBoxes.get(i).length; j++) {
                if (j % 2 == 0) { // x
                    predBBoxes.get(i)[j] *= w_ratio;
                } else { // y
                    predBBoxes.get(i)[j] *= h_ratio;
                }
            }
        }
        return predBBoxes;
    }

    public void close() {
        if (tableStructure != null) {
            tableStructure.close();
        }
    }
}
