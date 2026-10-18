package com.jiajia.table_recognition.utils;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Collections;
import java.util.List;

/**
 * 插入样式到 HTML、绘制表格框、保存图片等
 */
public class VisTable {

    private static final Logger LOGGER = LoggerFactory.getLogger(VisTable.class);

    private final LoadImage loadImage;

    public VisTable() {
        this.loadImage = new LoadImage();
    }

    /**
     * 主方法
     *
     * @param imgPath          图片路径
     * @param tableHtmlStr     HTML 字符串
     * @param saveHtmlPath     若不为 null，保存带有边框样式的 HTML 文件
     * @param tableCellBboxes  传入的表格单元格坐标，形状可为 N x 4 或 N x 8
     * @param saveDrawedPath   若不为 null，保存画了矩形/多边形的图片
     * @param logicPoints      单元格逻辑信息，例如 [row_start, row_end, col_start, col_end]
     * @param saveLogicPath    保存画了逻辑信息图片的路径
     * @return 返回已画好框的 Mat，若 tableCellBboxes 为空或不合法则返回 null
     */
    public Mat call(String imgPath,
                    String tableHtmlStr,
                    String saveHtmlPath,
                    List<float[]> tableCellBboxes,
                    String saveDrawedPath,
                    List<int[]> logicPoints,
                    String saveLogicPath) throws LoadImageError {

        // 1. 处理并保存 HTML 文件
        if (saveHtmlPath != null && !saveHtmlPath.isEmpty()) {
            String htmlWithBorder = insertBorderStyle(tableHtmlStr);
            saveHtml(saveHtmlPath, htmlWithBorder);
        }

        // 2. 如果 tableCellBboxes 为空则直接返回
        if (tableCellBboxes == null || tableCellBboxes.isEmpty()) {
            return null;
        }

        // 3. 加载图片
        Mat img = loadImage.call(imgPath);

        // 4. 判断坐标维度，进行不同的绘制
        int cols = tableCellBboxes.get(0).length;
        Mat drawedImg;
        if (cols == 4) {
            drawedImg = drawRectangle(img, tableCellBboxes);
        } else if (cols == 8) {
            drawedImg = drawPolylines(img, tableCellBboxes);
        } else {
            throw new IllegalArgumentException("tableCellBboxes 的列数必须是4或8，当前为：" + cols);
        }

        // 5. 若需要保存结果图
        if (saveDrawedPath != null && !saveDrawedPath.isEmpty()) {
            saveImg(saveDrawedPath, drawedImg);
        }

        // 6. 若需要在图片右侧添加逻辑信息
        if (saveLogicPath != null && !saveLogicPath.isEmpty() && logicPoints != null) {
            // 假设 polygons = [[x0, y0, x1, y1], ...]
            Mat polygons = new Mat(tableCellBboxes.size(), 4, CvType.CV_32FC1);
            for (int i = 0; i < tableCellBboxes.size(); i++) {
                float x0 = tableCellBboxes.get(i)[0];
                float y0 = tableCellBboxes.get(i)[1];
                float x1 = tableCellBboxes.get(i)[2];
                float y1 = tableCellBboxes.get(i)[3];
                polygons.put(i, 0, x0, y0, x1, y1);
            }
            plotRecBoxWithLogicInfo(imgPath, saveLogicPath, logicPoints, polygons);
        }

        return drawedImg;
    }

    /**
     * 向 table HTML 中插入简单的边框样式
     */
    public String insertBorderStyle(String tableHtmlStr) {
        // 简易实现，假设原 HTML 中含有 <body> 标签
        // 根据需要可改为更健壮的实现
        String styleRes = "<meta charset=\"UTF-8\"><style>\n"
                + "table {\n"
                + "    border-collapse: collapse;\n"
                + "    width: 100%;\n"
                + "}\n"
                + "th, td {\n"
                + "    border: 1px solid black;\n"
                + "    padding: 8px;\n"
                + "    text-align: center;\n"
                + "}\n"
                + "th {\n"
                + "    background-color: #f2f2f2;\n"
                + "}\n"
                + "</style>";
        // 简易字符串处理
        int idx = tableHtmlStr.indexOf("<body>");
        if (idx == -1) {
            // 如果找不到 <body>，直接拼在前面
            return styleRes + tableHtmlStr;
        } else {
            String prefixTable = tableHtmlStr.substring(0, idx);
            String suffixTable = tableHtmlStr.substring(idx);
            return prefixTable + styleRes + suffixTable;
        }
    }

    /**
     * 绘制矩形并在右侧添加逻辑信息
     *
     * @param imgPath        原图路径
     * @param outputPath     输出图像路径
     * @param logicPoints    [row_start, row_end, col_start, col_end]
     * @param sortedPolygons [x0, y0, x1, y1]
     */
    public void plotRecBoxWithLogicInfo(String imgPath,
                                        String outputPath,
                                        List<int[]> logicPoints,
                                        Mat sortedPolygons) throws LoadImageError {
        // 读取原图
        Mat img = Imgcodecs.imread(imgPath);
        if (img.empty()) {
            throw new LoadImageError("无法加载原始图片：" + imgPath);
        }

        // 给右侧多留出 100 像素做文本区
        int top = 0, bottom = 0, left = 0, right = 100;
        int newWidth = img.width() + right;
        int newHeight = img.height();
        Mat borderedImg = Mat.zeros(newHeight, newWidth, img.type());
        // 将原图拷贝到 borderedImg 的 ROI 中
        Mat roi = borderedImg.submat(0, img.rows(), 0, img.cols());
        img.copyTo(roi);

        // 绘制 polygons 矩形
        for (int i = 0; i < sortedPolygons.rows(); i++) {
            double x0 = sortedPolygons.get(i, 0)[0];
            double y0 = sortedPolygons.get(i, 1)[0];
            double x1 = sortedPolygons.get(i, 2)[0];
            double y1 = sortedPolygons.get(i, 3)[0];

            // 四舍五入
            Point pt1 = new Point(Math.round(x0), Math.round(y0));
            Point pt2 = new Point(Math.round(x1), Math.round(y1));

            // 画矩形
            Imgproc.rectangle(borderedImg, pt1, pt2, new Scalar(0, 0, 255), 1);

            // 写文字
            // 这里使用 Hershey_PLAIN，字体大小与粗细可适当增大
            int[] logicInfo = logicPoints.get(i);
            String rowStr = "row: " + logicInfo[0] + "-" + logicInfo[1];
            String colStr = "col: " + logicInfo[2] + "-" + logicInfo[3];

            // 写在矩形的左上角
            Imgproc.putText(borderedImg, rowStr, new Point(pt1.x + 3, pt1.y + 8),
                    Imgproc.FONT_HERSHEY_PLAIN, 0.9, new Scalar(0, 0, 255), 1);
            Imgproc.putText(borderedImg, colStr, new Point(pt1.x + 3, pt1.y + 18),
                    Imgproc.FONT_HERSHEY_PLAIN, 0.9, new Scalar(0, 0, 255), 1);
        }

        // 保存绘制后的图像
        File outFile = new File(outputPath);
        outFile.getParentFile().mkdirs();
        Imgcodecs.imwrite(outputPath, borderedImg);
    }

    /**
     * 绘制矩形(4维坐标)
     *
     * @param img  原图像
     * @param boxes  N x 4， 每行 [x1, y1, x2, y2]
     */
    public Mat drawRectangle(Mat img, List<float[]> boxes) {
        Mat imgCopy = img.clone();
        for (float[] box : boxes) {
            double x1 = box[0];
            double y1 = box[1];
            double x2 = box[2];
            double y2 = box[3];

            Point pt1 = new Point(x1, y1);
            Point pt2 = new Point(x2, y2);
            Imgproc.rectangle(imgCopy, pt1, pt2, new Scalar(255, 0, 0), 2);
        }
        return imgCopy;
    }

    /**
     * 绘制多边形(8维坐标)
     *
     * @param img    原图像
     * @param points N x 8， 每行 [x1,y1, x2,y2, x3,y3, x4,y4]
     */
    public Mat drawPolylines(Mat img, List<float[]> points) {
        Mat imgCopy = img.clone();
        for (float[] point : points) {
            // 每行8个数，代表4个点的x,y坐标
            // coords = [x1, y1, x2, y2, x3, y3, x4, y4]
            Point[] polygon = new Point[4];
            for (int j = 0; j < 4; j++) {
                double px = point[2 * j];
                double py = point[2 * j + 1];
                polygon[j] = new Point(Math.round(px), Math.round(py));
            }
            MatOfPoint mop = new MatOfPoint(polygon);
            // 画多边形
            Imgproc.polylines(imgCopy, Collections.singletonList(mop), true, new Scalar(255, 0, 0), 2);
        }
        return imgCopy;
    }

    /**
     * 保存图片到指定路径
     */
    public void saveImg(String savePath, Mat img) {
        Imgcodecs.imwrite(savePath, img);
    }

    /**
     * 保存 HTML 字符串到指定文件
     */
    public void saveHtml(String savePath, String html) {
        File file = new File(savePath);
        file.getParentFile().mkdirs();
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file), "UTF-8"))) {
            bw.write(html);
        } catch (IOException e) {
            LOGGER.error("Failed to save HTML file: {}", savePath, e);
        }
    }
}