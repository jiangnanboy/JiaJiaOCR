# JiaJiaOCR

[![中文 README](https://img.shields.io/badge/中文-README-red?style=flat-square)](README_CN.md)

## 🚀 A Java OCR Solution

When implementing OCR technology, Java developers often face a common dilemma: **the Python ecosystem is mature and feature-rich, while OCR integration for Java projects remains challenging**. Traditional approaches either rely on JNI to call external EXE/DLL files or encounter various compatibility issues during cross-platform deployment.

**JiaJiaOCR brings a groundbreaking solution for OCR implementation on Java platforms!** 🎉

This project is built purely with Java, featuring lightweight design, high efficiency and comprehensive functions. It requires no complicated environment configuration and can be used out of the box, perfectly fitting OCR scenarios in all types of Java applications. Continuously updated and iterated, it currently offers two editions: **Lite Version** and **Full Version** to meet diverse business demands.

### 📌 Version Overview

|Version|Size|Core Features|Applicable Scenarios|Runtime Environment|
|-|-|-|-|-|
|**🔥 Lite Version (1.0+)**|21MB+|General OCR recognition, PDF recognition|Rapid integration, resource-limited environments, basic text recognition|JDK 8+|
|**⚡ Full Version (2.0+ / 2.1.0 / 2.2.0)**|200MB+|General OCR + Handwritten OCR + Layout Detection + Table Recognition + PDF Recognition<br>v2.1.0 adds memory optimization, structured JSON output and task cancellation<br>**v2.2.0 fully supports x86 \& ARM architectures, compatible with domestic IT infrastructure**|Enterprise-level applications, complex document processing, structured table parsing, batch recognition, **deployment on domestic IT servers and terminals**|2.0.x: JDK 8+<br>**2.1.0 \& 2.2.0: JDK 11+**|

\---

## 📦 Release Notes

### 🔥 Lite Version (1.0+) - Compact \& Efficient

* ✅ **v1.0.1** - Optimized recognition for blank content in English OCR. Returns empty string instead of null to avoid NullPointerExceptions.
* ✅ **v1.0.2** - Added support for PDF document recognition and basic text extraction from multi-page PDFs.

### ⚡ Full Version Updates

#### v2.0.x Base Releases

* ✅ **v2.0.1** - Optimized recognition for blank content in English OCR, returning empty string rather than null.
* ✅ **v2.0.2** - Integrated PDF recognition capability.
* 🚀 **Performance Upgrade** - Implemented lazy model loading to reduce initial resource consumption by loading models on demand.
* 🎯 **Feature Expansion** - Added core capabilities including handwritten OCR, layout detection and table detection \& recognition.
* 📊 **Result Enhancement** - Supported coordinate output for layout elements and structured table results in HTML format.

#### ✨ v2.1.0 Stable Release

**Important Requirement: JDK 11 or higher**

Core optimizations and new features:

* 🔧 **Memory Optimization**: Fixed multiple memory leaks and improved stability for long-running and batch recognition tasks.
* 📋 **Enhanced Table Output**: Besides HTML format, tables can now be exported as **structured JSON data with row and column information** for easy secondary development.
* ⏹️ **Task Cancellation**: Allows manual termination of long-running recognition tasks to handle business interruptions flexibly.
* 🧵 **Multi-threading Optimization**: Optimized multi-threaded batch processing for concurrent recognition of multiple files and images.

#### 🆕 v2.2.0 Latest Recommended Release (Full Platform \& Domestic IT Support)

**Important Requirement: JDK 11 or higher**

* 🖥️ **Multi-architecture Compatibility**: Fully supports **x86\_64 and ARM** hardware architectures.
* 🇨🇳 **Domestic IT Adaptation**: Deeply compatible with domestic operating systems, ARM-based servers and terminals for localized IT projects.
* 🔧 Inherits all capabilities from v2.1.0: memory optimization, structured JSON for tables, task cancellation, multi-threaded processing and complete OCR features.

\---

## 🎯 Four Core Features

### 1\. 📝 General OCR: Premium Printed Text Recognition

Delivers high-precision printed text recognition across all versions. It supports mixed recognition of Chinese, English, numbers and symbols, and returns text content, confidence scores and 8-point coordinate data. Ideal for invoices, labels, billboards, regular documents and other printed text scenarios.

### 2\. ✍️ Handwritten OCR: Breakthrough in Handwriting Recognition

The model is specially optimized for Chinese handwriting, supporting neat handwriting and casual cursive styles. It solves pain points such as digitalizing handwritten notes and bulk entry of handwritten forms, achieving a recognition accuracy of **over 92%** on standard handwriting datasets.

### 3\. 📋 Layout Detection: Intelligent Document Structure Parsing

Automatically identifies layout elements including titles, paragraphs, images and tables within documents, and outputs element types, confidence scores and bounding box coordinates. It serves as the underlying capability for PDF-to-Word conversion, ancient book digitization and structured document archiving.

### 4\. 📊 Table Detection \& Recognition: Pure Java Structured Parsing

A rare dependency-free table recognition solution developed purely in Java. It covers the full workflow: table area detection, cell segmentation and content recognition.

**New upgrades in v2.1.0 and above**: Dual output of **HTML format** (directly renderable on web pages) and **structured JSON data** (with precise row and column information). Tables can be imported into Excel or databases with one click, eliminating manual data entry entirely.

\---

## 🛠️ Quick Start: 5-Minute Integration Guide

### 📋 Environment Requirements

* Lite Version 1.0+: JDK 8+
* Full Version 2.0.x: JDK 8+
* **Full Version 2.1.0 / 2.2.0: JDK 11+ (Mandatory)**
* Operating System: Windows 10+, Linux x86\_64, **Linux ARM, Domestic Operating Systems**
* Dependency Management: Maven (Recommended)

### 📦 Add Project Dependencies

Add the following core Maven dependencies to `pom.xml` for v2.1.0 and v2.2.0:

```XML
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

### 📥 Import Core JAR Files

Download the corresponding JAR package from the **Releases** page of the JiaJiaOCR GitHub repository:

* Lite Version 1.0+: Compact package for basic general OCR and PDF recognition.
* Full Version 2.2.0 (Recommended): 200MB+ package integrating all models, multi-architecture support, domestic IT adaptation and advanced features.

Place the downloaded JAR file into your project dependency directory to complete the import.

### 💻 Full Demo Code for v2.1.0 / 2.2.0

The sample code covers all core features including newly added functions: structured table JSON output, task cancellation and multi-threaded batch processing. You can directly run and test it.
File: `JiaJiaOCRFullTest.java`

```Java
import com.jiajia.common\_object.\*;
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

import static com.jiajia.common\_object.PdfConvert.convertPdfToImages;

/\*\*
 \* JiaJiaOCR 2.1.0 / 2.2.0 Full Function Test
 \* Compatible with JDK 11+, includes all new features
 \* Features: General OCR, Handwritten OCR, Layout Detection, Text Line Detection,
 \* Table Recognition, PDF Recognition, Task Cancellation, Multi-threaded Batch Processing
 \*/
public class JiaJiaOCRFullTest {

    // ==================== Test Resource Path Configuration ====================
    private static final String GENERAL\_OCR\_IMG = "general\_ocr.jpg";
    private static final String HANDWRITING\_IMG = "handwriting\_ocr.jpg";
    private static final String LAYOUT\_IMG = "layout.jpg";
    private static final String TABLE\_DET\_REC\_IMG = "table\_det\_rec.jpg";
    private static final String TABLE\_REC\_IMG = "table\_rec.png";
    private static final String PDF\_PATH = "pdf.pdf";
    private static final String OUTPUT\_DIR = "test\_output";

    public static void main(String\[] args) throws Exception {
        // Create output directory
        new File(OUTPUT\_DIR).mkdirs();

        System.out.println("========================================");
        System.out.println("     JiaJiaOCR Full Function Test");
        System.out.println("========================================\\n");

        // 1. General OCR Test
        if (new File(GENERAL\_OCR\_IMG).exists()) {
            System.out.println("【Test 1】General OCR - Printed Text Recognition");
            testGeneralOCR(GENERAL\_OCR\_IMG);
            System.out.println();
        }

        // 2. Handwritten OCR Test
        if (new File(HANDWRITING\_IMG).exists()) {
            System.out.println("【Test 2】Handwritten OCR - Handwriting Recognition");
            testHandwritingOCR(HANDWRITING\_IMG);
            System.out.println();
        }

        // 3. Layout Detection Test
        if (new File(LAYOUT\_IMG).exists()) {
            System.out.println("【Test 3】Layout Detection - Document Structure Analysis");
            testLayoutDetection(LAYOUT\_IMG);
            System.out.println();
        }

        // 4. Text Line Detection Test
        if (new File(GENERAL\_OCR\_IMG).exists()) {
            System.out.println("【Test 4】Text Line Detection - Precise Line Positioning");
            testTextLineDetection(GENERAL\_OCR\_IMG);
            System.out.println();
        }

        // 5. Table Detection Test
        if (new File(TABLE\_DET\_REC\_IMG).exists()) {
            System.out.println("【Test 5】Table Detection - Table Area Identification");
            testTableDetection(TABLE\_DET\_REC\_IMG);
            System.out.println();
        }

        // 6. Table Recognition Test (Dual HTML \& JSON Output)
        if (new File(TABLE\_REC\_IMG).exists()) {
            System.out.println("【Test 6】Table Recognition - Structured Data Extraction");
            testTableRecognition(TABLE\_REC\_IMG);
            System.out.println();
        }

        // 7. Integrated Table Detection \& Recognition
        if (new File(TABLE\_DET\_REC\_IMG).exists()) {
            System.out.println("【Test 7】All-in-one Table Detection \& Recognition");
            testTableDetRec(TABLE\_DET\_REC\_IMG);
            System.out.println();
        }

        // 8. New Feature: Task Cancellation Test
        if (new File(GENERAL\_OCR\_IMG).exists()) {
            System.out.println("【Test 8】New Feature: Task Cancellation Demo");
            testCancellation(GENERAL\_OCR\_IMG);
            System.out.println();
        }

        // 9. New Feature: Multi-threaded Batch Processing Test
        if (new File(GENERAL\_OCR\_IMG).exists()) {
            System.out.println("【Test 9】New Feature: Multi-threaded Batch Recognition Demo");
            testMultiThreadBatch(new String\[]{GENERAL\_OCR\_IMG, GENERAL\_OCR\_IMG, GENERAL\_OCR\_IMG});
            System.out.println();
        }

        // 10. Batch PDF OCR Test
        if (new File(PDF\_PATH).exists()) {
            System.out.println("【Test 10】Multi-page PDF Batch OCR");
            testPdfOCR(PDF\_PATH);
            System.out.println();
        }

        System.out.println("========================================");
        System.out.println("         All Tests Completed!");
        System.out.println("========================================");
    }

    // 1. General Printed Text OCR
    public static void testGeneralOCR(String imgPath) throws Exception {
        JiaJiaOCR ocr = JiaJiaOCR.builder();
        try {
            List<Pair<Text, Box>> results = ocr.recognizeGeneralText(imgPath);
            System.out.println("  Total recognized items: " + results.size());
            for (int i = 0; i < results.size(); i++) {
                Pair<Text, Box> pair = results.get(i);
                System.out.println("  Text " + (i + 1) + "：" + pair.getLeft().getText());
            }
        } finally {
            ocr.close();
        }
    }

    // 2. Handwritten Text OCR
    public static void testHandwritingOCR(String imgPath) throws Exception {
        JiaJiaOCR ocr = JiaJiaOCR.builder();
        try {
            List<Pair<Text, Box>> results = ocr.recognizeHandwrittenText(imgPath);
            System.out.println("  Total recognized items: " + results.size());
            for (Pair<Text, Box> pair : results) {
                System.out.println("  Handwritten text：" + pair.getLeft().getText());
            }
        } finally {
            ocr.close();
        }
    }

    // 3. Document Layout Detection
    public static void testLayoutDetection(String imgPath) throws Exception {
        JiaJiaOCR ocr = JiaJiaOCR.builder();
        try {
            List<Layout> layouts = ocr.detectLayout(imgPath);
            System.out.println("  Detected layout elements: " + layouts.size());

            Mat img = Imgcodecs.imread(imgPath);
            for (Layout layout : layouts) {
                int x0 = (int) layout.getBbox()\[0];
                int y0 = (int) layout.getBbox()\[1];
                int x1 = (int) layout.getBbox()\[2];
                int y1 = (int) layout.getBbox()\[3];

                Imgproc.rectangle(img, new Point(x0, y0), new Point(x1, y1),
                        new Scalar(0, 0, 255), 2);
                Imgproc.putText(img, layout.getLabel(),
                        new Point(Math.max(0, x0 + 5), Math.max(0, y0 - 10)),
                        Imgproc.FONT\_HERSHEY\_SIMPLEX, 0.7,
                        new Scalar(0, 255, 0), 2, Imgproc.LINE\_AA);

                System.out.println("  Element：" + layout.getLabel() +
                        "，Confidence：" + String.format("%.2f", layout.getConfidence()));
            }

            String outputPath = OUTPUT\_DIR + "/layout\_result.jpg";
            Imgcodecs.imwrite(outputPath, img);
            System.out.println("  Result saved to：" + outputPath);
            img.release();
        } finally {
            ocr.close();
        }
    }

    // 4. Text Line Detection
    public static void testTextLineDetection(String imgPath) throws Exception {
        JiaJiaOCR ocr = JiaJiaOCR.builder();
        try {
            Boxes boxes = ocr.detectTextLines(imgPath);
            List<Box> boxList = boxes.getBoxes();
            System.out.println("  Detected text lines: " + boxList.size());

            Mat img = Imgcodecs.imread(imgPath);
            for (Box box : boxList) {
                Point\[] points = new Point\[4];
                float\[] pos = box.getLinePosition();
                points\[0] = new Point(pos\[0], pos\[1]);
                points\[1] = new Point(pos\[2], pos\[3]);
                points\[2] = new Point(pos\[4], pos\[5]);
                points\[3] = new Point(pos\[6], pos\[7]);

                MatOfPoint polygon = new MatOfPoint(points);
                List<MatOfPoint> polygons = new ArrayList<>();
                polygons.add(polygon);
                Imgproc.polylines(img, polygons, true, new Scalar(0, 0, 255), 2);
            }

            String outputPath = OUTPUT\_DIR + "/textline\_result.jpg";
            Imgcodecs.imwrite(outputPath, img);
            System.out.println("  Result saved to：" + outputPath);
            img.release();
        } finally {
            ocr.close();
        }
    }

    // 5. Table Area Detection
    public static void testTableDetection(String imgPath) throws Exception {
        JiaJiaOCR ocr = JiaJiaOCR.builder();
        try {
            List<DetectionResult> results = ocr.detectTables(imgPath);
            System.out.println("  Detected tables: " + results.size());

            Mat img = Imgcodecs.imread(imgPath);
            for (DetectionResult result : results) {
                String label = result.getClassLabel() + ": " +
                        String.format("%.2f", result.getConfidence());
                Imgproc.putText(img, label,
                        new Point(result.getRect().x, result.getRect().y - 10),
                        Imgproc.FONT\_HERSHEY\_SIMPLEX, 1.0, new Scalar(0, 0, 255), 2);
                Imgproc.rectangle(img,
                        new Point(result.getRect().x, result.getRect().y),
                        new Point(result.getRect().x + result.getRect().width,
                                result.getRect().y + result.getRect().height),
                        new Scalar(0, 0, 255), 2);
            }

            String outputPath = OUTPUT\_DIR + "/table\_detection\_result.jpg";
            Imgcodecs.imwrite(outputPath, img);
            System.out.println("  Result saved to：" + outputPath);
            img.release();
        } finally {
            ocr.close();
        }
    }

    // 6. Structured Table Recognition (JSON output added since v2.1.0)
    public static void testTableRecognition(String imgPath) throws Exception {
        JiaJiaOCR ocr = JiaJiaOCR.builder();
        try {
            System.out.println("  Step 1: Extract text via general OCR");
            List<Pair<Text, Box>> ocrResults = ocr.recognizeGeneralText(imgPath);
            System.out.println("  Total OCR text items: " + ocrResults.size());

            System.out.println("  Step 2: Parse structured table data");
            TableResult tableResult = ocr.recognizeTableFromOCR(imgPath, ocrResults);

            // Original HTML format result
            System.out.println(" 【HTML Output】" + tableResult.getHtml());
            System.out.println("  Elapsed time: " + String.format("%.3f", tableResult.getElapse()) + " s");

            // New structured JSON data for rows and columns
            String tableJson = tableResult.getTableJson();
            System.out.println(" 【New JSON Row \& Column Data】" + tableJson);

        } finally {
            ocr.close();
        }
    }

    // 7. Integrated Table Detection \& Recognition
    public static void testTableDetRec(String imgPath) throws Exception {
        JiaJiaOCR ocr = JiaJiaOCR.builder();
        try {
            List<TableResult> tableResults = ocr.recognizeTables(imgPath);
            System.out.println("  Total recognized tables: " + tableResults.size());

            for (int i = 0; i < tableResults.size(); i++) {
                TableResult result = tableResults.get(i);
                System.out.println("\\n  --- Table " + (i + 1) + " Parsing Result ---");
                System.out.println("  HTML Format：" + result.getHtml());
                System.out.println("  Elapsed time：" + String.format("%.3f", result.getElapse()) + " s");

                // Structured row and column info
                ParsedTableData data = result.getParsedTableData();
                System.out.println("  Table dimension：" + data.getRows() + " rows × " + data.getColumns() + " columns");
                System.out.println("  Structured JSON Data：" + result.getTableJson());
            }
        } finally {
            ocr.close();
        }
    }

    // 8. New Feature: Task Cancellation
    public static void testCancellation(String imgPath) {
        JiaJiaOCR ocr = JiaJiaOCR.builder();
        CancellationToken token = new CancellationToken();

        // Trigger cancellation in child thread after delay
        new Thread(() -> {
            try {
                Thread.sleep(100);
                System.out.println("  【Cancellation Thread】Task cancellation requested");
                token.cancel();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        try {
            List<Pair<Text, Box>> results = ocr.recognizeGeneralText(imgPath, token);
            System.out.println("  Task completed, total items: " + results.size());
        } catch (Exception e) {
            System.out.println("  Task cancelled successfully, message: " + e.getMessage());
        } finally {
            ocr.close();
        }
    }

    // 9. New Feature: Multi-threaded Batch Processing
    public static void testMultiThreadBatch(String\[] imgPaths) {
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < imgPaths.length; i++) {
            final int index = i;
            final String path = imgPaths\[i];
            Thread t = new Thread(() -> {
                try {
                    System.out.println("  【Thread " + index + "】Start recognition task");
                    JiaJiaOCR ocr = JiaJiaOCR.builder();
                    try {
                        List<Pair<Text, Box>> results = ocr.recognizeGeneralText(path);
                        System.out.println("  【Thread " + index + "】Task finished, recognized items: " + results.size());
                    } finally {
                        ocr.close();
                    }
                } catch (Exception e) {
                    System.out.println("  【Thread " + index + "】Task failed: " + e.getMessage());
                }
            });
            threads.add(t);
            t.start();
        }

        // Wait for all threads to finish
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("  All batch tasks completed");
    }

    // 10. Multi-page PDF Batch OCR
    public static void testPdfOCR(String pdfPath) throws IOException, Exception {
        String pdfOutputDir = OUTPUT\_DIR + "/pdf\_images";
        new File(pdfOutputDir).mkdirs();

        System.out.println("  Converting PDF to images...");
        List<String> pdfImgPaths = convertPdfToImages(pdfPath, pdfOutputDir);
        System.out.println("  Total PDF pages: " + pdfImgPaths.size());

        JiaJiaOCR ocr = JiaJiaOCR.builder();
        try {
            for (int i = 0; i < pdfImgPaths.size(); i++) {
                String imgPath = pdfImgPaths.get(i);
                System.out.println("  Recognizing page " + (i + 1) + "/" + pdfImgPaths.size());
                List<Pair<Text, Box>> results = ocr.recognizeGeneralText(imgPath);
                System.out.println("  Page " + (i + 1) + " recognized items: " + results.size());
            }
        } finally {
            ocr.close();
        }
    }
}
```

\---

## 📊 Return Data Specification

JiaJiaOCR 2.1.0 / 2.2.0 returns standardized structured data to adapt to various business scenarios. Table results support dual-format output in new versions:

|Module|Main Returned Content|Data Description|
|-|-|-|
|General OCR / Handwritten OCR|Text content, coordinates, confidence|`List<Pair<Text, Box>>`<br>Text: content \& confidence<br>Box: 8 vertex coordinates|
|Layout Detection|Element type, coordinates, confidence|`List<Layout>`<br>Includes label (title/paragraph/table etc.), bounding box and confidence|
|Table Detection|Table area, confidence|`List<DetectionResult>`<br>Contains table rectangle area and recognition confidence|
|Table Recognition (Updated since v2.1.0)|Table structure, coordinates, row \& column data|`TableResult`<br>Dual output: **HTML** (web rendering) + **JSON** (structured rows \& columns), including coordinates, time cost and table dimension|

\---

## 🖼️ Demo Results

### 📝 General Printed OCR

<img src="generalocr.png" alt="通用OCR效果" width="800"/>

### ✍️ Handwriting OCR

<img src="handwrittingocr.png" alt="手写OCR效果" width="800"/>

### 📋 Document Layout Analysis

<img src="layout.png" alt="版面检测效果" width="800"/>

### 📊 Table Region Detection

<img src="table_det.png" alt="表格检测效果" width="800"/>

### 📈 Structured Table Parsing

<img src="table_rec.png" alt="表格识别效果" width="800"/>

\---

## 📞 Contact

For feature suggestions, bug reports or technical communication:

* 🐙 **GitHub**: [jiangnanboy](https://github.com/jiangnanboy)
* 💬 **QQ**: 2229029156
* 📧 **Email**: 2229029156@qq.com

## ⭐ Support This Project

If this project helps your development, kindly leave a ⭐ Star, your encouragement drives continuous optimization ❤️

[![GitHub stars](https://img.shields.io/github/stars/jiangnanboy/JiaJiaOCR?style=social)](https://github.com/jiangnanboy/JiaJiaOCR/stargazers)
