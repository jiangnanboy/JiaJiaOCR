package com.jiajia.layout_analysis;

import ai.onnxruntime.OrtException;
import com.jiajia.common_onnx.Model;
import com.jiajia.utils.common.CollectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author jiangnanboy
 * @date 2024/12/11 17:03
 */
public class LayoutModel extends Model {

    private static final Logger LOGGER = LoggerFactory.getLogger(LayoutModel.class);

    private Map<Integer, String> labelMapping;

    public LayoutModel (byte[] layoutBytes) throws OrtException {
        super(layoutBytes);
        initLabel();
    }
    /**
     * names: {0: 'title', 1: 'plain text', 2: 'abandon', 3: 'figure', 4: 'figure_caption', 5: 'table',
     * 6: 'table_caption', 7: 'table_footnote', 8: 'isolate_formula', 9: 'formula_caption'}
     */
    private void initLabel() {
        LOGGER.info("init doc layout label...");

        labelMapping = new HashMap<Integer, String>() {{
            put(0, "title");
            put(1, "plain_text");
            put(2, "abandon");
            put(3, "figure");
            put(4, "figure_caption");
            put(5, "table");
            put(6, "table_caption");
            put(7, "table_footnote");
            put(8, "isolate_formula");
            put(9, "formula_caption");
        }};
    }


    /**
     * @return
     */
    public Map<Integer, String> getLabelMapping() {
        return this.labelMapping;
    }

}
