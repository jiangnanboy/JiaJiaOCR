package com.jiajia.table_recognition.tablestructure;

import com.jiajia.common_object.OrtInferConfig;
import com.jiajia.common_object.Pair;
import com.jiajia.common_object.Triple;
import org.opencv.core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class TableStructurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(TableStructurer.class);

    private TablePreprocess preprocessOp;
    private OrtInferSession session;
    private TableLabelDecode postprocessOp;
    private List<String> character;

    public TableStructurer(OrtInferConfig config) {
        this.preprocessOp = new TablePreprocess();
        this.session = new OrtInferSession(config);
        this.character = this.session.getCharacterList("character");
        this.postprocessOp = new TableLabelDecode(this.character, true);
    }

    public TableStructurer(OrtInferConfig config, byte[] tableRegBytes) {
        this.preprocessOp = new TablePreprocess();
        this.session = new OrtInferSession(config, tableRegBytes);
        this.character = this.session.getCharacterList("character");
        this.postprocessOp = new TableLabelDecode(this.character, true);
    }

    public void close() {
        if (session != null) {
            session.close();
        }
    }
    public Triple<List<String>, List<float[]>, Double> call(Mat img) {
        long startTime = System.currentTimeMillis();

        // 1. 构造待处理数据，Python 中 data = {"image": img}
        Map<String, Object> data = new HashMap<>();
        data.put("image", img);

        // 2. 进行预处理： data = self.preprocess_op(data)
        Map<String, Object> processedData = this.preprocessOp.call(data);
        if (processedData == null) {
            return Triple.of(null, null, 0.0);
        }

        // 3. 获取预处理结果：image数据和 shape
        float[][][] processedImg = (float[][][]) processedData.get("image");  // 预处理后真正的图像数据
        float[] shapeArr = (float[]) processedData.get("shape");  // 预处理后记录的 shape 信息

        // 如果图像为空，直接返回
        if (img == null) {
            return Triple.of(null, null, 0.0);
        }

        // 4. 将图像 expand_dim, 以匹配推理输入
        float[][][][] inputData = expandDims(processedImg);

        // 5. 调用推理 session
        Object[] outputs;
        try {
            outputs = session.run(inputData);
        } catch (Exception e) {
            LOGGER.error("Table structure inference failed", e);
            return Triple.of(Collections.emptyList(), Collections.emptyList(), 0.0);
        }

        // 6. 解析推理结果
        Map<String, float[][][]> preds = new HashMap<>();
        preds.put("loc_preds", (float[][][]) outputs[0]);
        preds.put("structure_probs", (float[][][]) outputs[1]);

        // 7. shape_list = np.expand_dims(data[-1], axis=0)
        float[][] shapeList = expandDimShape(shapeArr);

        // 8. 调用后处理
        List<float[][]> postprocessBatch = new ArrayList<>();
        postprocessBatch.add(shapeList);

        Map<String, Object> postResult = this.postprocessOp.call(preds, postprocessBatch);

        // 9. 从 postResult 中取出 bbox_list & structure_batch_list
        List<float[]> bboxList = ((List<List<float[]>>) postResult.get("bbox_batch_list")).get(0);
        List<String> structureBatchList = ((List<Pair<List<String>, Float>>) postResult.get("structure_batch_list")).get(0).getLeft();

        // 10. 给结构前后插入 <html>, <body>, <table>, </table>, </body>, </html>
        List<String> structureStrList = new ArrayList<>();
        structureStrList.add("<html>");
        structureStrList.add("<body>");
        structureStrList.add("<table>");
        structureStrList.addAll(structureBatchList);
        structureStrList.add("</table>");
        structureStrList.add("</body>");
        structureStrList.add("</html>");
        // 11. 计算耗时
        double elapse = (System.currentTimeMillis() - startTime) / 1000.0;

        // 12. 返回 (structure_str_list, bbox_list, elapse)
        return Triple.of(structureStrList, bboxList, elapse);
    }

    /**
     * 将 shapeObj (通常是 float[]) 扩展为 [1, ...]
     */
    private float[][] expandDimShape(float[] shapeArr) {
        float[][] result = new float[1][shapeArr.length];
        System.arraycopy(shapeArr, 0, result[0], 0, shapeArr.length);
        return result;
    }

    /**
     * 扩展一个 batch 维度 => [1, C, H, W]
     */
    private float[][][][] expandDims(float[][][] permuted) {
        int c = permuted.length;
        int h = permuted[0].length;
        int w = permuted[0][0].length;

        float[][][][] out = new float[1][c][h][w];
        for (int cc = 0; cc < c; cc++) {
            for (int hh = 0; hh < h; hh++) {
                System.arraycopy(permuted[cc][hh], 0, out[0][cc][hh], 0, w);
            }
        }
        return out;
    }
}
