package com.jiajia.table_recognition.utils;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 加载图片
 */
public class LoadImage {

    public Mat call(Object imgInput) throws LoadImageError {
        Mat mat = loadImg(imgInput);

        // 如果是单通道图像，则转换为 BGR 三通道
        if (mat.channels() == 1) {
            Mat grayMat = new Mat();
            Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_GRAY2BGR);
            mat.release();
            return grayMat;
        }

        // 如果是四通道图像（RGBA），转换为 BGR
        if (mat.channels() == 4) {
            Mat convertedMat = cvtFourToThree(mat);
            mat.release();
            return convertedMat;
        }

        return mat;
    }

    /**
     * 根据传入的对象类型，加载不同来源的图像数据
     */
    private Mat loadImg(Object img) throws LoadImageError {
        // 1. 如果是字符串或 Path，认为是图片文件路径
        if (img instanceof String || img instanceof Path) {
            String filePath = img instanceof String ? (String) img : ((Path) img).toString();
            verifyExist(filePath);
            boolean containsChinese = filePath.matches(".*[\\u4e00-\\u9fa5]+.*");
            Mat mat;
            if (!containsChinese) {
                mat = Imgcodecs.imread(filePath, Imgcodecs.IMREAD_COLOR);
            } else {
                // OpenCV 中的 imread 方法不支持中文路径，使用字节数组byte[]
                byte[] bytes;
                try {
                    bytes = Files.readAllBytes(Paths.get(filePath));
                } catch (IOException e) {
                    throw new LoadImageError("无法识别或读取图片: " + filePath);
                }
                MatOfByte mob = new MatOfByte(bytes);
                mat = Imgcodecs.imdecode(mob, Imgcodecs.IMREAD_COLOR);
            }
            if (mat.empty()) {
                throw new LoadImageError("无法识别或读取图片: " + filePath);
            }
            return mat;
        }

        // 2. 如果是 byte[]，认为是图片的二进制内容
        if (img instanceof byte[]) {
            byte[] bytes = (byte[]) img;
            MatOfByte mob = new MatOfByte(bytes);
            Mat mat = Imgcodecs.imdecode(mob, Imgcodecs.IMREAD_COLOR);
            if (mat.empty()) {
                throw new LoadImageError("无法识别或读取二进制图片数据");
            }
            return mat;
        }

        // 3. 如果已经是 Mat，则直接返回
        if (img instanceof Mat) {
            return (Mat) img;
        }

        // 4. 如果是 BufferedImage 转 Mat
        if (img instanceof BufferedImage) {
            return bufferedImageToMat((BufferedImage) img);
        }

        // 4. 其他类型不支持
        throw new LoadImageError("不支持的图片输入类型: " + img.getClass().getName());
    }

    /**
     * RGBA → BGR 的转换示例
     */
    private Mat cvtFourToThree(Mat rgbaMat) {
        List<Mat> rgbaChannels = new java.util.ArrayList<>();
        Core.split(rgbaMat, rgbaChannels);

        Mat b = rgbaChannels.get(0);
        Mat g = rgbaChannels.get(1);
        Mat r = rgbaChannels.get(2);
        Mat a = rgbaChannels.get(3);

        // 合并 b、g、r
        List<Mat> bgrList = new java.util.ArrayList<>();
        bgrList.add(b);
        bgrList.add(g);
        bgrList.add(r);
        Mat bgrMat = new Mat();
        Core.merge(bgrList, bgrMat);

        // 取反 alpha 通道
        Mat notA = new Mat();
        Core.bitwise_not(a, notA);
        // 将单通道 notA 转为三通道
        Imgproc.cvtColor(notA, notA, Imgproc.COLOR_GRAY2BGR);

        // 使用 alpha 通道作为 mask 做与运算
        Mat masked = new Mat();
        Core.bitwise_and(bgrMat, bgrMat, masked, a);

        // 与取反的 alpha 通道进行加运算
        Mat result = new Mat();
        Core.add(masked, notA, result);

        // 释放中间 Mat 对象
        for (Mat channel : rgbaChannels) {
            channel.release();
        }
        bgrMat.release();
        notA.release();
        masked.release();

        return result;
    }

    /**
     * 校验文件是否存在
     */
    private void verifyExist(String filePath) throws LoadImageError {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new LoadImageError("文件不存在：" + filePath);
        }
    }

    /**
     * 将 BufferedImage 转为 Mat
     * @param bi 传入的 BufferedImage
     * @return 转换后的 Mat
     */
    private Mat bufferedImageToMat(BufferedImage bi) {
        // 先转换为 TYPE_3BYTE_BGR 类型（OpenCV 默认是 BGR）
        if (bi.getType() != BufferedImage.TYPE_3BYTE_BGR) {
            BufferedImage convertedImg = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            Graphics2D g = convertedImg.createGraphics();
            g.drawImage(bi, 0, 0, null);
            g.dispose();
            bi = convertedImg;
        }

        byte[] data = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
        Mat mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC3);
        mat.put(0, 0, data);
        return mat;
    }
}