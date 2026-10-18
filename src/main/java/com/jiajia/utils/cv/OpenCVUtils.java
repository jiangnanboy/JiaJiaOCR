package com.jiajia.utils.cv;

import com.jiajia.common_object.Box;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.List;

/**
 * @author jiangnanboy
 * @date 2024/7/1 19:23
 */
public class OpenCVUtils {

    /**
     * perspective transform
     * @param src
     * @param srcMat
     * @param dstMat
     * @return
     */
    public static Mat perspectiveTransform(Mat src, Mat srcMat, Mat dstMat) {
        Mat dst = src.clone();
        Mat warpMat = Imgproc.getPerspectiveTransform(srcMat, dstMat);
        Imgproc.warpPerspective(src, dst, warpMat, dst.size());
        warpMat.release();
        return dst;
    }

    /**
     * mat to bufferedimage
     * @param mat
     * @return
     */
    public static BufferedImage mat2Image(Mat mat) {
        Mat cloneMat = mat.clone();
        try {
            int width = cloneMat.width();
            int height = cloneMat.height();
            byte[] data = new byte[width * height * (int) cloneMat.elemSize()];
            Imgproc.cvtColor(cloneMat, cloneMat, 4);
            cloneMat.get(0, 0, data);
            BufferedImage ret = new BufferedImage(width, height, 5);
            ret.getRaster().setDataElements(0, 0, width, height, data);
            return ret;
        } finally {
            cloneMat.release();
        }
    }

    /**
     * mat to bufferedimage
     * @param frame
     * @return
     */
    public static BufferedImage matToBufferedImage(Mat frame) {
        int type = 0;
        if (frame.channels() == 1) {
            type = BufferedImage.TYPE_BYTE_GRAY;
        } else if (frame.channels() == 3) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        BufferedImage image = new BufferedImage(frame.width(), frame.height(), type);
        java.awt.image.WritableRaster raster = image.getRaster();
        DataBufferByte dataBuffer = (DataBufferByte) raster.getDataBuffer();
        byte[] data = dataBuffer.getData();
        frame.get(0, 0, data);
        return image;
    }

    /**
     * bufferedimage to mat
     * @param img
     * @return
     */
    public static Mat image2Mat(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        byte[] data = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
        Mat mat = new Mat(height, width, CvType.CV_8UC3);
        mat.put(0, 0, data);
        return mat;
    }

    public static Mat getRotateCropImage(Mat imgMat, Box box) {
        float[] pointsArr = box.getLinePosition();
        float x0 = pointsArr[0];
        float y0 = pointsArr[1];
        float x1 = pointsArr[2];
        float y1 = pointsArr[3];
        float x2 = pointsArr[4];
        float y2 = pointsArr[5];
        float x3 = pointsArr[6];
        float y3 = pointsArr[7];


        Point[] points = new Point[4];
        points[0] = new Point(x0, y0);
        points[1] = new Point(x1, y1);
        points[2] = new Point(x2, y2);
        points[3] = new Point(x3, y3);
        // 计算裁剪区域的宽度
        double widthTop = distance(points[0], points[1]);
        double widthBottom = distance(points[2], points[3]);
        int imgCropWidth = (int) Math.max(widthTop, widthBottom);

        // 计算裁剪区域的高度
        double heightLeft = distance(points[0], points[3]);
        double heightRight = distance(points[1], points[2]);
        int imgCropHeight = (int) Math.max(heightLeft, heightRight);

        // 定义标准的裁剪区域四个顶点
        MatOfPoint2f ptsStd = new MatOfPoint2f(
                new Point(0, 0),
                new Point(imgCropWidth, 0),
                new Point(imgCropWidth, imgCropHeight),
                new Point(0, imgCropHeight)
        );

        // 将检测框顶点转换为MatOfPoint2f
        MatOfPoint2f ptsSrc = new MatOfPoint2f(points);

        // 计算透视变换矩阵
        Mat perspectiveTransform = Imgproc.getPerspectiveTransform(ptsSrc, ptsStd);
        ptsStd.release();
        ptsSrc.release();

        // 执行透视变换裁剪图像
        Mat dstImg = new Mat();
        Imgproc.warpPerspective(imgMat, dstImg, perspectiveTransform,
                new Size(imgCropWidth, imgCropHeight),
                Imgproc.INTER_CUBIC, Core.BORDER_REPLICATE);

        perspectiveTransform.release();

        // 判断裁剪后的图像是否需要旋转
        if (((double) dstImg.rows() / dstImg.cols()) >= 1.5) {
            Mat rotatedImg = new Mat();
            Core.rotate(dstImg, rotatedImg, Core.ROTATE_90_CLOCKWISE);
            dstImg.release();
            dstImg = rotatedImg;
        }

        return dstImg;
    }
    private static double distance(Point p1, Point p2) {
        return Math.hypot(p1.x - p2.x, p1.y - p2.y);
    }

    public static float distance(float[] point1, float[] point2) {
        float disX = point1[0] - point2[0];
        float disY = point1[1] - point2[1];
        float dis = (float) Math.sqrt(disX * disX + disY * disY);
        return dis;
    }

    public static Mat toMat(List<Point> points) {
        Mat mat = new Mat(points.size(), 2, CvType.CV_32F);
        for (int i = 0; i < points.size(); i++) {
            Point point = points.get(i);
            mat.put(i, 0, (float) point.x);
            mat.put(i, 1, (float) point.y);
        }
        return mat;
    }


}
