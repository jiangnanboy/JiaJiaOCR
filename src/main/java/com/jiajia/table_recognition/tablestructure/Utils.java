package com.jiajia.table_recognition.tablestructure;

import com.jiajia.common_object.Pair;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.*;

class TableLabelDecode {
    // 字符对应的 Map
    private Map<String, Integer> dictMap;
    // 字符列表
    private List<String> character;
    // 一些特殊 token
    private String begStr = "sos";  // 对应 beg_str
    private String endStr = "eos";  // 对应 end_str
    // <td> 相关 token
    private List<String> tdToken = Arrays.asList("<td>", "<td", "<td></td>");

    public TableLabelDecode(List<String> dictCharacter, boolean mergeNoSpanStructure) {
        if (mergeNoSpanStructure) {
            // 若字典中不存在 "<td></td>" 则添加
            if (!dictCharacter.contains("<td></td>")) {
                dictCharacter.add("<td></td>");
            }
            // 若字典中存在 "<td>" 则移除
            dictCharacter.remove("<td>");
        }
        // 在头尾添加特殊字符
        dictCharacter = addSpecialChar(dictCharacter);
        this.character = dictCharacter;
        // 构建 map
        this.dictMap = new HashMap<>();
        for (int i = 0; i < dictCharacter.size(); i++) {
            this.dictMap.put(dictCharacter.get(i), i);
        }
    }

    public Map<String, Object> call(Map<String, float[][][]> preds, List<float[][]> batch) {
        float[][][] structureProbs = preds.get("structure_probs");
        float[][][] bboxPreds = preds.get("loc_preds");

        float[][] shapeList = batch.get(batch.size() - 1);

        // 1. 先做 decode
        Map<String, Object> result = decode(structureProbs, bboxPreds, shapeList);

        // 如果 batch 仅有 1 个元素（只包含 shapeList），直接返回
        if (batch.size() == 1) {
            return result;
        }
        return result;
    }

    /**
     * 解析结构 & bbox
     *
     * @param structureProbs [batch, seq_len, vocab_size]
     * @param bboxPreds      [batch, seq_len, 8] 或 [batch, seq_len, 4]
     * @param shapeList      每张图的宽高信息等
     * @return 解析结果（结构+坐标）
     */
    public Map<String, Object> decode(float[][][] structureProbs, float[][][] bboxPreds, float[][] shapeList) {
        // end_idx
        int endIdx = dictMap.get(endStr);
        // 获取忽略 token
        List<Integer> ignoredTokens = getIgnoredTokens();

        // argmax
        // structureProbs.argmax(axis=2) & max(axis=2) 模拟
        int batchSize = structureProbs.length;
        List<Pair<List<String>, Float>> structureBatchList = new ArrayList<>();
        List<List<float[]>> bboxBatchList = new ArrayList<>();

        for (int bIdx = 0; bIdx < batchSize; bIdx++) {
            float[][] seqProb = structureProbs[bIdx];  // [seq_len, vocab_size]
            float[][] seqBbox = bboxPreds[bIdx];       // [seq_len, 8] or [seq_len, 4]
            List<String> structureList = new ArrayList<>();
            List<Float> scoreList = new ArrayList<>();
            List<float[]> bboxList = new ArrayList<>();

            for (int t = 0; t < seqProb.length; t++) {
                // argmax
                int charIdx = argmax(seqProb[t]);
                float charScore = seqProb[t][charIdx];

                // 终止条件
                if (t > 0 && charIdx == endIdx) {
                    break;
                }
                // 忽略
                if (ignoredTokens.contains(charIdx)) {
                    continue;
                }

                // 获取文字
                String text = character.get(charIdx);
                // 如果是 <td> 相关token，则存 bbox
                if (tdToken.contains(text)) {
                    float[] bbox = seqBbox[t];
                    // 解码
                    float[] decodedBbox = bboxDecode(bbox, shapeList[bIdx]);
                    bboxList.add(decodedBbox);
                }
                structureList.add(text);
                scoreList.add(charScore);
            }
            // 计算平均 score
            float avgScore = 0.0f;
            if (!scoreList.isEmpty()) {
                float sum = 0.0f;
                for (Float sc : scoreList) {
                    sum += sc;
                }
                avgScore = sum / scoreList.size();
            }
            structureBatchList.add(Pair.of(structureList, avgScore));
            bboxBatchList.add(bboxList);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("bbox_batch_list", bboxBatchList);
        result.put("structure_batch_list", structureBatchList);
        return result;
    }

    /**
     * 对单个 bbox 进行解码
     *
     * @param bbox  bbox 数组 [x0, y0, x1, y1, ...] 等
     * @param shape [h, w, ratio, ratio] (假设)
     */
    private float[] bboxDecode(float[] bbox, float[] shape) {
        float h = shape[0];
        float w = shape[1];
        // 仅演示：bbox[0::2] *= w, bbox[1::2] *= h
        float[] decoded = Arrays.copyOf(bbox, bbox.length);
        for (int i = 0; i < decoded.length; i++) {
            if (i % 2 == 0) { // x
                decoded[i] = decoded[i] * w;
            } else { // y
                decoded[i] = decoded[i] * h;
            }
        }
        return decoded;
    }

    /**
     * 获取忽略 token
     */
    private List<Integer> getIgnoredTokens() {
        int begIdx = getBegEndFlagIdx("beg");
        int endIdx = getBegEndFlagIdx("end");
        return Arrays.asList(begIdx, endIdx);
    }

    /**
     * 获取 beg/end token 的索引
     */
    private int getBegEndFlagIdx(String begOrEnd) {
        if ("beg".equals(begOrEnd)) {
            return dictMap.get(begStr);
        } else if ("end".equals(begOrEnd)) {
            return dictMap.get(endStr);
        } else {
            throw new RuntimeException("Unsupported type: " + begOrEnd);
        }
    }

    /**
     * 向字符字典头尾添加特殊字符
     */
    private List<String> addSpecialChar(List<String> dictCharacter) {
        List<String> newList = new ArrayList<>();
        newList.add(begStr);
        newList.addAll(dictCharacter);
        newList.add(endStr);
        return newList;
    }

    /**
     * 手动实现 argmax，并返回索引
     */
    private int argmax(float[] arr) {
        int maxIndex = 0;
        float maxVal = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] > maxVal) {
                maxVal = arr[i];
                maxIndex = i;
            }
        }
        return maxIndex;
    }
}

class TablePreprocess {
    private int tableMaxLen = 488;
    // 预处理操作列表
    private List<Map<String, Object>> preProcessList;
    private List<Operator> ops;

    public TablePreprocess() {
        buildPreProcessList();
        this.ops = createOperators();
    }

    /**
     * 调用入口
     *
     * @param data 传入需要处理的 Map，比如包含 "image", "bboxes" 等
     */
    public Map<String, Object> call(Map<String, Object> data) {
        if (this.ops == null) {
            this.ops = new ArrayList<>();
        }
        for (Operator op : this.ops) {
            data = op.apply(data);
            if (data == null) {
                return null;
            }
        }
        return data;
    }

    /**
     * 根据 preProcessList 创建具体的 Operator
     */
    private List<Operator> createOperators() {
        List<Operator> ops = new ArrayList<>();
        for (Map<String, Object> operator : preProcessList) {
            // YAML 结构: operator = { "ResizeTableImage": { "max_len": 488 } }
            // 这里将 key 视为类名，value 视为参数
            for (Map.Entry<String, Object> entry : operator.entrySet()) {
                String opName = entry.getKey();
                Object param = entry.getValue();
                Operator op = buildOperator(opName, param);
                if (op != null) {
                    ops.add(op);
                }
            }
        }
        return ops;
    }

    private Operator buildOperator(String opName, Object param) {
        if ("ResizeTableImage".equals(opName)) {
            // param 是一个 Map
            Map<String, Object> p = (Map<String, Object>) param;
            int maxLen = (int) p.get("max_len");
            return new ResizeTableImageOperator(maxLen, false, false);
        } else if ("PaddingTableImage".equals(opName)) {
            Map<String, Object> p = (Map<String, Object>) param;
            List<Integer> sizeList = (List<Integer>) p.get("size");
            int padH = sizeList.get(0).intValue();
            int padW = sizeList.get(1).intValue();
            return new PaddingTableImageOperator(padH, padW);
        } else if ("NormalizeImage".equals(opName)) {
            Map<String, Object> p = (Map<String, Object>) param;
            // 解析 mean, std, scale, order
            String order = (String) p.get("order");
            double scale = (double) p.get("scale");
            Scalar mean = (Scalar) p.get("mean");
            Scalar std = (Scalar) p.get("std");
            return new NormalizeImageOperator(scale, mean, std, order);
        } else if ("ToCHWImage".equals(opName)) {
            return new ToCHWImageOperator();
        } else if ("KeepKeys".equals(opName)) {
            Map<String, Object> p = (Map<String, Object>) param;
            List<String> keepKeys = (List<String>) p.get("keep_keys");
            return new KeepKeysOperator(keepKeys);
        }
        return null;
    }

    /**
     * 初始化预处理操作列表
     */
    private void buildPreProcessList() {
        this.preProcessList = new ArrayList<>();

        Map<String, Object> resizeOp = new HashMap<>();
        Map<String, Object> resizeParam = new HashMap<>();
        resizeParam.put("max_len", tableMaxLen);
        resizeOp.put("ResizeTableImage", resizeParam);

        Map<String, Object> normalizeOp = new HashMap<>();
        Map<String, Object> normParam = new HashMap<>();
        normParam.put("std", new Scalar(0.229, 0.224, 0.225));
        normParam.put("mean", new Scalar(0.485, 0.456, 0.406));
        normParam.put("scale", 1.0 / 255.0);
        normParam.put("order", "hwc");
        normalizeOp.put("NormalizeImage", normParam);

        Map<String, Object> padOp = new HashMap<>();
        Map<String, Object> padParam = new HashMap<>();
        padParam.put("size", Arrays.asList(tableMaxLen, tableMaxLen));
        padOp.put("PaddingTableImage", padParam);

        Map<String, Object> tochwOp = new HashMap<>();
        tochwOp.put("ToCHWImage", null);

        Map<String, Object> keepKeysOp = new HashMap<>();
        Map<String, Object> keepKeysParam = new HashMap<>();
        keepKeysParam.put("keep_keys", Arrays.asList("image", "shape"));
        keepKeysOp.put("KeepKeys", keepKeysParam);

        // 注意顺序
        this.preProcessList.add(resizeOp);
        this.preProcessList.add(normalizeOp);
        this.preProcessList.add(padOp);
        this.preProcessList.add(tochwOp);
        this.preProcessList.add(keepKeysOp);
    }
}

/**
 * 定义 Operator 接口，所有预处理操作都实现此接口
 */
interface Operator {
    Map<String, Object> apply(Map<String, Object> data);
}

class ResizeTableImageOperator implements Operator {
    private int maxLen;
    private boolean resizeBboxes;
    private boolean inferMode;

    public ResizeTableImageOperator(int maxLen, boolean resizeBboxes, boolean inferMode) {
        this.maxLen = maxLen;
        this.resizeBboxes = resizeBboxes;
        this.inferMode = inferMode;
    }

    @Override
    public Map<String, Object> apply(Map<String, Object> data) {
        Mat img = (Mat) data.get("image");
        int height = img.rows();
        int width = img.cols();
        float ratio = (float) maxLen / Math.max(height, width);
        int resizeH = Math.round(height * ratio);
        int resizeW = Math.round(width * ratio);

        Mat resizeImg = new Mat();
        Imgproc.resize(img, resizeImg, new Size(resizeW, resizeH));

        if (resizeBboxes && !inferMode) {
            float[][] bboxes = (float[][]) data.get("bboxes");
            for (float[] box : bboxes) {
                for (int i = 0; i < box.length; i++) {
                    box[i] *= ratio;
                }
            }
            data.put("bboxes", bboxes);
        }

        data.put("image", resizeImg);
        data.put("src_img", img);
        // shape: [height, width, ratio, ratio]
        data.put("shape", new float[]{height, width, ratio, ratio});
        data.put("max_len", maxLen);
        return data;
    }
}

class PaddingTableImageOperator implements Operator {
    private int padH;
    private int padW;

    public PaddingTableImageOperator(int padH, int padW) {
        this.padH = padH;
        this.padW = padW;
    }

    @Override
    public Map<String, Object> apply(Map<String, Object> data) {
        Mat img = (Mat) data.get("image");
        int height = img.rows();
        int width = img.cols();

        // 创建一个新图，用于填充
        Mat paddingImg = Mat.zeros(padH, padW, img.type());

        // 将原图复制到 paddingImg 的左上角
        // ROI
        Mat roi = paddingImg.submat(0, height, 0, width);
        img.copyTo(roi);

        data.put("image", paddingImg);

        // 更新 shape
        float[] shape = (float[]) data.get("shape");
        // shape: [h, w, ratio, ratio], 这里扩展 [padH, padW]
        float[] newShape = Arrays.copyOf(shape, shape.length + 2);
        newShape[shape.length] = padH;
        newShape[shape.length + 1] = padW;
        data.put("shape", newShape);
        return data;
    }
}

class NormalizeImageOperator implements Operator {
    private double scale;
    private Scalar mean;
    private Scalar std;
    private String order;

    public NormalizeImageOperator(double scale, Scalar mean, Scalar std, String order) {
        this.scale = scale;
        this.mean = mean;
        this.std = std;
        this.order = order;
    }

    @Override
    public Map<String, Object> apply(Map<String, Object> data) {
        Mat img = (Mat) data.get("image");

        // 转 float
        if (img.type() != CvType.CV_32FC3) {
            img.convertTo(img, CvType.CV_32FC3);
        }

        // 遍历每个像素 (H*W) 并进行 (pixel*scale - mean) / std
        Mat normImg = normalize(img);

        data.put("image", normImg);
        return data;
    }

    /**
     * normalize：
     * 先将图像类型转为 CV_32FC3，并在 convertTo(...)时乘以 scale
     * 再用 Scalar 减去 mean，除以 std
     */
    private Mat normalize(Mat img) {
        // 先将 img 转成 float32，并乘以 scale => (img * scale)
        img.convertTo(img, CvType.CV_32FC3, scale);

        // 通过 Core.subtract & Core.divide 实现 (pixel - mean) / std
        // 现在 img = (img * scale)
        // subtract => (img - mean)
        Core.subtract(img, mean, img);
        // divide => (img - mean) / std
        Core.divide(img, std, img);

        return img;
    }
}

class ToCHWImageOperator implements Operator {
    @Override
    public Map<String, Object> apply(Map<String, Object> data) {
        Mat img = (Mat) data.get("image");
        float[][][] permuted = permute(img);
        data.put("image", permuted);
        return data;
    }

    /**
     * 将图像从 (H,W,C) 转换为 (C,H,W) 的三维 float 数组
     */
    private float[][][] permute(Mat img) {
        int h = img.rows();
        int w = img.cols();
        int c = img.channels();
        float[][][] output = new float[c][h][w];

        for (int row = 0; row < h; row++) {
            for (int col = 0; col < w; col++) {
                double[] pixel = img.get(row, col);  // size = c
                for (int ch = 0; ch < c; ch++) {
                    output[ch][row][col] = (float) pixel[ch];
                }
            }
        }
        return output;
    }
}

class KeepKeysOperator implements Operator {
    private List<String> keepKeys;

    public KeepKeysOperator(List<String> keepKeys) {
        this.keepKeys = keepKeys;
    }

    @Override
    public Map<String, Object> apply(Map<String, Object> data) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : keepKeys) {
            result.put(key, data.get(key));
        }
        return result;
    }
}