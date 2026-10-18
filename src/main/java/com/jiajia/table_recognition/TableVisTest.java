//package com.jiajia.table_recognition;
//
//import com.ocr.OCRCore;
//import com.ocr.entity.OcrResult;
//import com.jiajia.table_recognition.entity.TableResult;
//import com.jiajia.table_recognition.utils.VisTable;
//
//import java.io.File;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//
//public class TableVisTest {
//
//    public static void main(String...args) throws Exception {
//        TableVisTest tableVisTest = new TableVisTest();
//        tableVisTest.testTable();
//    }
//
//    public void testTable() throws Exception {
//        TableCore tableCore = new TableCore();
//        OCRCore ocrEngine = new OCRCore();
//        VisTable viser = new VisTable();
//
//        File file = new File("2025-6-4-16-2-58-763-common_table-extract-0.jpg");
//        String imgPath = file.getAbsolutePath();
//        OcrResult ocrResult = ocrEngine.ocr(imgPath);
//
//        TableResult table = tableCore.table(imgPath, ocrResult.getRecRes(), true);
//
//        System.out.println(table.getHtmlStr());
//        System.out.println(table);
//
//        Path saveDir = Paths.get("src/test/resources/inference_results").toAbsolutePath();
//
//        Files.createDirectories(saveDir);
//
//        // 获取文件的stem（不含扩展名）
//        String stem = getFileStem(imgPath);
//
//        // 构建保存HTML的路径
//        Path saveHtmlPath = saveDir.resolve(stem + ".html");
//
//        // 获取输入图片的文件名
//        Path inputPath = Paths.get(imgPath);
//        String inputFileName = inputPath.getFileName().toString();
//
//        // 构建保存绘制后图片的路径
//        Path saveDrawedPath = saveDir.resolve("vis_" + inputFileName);
//
//        viser.call(imgPath, table.getHtmlStr(), saveHtmlPath.toString(), table.getCellBoxes(), saveDrawedPath.toString(), null, null);
//
//        Path saveLogicPath = saveDir.resolve("vis_logic_" + inputFileName);
//        viser.call(imgPath, table.getHtmlStr(), saveHtmlPath.toString(), table.getCellBoxes(), saveDrawedPath.toString(), table.getLogicPoints(), saveLogicPath.toString());
//    }
//
//
//    private static String getFileStem(String filePath) {
//        Path path = Paths.get(filePath);
//        String fileName = path.getFileName().toString();
//        int dotIndex = fileName.lastIndexOf('.');
//        if (dotIndex == -1) {
//            return fileName;
//        }
//        return fileName.substring(0, dotIndex);
//    }
//}
