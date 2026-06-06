# JiaJiaOCR

[![English README](https://img.shields.io/badge/English-README-blue?style=flat-square)](README.md)

## 🚀 Java OCR解决方案

在 OCR 技术落地过程中，Java 开发者常面临 **"Python 生态繁荣，Java 集成困难"** 的困境 —— 要么依赖jni调用 exe/dll 外部文件，要么跨平台部署踩坑不断。

**JiaJiaOCR 为Java端OCR落地带来革命性突破！** 🎉

本项目主打**纯Java实现、轻量高效、功能全面**，无需复杂环境配置，开箱即用，完美适配各类Java项目OCR场景。项目持续迭代更新，目前提供**轻量版**与**全功能版**两大版本，满足不同场景需求。

### 📌 版本整体介绍

|版本|大小|核心功能|适用场景|运行环境|
|---|---|---|---|---|
|**🔥 轻量版 \(1\.0\+\)**|21MB\+|通用OCR识别、PDF识别|快速集成、资源受限环境、基础文字识别场景|JDK8\+|
|**⚡ 全功能版 \(2\.0\+ / 2\.1\.0\)**|200MB\+|通用OCR\+手写OCR\+版面检测\+表格识别\+PDF识别，2\.1\.0新增内存优化、JSON结构化输出、任务取消功能|企业级应用、复杂文档处理、表格结构化解析、大批量识别场景|2\.0\.x：JDK8\+ / **2\.1\.0：JDK11\+**|

---

## 📦 版本更新日志

### 🔥 轻量版 \(1\.0\+\) \- 精简高效

- ✅ **v1\.0\.1** \- 优化英文OCR空白识别，返回空串而非null，避免空指针异常

- ✅ **v1\.0\.2** \- 新增PDF文档识别支持，适配多页PDF基础文字提取

### ⚡ 全功能版迭代更新

#### v2\.0\.x 基础版本

- ✅ **v2\.0\.1** \- 优化英文OCR空白识别 → 返回空串而非null

- ✅ **v2\.0\.2** \- 新增PDF识别支持

- 🚀 **性能升级** \- 引入模型懒加载机制，按需加载模型，降低初始资源占用

- 🎯 **功能扩容** \- 新增手写OCR、版面检测、表格检测与识别核心能力

- 📊 **结果升级** \- 新增版面元素坐标、表格HTML格式结构化输出

#### ✨ v2\.1\.0 最新稳定版（推荐）

**重要环境要求：JDK11 及以上版本**

核心优化与新增功能：

- 🔧 **内存优化**：修复多处内存泄漏问题，提升大批量、长时间识别任务的稳定性

- 📋 **表格结果升级**：表格识别除原有HTML格式外，新增**行列结构化JSON数据**返回，可直接解析获取精准行列信息，适配二次开发

- ⏹️**新增任务取消能力**：支持识别过程中手动取消长耗时任务，灵活适配业务中断场景

- 🧵 **多线程适配优化**：完善多线程批量识别能力，支持并发处理多文件、多图片识别任务

---

## 🎯 四大核心功能详解

### 1\. 📝 通用OCR：印刷体识别标杆

延续全版本高精度印刷体识别能力，支持中英文、数字、符号混合识别，精准返回文本内容、置信度及8点坐标信息，适配发票、标签、广告牌、普通文档等各类印刷体识别场景。

### 2\. ✍️ 手写OCR：突破手写识别难点

针对中文手写体专项优化模型，兼容工整手写、日常连笔手写等主流手写风格，解决手写笔记数字化、手写表单批量录入等行业痛点，标准手写样本测试识别准确率**达92%以上**。

### 3\. 📋 版面检测：文档结构智能解析

自动智能识别文档内标题、段落、图片、表格等各类版面元素，返回元素类型、置信度及矩形坐标信息，为PDF转Word、古籍数字化、文档结构化归档等场景提供底层能力支撑。

### 4\. 📊 表格检测与识别：纯Java结构化解析方案

业内稀缺纯Java无依赖表格识别方案，全程完成表格区域检测、单元格分割、内容识别全流程。

**2\.1\.0版本全新升级**：同时输出 **HTML格式**（可直接网页渲染）和 **JSON结构化数据**（含精准行列信息），支持一键导入Excel、数据库，彻底告别手动表格录入。

---

## 🛠️ 快速上手：5分钟集成教程

### 📋 环境适配说明

- 轻量版 1\.0\+：JDK8\+

- 全功能版 2\.0\.x：JDK8\+

- **全功能版 2\.1\.0：JDK11\+（必填）**

- 运行系统：Windows 10\+、Linux x86\_64

- 依赖管理：Maven（推荐）

### 📦 引入项目依赖

2.1.0版本通用Maven核心依赖，在项目pom\.xml中引入：

```xml
<dependencies>
    <dependency>
        <groupId>com.microsoft.onnxruntime</groupId>
        <artifactId>onnxruntime</artifactId>
        <version>1.19.0</version>
    </dependency>
    <dependency>
        <groupId>org.openpnp</groupId>
        <artifactId>opencv</artifactId>
        <version>4.9.0-0</version>
    </dependency>
    <dependency>
        <groupId>org.apache.pdfbox</groupId>
        <artifactId>pdfbox</artifactId>
        <version>3.0.2</version>
    </dependency>
</dependencies>
```

### 📥 核心Jar包引入

前往 GitHub 仓库 [JiaJiaOCR](https://github.com/jiangnanboy/JiaJiaOCR) releases 页面，下载对应版本Jar包：

- 轻量版1\.0\+：仅需基础通用OCR、PDF识别功能，体积小巧

- 全功能版2\.1\.0（推荐）：200MB\+，集成全套模型与最新优化功能，支持所有高级特性

下载后将Jar包放入项目依赖目录，完成引入即可使用。

### 💻 2\.1\.0 完整功能示例代码

示例覆盖**全部核心功能**，包含新版特色：表格JSON结构化输出、任务取消、多线程批量处理等，可直接复制运行测试：

具体见：JiaJiaOCRFullTest.java

```java
import com.jiajia.common_object.*;
import com.jiajia.core.JiaJiaOCR;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.jiajia.common_object.PdfConvert.convertPdfToImages;

/**
 * JiaJiaOCR 2.1.0 完整功能测试程序
 * 适配JDK11+，包含新版全部特色功能
 * 功能覆盖：通用OCR、手写OCR、版面检测、文本行检测、表格识别、PDF识别、任务取消、多线程批量处理
 */
public class JiaJiaOCRFullTest {

    // ==================== 测试资源路径配置（按需修改） ====================
    private static final String GENERAL_OCR_IMG = "general_ocr.jpg";
    private static final String HANDWRITING_IMG = "handwriting_ocr.jpg";
    private static final String LAYOUT_IMG = "layout.jpg";
    private static final String TABLE_DET_REC_IMG = "table_det_rec.jpg";
    private static final String TABLE_REC_IMG = "table_rec.png";
    private static final String PDF_PATH = "pdf.pdf";
    private static final String OUTPUT_DIR = "test_output";

    public static void main(String[] args) throws Exception {
        // 创建结果输出目录
        new File(OUTPUT_DIR).mkdirs();

        System.out.println("========================================");
        System.out.println("     JiaJiaOCR 2.1.0 完整功能测试");
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
            System.out.println("【测试3】版面检测 - 文档版面智能分析");
            testLayoutDetection(LAYOUT_IMG);
            System.out.println();
        }

        // 4. 文本行检测测试
        if (new File(GENERAL_OCR_IMG).exists()) {
            System.out.println("【测试4】文本行检测 - 精准定位文本行位置");
            testTextLineDetection(GENERAL_OCR_IMG);
            System.out.println();
        }

        // 5. 表格检测测试
        if (new File(TABLE_DET_REC_IMG).exists()) {
            System.out.println("【测试5】表格检测 - 智能识别表格区域");
            testTableDetection(TABLE_DET_REC_IMG);
            System.out.println();
        }

        // 6. 表格识别测试（新版支持JSON+HTML双格式输出）
        if (new File(TABLE_REC_IMG).exists()) {
            System.out.println("【测试6】表格识别 - 结构化数据提取");
            testTableRecognition(TABLE_REC_IMG);
            System.out.println();
        }

        // 7. 表格一键检测+识别
        if (new File(TABLE_DET_REC_IMG).exists()) {
            System.out.println("【测试7】表格一站式检测识别");
            testTableDetRec(TABLE_DET_REC_IMG);
            System.out.println();
        }

        // 8. 新版特色：任务取消功能测试
        if (new File(GENERAL_OCR_IMG).exists()) {
            System.out.println("【测试8】新版特色：识别任务取消演示");
            testCancellation(GENERAL_OCR_IMG);
            System.out.println();
        }

        // 9. 新版特色：多线程批量处理测试
        if (new File(GENERAL_OCR_IMG).exists()) {
            System.out.println("【测试9】新版特色：多线程批量识别演示");
            testMultiThreadBatch(new String[]{GENERAL_OCR_IMG, GENERAL_OCR_IMG, GENERAL_OCR_IMG});
            System.out.println();
        }

        // 10. PDF批量识别测试
        if (new File(PDF_PATH).exists()) {
            System.out.println("【测试10】PDF多页批量OCR识别");
            testPdfOCR(PDF_PATH);
            System.out.println();
        }

        System.out.println("========================================");
        System.out.println("         所有测试执行完毕！");
        System.out.println("========================================");
    }

    // 1. 通用印刷体OCR识别
    public static void testGeneralOCR(String imgPath) throws Exception {
        JiaJiaOCR ocr = JiaJiaOCR.builder();
        try {
            List<Pair<Text, Box>> results = ocr.recognizeGeneralText(imgPath);
            System.out.println("  识别结果数量：" + results.size());
            for (int i = 0; i < results.size(); i++) {
                Pair<Text, Box> pair = results.get(i);
                System.out.println("  文本 " + (i + 1) + "：" + pair.getLeft().getText());
            }
        } finally {
            ocr.close();
        }
    }

    // 2. 手写体OCR识别
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

    // 3. 文档版面检测
    public static void testLayoutDetection(String imgPath) throws Exception {
        JiaJiaOCR ocr = JiaJiaOCR.builder();
        try {
            List<Layout> layouts = ocr.detectLayout(imgPath);
            System.out.println("  检测到的版面元素数量：" + layouts.size());

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

    // 4. 文本行检测
    public static void testTextLineDetection(String imgPath) throws Exception {
        JiaJiaOCR ocr = JiaJiaOCR.builder();
        try {
            Boxes boxes = ocr.detectTextLines(imgPath);
            List<Box> boxList = boxes.getBoxes();
            System.out.println("  检测到的文本行数量：" + boxList.size());

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

    // 5. 表格区域检测
    public static void testTableDetection(String imgPath) throws Exception {
        JiaJiaOCR ocr = JiaJiaOCR.builder();
        try {
            List<DetectionResult> results = ocr.detectTables(imgPath);
            System.out.println("  检测到的表格数量：" + results.size());

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

    // 6. 表格结构化识别（2.1.0新增JSON行列信息输出）
    public static void testTableRecognition(String imgPath) throws Exception {
        JiaJiaOCR ocr = JiaJiaOCR.builder();
        try {
            System.out.println("  步骤1：执行通用OCR提取文本");
            List<Pair<Text, Box>> ocrResults = ocr.recognizeGeneralText(imgPath);
            System.out.println("  OCR识别文本总数：" + ocrResults.size());

            System.out.println("  步骤2：解析表格结构化数据");
            TableResult tableResult = ocr.recognizeTableFromOCR(imgPath, ocrResults);

            // 原有HTML格式结果
            System.out.println("  【HTML格式结果】" + tableResult.getHtml());
            System.out.println("  识别耗时：" + String.format("%.3f", tableResult.getElapse()) + " 秒");

            // 2.1.0新增：JSON行列结构化数据
            String tableJson = tableResult.getTableJson();
            System.out.println("  【新版JSON行列数据】" + tableJson);

        } finally {
            ocr.close();
        }
    }

    // 7. 表格一键检测+识别
    public static void testTableDetRec(String imgPath) throws Exception {
        JiaJiaOCR ocr = JiaJiaOCR.builder();
        try {
            List<TableResult> tableResults = ocr.recognizeTables(imgPath);
            System.out.println("  识别到的表格总数：" + tableResults.size());

            for (int i = 0; i < tableResults.size(); i++) {
                TableResult result = tableResults.get(i);
                System.out.println("\n  --- 表格 " + (i + 1) + " 解析结果 ---");
                System.out.println("  HTML格式：" + result.getHtml());
                System.out.println("  识别耗时：" + String.format("%.3f", result.getElapse()) + " 秒");

                // 结构化行列信息
                ParsedTableData data = result.getParsedTableData();
                System.out.println("  表格规格：" + data.getRows() + "行 × " + data.getColumns() + "列");
                System.out.println("  JSON结构化数据：" + result.getTableJson());
            }
        } finally {
            ocr.close();
        }
    }

    // 8. 2.1.0新增：任务取消功能测试
    public static void testCancellation(String imgPath) {
        JiaJiaOCR ocr = JiaJiaOCR.builder();
        CancellationToken token = new CancellationToken();

        // 子线程延时触发取消任务
        new Thread(() -> {
            try {
                Thread.sleep(100);
                System.out.println("  【取消线程】已触发任务取消请求");
                token.cancel();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        try {
            List<Pair<Text, Box>> results = ocr.recognizeGeneralText(imgPath, token);
            System.out.println("  识别完成，结果数量：" + results.size());
        } catch (Exception e) {
            System.out.println("  任务已成功取消，提示：" + e.getMessage());
        } finally {
            ocr.close();
        }
    }

    // 9. 2.1.0优化：多线程批量处理测试
    public static void testMultiThreadBatch(String[] imgPaths) {
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < imgPaths.length; i++) {
            final int index = i;
            final String path = imgPaths[i];
            Thread t = new Thread(() -> {
                try {
                    System.out.println("  【线程 " + index + "】开始识别任务");
                    JiaJiaOCR ocr = JiaJiaOCR.builder();
                    try {
                        List<Pair<Text, Box>> results = ocr.recognizeGeneralText(path);
                        System.out.println("  【线程 " + index + "】任务完成，识别文本数：" + results.size());
                    } finally {
                        ocr.close();
                    }
                } catch (Exception e) {
                    System.out.println("  【线程 " + index + "】任务失败：" + e.getMessage());
                }
            });
            threads.add(t);
            t.start();
        }

        // 等待所有线程执行完毕
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("  所有批量任务执行完毕");
    }

    // 10. PDF多页批量OCR识别
    public static void testPdfOCR(String pdfPath) throws IOException, Exception {
        String pdfOutputDir = OUTPUT_DIR + "/pdf_images";
        new File(pdfOutputDir).mkdirs();

        System.out.println("  正在转换PDF为图片...");
        List<String> pdfImgPaths = convertPdfToImages(pdfPath, pdfOutputDir);
        System.out.println("  PDF总页数：" + pdfImgPaths.size());

        JiaJiaOCR ocr = JiaJiaOCR.builder();
        try {
            for (int i = 0; i < pdfImgPaths.size(); i++) {
                String imgPath = pdfImgPaths.get(i);
                System.out.println("  正在识别第 " + (i + 1) + "/" + pdfImgPaths.size() + " 页");
                List<Pair<Text, Box>> results = ocr.recognizeGeneralText(imgPath);
                System.out.println("  第 " + (i + 1) + " 页识别文本数：" + results.size());
            }
        } finally {
            ocr.close();
        }
    }
}

```

---

## 📊 核心返回结果说明

JiaJiaOCR 2\.1\.0 提供标准化、结构化返回数据，适配各类业务解析需求，新版升级表格双格式输出：

|功能模块|核心返回内容|数据格式说明|
|---|---|---|
|通用 OCR / 手写 OCR|文本内容、坐标、置信度|`List<Pair<Text, Box>>`，Text含文本内容与置信度，Box含8个顶点坐标|
|版面检测|元素类型、坐标、置信度|`List<Layout>`，含label（标题/段落/表格等）、bbox矩形坐标、置信度|
|表格检测|表格区域、置信度|`List<DetectionResult>`，含表格矩形区域、识别置信度|
|表格识别（2\.1\.0升级）|表格结构、坐标、行列信息|`TableResult`，支持**HTML格式**（网页渲染）\+**JSON格式**（行列结构化数据）双输出，含坐标、耗时、行列数|

---

## 🖼️ 效果展示

各功能模块识别效果预览：

### 📝 通用OCR效果
<img src="generalocr.png" alt="通用OCR效果" width="800"/>

### ✍️ 手写OCR效果
<img src="handwrittingocr.png" alt="手写OCR效果" width="800"/>

### 📋 版面检测效果
<img src="layout.png" alt="版面检测效果" width="800"/>

### 📊 表格检测效果
<img src="table_det.png" alt="表格检测效果" width="800"/>

### 📈 表格识别效果
<img src="table_rec.png" alt="表格识别效果" width="800"/>

---

## 📞 联系方式

如有功能建议、问题反馈或技术交流，欢迎随时联系：

- 🐙 **GitHub**：[jiangnanboy](https://github.com/jiangnanboy)

- 💬 **QQ**：2229029156

- 📧 **Email**：2229029156@qq\.com

## ⭐ 支持项目

如果本项目对你有帮助，欢迎点亮 **Star**，您的支持是项目持续迭代优化的最大动力 ❤️
---

[![GitHub stars](https://img.shields.io/github/stars/jiangnanboy/JiaJiaOCR?style=social)](https://github.com/jiangnanboy/JiaJiaOCR/stargazers)
