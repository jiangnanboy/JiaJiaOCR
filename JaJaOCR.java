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
 * @author sy
 * @date 2025/12/4 15:56
 */
public class JaJaOCR {

    public static void main(String...args) {
        String imgPath = "38.jpg";
//        generalOCRTest(imgPath);
        textLineDetText(imgPath);
    }


    public static void generalOCRTest(String imgPath) {
        JiaJiaOCR jiaJiaOCR = JiaJiaOCR.builder();
        List<Pair<Text, Box>> pairList = jiaJiaOCR.recognizeGeneralText(imgPath);
        System.out.println(pairList);

    }

    
    public static void textLineDetText(String imgPath) {
        JiaJiaOCR jiaJiaOCR = JiaJiaOCR.builder();
        Boxes boxes = jiaJiaOCR.detectTextLines(imgPath);
        System.out.println(boxes);
        Mat img = Imgcodecs.imread(imgPath);
        drawPredictions(img, boxes);
        Imgcodecs.imwrite("38textline.jpg", img);
    }

    public static Mat drawResults(Mat image, List<DetectionResult> results) {
        Mat resultImage = image.clone();
        for (DetectionResult result : results) {
            String label = result.getClassLabel() + ": " + String.format("%.2f", result.getConfidence());
            Imgproc.putText(resultImage, label, new Point(result.getRect().x, result.getRect().y - 10),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 1.0, new Scalar(0, 0, 255), 2);
            Imgproc.rectangle(resultImage, new Point(result.getRect().x, result.getRect().y),
                    new Point(result.getRect().x + result.getRect().width,
                            result.getRect().y + result.getRect().height),
                    new Scalar(0, 0, 255), 2);
        }
        return resultImage;
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
            Imgproc.putText(img, layout.getLabel(), new Point(textX0, textY0), Imgproc.FONT_HERSHEY_SIMPLEX, 0.7,new Scalar(0, 255, 0),2, Imgproc.LINE_AA);
        }
    }

    public static void drawPredictions(Mat img, Boxes detBoxes) {
        List<Box> boxList = detBoxes.getBoxes();
//        for(Box box : boxList) {
//            Imgproc.rectangle(img,
//                    new Point(box.getLinePosition()[0], box.getLinePosition()[1]),
//                    new Point(box.getLinePosition()[2], box.getLinePosition()[3]),
//                    new Scalar(1),
//                    2);
//        }

        for(Box box : boxList) {
            Point[] points = new Point[4];
            points[0] = new Point(box.getLinePosition()[0], box.getLinePosition()[1]);
            points[1] = new Point(box.getLinePosition()[2], box.getLinePosition()[3]);
            points[2] = new Point(box.getLinePosition()[4], box.getLinePosition()[5]);
            points[3] = new Point(box.getLinePosition()[6], box.getLinePosition()[7]);
            // 将Point数组转换为MatOfPoint
            MatOfPoint polygon = new MatOfPoint(points);
            List<MatOfPoint> matOfPointList = new ArrayList<MatOfPoint>();
            matOfPointList.add(polygon);
            Imgproc.polylines(img,
                    matOfPointList,
                    true,               // 是否闭合（首尾相连）
                    new Scalar(0, 0, 255), // 颜色（BGR格式：红色）
                    1
            );
        }
    }

}
