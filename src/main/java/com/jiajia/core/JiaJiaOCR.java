package com.jiajia.core;

import ai.onnxruntime.OrtException;
import com.jiajia.common_object.Box;
import com.jiajia.common_object.Boxes;
import com.jiajia.common_object.CancellationToken;
import com.jiajia.common_object.Layout;
import com.jiajia.common_object.Text;
import com.jiajia.utils.common.ImageUtils;
import com.jiajia.common_object.DetectionResult;
import com.jiajia.common_object.TableResult;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * JiaJia OCR Library - Main Facade.
 * <p>
 * This class provides a unified, secure, and high-performance interface to all JiaJia OCR functionalities.
 * Models are loaded on-demand when a feature is first used, ensuring fast startup and low memory footprint.
 * OpenCV is loaded automatically upon the first instance creation.
 * </p>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // 1. Create an instance (no models are loaded yet)
 * JiaJiaOCR ocr = JiaJiaOCR.builder().build();
 *
 * // 2. Load an image
 * Mat image = Imgcodecs.imread("path/to/your/image.jpg");
 *
 * // 3. Perform OCR (models are loaded on this first call)
 * List<Pair<Text, Box>> results = ocr.recognizeGeneralText(image);
 *
 * // 4. Don't forget to release the Mat when you're done
 * image.release();
 * }</pre>
 */
public final class JiaJiaOCR implements AutoCloseable {

    /** 外部模型目录（可选）：通过 -Djiajia.model.dir 指定；为空时从 classpath /models/ 资源加载 */
    private static final String MODEL_DIR = System.getProperty("jiajia.model.dir");

    // --- Engine Instances (Lazy-Loaded) ---
    private GeneralOCR generalOCR;
    private HandwritingOCR handwritingOCR;
    private LayoutDetection layoutDetection;
    private TableDetection tableDetection;
    private TableRecognition tableRecognition;
    private TableDetRec tableDetRec;
    private TextLineDetection textLineDetection;

    // --- Closed Flag ---
    private volatile boolean closed = false;

    // --- Private Constructor ---
    private JiaJiaOCR() {
        // Load OpenCV library once, so the user doesn't have to.
        nu.pattern.OpenCV.loadLocally();
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("JiaJiaOCR has been closed. Please create a new instance with JiaJiaOCR.builder().");
        }
    }

    // --- Public Factory Method ---
    /**
     * Creates a new instance of the JiaJia OCR engine.
     * This is the recommended way to obtain an instance.
     *
     * @return a new JiaJiaOCR instance.
     */
    public static JiaJiaOCR builder() {
        return new JiaJiaOCR();
    }

    // ========================================================================
    // CORE API (High-performance, uses org.opencv.core.Mat)
    // ========================================================================

    /**
     * 1. Performs general-purpose optical character recognition (OCR).
     * Models are loaded on the first call.
     *
     * @param image The input image as a Mat object.
     * @return A list of pairs, each containing recognized text and its bounding box.
     */
    public List<Pair<Text, Box>> recognizeGeneralText(Mat image) throws OrtException {
        return recognizeGeneralText(image, null);
    }

    public List<Pair<Text, Box>> recognizeGeneralText(Mat image, CancellationToken cancellationToken) throws OrtException {
        ensureOpen();
        if (this.generalOCR == null) {
            synchronized (this) {
                if (this.generalOCR == null) {
                    this.generalOCR = createGeneralOCREngine();
                }
            }
        }
        return this.generalOCR.run(image, cancellationToken);
    }

    public List<Pair<Text, Box>> recognizeHandwrittenText(String imagePath) throws OrtException {
        return recognizeHandwrittenText(imagePath, null);
    }

    public List<Pair<Text, Box>> recognizeHandwrittenText(String imagePath, CancellationToken cancellationToken) throws OrtException {
        ensureOpen();
        Mat image = Imgcodecs.imread(imagePath);
        if (image.empty()) {
            throw new IllegalArgumentException("Could not load image from path: " + imagePath);
        }
        try {
            return recognizeHandwrittenText(image, cancellationToken);
        } finally {
            image.release();
        }
    }

    /** @see #recognizeGeneralText(Mat) */
    public List<Pair<Text, Box>> recognizeHandwrittenText(BufferedImage image) throws IOException, OrtException {
        return recognizeHandwrittenText(image, null);
    }

    public List<Pair<Text, Box>> recognizeHandwrittenText(BufferedImage image, CancellationToken cancellationToken) throws IOException, OrtException {
        ensureOpen();
        Mat mat = ImageUtils.bufferedImageToMat(image);
        try {
            return recognizeHandwrittenText(mat, cancellationToken);
        } finally {
            mat.release();
        }
    }

    /**
     * 2. Performs handwritten text OCR.
     * Models are loaded on the first call.
     *
     * @param image The input image as a Mat object.
     * @return A list of pairs, each containing recognized handwritten text and its bounding box.
     */
    public List<Pair<Text, Box>> recognizeHandwrittenText(Mat image) throws OrtException {
        return recognizeHandwrittenText(image, null);
    }

    public List<Pair<Text, Box>> recognizeHandwrittenText(Mat image, CancellationToken cancellationToken) throws OrtException {
        ensureOpen();
        if (this.handwritingOCR == null) {
            synchronized (this) {
                if (this.handwritingOCR == null) {
                    this.handwritingOCR = createHandwritingOCREngine();
                }
            }
        }
        return this.handwritingOCR.run(image, cancellationToken);
    }

    /**
     * 3. Detects document layout elements (e.g., title, paragraph, figure).
     * The model is loaded on the first call.
     *
     * @param image The input image as a Mat object.
     * @return A list of detected layout elements.
     */
    public List<Layout> detectLayout(Mat image) throws OrtException {
        ensureOpen();
        if (this.layoutDetection == null) {
            synchronized (this) {
                if (this.layoutDetection == null) {
                    this.layoutDetection = createLayoutDetectionEngine();
                }
            }
        }
        return this.layoutDetection.run(image);
    }

    public List<Layout> detectLayout(BufferedImage image) throws OrtException, IOException {
        ensureOpen();
        Mat mat = ImageUtils.bufferedImageToMat(image);
        try {
            return detectLayout(mat);
        } finally {
            mat.release();
        }
    }

    /**
     * 4. Detects table regions within an image.
     * The model is loaded on the first call.
     *
     * @param image The input image as a Mat object.
     * @return A list of detected table regions.
     */
    public List<DetectionResult> detectTables(Mat image) throws OrtException {
        ensureOpen();
        if (this.tableDetection == null) {
            synchronized (this) {
                if (this.tableDetection == null) {
                    this.tableDetection = createTableDetectionEngine();
                }
            }
        }
        return this.tableDetection.run(image);
    }

    public List<DetectionResult> detectTables(String imagePath) throws OrtException {
        ensureOpen();
        Mat image = Imgcodecs.imread(imagePath);
        if (image.empty()) {
            throw new IllegalArgumentException("Could not load image from path: " + imagePath);
        }
        try {
            return detectTables(image);
        } finally {
            image.release();
        }
    }

    public List<DetectionResult> detectTables(BufferedImage image) throws OrtException, IOException {
        ensureOpen();
        Mat mat = ImageUtils.bufferedImageToMat(image);
        try {
            return detectTables(mat);
        } finally {
            mat.release();
        }
    }

    /**
     * 5. Recognizes the structure and content of a table.
     * This method requires pre-computed OCR results. For a one-step solution,
     * see {@link #recognizeTables(Mat)}.
     * The model is loaded on the first call.
     *
     * @param image The input image as a Mat object.
     * @param ocrResultList A list of OCR results for the text within the table.
     * @return A structured representation of the recognized table.
     */
    public TableResult recognizeTableFromOCR(Mat image, List<Pair<Text, Box>> ocrResultList) throws Exception {
        return recognizeTableFromOCR(image, ocrResultList, null);
    }

    public TableResult recognizeTableFromOCR(Mat image, List<Pair<Text, Box>> ocrResultList, CancellationToken cancellationToken) throws Exception {
        ensureOpen();
        if (this.tableRecognition == null) {
            synchronized (this) {
                if (this.tableRecognition == null) {
                    this.tableRecognition = createTableRecognitionEngine();
                }
            }
        }
        return this.tableRecognition.run(image, ocrResultList, cancellationToken);
    }

    public TableResult recognizeTableFromOCR(BufferedImage image, List<Pair<Text, Box>> ocrResultList) throws Exception {
        return recognizeTableFromOCR(image, ocrResultList, null);
    }

    public TableResult recognizeTableFromOCR(BufferedImage image, List<Pair<Text, Box>> ocrResultList, CancellationToken cancellationToken) throws Exception {
        ensureOpen();
        Mat mat = ImageUtils.bufferedImageToMat(image);
        try {
            return recognizeTableFromOCR(mat, ocrResultList, cancellationToken);
        } finally {
            mat.release();
        }
    }

    public TableResult recognizeTableFromOCR(String imagePath, List<Pair<Text, Box>> ocrResultList) throws Exception {
        return recognizeTableFromOCR(imagePath, ocrResultList, null);
    }

    public TableResult recognizeTableFromOCR(String imagePath, List<Pair<Text, Box>> ocrResultList, CancellationToken cancellationToken) throws Exception {
        ensureOpen();
        Mat image = Imgcodecs.imread(imagePath);
        if (image.empty()) {
            throw new IllegalArgumentException("Could not load image from path: " + imagePath);
        }
        try {
            return recognizeTableFromOCR(image, ocrResultList, cancellationToken);
        } finally {
            image.release();
        }
    }

    /**
     * 6. Detects and recognizes all tables in an image in a single step.
     * This is the recommended method for table processing. It handles detection,
     * OCR, and recognition internally.
     * Dependent models are loaded on the first call.
     *
     * @param image The input image as a Mat object.
     * @return A list of structured table results.
     */
    public List<TableResult> recognizeTables(Mat image) throws Exception {
        return recognizeTables(image, null);
    }

    public List<TableResult> recognizeTables(Mat image, CancellationToken cancellationToken) throws Exception {
        ensureOpen();
        if (this.tableDetRec == null) {
            synchronized (this) {
                if (this.tableDetRec == null) {
                    if (this.generalOCR == null) this.generalOCR = createGeneralOCREngine();
                    if (this.tableDetection == null) this.tableDetection = createTableDetectionEngine();
                    if (this.tableRecognition == null) this.tableRecognition = createTableRecognitionEngine();
                    this.tableDetRec = new TableDetRec(this.tableDetection, this.tableRecognition, this.generalOCR);
                }
            }
        }
        return this.tableDetRec.run(image, cancellationToken);
    }

    /**
     * 7. Detects text lines (without recognizing the content).
     * The model is loaded on the first call.
     *
     * @param image The input image as a Mat object.
     * @return A collection of bounding boxes for detected text lines.
     */
    public Boxes detectTextLines(Mat image) throws OrtException {
        ensureOpen();
        if (this.textLineDetection == null) {
            synchronized (this) {
                if (this.textLineDetection == null) {
                    this.textLineDetection = createTextLineDetectionEngine();
                }
            }
        }
        return this.textLineDetection.run(image);
    }

    public Boxes detectTextLines(String imagePath) throws OrtException {
        ensureOpen();
        Mat image = Imgcodecs.imread(imagePath);
        if (image.empty()) {
            throw new IllegalArgumentException("Could not load image from path: " + imagePath);
        }
        try {
            return detectTextLines(image);
        } finally {
            image.release();
        }
    }

    public Boxes detectTextLines(BufferedImage image) throws OrtException, IOException {
        Mat mat = ImageUtils.bufferedImageToMat(image);
        try {
            return detectTextLines(mat);
        } finally {
            mat.release();
        }
    }

    // ========================================================================
    // CONVENIENCE API (Overloaded methods for ease of use)
    // ========================================================================

    /** @see #recognizeGeneralText(Mat) */
    public List<Pair<Text, Box>> recognizeGeneralText(String imagePath) throws OrtException {
        return recognizeGeneralText(imagePath, null);
    }

    public List<Pair<Text, Box>> recognizeGeneralText(String imagePath, CancellationToken cancellationToken) throws OrtException {
        Mat image = Imgcodecs.imread(imagePath);
        if (image.empty()) {
            throw new IllegalArgumentException("Could not load image from path: " + imagePath);
        }
        try {
            return recognizeGeneralText(image, cancellationToken);
        } finally {
            image.release();
        }
    }

    /** @see #recognizeGeneralText(Mat) */
    public List<Pair<Text, Box>> recognizeGeneralText(BufferedImage image) throws IOException, OrtException {
        return recognizeGeneralText(image, null);
    }

    public List<Pair<Text, Box>> recognizeGeneralText(BufferedImage image, CancellationToken cancellationToken) throws IOException, OrtException {
        Mat mat = ImageUtils.bufferedImageToMat(image);
        try {
            return recognizeGeneralText(mat, cancellationToken);
        } finally {
            mat.release();
        }
    }

    /** @see #detectLayout(Mat) */
    public List<Layout> detectLayout(String imagePath) throws OrtException {
        Mat image = Imgcodecs.imread(imagePath);
        if (image.empty()) {
            throw new IllegalArgumentException("Could not load image from path: " + imagePath);
        }
        try {
            return detectLayout(image);
        } finally {
            image.release();
        }
    }

    /** @see #recognizeTables(Mat) */
    public List<TableResult> recognizeTables(String imagePath) throws Exception {
        return recognizeTables(imagePath, null);
    }

    public List<TableResult> recognizeTables(String imagePath, CancellationToken cancellationToken) throws Exception {
        Mat image = Imgcodecs.imread(imagePath);
        if (image.empty()) {
            throw new IllegalArgumentException("Could not load image from path: " + imagePath);
        }
        try {
            return recognizeTables(image, cancellationToken);
        } finally {
            image.release();
        }
    }

    public List<TableResult> recognizeTables(BufferedImage image) throws Exception {
        return recognizeTables(image, null);
    }

    public List<TableResult> recognizeTables(BufferedImage image, CancellationToken cancellationToken) throws Exception {
        Mat mat = ImageUtils.bufferedImageToMat(image);
        try {
            return recognizeTables(mat, cancellationToken);
        } finally {
            mat.release();
        }
    }

    // ... you can add more convenience methods for other features as needed ...


    // ========================================================================
    // PRIVATE ENGINE INITIALIZATION (On-Demand Loading)
    // ========================================================================

    private byte[] readModelFile(String relativePath) throws IOException {
        // 1. 指定了外部模型目录时，优先从外部目录读取
        if (MODEL_DIR != null && !MODEL_DIR.trim().isEmpty()) {
            Path path = Paths.get(MODEL_DIR, relativePath);
            if (Files.exists(path)) {
                return Files.readAllBytes(path);
            }
            throw new IOException("Model file not found: " + path);
        }
        // 2. 默认从 classpath /models/ 资源读取（模型放在 src/main/resources/models 下）
        String resource = "/models/" + relativePath;
        try (InputStream in = JiaJiaOCR.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("Model resource not found: " + resource
                        + ". Put models under src/main/resources/models, or set -Djiajia.model.dir=<models directory>.");
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                bos.write(buf, 0, n);
            }
            return bos.toByteArray();
        }
    }

    private GeneralOCR createGeneralOCREngine() {
        try {
            byte[] detBytes = readModelFile("text_detection/det.onnx");
            byte[] clsBytes = readModelFile("direction_detection/cls.onnx");
            byte[] recBytes = readModelFile("text_recognition/rec.onnx");
            byte[] vocabBytes = readModelFile("text_recognition/dict.txt");
            return new GeneralOCR(clsBytes, detBytes, recBytes, vocabBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize GeneralOCR engine.", e);
        }
    }

    private HandwritingOCR createHandwritingOCREngine() {
        try {
            byte[] clsBytes = readModelFile("direction_detection/cls.onnx");
            byte[] detBytes = readModelFile("text_detection/det.onnx");
            byte[] recHandBytes = readModelFile("handwriting_rec/rec.onnx");
            byte[] vocabHandBytes = readModelFile("handwriting_rec/dict.txt");
            return new HandwritingOCR(clsBytes, detBytes, recHandBytes, vocabHandBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize HandwritingOCR engine.", e);
        }
    }

    private LayoutDetection createLayoutDetectionEngine() {
        try {
            byte[] layoutBytes = readModelFile("layout_analysis/layout.onnx");
            return new LayoutDetection(layoutBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize LayoutDetection engine.", e);
        }
    }

    private TableDetection createTableDetectionEngine() {
        try {
            byte[] tableDetBytes = readModelFile("table_recognition/table_det.onnx");
            return new TableDetection(tableDetBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize TableDetection engine.", e);
        }
    }

    private TableRecognition createTableRecognitionEngine() {
        try {
            byte[] tableRegBytes = readModelFile("table_recognition/table_reg.onnx");
            return new TableRecognition(tableRegBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize TableRecognition engine.", e);
        }
    }

    private TextLineDetection createTextLineDetectionEngine() {
        try {
            byte[] detBytes = readModelFile("text_detection/det.onnx");
            return new TextLineDetection(detBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize TextLineDetection engine.", e);
        }
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;

        if (generalOCR != null) {
            generalOCR.close();
            generalOCR = null;
        }
        if (handwritingOCR != null) {
            handwritingOCR.close();
            handwritingOCR = null;
        }
        if (layoutDetection != null) {
            layoutDetection.close();
            layoutDetection = null;
        }
        if (tableDetection != null) {
            tableDetection.close();
            tableDetection = null;
        }
        if (tableRecognition != null) {
            tableRecognition.close();
            tableRecognition = null;
        }
        if (textLineDetection != null) {
            textLineDetection.close();
            textLineDetection = null;
        }
        tableDetRec = null;
    }

}

