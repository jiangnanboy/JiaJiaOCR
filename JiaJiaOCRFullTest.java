import com.jiajia.common_object.*;
import com.jiajia.core.JiaJiaOCR;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.jiajia.common_object.PdfConvert.convertPdfToImages;

/**
 * JiaJiaOCR 完整功能测试程序
 *
 * 本测试程序演示 JiaJiaOCR 库的所有功能：
 * 1. 通用OCR（印刷体文字识别）
 * 2. 手写OCR（手写体文字识别）
 * 3. 版面检测（文档版面分析）
 * 4. 文本行检测（仅检测文本行位置）
 * 5. 表格检测（仅检测表格区域）
 * 6. 表格识别（基于已有OCR结果识别表格结构）
 * 7. 表格检测及识别（一键完成表格检测+OCR+识别）
 * 8. PDF OCR（多页PDF批量识别）
 * 9. 取消操作演示（长任务中途取消）
 * 10. 多线程批量处理演示
 *
 * 使用方法：
 * 1. 确保测试图片存在于指定路径，或修改 imgPath 变量
 * 2. 运行 main 方法即可执行全部测试
 * 3. 可通过注释掉不需要的测试方法来选择性执行
 */
public class JiaJiaOCRFullTest {

    // ==================== 测试图片路径配置 ====================
    // 请根据实际环境修改以下路径
    private static final String GENERAL_OCR_IMG = "general_ocr.jpg";
    private static final String HANDWRITING_IMG = "handwriting_ocr.jpg";
    private static final String LAYOUT_IMG = "layout.jpg";
    private static final String TABLE_DET_REC_IMG = "table_det_rec.jpg";
    private static final String TABLE_REC_IMG = "table_rec.png";
    private static final String PDF_PATH = "pdf.pdf";
    private static final String OUTPUT_DIR = "test_output";

    public static void main(String[] args) throws Exception {
        // 创建输出目录
        new File(OUTPUT_DIR).mkdirs();

        System.out.println("========================================");
        System.out.println("     JiaJiaOCR 完整功能测试程序");
        System.out.println("========================================\n");

        // 1. 通用OCR测试
        if (new File(GENERAL_OCR_IMG).exists()) {
            System.out.println("【测试1】通用OCR - 印刷体文字识别");
            testGeneralOCR(GENERAL_OCR_IMG);
            System.out.println();
        }

//         2. 手写OCR测试
        if (new File(HANDWRITING_IMG).exists()) {
            System.out.println("【测试2】手写OCR - 手写体文字识别");
            testHandwritingOCR(HANDWRITING_IMG);
            System.out.println();
        }

        // 3. 版面检测测试
        if (new File(LAYOUT_IMG).exists()) {
            System.out.println("【测试3】版面检测 - 文档版面分析");
            testLayoutDetection(LAYOUT_IMG);
            System.out.println();
        }

        // 4. 文本行检测测试
        if (new File(GENERAL_OCR_IMG).exists()) {
            System.out.println("【测试4】文本行检测 - 仅检测文本行位置");
            testTextLineDetection(GENERAL_OCR_IMG);
            System.out.println();
        }

        // 5. 表格检测测试
        if (new File(TABLE_DET_REC_IMG).exists()) {
            System.out.println("【测试5】表格检测 - 仅检测表格区域");
            testTableDetection(TABLE_DET_REC_IMG);
            System.out.println();
        }

        // 6. 表格识别测试（需先进行OCR）
        if (new File(TABLE_REC_IMG).exists()) {
            System.out.println("【测试6】表格识别 - 基于OCR结果识别表格结构");
            testTableRecognition(TABLE_REC_IMG);
            System.out.println();
        }

        // 7. 表格检测及识别（一键完成）
        if (new File(TABLE_DET_REC_IMG).exists()) {
            System.out.println("【测试7】表格检测及识别 - 一键完成");
            testTableDetRec(TABLE_DET_REC_IMG);
            System.out.println();
        }

        // 8. 取消操作演示
        if (new File(GENERAL_OCR_IMG).exists()) {
            System.out.println("【测试8】取消操作演示");
            testCancellation(GENERAL_OCR_IMG);
            System.out.println();
        }

        // 9. 多线程批量处理演示
        if (new File(GENERAL_OCR_IMG).exists()) {
            System.out.println("【测试9】多线程批量处理演示");
            testMultiThreadBatch(new String[]{GENERAL_OCR_IMG, GENERAL_OCR_IMG, GENERAL_OCR_IMG});
            System.out.println();
        }

        // 10. PDF OCR测试
        if (new File(PDF_PATH).exists()) {
            System.out.println("【测试10】PDF OCR - 多页PDF批量识别");
            testPdfOCR(PDF_PATH);
            System.out.println();
        }

        System.out.println("========================================");
        System.out.println("         所有测试执行完毕！");
        System.out.println("========================================");
    }

    // ==================== 1. 通用OCR ====================
    public static void testGeneralOCR(String imgPath) throws Exception {
        JiaJiaOCR ocr = JiaJiaOCR.builder();
        try {
            // 方式1：直接传入图片路径（最简便）
            List<Pair<Text, Box>> results = ocr.recognizeGeneralText(imgPath);
            System.out.println("  识别结果数量：" + results.size());
            for (int i = 0; i < results.size(); i++) {
                Pair<Text, Box> pair = results.get(i);
                System.out.println("  文本 " + (i + 1) + "：" + pair.getLeft().getText());
            }
            System.out.println("  ... 共 " + results.size() + " 条结果");

        } finally {
            ocr.close();
        }
    }

    // ==================== 2. 手写OCR ====================
    public static void testHandwritingOCR(String imgPath) throws Exception {
        JiaJiaOCR ocr = JiaJiaOCR.builder();
        try {
            List<Pair<Text, Box>> results = ocr.recognizeHandwrittenText(imgPath);
            System.out.println("  识别结果数量：" + results.size());
            for (Pair<Text, Box> pair : results) {
                System.out.println("  手写文本：" + pair.getLeft().getText());
            }
        } finally {
            ocr.close();
        }
    }

    // ==================== 3. 版面检测 ====================
    public static void testLayoutDetection(String imgPath) throws Exception {
        JiaJiaOCR ocr = JiaJiaOCR.builder();
        try {
            List<Layout> layouts = ocr.detectLayout(imgPath);
            System.out.println("  检测到的版面元素数量：" + layouts.size());

            // 读取原图并绘制检测结果
            Mat img = Imgcodecs.imread(imgPath);
            for (Layout layout : layouts) {
                int x0 = (int) layout.getBbox()[0];
                int y0 = (int) layout.getBbox()[1];
                int x1 = (int) layout.getBbox()[2];
                int y1 = (int) layout.getBbox()[3];

                Imgproc.rectangle(img, new Point(x0, y0), new Point(x1, y1),
                        new Scalar(0, 0, 255), 2);
                Imgproc.putText(img, layout.getLabel(),
                        new Point(Math.max(0, x0 + 5), Math.max(0, y0 - 10)),
                        Imgproc.FONT_HERSHEY_SIMPLEX, 0.7,
                        new Scalar(0, 255, 0), 2, Imgproc.LINE_AA);

                System.out.println("  元素：" + layout.getLabel() +
                        "，置信度：" + String.format("%.2f", layout.getConfidence()));
            }

            String outputPath = OUTPUT_DIR + "/layout_result.jpg";
            Imgcodecs.imwrite(outputPath, img);
            System.out.println("  结果已保存至：" + outputPath);
            img.release();
        } finally {
            ocr.close();
        }
    }

    // ==================== 4. 文本行检测 ====================
    public static void testTextLineDetection(String imgPath) throws Exception {
        JiaJiaOCR ocr = JiaJiaOCR.builder();
        try {
            Boxes boxes = ocr.detectTextLines(imgPath);
            List<Box> boxList = boxes.getBoxes();
            System.out.println("  检测到的文本行数量：" + boxList.size());

            // 绘制检测结果
            Mat img = Imgcodecs.imread(imgPath);
            for (Box box : boxList) {
                Point[] points = new Point[4];
                float[] pos = box.getLinePosition();
                points[0] = new Point(pos[0], pos[1]);
                points[1] = new Point(pos[2], pos[3]);
                points[2] = new Point(pos[4], pos[5]);
                points[3] = new Point(pos[6], pos[7]);

                MatOfPoint polygon = new MatOfPoint(points);
                List<MatOfPoint> polygons = new ArrayList<>();
                polygons.add(polygon);
                Imgproc.polylines(img, polygons, true, new Scalar(0, 0, 255), 2);
            }

            String outputPath = OUTPUT_DIR + "/textline_result.jpg";
            Imgcodecs.imwrite(outputPath, img);
            System.out.println("  结果已保存至：" + outputPath);
            img.release();
        } finally {
            ocr.close();
        }
    }

    // ==================== 5. 表格检测 ====================
    public static void testTableDetection(String imgPath) throws Exception {
        JiaJiaOCR ocr = JiaJiaOCR.builder();
        try {
            List<DetectionResult> results = ocr.detectTables(imgPath);
            System.out.println("  检测到的表格数量：" + results.size());

            // 绘制检测结果
            Mat img = Imgcodecs.imread(imgPath);
            for (DetectionResult result : results) {
                String label = result.getClassLabel() + ": " +
                        String.format("%.2f", result.getConfidence());
                Imgproc.putText(img, label,
                        new Point(result.getRect().x, result.getRect().y - 10),
                        Imgproc.FONT_HERSHEY_SIMPLEX, 1.0, new Scalar(0, 0, 255), 2);
                Imgproc.rectangle(img,
                        new Point(result.getRect().x, result.getRect().y),
                        new Point(result.getRect().x + result.getRect().width,
                                result.getRect().y + result.getRect().height),
                        new Scalar(0, 0, 255), 2);
            }

            String outputPath = OUTPUT_DIR + "/table_detection_result.jpg";
            Imgcodecs.imwrite(outputPath, img);
            System.out.println("  结果已保存至：" + outputPath);
            img.release();
        } finally {
            ocr.close();
        }
    }

    // ==================== 6. 表格识别 ====================
    public static void testTableRecognition(String imgPath) throws Exception {
        JiaJiaOCR ocr = JiaJiaOCR.builder();
        try {
            // 步骤1：先进行通用OCR获取文本结果
            System.out.println("  步骤1：执行通用OCR...");
            List<Pair<Text, Box>> ocrResults = ocr.recognizeGeneralText(imgPath);
            System.out.println("  OCR结果数量：" + ocrResults.size());

            // 步骤2：基于OCR结果进行表格结构识别
            System.out.println("  步骤2：基于OCR结果识别表格结构...");
            TableResult tableResult = ocr.recognizeTableFromOCR(imgPath, ocrResults);

            System.out.println("  HTML结果预览：" +
                    tableResult.getHtml());
            System.out.println("  耗时：" + String.format("%.3f", tableResult.getElapse()) + " 秒");

            // 步骤3：获取JSON格式的结构化数据，包括行列信息
            String json = tableResult.getTableJson();
            System.out.println("  JSON结果预览：" +
                    json);

        } finally {
            ocr.close();
        }
    }

    // ==================== 7. 表格检测及识别（一键完成）====================
    public static void testTableDetRec(String imgPath) throws Exception {
        JiaJiaOCR ocr = JiaJiaOCR.builder();
        try {
            List<TableResult> tableResults = ocr.recognizeTables(imgPath);
            System.out.println("  识别到的表格数量：" + tableResults.size());

            for (int i = 0; i < tableResults.size(); i++) {
                TableResult result = tableResults.get(i);
                System.out.println("\n  --- 表格 " + (i + 1) + " ---");
                System.out.println("  HTML结果预览：" +
                        result.getHtml());
                System.out.println("  耗时：" + String.format("%.3f", result.getElapse()) + " 秒");

                // 获取结构化数据
                ParsedTableData data = result.getParsedTableData();
                System.out.println("  行数：" + data.getRows() +
                        "，列数：" + data.getColumns());

                // 获取JSON
                String json = result.getTableJson();
                System.out.println("  JSON结果预览：" +
                        json);
            }
        } finally {
            ocr.close();
        }
    }

    // ==================== 8. 取消操作演示 ====================
    public static void testCancellation(String imgPath) {
        JiaJiaOCR ocr = JiaJiaOCR.builder();
        CancellationToken token = new CancellationToken();

        // 启动一个线程，在100毫秒后取消操作
        new Thread(() -> {
            try {
                Thread.sleep(100);
                System.out.println("  【取消线程】发出取消请求...");
                token.cancel();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        try {
            List<Pair<Text, Box>> results = ocr.recognizeGeneralText(imgPath, token);
            System.out.println("  识别完成，结果数量：" + results.size());
        } catch (Exception e) {
            System.out.println("  操作已被取消，异常信息：" + e.getMessage());
        } finally {
            ocr.close();
        }
    }

    // ==================== 9. 多线程批量处理 ====================
    public static void testMultiThreadBatch(String[] imgPaths) {
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < imgPaths.length; i++) {
            final int index = i;
            final String path = imgPaths[i];
            Thread t = new Thread(() -> {
                try {
                    System.out.println("  【线程 " + index + "】开始处理...");
                    JiaJiaOCR ocr = JiaJiaOCR.builder();
                    try {
                        List<Pair<Text, Box>> results = ocr.recognizeGeneralText(path);
                        System.out.println("  【线程 " + index + "】完成，识别到 " +
                                results.size() + " 条文本");
                    } finally {
                        ocr.close();
                    }
                } catch (Exception e) {
                    System.out.println("  【线程 " + index + "】处理失败：" + e.getMessage());
                }
            });
            threads.add(t);
            t.start();
        }

        // 等待所有线程完成
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("  所有线程处理完毕");
    }

    // ==================== 10. PDF OCR ====================
    public static void testPdfOCR(String pdfPath) throws IOException, Exception {
        String pdfOutputDir = OUTPUT_DIR + "/pdf_images";
        new File(pdfOutputDir).mkdirs();

        System.out.println("  正在将PDF转换为图片...");
        List<String> pdfImgPaths = convertPdfToImages(pdfPath, pdfOutputDir);
        System.out.println("  PDF共 " + pdfImgPaths.size() + " 页");

        JiaJiaOCR ocr = JiaJiaOCR.builder();
        try {
            for (int i = 0; i < pdfImgPaths.size(); i++) {
                String imgPath = pdfImgPaths.get(i);
                System.out.println("  正在识别第 " + (i + 1) + "/" + pdfImgPaths.size() + " 页...");
                List<Pair<Text, Box>> results = ocr.recognizeGeneralText(imgPath);
                System.out.println("  第 " + (i + 1) + " 页识别到 " + results.size() + " 条文本");
            }
        } finally {
            ocr.close();
        }
    }
}
