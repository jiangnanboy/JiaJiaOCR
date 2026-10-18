package com.jiajia.table_recognition;//import com.ocr.OCRCore;
//import com.ocr.entity.OcrResult;
//import com.ocr.entity.ParamConfig;
//import org.opencv.core.Mat;
//import org.opencv.imgcodecs.Imgcodecs;
//import com.table.rec.TableCore;
//import com.table.rec.entity.TableResult;
//
//
//import javax.imageio.ImageIO;
//import java.awt.image.BufferedImage;
//import java.io.File;
//import java.nio.file.Files;
//
//public class TableTest {
//
//    public void testPath() throws Exception {
//        TableCore tableCore = new TableCore();
//        OCRCore rapidOCR = new OCRCore();
//
//        File file = new File("src/test/resources/table_01.jpg");
//        String imgContent = file.getAbsolutePath();
//        OcrResult ocrResult = rapidOCR.ocr(imgContent);
//        TableResult tableResult = tableCore.table(imgContent, ocrResult.getRecRes());
////        Assertions.assertFalse(tableResult.getCellBoxes().isEmpty());
//        System.out.println(tableResult);
//    }
//
//    public void testBufferedImage() throws Exception {
//        TableCore tableCore = new TableCore();
//        OCRCore rapidOCR = OCRCore.create();
//        File file = new File("src/test/resources/table_01.jpg");
//        BufferedImage imgContent = ImageIO.read(file);
//
//        ParamConfig paramConfig = new ParamConfig();
//        OcrResult ocrResult = rapidOCR.ocr(imgContent, paramConfig);
//        TableResult tableResult = tableCore.table(imgContent, ocrResult.getRecRes());
////        Assertions.assertFalse(tableResult.getCellBoxes().isEmpty());
//        System.out.println(tableResult);
//    }
//
//    public void testByte() throws Exception {
//        TableCore tableCore = new TableCore();
//        OCRCore rapidOCR = OCRCore.create();
//        File file = new File("src/test/resources/table_01.jpg");
//        byte[] imgContent = Files.readAllBytes(file.toPath());
//        OcrResult ocrResult = rapidOCR.ocr(imgContent);
//        TableResult tableResult = tableCore.table(imgContent, ocrResult.getRecRes());
////        Assertions.assertFalse(tableResult.getCellBoxes().isEmpty());
//        System.out.println(tableResult);
//    }
//
//    public void testMat() throws Exception {
//        TableCore tableCore = new TableCore();
//        OCRCore rapidOCR = OCRCore.create();
//        File file = new File("src/test/resources/table_01.jpg");
//        Mat imgContent = Imgcodecs.imread(file.getAbsolutePath());
//        OcrResult ocrResult = rapidOCR.ocr(imgContent);
//        TableResult tableResult = tableCore.table(imgContent, ocrResult.getRecRes());
////        Assertions.assertFalse(tableResult.getCellBoxes().isEmpty());
//        System.out.println(tableResult);
//    }
//
//}
