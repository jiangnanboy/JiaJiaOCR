# JiaJiaOCR

[![中文 README](https://img.shields.io/badge/中文-README-red?style=flat-square)](README.md)

## 🚀 Java OCR Solution

In the process of OCR technology implementation, Java developers often face the dilemma of **"prosperous Python ecosystem, difficult Java integration"** — either relying on JNI to call external exe/dll files, or encountering endless pitfalls in cross-platform deployment.

**JiaJiaOCR brings you a revolutionary breakthrough!** 🎉

This project will synchronously update and upgrade lightweight and full-featured versions:



| Version                            | Size   | Core Features                                                        | Applicable Scenarios                                       |
| ---------------------------------- | ------ | -------------------------------------------------------------------- | ---------------------------------------------------------- |
| **🔥 Lightweight Edition (1.0+)**  | 21MB+  | General OCR Recognition                                              | Rapid integration, resource-constrained environments       |
| **⚡ Full-Featured Edition (2.0+)** | 200MB+ | General OCR + Handwritten OCR + Layout Detection + Table Recognition | Enterprise-level applications, complex document processing |



***

## 📦 Version Update Overview

### 🔥 Lightweight Edition (1.0+) - Streamlined & Efficient



* ✅ **v1.0.1** - Optimized blank recognition for English OCR → returns empty string instead of null

* ✅ **v1.0.2** - Added PDF recognition support

### ⚡ Full-Featured Edition (2.0+) - Comprehensive Functions



* ✅ **v2.0.1** - Optimized blank recognition for English OCR → returns empty string instead of null

* ✅ **v2.0.2** - Added PDF recognition support

* 🚀 **40% Performance Improvement** - Model lazy loading mechanism, loaded on demand

* 🎯 **Function Expansion** - Added handwritten OCR, layout detection, table detection and recognition

* 📊 **Result Upgrade** - Added layout element coordinates, table HTML format output



***

## 🎯 Detailed Explanation of Four Core Functions

### 1. 📝 General OCR: Benchmark for Printed Text Recognition

Inherits the high-precision printed text recognition capability of version 1.0+, supports mixed recognition of Chinese, English, numbers and symbols, returns text content and coordinates, suitable for printed text scenarios such as invoices, labels, billboards, etc.

### 2. ✍️ Handwritten OCR: Breaking Through the Difficulties of Handwriting Recognition

Added a specially optimized model for Chinese handwriting, supports common handwriting styles such as neat handwriting and cursive handwriting, solves the pain points of digitizing handwritten notes and entering handwritten forms, with a recognition accuracy rate of **over 92%** (tested with standard handwriting samples).

### 3. 📋 Layout Detection: Intelligent Analysis of Document Structure

Can automatically identify layout elements such as titles, paragraphs, images and tables in documents, returns the coordinates and type labels of each element, provides a basis for document structured processing, suitable for scenarios such as PDF to Word, ancient book digitization, etc.

### 4. 📊 Table Detection and Recognition: Direct Extraction of Structured Data

A rare pure Java table recognition solution in the industry, supports the whole process of table area detection, cell segmentation and content recognition, and finally outputs structured results in HTML format, which can be directly rendered as tables or imported into Excel, completely getting rid of the tedious manual entry of table data.



***

## 🛠️ Quick Start: 5-Minute Integration Tutorial

### 📋 Environment Preparation



* **Development Environment**: JDK 8 or above

* **Runtime Environment**: Windows 10+, Linux x86 _64

* **Dependency Management**: Maven (Recommended)

### 📦 Import Dependencies and Jar Packages

#### Step 1: Add Maven Dependencies



```
 <dependencies>

     <!-- ONNX Runtime: Core dependency for model inference -->

     <dependency>

         <groupId>com.microsoft.onnxruntime </groupId>

         <artifactId>onnxruntime </artifactId>

         <version>1.19.0 </version>

     </dependency>

     <!-- DJL MXNet Engine: Deep learning framework support -->

     <dependency>

         <groupId>ai.djl.mxnet </groupId>

         <artifactId>mxnet-engine </artifactId>

         <version>0.31.0 </version>

     </dependency>

     <!-- OpenCV: Image processing dependency -->

     <dependency>

         <groupId>ai.djl.opencv </groupId>

         <artifactId>opencv </artifactId>

         <version>0.31.0 </version>

     </dependency>

     <!-- DJL Core API: Model management -->

     <dependency>

         <groupId>ai.djl </groupId>

         <artifactId>api </artifactId>

         <version>0.31.0 </version>

     </dependency>

    <!-- PDF manipulation -->

     <dependency>

         <groupId>org.apache.pdfbox </groupId>

         <artifactId>pdfbox </artifactId>

         <version>3.0.2 </version>

     </dependency>

 </dependencies>
```

#### Step 2: Download Core Jar Package

Go to the releases page of the GitHub repository [JiaJiaOCR](https://github.com/jiangnanboy/JiaJiaOCR), download the jar package of JiaJiaOCR 2.0+ version, put it into the project dependency directory and import it. The jar package is 200MB (integrated with model files).

JiaJiaOCR 1.0+, just use general OCR and PDF recognition.

### 💻 Complete Function Example Code


```
import com.jiajia.common _object. *;

import com.jiajia.core.JiaJiaOCR;

import org.apache.commons.lang3.tuple.Pair;

import org.opencv.core.Mat;

import org.opencv.core.MatOfPoint;

import org.opencv.core.Point;

import org.opencv.core.Scalar;

import org.opencv.imgcodecs.Imgcodecs;

import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

import java.util.List;

import java.io.IOException;

import ai.onnxruntime.OrtException;

import static com.jiajia.common _object.PdfConvert.convertPdfToImages;

/ * *

  * JiaJiaOCR 2.0 Full-Feature Demo

  * @author sy

  * @date 2025/12/4 19:56

  */

public class JiaJiaOCR2Demo {

    public static void main(String...args) {

        String imgPath = "test.jpg"; // Replace with your image path

        // 🚀 Unlock functions on demand (uncomment to use)

        // generalOCRTest(imgPath);    // 1. General OCR (Printed Text)

        // handwrittingOCRTest(imgPath); // 2. Handwritten OCR

        // layoutDetTest(imgPath);     // 3. Layout Detection (with visualization)

        // tableDetTest(imgPath);      // 4. Table Detection (with visualization)

        // tableRecTest(imgPath);      // 5. Table Recognition (output HTML)

        // tableDetRecTest(imgPath);   // 6. Table Detection + Recognition (One-stop)

        // textLineDetText(imgPath);   // 7. Text Line Detection (with visualization)

        // try {

        //     pdfOCR();               // 8. PDF Recognition Demo 

        // } catch (IOException | OrtException e) {

        //     e.printStackTrace();

        // }

    }

    / * *

      * 1. General OCR (Printed Text Recognition)

      * @param imgPath Image path

      */

    public static void generalOCRTest(String imgPath) {

        // Lazy initialization: no model loaded at this time

        JiaJiaOCR jiaJiaOCR = JiaJiaOCR.builder();

        // Corresponding model is loaded only when the function is called

        List <Pair <Text, Box>> result = jiaJiaOCR.recognizeGeneralText(imgPath);

        // Output: Text content + Quadrilateral coordinates

        System.out.println("General OCR Result: " + result);

    }

    

    / * *

      * 2. Handwritten OCR Recognition

      * @param imgPath Image path

      */

    public static void handwrittingOCRTest(String imgPath) {

        JiaJiaOCR jiaJiaOCR = JiaJiaOCR.builder();

        List <Pair <Text, Box>> result = jiaJiaOCR.recognizeHandwrittenText(imgPath);

        System.out.println("Handwritten OCR Result: " + result);

    }

    

    / * *

      * 3. Layout Detection (Identify elements such as title/paragraph/table, with visualization)

      * @param imgPath Image path

      */

    public static void layoutDetTest(String imgPath) {

        JiaJiaOCR jiaJiaOCR = JiaJiaOCR.builder();

        List <Layout> layoutList = jiaJiaOCR.detectLayout(imgPath);

        // Visualize detection results and save

        Mat img = Imgcodecs.imread(imgPath);

        drawLayoutPredictions(img, layoutList);

        Imgcodecs.imwrite("layout _result.jpg", img);

        System.out.println("Layout detection result saved, element information: " + layoutList);

    }

    

    / * *

      * 4. Table Detection (Only detect table area, with visualization)

      * @param imgPath Image path

      */

    public static void tableDetTest(String imgPath) {

        JiaJiaOCR jiaJiaOCR = JiaJiaOCR.builder();

        List <DetectionResult> tableList = jiaJiaOCR.detectTables(imgPath);

        // Visualize table area

        Mat img = Imgcodecs.imread(imgPath);

        Mat resultMat = drawTableResults(img, tableList);

        Imgcodecs.imwrite("table _detect _result.jpg", resultMat);

        System.out.println("Table detection area: " + tableList);

    }

    

    / * *

      * 5. Table Recognition (Extract table structure based on general OCR results, output HTML)

      * @param imgPath Image path

      */

    public static void tableRecTest(String imgPath) {

        JiaJiaOCR jiaJiaOCR = JiaJiaOCR.builder();

        // First get general OCR results

        List <Pair <Text, Box>> ocrResult = jiaJiaOCR.recognizeGeneralText(imgPath);

        // Extract table structure based on OCR results

        TableResult tableResult = jiaJiaOCR.recognizeTableFromOCR(imgPath, ocrResult);

        // Output HTML format (can be directly rendered)

        System.out.println("Table Recognition HTML Result: " + tableResult.getHtmlContent());

    }

    

    / * *

      * 6. One-stop call for table detection + recognition

      * @param imgPath Image path

      */

    public static void tableDetRecTest(String imgPath) {

        JiaJiaOCR jiaJiaOCR = JiaJiaOCR.builder();

        // Get table structured results in one step

        List <TableResult> tableResults = jiaJiaOCR.recognizeTables(imgPath);

        for (TableResult table : tableResults) {

            System.out.println("Table HTML: " + table.getHtmlContent());

            System.out.println("Table Coordinates: " + table.getBox());

        }

    }

    

    / * *

      * 7. Text Line Detection (with visualization)

      * @param imgPath Image path

      */

    public static void textLineDetText(String imgPath) {

        JiaJiaOCR jiaJiaOCR = JiaJiaOCR.builder();

        Boxes textLines = jiaJiaOCR.detectTextLines(imgPath);

        // Visualize text lines

        Mat img = Imgcodecs.imread(imgPath);

        drawTextLinePredictions(img, textLines);

        Imgcodecs.imwrite("textline _result.jpg", img);

        System.out.println("Text Line Detection Result: " + textLines);

    }

    / * *

      * 8. PDF Recognition

      */

    public static void pdfOCR() throws IOException, OrtException {

        String pdfPath = "How _To.pdf";

        String pdfOutputDir = "pdf _image"; // Directory to save images converted from PDF

        JiaJiaOCR jiaJiaOCR = JiaJiaOCR.builder();

        List <String> pdfPathList = convertPdfToImages(pdfPath, pdfOutputDir);

        for(String pdfImgPath:pdfPathList) {

            List <Pair <Text, Box>> pairList = jiaJiaOCR.recognizeGeneralText(pdfImgPath);

            System.out.println(pairList);

        }

    }

    // ------------------- Visualization Tool Methods -------------------

    / * *

      * Draw layout detection results

      */

    public static void drawLayoutPredictions(Mat img, List <Layout> detLayout) {

        for(Layout layout : detLayout) {

            int [] bbox = layout.getBbox();

            // Draw rectangle (red, line width 2)

            Imgproc.rectangle(img, new Point(bbox [0], bbox [1]), 

                             new Point(bbox [2], bbox [3]), new Scalar(0, 0, 255), 2);

            // Draw element label (green text)

            String label = layout.getLabel();

            Imgproc.putText(img, label, new Point(bbox [0]+5, bbox [1]-10), 

                           Imgproc.FONT _HERSHEY _SIMPLEX, 0.7, new Scalar(0, 255, 0), 2);

        }

    }

    

    / * *

      * Draw table detection results

      */

    public static Mat drawTableResults(Mat image, List <DetectionResult> results) {

        Mat resultImg = image.clone();

        for (DetectionResult result : results) {

            String label = "Table: " + String.format("%.2f", result.getConfidence());

            // Draw label and rectangle

            Imgproc.putText(resultImg, label, new Point(result.getRect().x, result.getRect().y-10),

                           Imgproc.FONT _HERSHEY _SIMPLEX, 1.0, new Scalar(0, 0, 255), 2);

            Imgproc.rectangle(resultImg, new Point(result.getRect().x, result.getRect().y),

                           new Point(result.getRect().x+result.getRect().width, 

                                     result.getRect().y+result.getRect().height),

                           new Scalar(0, 0, 255), 2);

        }

        return resultImg;

    }

    

    / * *

      * Draw text line detection results (polygon adaptation for any angle)

      */

    public static void drawTextLinePredictions(Mat img, Boxes detBoxes) {

        List <Box> boxList = detBoxes.getBoxes();

        for(Box box : boxList) {

            double [] pos = box.getLinePosition();

            // Build quadrilateral vertices

            Point [] points = new Point [4];

            points [0] = new Point(pos [0], pos [1]);

            points [1] = new Point(pos [2], pos [3]);

            points [2] = new Point(pos [4], pos [5]);

            points [3] = new Point(pos [6], pos [7]);

            // Draw polygon (red, line width 1)

            MatOfPoint polygon = new MatOfPoint(points);

            List <MatOfPoint> polygons = new ArrayList<>();

            polygons.add(polygon);

            Imgproc.polylines(img, polygons, true, new Scalar(0, 0, 255), 1);

        }

    }

}
```


***

## 📊 Key Return Result Description

JiaJiaOCR 2.0 provides structured and easy-to-parse return results, covering the needs of different functional scenarios:



| Function Module               | Core Return Content                   | Data Format Description                                                                          |
| ----------------------------- | ------------------------------------- | ------------------------------------------------------------------------------------------------ |
| General OCR / Handwritten OCR | Text content, coordinates, confidence | `List<Pair<Text, Box>>`, Text contains content and confidence, Box contains 8 vertex coordinates |
| Layout Detection              | Element type, coordinates, confidence | `List<Layout>`, contains label (title/paragraph, etc.), bbox (rectangular coordinates)           |
| Table Detection               | Table area, confidence                | `List<DetectionResult>`, contains rectangular coordinates and confidence                         |
| Table Recognition             | Table structure, content, coordinates | `TableResult`, contains htmlContent (HTML format), box (coordinates)                             |


***

## 🖼️ Effect Demonstration

### 📝 General OCR Effect


![General OCR Effect](generalocr.png)

### ✍️ Handwritten OCR Effect


![Handwritten OCR Effect](handwrittingocr.png)

### 📋 Layout Detection Effect


![Layout Detection Effect](layout.png)

### 📊 Table Detection Effect


![Table Detection Effect](table_det.png)

### 📈 Table Recognition Effect


![Table Recognition Effect](table_rec.png)


***

## 📞 Contact Me

If you have any ideas or questions, feel free to contact me:


1. 🐙 **GitHub**: [jiangnanboy](https://github.com/jiangnanboy)

2. 💬 **QQ**: 2229029156

3. 📧 **Email**: 2229029156@qq.com


***

## ⭐ Support Us

If this project is helpful to you, please give us a **Star**! Your support is the driving force for our continuous improvement ❤️


![GitHub stars](https://img.shields.io/github/stars/jiangnanboy/JiaJiaOCR?style=social)
