package com.jiajia.core;

import ai.onnxruntime.OrtException;
import com.alibaba.fastjson2.JSON;
import com.jiajia.common_object.Layout;
import com.jiajia.layout_analysis.LayoutAnalysis;
import com.jiajia.layout_analysis.LayoutModel;
import com.jiajia.utils.common.ImageUtils;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.util.List;

/**
 * @author jiangnanboy
 * @date 2025/11/24 16:40
 */
public class LayoutDetection {

    private LayoutAnalysis layoutAnalysis;
    private LayoutModel layoutModel;

    public LayoutDetection(byte[] layoutBytes) throws OrtException {
        layoutModel = new LayoutModel(layoutBytes);
        layoutAnalysis = new LayoutAnalysis(layoutModel);
    }

    public List<Layout> run(String imgPath) throws OrtException {
        Mat img = Imgcodecs.imread(imgPath);
        try {
            return this.run(img);
        } finally {
            if (img != null && !img.empty()) {
                img.release();
            }
        }
    }

    public List<Layout> run(Mat img) throws OrtException {
        List<Layout> detectionList = layoutAnalysis.detect(img);
        return detectionList;
    }

    public void close() {
        if (layoutModel != null) {
            layoutModel.closeModel();
        }
    }
}
