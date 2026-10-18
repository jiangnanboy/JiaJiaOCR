import ai.onnxruntime.OrtException;
import com.jiajia.common_object.*;
import com.jiajia.core.JiaJiaOCR;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
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
    // 以下为相对于项目根目录的路径，请按需修改
    private static final String GENERAL_OCR_IMG = "src/test/java/general.png";
    private static final String HANDWRITING_IMG = "src/test/java/handwriting.jpg";
    private static final String LAYOUT_IMG = "src/test/java/layout.png";
    private static final String TABLE_DET_REC_IMG = "src/test/java/table_det_rec.jpg";
    private static final String TABLE_REC_IMG = "src/test/java/table.jpg";
    private static final String PDF_PATH = "src/test/java/howtouseopencv.pdf";
    private static final String OUTPUT_DIR = "test_output";

    /** 计时：返回当前纳秒时间戳 */
    private static long now() {
        return System.nanoTime();
    }

    /** 计时：返回从 startNanos 到当前的耗时（毫秒） */
    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

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

        // 2. 手写OCR测试
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


    public static void testManyOCR(String imgPath) throws OrtException {
        // 测试内存泄露问题
        JiaJiaOCR ocr = JiaJiaOCR.builder();
        try {
            for (int i = 0; i < 100; i++) {
                List<Pair<Text, Box>> result = ocr.recognizeGeneralText(imgPath);
                System.out.println("第 " + (i+1) + " 次完成，结果数: " + result.size());

                // 每10次观察内存
                if (i % 10 == 0) {
                    Runtime runtime = Runtime.getRuntime();
                    long usedMB = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
                    System.out.println("内存使用: " + usedMB + " MB");
                }
            }
        } finally {
            ocr.close();
        }
    }
    // ==================== 1. 通用OCR ====================
    public static void testGeneralOCR(String imgPath) throws Exception {
        JiaJiaOCR ocr = JiaJiaOCR.builder();

        try {
            // 首次调用（冷启动，包含模型懒加载）
            long coldStart = now();
            List<Pair<Text, Box>> results = ocr.recognizeGeneralText(imgPath);
            System.out.println("  冷启动耗时（含模型加载）：" + elapsedMs(coldStart) + " ms");
            System.out.println("  识别结果数量：" + results.size());
            for (int i = 0; i < Math.min(3, results.size()); i++) {
                Pair<Text, Box> pair = results.get(i);
                System.out.println("  文本 " + (i + 1) + "：" + pair.getLeft().getText());
            }
            if (results.size() > 3) {
                System.out.println("  ... 共 " + results.size() + " 条结果");
            }

            // 二次调用（热启动，纯推理耗时）
            long warmStart = now();
            ocr.recognizeGeneralText(imgPath);
            System.out.println("  热启动耗时（纯推理）：" + elapsedMs(warmStart) + " ms");
        } finally {
            ocr.close();
        }
    }

    // ==================== 2. 手写OCR ====================
    public static void testHandwritingOCR(String imgPath) throws Exception {
        JiaJiaOCR ocr = JiaJiaOCR.builder();
        try {
            // 首次调用（冷启动，包含模型懒加载）
            long coldStart = now();
            List<Pair<Text, Box>> results = ocr.recognizeHandwrittenText(imgPath);
            System.out.println("  冷启动耗时（含模型加载）：" + elapsedMs(coldStart) + " ms");
            System.out.println("  识别结果数量：" + results.size());
            for (Pair<Text, Box> pair : results) {
                System.out.println("  手写文本：" + pair.getLeft().getText());
            }

            // 二次调用（热启动，纯推理耗时）
            long warmStart = now();
            ocr.recognizeHandwrittenText(imgPath);
            System.out.println("  热启动耗时（纯推理）：" + elapsedMs(warmStart) + " ms");
        } finally {
            ocr.close();
        }
    }

    // ==================== 3. 版面检测 ====================
    public static void testLayoutDetection(String imgPath) throws Exception {
        JiaJiaOCR ocr = JiaJiaOCR.builder();
        try {
            // 首次调用（冷启动，包含模型懒加载）
            long coldStart = now();
            List<Layout> layouts = ocr.detectLayout(imgPath);
            System.out.println("  冷启动耗时（含模型加载）：" + elapsedMs(coldStart) + " ms");
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

            // 二次调用（热启动，纯推理耗时）
            long warmStart = now();
            ocr.detectLayout(imgPath);
            System.out.println("  热启动耗时（纯推理）：" + elapsedMs(warmStart) + " ms");
        } finally {
            ocr.close();
        }
    }

    // ==================== 4. 文本行检测 ====================
    public static void testTextLineDetection(String imgPath) throws Exception {
        JiaJiaOCR ocr = JiaJiaOCR.builder();
        try {
            // 首次调用（冷启动，包含模型懒加载）
            long coldStart = now();
            Boxes boxes = ocr.detectTextLines(imgPath);
            System.out.println("  冷启动耗时（含模型加载）：" + elapsedMs(coldStart) + " ms");
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

            // 二次调用（热启动，纯推理耗时）
            long warmStart = now();
            ocr.detectTextLines(imgPath);
            System.out.println("  热启动耗时（纯推理）：" + elapsedMs(warmStart) + " ms");
        } finally {
            ocr.close();
        }
    }

    // ==================== 5. 表格检测 ====================
    public static void testTableDetection(String imgPath) throws Exception {
        JiaJiaOCR ocr = JiaJiaOCR.builder();
        try {
            // 首次调用（冷启动，包含模型懒加载）
            long coldStart = now();
            List<DetectionResult> results = ocr.detectTables(imgPath);
            System.out.println("  冷启动耗时（含模型加载）：" + elapsedMs(coldStart) + " ms");
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

            // 二次调用（热启动，纯推理耗时）
            long warmStart = now();
            ocr.detectTables(imgPath);
            System.out.println("  热启动耗时（纯推理）：" + elapsedMs(warmStart) + " ms");
        } finally {
            ocr.close();
        }
    }

    // ==================== 6. 表格识别 ====================
    public static void testTableRecognition(String imgPath) throws Exception {
        JiaJiaOCR ocr = JiaJiaOCR.builder();
        try {
            long totalStart = now();

            // 步骤1：先进行通用OCR获取文本结果
            System.out.println("  步骤1：执行通用OCR...");
            long step1Start = now();
            List<Pair<Text, Box>> ocrResults = ocr.recognizeGeneralText(imgPath);
            System.out.println("  步骤1 OCR耗时（冷启动）：" + elapsedMs(step1Start) + " ms");
            System.out.println("  OCR结果数量：" + ocrResults.size());

            // 步骤2：基于OCR结果进行表格结构识别
            System.out.println("  步骤2：基于OCR结果识别表格结构...");
            long step2Start = now();
            TableResult tableResult = ocr.recognizeTableFromOCR(imgPath, ocrResults);
            System.out.println("  步骤2 表格识别耗时（冷启动）：" + elapsedMs(step2Start) + " ms");

            System.out.println("  冷启动总耗时：" + elapsedMs(totalStart) + " ms");
            System.out.println("  HTML结果预览：" +
                    tableResult.getHtml().substring(0, Math.min(100, tableResult.getHtml().length())) + "...");
            System.out.println("  库内表格匹配耗时：" + String.format("%.3f", tableResult.getElapse()) + " 秒");

            // 步骤3：获取JSON格式的结构化数据
            String json = tableResult.getTableJson();
            System.out.println("  JSON结果预览：" +
                    json.substring(0, Math.min(200, json.length())) + "...");

            // 二次调用（热启动，纯推理耗时）
            long warmTotalStart = now();
            List<Pair<Text, Box>> warmOcrResults = ocr.recognizeGeneralText(imgPath);
            long warmStep2Start = now();
            ocr.recognizeTableFromOCR(imgPath, warmOcrResults);
            System.out.println("  热启动总耗时（纯推理）：" + elapsedMs(warmTotalStart) + " ms");
            System.out.println("  热启动中表格识别耗时：" + elapsedMs(warmStep2Start) + " ms");

        } finally {
            ocr.close();
        }
    }

    // ==================== 7. 表格检测及识别（一键完成）====================
    public static void testTableDetRec(String imgPath) throws Exception {
        JiaJiaOCR ocr = JiaJiaOCR.builder();

        try {
            // 首次调用（冷启动，包含模型懒加载）
            long coldStart = now();
            List<TableResult> tableResults = ocr.recognizeTables(imgPath);
            System.out.println("  冷启动耗时（含模型加载）：" + elapsedMs(coldStart) + " ms");
            System.out.println("  识别到的表格数量：" + tableResults.size());

            for (int i = 0; i < tableResults.size(); i++) {
                TableResult result = tableResults.get(i);
                System.out.println("\n  --- 表格 " + (i + 1) + " ---");
                System.out.println("  HTML结果预览：" +
                        result.getHtml().substring(0, Math.min(150, result.getHtml().length())) + "...");
                System.out.println("  耗时：" + String.format("%.3f", result.getElapse()) + " 秒");

                // 获取结构化数据
                ParsedTableData data = result.getParsedTableData();
                System.out.println("  行数：" + data.getRows() +
                        "，列数：" + data.getColumns());

                // 获取JSON
                String json = result.getTableJson();
                System.out.println("  JSON结果预览：" +
                        json.substring(0, Math.min(200, json.length())) + "...");
            }

            // 二次调用（热启动，纯推理耗时）
            long warmStart = now();
            ocr.recognizeTables(imgPath);
            System.out.println("  热启动耗时（纯推理）：" + elapsedMs(warmStart) + " ms");
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

        long start = now();
        try {
            List<Pair<Text, Box>> results = ocr.recognizeGeneralText(imgPath, token);
            System.out.println("  识别完成，结果数量：" + results.size() + "，耗时：" + elapsedMs(start) + " ms");
        } catch (Exception e) {
            System.out.println("  操作已被取消，耗时：" + elapsedMs(start) + " ms，异常信息：" + e.getMessage());
        } finally {
            ocr.close();
        }
    }

    // ==================== 9. 多线程批量处理 ====================
    public static void testMultiThreadBatch(String[] imgPaths) throws Exception {
        // 多线程共享同一个 JiaJiaOCR 实例，避免每个线程重复加载模型
        JiaJiaOCR sharedOcr = JiaJiaOCR.builder();
        try {
            // 预热：先跑一次完成模型懒加载
            long warmStart = now();
            sharedOcr.recognizeGeneralText(imgPaths[0]);
            System.out.println("  共享实例预热耗时（含模型加载）：" + elapsedMs(warmStart) + " ms");

            long totalStart = now();
            List<Thread> threads = new ArrayList<>();

            for (int i = 0; i < imgPaths.length; i++) {
                final int index = i;
                final String path = imgPaths[i];
                Thread t = new Thread(() -> {
                    try {
                        long start = now();
                        List<Pair<Text, Box>> results = sharedOcr.recognizeGeneralText(path);
                        System.out.println("  【线程 " + index + "】完成，识别到 " +
                                results.size() + " 条文本，耗时：" + elapsedMs(start) + " ms");
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
            System.out.println("  所有线程处理完毕，总耗时：" + elapsedMs(totalStart) + " ms");
        } finally {
            sharedOcr.close();
        }
    }

    // ==================== 10. PDF OCR ====================
    public static void testPdfOCR(String pdfPath) throws IOException, Exception {
        String pdfOutputDir = OUTPUT_DIR + "/pdf_images";
        new File(pdfOutputDir).mkdirs();

        System.out.println("  正在将PDF转换为图片...");
        long convertStart = now();
        List<String> pdfImgPaths = convertPdfToImages(pdfPath, pdfOutputDir);
        System.out.println("  PDF转图片耗时：" + elapsedMs(convertStart) + " ms");
        System.out.println("  PDF共 " + pdfImgPaths.size() + " 页");

        JiaJiaOCR ocr = JiaJiaOCR.builder();
        long ocrTotalStart = now();
        try {
            for (int i = 0; i < pdfImgPaths.size(); i++) {
                String imgPath = pdfImgPaths.get(i);
                System.out.println("  正在识别第 " + (i + 1) + "/" + pdfImgPaths.size() + " 页...");
                long pageStart = now();
                List<Pair<Text, Box>> results = ocr.recognizeGeneralText(imgPath);
                System.out.println("  第 " + (i + 1) + " 页识别到 " + results.size() +
                        " 条文本，耗时：" + elapsedMs(pageStart) + " ms");
            }
            System.out.println("  OCR总耗时：" + elapsedMs(ocrTotalStart) + " ms");
        } finally {
            ocr.close();
        }
    }
}
