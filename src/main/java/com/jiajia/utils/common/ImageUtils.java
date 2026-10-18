package com.jiajia.utils.common;

import com.jiajia.common_object.Box;
import com.jiajia.common_object.Boxes;
import com.jiajia.common_object.Layout;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ImageUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageUtils.class);

    public static void saveImage(BufferedImage img, String name, String path) {
        java.nio.file.Path outputDir = java.nio.file.Paths.get(path);
        try {
            ImageIO.write(img, "png", outputDir.resolve(name).toFile());
        } catch (IOException e) {
            LOGGER.error("Failed to save image: {}", name, e);
        }
    }

    public static void drawPredictions(Mat img, Boxes detBoxes) {
        List<Box> boxList = detBoxes.getBoxes();
        for(Box box : boxList) {
            Point[] points = new Point[4];
            points[0] = new Point(box.getLinePosition()[0], box.getLinePosition()[1]);
            points[1] = new Point(box.getLinePosition()[2], box.getLinePosition()[3]);
            points[2] = new Point(box.getLinePosition()[4], box.getLinePosition()[5]);
            points[3] = new Point(box.getLinePosition()[6], box.getLinePosition()[7]);
            MatOfPoint polygon = new MatOfPoint(points);
            List<MatOfPoint> matOfPointList = CollectionUtil.newArrayList();
            matOfPointList.add(polygon);
            Imgproc.polylines(img,
                    matOfPointList,
                    true,
                    new Scalar(0, 0, 255),
                    1
            );
            polygon.release();
        }
    }

    public static void drawPredictions(Mat img, List<Layout> detLayout) {
        for(Layout layout : detLayout) {
            int x0 = (int) layout.getBbox()[0];
            int y0 = (int) layout.getBbox()[1];
            int x1 = (int) layout.getBbox()[2];
            int y1 = (int) layout.getBbox()[3];

            Imgproc.rectangle(img,
                    new Point(x0, y0),
                    new Point(x1, y1),
                    new Scalar(0, 0, 255),
                    2
            );
            int textX0 = x0 + 5;
            int textY0 = y0 - 10;
            textX0 = Math.max(0, textX0);
            textY0 = Math.max(0, textY0);
            Imgproc.putText(img, layout.getLabel(), new Point(textX0, textY0), Imgproc.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(0, 255, 0), 2, Imgproc.LINE_AA);
        }
    }

    public static float[] whc2cwh(float[] src) {
        float[] chw = new float[src.length];
        int j = 0;
        for (int ch = 0; ch < 3; ch++) {
            for (int i = ch; i < src.length; i += 3) {
                chw[j] = src[i];
                j++;
            }
        }
        return chw;
    }

    public static void xywh2xyxy(float[] bbox) {
        float x = bbox[0];
        float y = bbox[1];
        float w = bbox[2];
        float h = bbox[3];
        bbox[0] = x - w * 0.5f;
        bbox[1] = y - h * 0.5f;
        bbox[2] = x + w * 0.5f;
        bbox[3] = y + h * 0.5f;
    }

    public static List<float[]> nonMaxSuppression(List<float[]> bboxes, float iouThreshold) {
        List<float[]> bestBboxes = CollectionUtil.newArrayList();
        bboxes.sort(Comparator.comparing(a -> a[4]));
        while (!bboxes.isEmpty()) {
            float[] bestBbox = bboxes.remove(bboxes.size() - 1);
            bestBboxes.add(bestBbox);
            bboxes = bboxes.stream().filter(a -> computeIOU(a, bestBbox) < iouThreshold).collect(Collectors.toList());
        }
        return bestBboxes;
    }

    public static float computeIOU(float[] box1, float[] box2) {
        float area1 = (box1[2] - box1[0]) * (box1[3] - box1[1]);
        float area2 = (box2[2] - box2[0]) * (box2[3] - box2[1]);

        float left = Math.max(box1[0], box2[0]);
        float top = Math.max(box1[1], box2[1]);
        float right = Math.min(box1[2], box2[2]);
        float bottom = Math.min(box1[3], box2[3]);

        float interArea = Math.max(right - left, 0) * Math.max(bottom - top, 0);
        float unionArea = area1 + area2 - interArea;
        return Math.max(interArea / unionArea, 1e-8f);
    }

    public static MatOfPoint2f matOfPointToMatOfPoint2f(MatOfPoint src) {
        MatOfPoint2f dst = new MatOfPoint2f();
        src.convertTo(dst, CvType.CV_32F);
        return dst;
    }

    public static MatOfPoint matOfPoint2fToMatOfPoint(MatOfPoint2f src) {
        MatOfPoint dst = new MatOfPoint();
        src.convertTo(dst, CvType.CV_32S);
        return dst;
    }

    public static MatOfPoint matToMatOfPoint(Mat src) {
        MatOfPoint dst = new MatOfPoint();
        src.convertTo(dst, CvType.CV_32S);
        return dst;
    }

    public static Mat bufferedImageToMat(BufferedImage bufferedImage) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", byteArrayOutputStream);
        byte[] imageBytes = byteArrayOutputStream.toByteArray();
        MatOfByte matOfByte = new MatOfByte(imageBytes);
        try {
            return Imgcodecs.imdecode(matOfByte, Imgcodecs.IMREAD_UNCHANGED);
        } finally {
            matOfByte.release();
        }
    }

    public static Mat readMatFromFile(String filePath) {
        return Imgcodecs.imread(filePath);
    }
}
