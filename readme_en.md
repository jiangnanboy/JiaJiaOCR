# JiaJiaOCR

[![中文 README](https://img.shields.io/badge/中文-README-red?style=flat-square)](README.md)

In the implementation of OCR technology, Java developers often face the dilemma of "prosperous Python ecosystem vs. difficult Java integration" — either relying on external exe/dll files or encountering countless pitfalls in cross-platform deployment. Now, JiaJiaOCR 2.0 has undergone a major upgrade! It not only retains the advantage of CPU inference but also adds three core functions: Handwritten OCR, Layout Detection, and Table Detection & Recognition. The model loading method has also been fully optimized, taking practicality to a new level!

## I. Core Upgrade Highlights of Version 2.0

JiaJiaOCR 1.0 only implements general OCR functions. You can download version 1.0 in releases, which is 21MB in size.

JiaJiaOCR 2.0 achieves three breakthroughs in functionality, performance, and usability, directly addressing the needs of more practical business scenarios:

* **Function Expansion: From "General-Purpose" to "All-Round"**

  On the basis of the original general text recognition, new functions such as Handwritten OCR, Layout Detection, and Table Detection & Recognition are added, covering full scenarios including printed text, handwritten text, and document structure parsing.

* **Performance Optimization: More Efficient Lazy Loading of Models**

  Adopts a lazy loading mechanism that loads models on demand. No need to load all models during initialization, reducing memory usage and increasing startup speed by 40%.

* **Experience Upgrade: Richer Result Output**

  In addition to the original text content and coordinates, it adds layout element coordinates and HTML format output for table recognition, directly supporting structured data extraction.

* **Unchanged Compatibility: Cross-Platform & Dependency-Free**

  Fully implemented in pure Java without calling exe/dll files. Perfectly compatible with Windows and Linux x86 environments, and can run on JDK 8+.

## II. Detailed Explanation of Four Core Functions

Centered on "full-scenario text parsing", JiaJiaOCR 2.0's four core functions cover the complete needs from single-character recognition to document structuring:

### 1. General OCR: Benchmark for Printed Text Recognition

Inherits the high-precision printed text recognition capability of Version 1.0. Supports mixed recognition of Chinese, English, numbers, and symbols, returning text content and coordinates. Suitable for printed text scenarios such as invoices, labels, and billboards.

### 2. Handwritten OCR: Breaking Through Handwriting Recognition Difficulties

Adds a specially optimized model for Chinese handwriting, supporting common handwriting styles such as neat handwriting and cursive handwriting. Solves pain points like digitization of handwritten notes and entry of handwritten forms. The recognition accuracy exceeds 92% (tested with standard handwritten samples).

### 3. Layout Detection: Intelligent Document Structure Parsing

Automatically recognizes layout elements in documents such as titles, paragraphs, images, and tables, returning the coordinates and type labels of each element. Provides a foundation for document structured processing, suitable for scenarios like PDF-to-Word conversion and ancient book digitization.

### 4. Table Detection & Recognition: Direct Extraction of Structured Data

A rare pure Java table recognition solution in the industry. Supports the entire process of table area detection, cell segmentation, and content recognition. Finally outputs structured results in HTML format, which can be directly rendered as tables or imported into Excel, eliminating the tedious manual entry of table data.

## III. Quick Start: 5-Minute Integration Tutorial

### 1. Environment Preparation

* Development Environment: JDK 8 or higher

* Runtime Environment: Windows 10+, Linux x86\_64

* Dependency: Maven (recommended)

### 2. Import Dependencies and Jar Packages

**Step 1: Add the following dependencies to pom.xml (deep learning engine and image processing dependencies):**

```
<dependencies>
    <!-- ONNX Runtime：Core dependencies of model inference -->
    <dependency>
        <groupId>com.microsoft.onnxruntime</groupId>
        <artifactId>onnxruntime</artifactId>
        <version>1.19.0</version>
    </dependency>
    <!-- DJL MXNetEngine: Supported by deep learning frameworks -->
    <dependency>
        <groupId>ai.djl.mxnet</groupId>
        <artifactId>mxnet-engine</artifactId>
        <version>0.31.0</version>
    </dependency>
    <!-- OpenCV：Image processing dependency -->
    <dependency>
        <groupId>ai.djl.opencv</groupId>
        <artifactId>opencv</artifactId>
        <version>0.31.0</version>
    </dependency>
    <!-- DJL Core API: Model Management -->
    <dependency>
        <groupId>ai.djl</groupId>
        <artifactId>api</artifactId>
        <version>0.31.0</version>
    </dependency>
</dependencies>
```

**Step 2: Download the core Jar package:**

Go to the releases page of the GitHub repository: [https://github.com/jiangnanboy/JiaJiaOCR](https://github.com/jiangnanboy/JiaJiaOCR), download the Jar package of JiaJiaOCR 2.0, place it in the project's dependency directory, and import it. The Jar package is 200MB as it integrates models.

### 3. Complete Function Example Code

The following code includes call examples for all core functions of Version 2.0 with clear comments, which can be directly copied and used. Replace `imgPath` with the actual image path:

```
import com.jiajia.common_object.*;
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
/**
 * JiaJiaOCR 2.0 Full-featured example
 * @author sy
 * @date 2025/12/4 15:56
 */
public class JiaJiaOCR2Demo {
    public static void main(String...args) {
        String imgPath = "test.jpg"; // Replace it with your image path

        // Unlock function on demand (use it after unplugging the annotations)
        // generalOCRTest(imgPath);    // 1.General-purpose OCR (Printed Text)
        // handwrittingOCRTest(imgPath); // 2.handwritting OCR
        // layoutDetTest(imgPath);     // 3.Layout detection (with visualization)
        // tableDetTest(imgPath);      // 4.Table detection (with visualization)
        // tableRecTest(imgPath);      // 5.Table recognition (Output HTML)
        // tableDetRecTest(imgPath);   // 6.Table detection + Recognition (One-stop)
        // textLineDetText(imgPath);   // 7.Text line detection (with visualization)
    }
    /**
     * 1. General-purpose OCR (Printed Text)
     * @param imgPath
     */
    public static void generalOCRTest(String imgPath) {
        // Lazy loading initialization: No model is loaded at this time
        JiaJiaOCR jiaJiaOCR = JiaJiaOCR.builder();
        // The corresponding model is loaded only when the function is called
        List<Pair<Text, Box>> result = jiaJiaOCR.recognizeGeneralText(imgPath);
        // Output: Text content + coordinates
        System.out.println("General OCR result：" + result);
    }
    /**
     * 2. handwritting OCR
     * @param imgPath
     */
    public static void handwrittingOCRTest(String imgPath) {
        JiaJiaOCR jiaJiaOCR = JiaJiaOCR.builder();
        List<Pair<Text, Box>> result = jiaJiaOCR.recognizeHandwrittenText(imgPath);
        System.out.println("Handwritting OCR result：" + result);
    }
    /**
     * 3. Layout detection
     * @param imgPath
     */
    public static void layoutDetTest(String imgPath) {
        JiaJiaOCR jiaJiaOCR = JiaJiaOCR.builder();
        List<Layout> layoutList = jiaJiaOCR.detectLayout(imgPath);
        Mat img = Imgcodecs.imread(imgPath);
        drawLayoutPredictions(img, layoutList);
        Imgcodecs.imwrite("layout_result.jpg", img);
    }
    /**
     * 4. Table detection
     * @param imgPath
     */
    public static void tableDetTest(String imgPath) {
        JiaJiaOCR jiaJiaOCR = JiaJiaOCR.builder();
        List<DetectionResult> tableList = jiaJiaOCR.detectTables(imgPath);
        Mat img = Imgcodecs.imread(imgPath);
        Mat resultMat = drawTableResults(img, tableList);
        Imgcodecs.imwrite("table_detect_result.jpg", resultMat);
    }
    /**
     * 5. Table recognition
     * @param imgPath
     */
    public static void tableRecTest(String imgPath) {
        JiaJiaOCR jiaJiaOCR = JiaJiaOCR.builder();
        // First, obtain the general OCR result
        List<Pair<Text, Box>> ocrResult = jiaJiaOCR.recognizeGeneralText(imgPath);
        // Extract the table structure based on the OCR results
        TableResult tableResult = jiaJiaOCR.recognizeTableFromOCR(imgPath, ocrResult);
        // Output HTML format
        System.out.println("HTML：" + tableResult.getHtmlContent());
    }
    /**
     * 6. Table detection + Recognition
     * @param imgPath
     */
    public static void tableDetRecTest(String imgPath) {
        JiaJiaOCR jiaJiaOCR = JiaJiaOCR.builder();
        List<TableResult> tableResults = jiaJiaOCR.recognizeTables(imgPath);
        for (TableResult table : tableResults) {
            System.out.println("HTML：" + table.getHtmlContent());
            System.out.println("coordinate：" + table.getBox());
        }
    }
    /**
     * 7. Text line detection
     * @param imgPath
     */
    public static void textLineDetText(String imgPath) {
        JiaJiaOCR jiaJiaOCR = JiaJiaOCR.builder();
        Boxes textLines = jiaJiaOCR.detectTextLines(imgPath);
        Mat img = Imgcodecs.imread(imgPath);
        drawTextLinePredictions(img, textLines);
        Imgcodecs.imwrite("textline_result.jpg", img);
        System.out.println("Text line detection result：" + textLines);
    }
    // ------------------- Visualization tool methods -------------------
    /**
     * Draw the layout detection results
     */
    public static void drawLayoutPredictions(Mat img, List<Layout> detLayout) {
        for(Layout layout : detLayout) {
            int[] bbox = layout.getBbox();
            Imgproc.rectangle(img, new Point(bbox[0], bbox[1]), 
                             new Point(bbox[2], bbox[3]), new Scalar(0, 0, 255), 2);
            String label = layout.getLabel();
            Imgproc.putText(img, label, new Point(bbox[0]+5, bbox[1]-10), 
                           Imgproc.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(0, 255, 0), 2);
        }
    }
    /**
     * Draw a table to test the results
     */
    public static Mat drawTableResults(Mat image, List<DetectionResult> results) {
        Mat resultImg = image.clone();
        for (DetectionResult result : results) {
            String label = "table：" + String.format("%.2f", result.getConfidence());
            Imgproc.putText(resultImg, label, new Point(result.getRect().x, result.getRect().y-10),
                           Imgproc.FONT_HERSHEY_SIMPLEX, 1.0, new Scalar(0, 0, 255), 2);
            Imgproc.rectangle(resultImg, new Point(result.getRect().x, result.getRect().y),
                           new Point(result.getRect().x+result.getRect().width, 
                                     result.getRect().y+result.getRect().height),
                           new Scalar(0, 0, 255), 2);
        }
        return resultImg;
    }
    /**
     * Draw the text line detection results
     */
    public static void drawTextLinePredictions(Mat img, Boxes detBoxes) {
        List<Box> boxList = detBoxes.getBoxes();
        for(Box box : boxList) {
            double[] pos = box.getLinePosition();
            Point[] points = new Point[4];
            points[0] = new Point(pos[0], pos[1]);
            points[1] = new Point(pos[2], pos[3]);
            points[2] = new Point(pos[4], pos[5]);
            points[3] = new Point(pos[6], pos[7]);
            MatOfPoint polygon = new MatOfPoint(points);
            List<MatOfPoint> polygons = new ArrayList<>();
            polygons.add(polygon);
            Imgproc.polylines(img, polygons, true, new Scalar(0, 0, 255), 1);
        }
    }
}
```

## IV. Explanation of Key Return Results

JiaJiaOCR 2.0 provides structured and easily parsable return results to meet the needs of different functional scenarios:

| Function Module             | Core Return Content                   | Data Format Description                                                                              |
| --------------------------- | ------------------------------------- | ---------------------------------------------------------------------------------------------------- |
| General OCR/Handwritten OCR | Text content, coordinates, confidence | `List<Pair<Text, Box>>`; `Text` contains content and confidence; `Box` contains 8 vertex coordinates |
| Layout Detection            | Element type, coordinates, confidence | `List<Layout>`; includes `label` (title/paragraph, etc.) and `bbox` (rectangle coordinates)          |
| Table Detection             | Table area, confidence                | `List<DetectionResult>`; includes rectangle coordinates and confidence                               |
| Table Recognition           | Table structure, content, coordinates | `TableResult`; includes `htmlContent` (HTML format) and `box` (coordinates)                          |

<img src="generalocr.png" />

<img src="handwrittingocr.png" />

<img src="layout.png" />

<img src="table_det.png" />

<img src="table_rec.png" />

## V. Contact Me
If you have any ideas or questions, please contact me
1. github:https://github.com/jiangnanboy
2. QQ:2229029156
3. email:2229029156@qq.com