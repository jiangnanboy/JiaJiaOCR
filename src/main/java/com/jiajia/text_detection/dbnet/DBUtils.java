package com.jiajia.text_detection.dbnet;

import com.clipper.Clipper;
import com.clipper.core.Path64;
import com.clipper.core.Paths64;
import com.clipper.core.Point64;
import com.clipper.offset.ClipperOffset;
import com.clipper.offset.EndType;
import com.clipper.offset.JoinType;
import com.jiajia.utils.common.CollectionUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DBUtils {

    public static Pair<List<float[][][]>, float[]> boxesFromBitmap(float[][] pred, Mat mat, float boxThresh) {
        int destHeight = pred.length;
        int destWidth = pred[0].length;
        int height = mat.rows();
        int width = mat.cols();

        List<MatOfPoint> contours = CollectionUtil.newArrayList();
        Mat hierarchy = new Mat();
        Imgproc.findContours(mat, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));

        int numContours = Math.min(contours.size(), 1000);
        List<float[][][]> boxList = new ArrayList<>();
        float[] scoreList = new float[numContours];
        int count = 0;

        for (int i = 0; i < numContours; i++) {
            MatOfPoint contour = contours.get(i);
            try {
                float[][] pointsArr = new float[4][2];
                int sside = getMiniBoxes(contour, pointsArr);
                if (sside < 3) {
                    continue;
                }
                float score = boxScoreFast(pred, pointsArr, destHeight, destWidth);
                if (score < boxThresh) {
                    continue;
                }

                float[][][] box = unClip(pointsArr);
                if (box == null) {
                    continue;
                }

                sside = getMiniBoxes(box, pointsArr);
                if (sside < 3 + 2) {
                    continue;
                }

                for (int p = 0; p < 4; p++) {
                    pointsArr[p][0] = Math.max(0, Math.min(destWidth, (float) Math.round(pointsArr[p][0] / (float) width * destWidth)));
                    pointsArr[p][1] = Math.max(0, Math.min(destHeight, (float) Math.round(pointsArr[p][1] / (float) height * destHeight)));
                }

                float[][][] finalBox = new float[1][4][2];
                for (int p = 0; p < 4; p++) {
                    finalBox[0][p][0] = pointsArr[p][0];
                    finalBox[0][p][1] = pointsArr[p][1];
                }
                boxList.add(finalBox);
                scoreList[count++] = score;
            } finally {
                contour.release();
            }
        }

        float[] finalScoreList = new float[count];
        System.arraycopy(scoreList, 0, finalScoreList, 0, count);
        Pair<List<float[][][]>, float[]> pair = null;
        if (boxList.size() > 0) {
            pair = Pair.of(boxList, finalScoreList);
        }
        
        hierarchy.release();
        return pair;
    }

    private static float[][][] unClip(float[][] points) {
        return unClip(points, 1.5f);
    }

    private static float[][][] unClip(float[][] points, float unclipRatio) {
        Mat mat = new Mat(4, 1, CvType.CV_32FC2);
        for (int i = 0; i < 4; i++) {
            mat.put(i, 0, new double[]{points[i][0], points[i][1]});
        }

        double area = Imgproc.contourArea(mat);
        MatOfPoint2f matOfPoint2f = new MatOfPoint2f(mat);
        double length = Imgproc.arcLength(matOfPoint2f, true);
        double distance = area * unclipRatio / length;

        ClipperOffset offset = new ClipperOffset();

        Path64 path = new Path64();
        for (int i = 0; i < 4; i++) {
            path.add(new Point64((int) Math.round(points[i][0]), (int) Math.round(points[i][1])));
        }

        Paths64 paths = new Paths64();
        paths.add(path);
        offset.AddPath(path, JoinType.Round, EndType.Polygon);

        Paths64 solution = new Paths64();
        offset.Execute(distance, solution);

        if (Optional.ofNullable(solution).isPresent() && solution.size() > 0) {
            int solutionSize = solution.get(0).size();
            float[][][] result = new float[1][solutionSize][2];
            for (int i = 0; i < solutionSize; i++) {
                Point64 point64 = solution.get(0).get(i);
                result[0][i][0] = (float) point64.x;
                result[0][i][1] = (float) point64.y;
            }
            mat.release();
            matOfPoint2f.release();
            return result;
        } else {
            mat.release();
            matOfPoint2f.release();
            return null;
        }
    }

    private static int getMiniBoxes(MatOfPoint contour, float[][] pointsArr) {
        MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
        RotatedRect rect = Imgproc.minAreaRect(contour2f);
        Mat points = new Mat();
        Imgproc.boxPoints(rect, points);

        float[][] fourPoints = new float[4][2];
        for (int row = 0; row < fourPoints.length; row++) {
            fourPoints[row][0] = (float) points.get(row, 0)[0];
            fourPoints[row][1] = (float) points.get(row, 1)[0];
        }

        float[] tmpPoint = new float[2];
        for (int i = 0; i < fourPoints.length; i++) {
            for (int j = i + 1; j < fourPoints.length; j++) {
                if (fourPoints[j][0] < fourPoints[i][0]) {
                    tmpPoint[0] = fourPoints[i][0];
                    tmpPoint[1] = fourPoints[i][1];
                    fourPoints[i][0] = fourPoints[j][0];
                    fourPoints[i][1] = fourPoints[j][1];
                    fourPoints[j][0] = tmpPoint[0];
                    fourPoints[j][1] = tmpPoint[1];
                }
            }
        }

        int index1 = 0, index2 = 1, index3 = 2, index4 = 3;

        if (fourPoints[1][1] > fourPoints[0][1]) {
            index1 = 0;
            index4 = 1;
        } else {
            index1 = 1;
            index4 = 0;
        }

        if (fourPoints[3][1] > fourPoints[2][1]) {
            index2 = 2;
            index3 = 3;
        } else {
            index2 = 3;
            index3 = 2;
        }

        pointsArr[0] = fourPoints[index1];
        pointsArr[1] = fourPoints[index2];
        pointsArr[2] = fourPoints[index3];
        pointsArr[3] = fourPoints[index4];

        int height = rect.boundingRect().height;
        int width = rect.boundingRect().width;
        int sside = Math.min(height, width);
        
        points.release();
        contour2f.release();
        return sside;
    }

    private static int getMiniBoxes(float[][][] box, float[][] pointsArr) {
        List<Point> pointList = new ArrayList<>();
        for (int i = 0; i < box[0].length; i++) {
            pointList.add(new Point(box[0][i][0], box[0][i][1]));
        }
        MatOfPoint matOfPoint = new MatOfPoint();
        matOfPoint.fromList(pointList);
        int result = getMiniBoxes(matOfPoint, pointsArr);
        matOfPoint.release();
        return result;
    }

    private static float boxScoreFast(float[][] bitmap, float[][] points, int destHeight, int destWidth) {
        int xmin = destWidth, xmax = 0, ymin = destHeight, ymax = 0;
        for (float[] point : points) {
            int x = (int) Math.floor(point[0]);
            int y = (int) Math.floor(point[1]);
            xmin = Math.min(xmin, x);
            xmax = Math.max(xmax, x);
            ymin = Math.min(ymin, y);
            ymax = Math.max(ymax, y);
        }

        xmin = Math.max(0, xmin);
        xmax = Math.min(destWidth - 1, xmax);
        ymin = Math.max(0, ymin);
        ymax = Math.min(destHeight - 1, ymax);

        int w = xmax - xmin + 1;
        int h = ymax - ymin + 1;

        Mat mask = Mat.zeros(h, w, CvType.CV_8UC1);
        MatOfPoint contour = new MatOfPoint();
        List<Point> pointList = new ArrayList<>();
        for (float[] point : points) {
            pointList.add(new Point(point[0] - xmin, point[1] - ymin));
        }
        contour.fromList(pointList);
        List<MatOfPoint> contours = new ArrayList<>();
        contours.add(contour);
        Imgproc.fillPoly(mask, contours, new Scalar(1));

        float sum = 0;
        int count = 0;
        for (int y = ymin; y <= ymax; y++) {
            for (int x = xmin; x <= xmax; x++) {
                if (mask.get(y - ymin, x - xmin)[0] > 0) {
                    sum += bitmap[y][x];
                    count++;
                }
            }
        }

        float score = count > 0 ? sum / count : 0;
        mask.release();
        contour.release();
        return score;
    }
}
