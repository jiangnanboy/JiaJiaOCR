package com.jiajia.common_object;

import com.jiajia.utils.common.CollectionUtil;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.Loader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author jiangnanboy
 * @date 2025/12/29 18:40
 */
public class PdfConvert {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfConvert.class);

    // PDF转图片的临时输出目录（可自定义）
    private static final String PDF_IMG_OUTPUT_DIR = "pdf_image";
    // PDF转图片的分辨率（300DPI保证OCR识别精度）
    private static final float PDF_DPI = 300;
    // 图片格式（PNG比JPG更适合OCR）
    private static final String IMG_FORMAT = "png";

    /**
     * PDF转图片核心方法（返回生成的图片路径列表）
     * @param pdfPath PDF文件路径
     * @return 生成的图片路径列表（按页码排序）
     * @throws IOException IO异常
     */
    public static List<String> convertPdfToImages(String pdfPath, String PdfOutputDir) throws IOException {
        // 校验PDF文件
        File pdfFile = new File(pdfPath);
        if (!pdfFile.exists()) {
            throw new IllegalArgumentException("PDF文件不存在：" + pdfPath);
        }
        // 创建输出目录
        File outDir = new File(PdfOutputDir);
        if (!outDir.exists()) {
            boolean mkdirs = outDir.mkdirs();
            if (!mkdirs) {
                throw new IOException("创建PDF图片输出目录失败：" + PdfOutputDir);
            }
        }

        List<String> imgPathList = CollectionUtil.newArrayList();
        // 加载PDF并逐页转换
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDFRenderer renderer = new PDFRenderer(document);
            int pageCount = document.getNumberOfPages();
            String name = pdfFile.getName();
            int dot = name.lastIndexOf('.');
            String pdfFileName = dot > 0 ? name.substring(0, dot) : name;

            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                // 渲染为高分辨率图片（适配OCR）
                BufferedImage image = renderer.renderImageWithDPI(pageIndex, PDF_DPI);
                // 生成图片文件名（pdf名称_页码.png）
                String imgFileName = pdfFileName + "_page_" + (pageIndex + 1) + "." + IMG_FORMAT;
                String imgPath = PdfOutputDir + File.separator + imgFileName;
                // 写入图片文件
                ImageIO.write(image, IMG_FORMAT, new File(imgPath));
                imgPathList.add(imgPath);
                LOGGER.info("PDF第{}页转换为：{}", (pageIndex + 1), imgPath);
            }
        }
        return imgPathList;
    }

    public static List<String> convertPdfToImages(String pdfPath) throws IOException {
        return convertPdfToImages(pdfPath, PDF_IMG_OUTPUT_DIR);
    }
}
