package com.jiajia.utils.cv;

import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class NDArrayUtils {
  
  public static MatOfPoint matToMatOfPoint(Mat mat) {
    int rows = mat.rows();
    MatOfPoint matOfPoint = new MatOfPoint();
    List<Point> list = new ArrayList<>();
    for (int i = 0; i < rows; i++) {
      list.add(new Point((float) mat.get(i, 0)[0], (float) mat.get(i, 1)[0]));
    }
    matOfPoint.fromList(list);
    return matOfPoint;
  }
  
  public static double[][] matToDoubleArray(Mat mat) {
    int rows = mat.rows();
    int cols = mat.cols();
    double[][] doubles = new double[rows][cols];
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        doubles[i][j] = mat.get(i, j)[0];
      }
    }
    return doubles;
  }
  
  public static float[][] matToFloatArray(Mat mat) {
    int rows = mat.rows();
    int cols = mat.cols();
    float[][] floats = new float[rows][cols];
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        floats[i][j] = (float) mat.get(i, j)[0];
      }
    }
    return floats;
  }
  
  public static byte[][] matToUint8Array(Mat mat) {
    int rows = mat.rows();
    int cols = mat.cols();
    byte[][] bytes = new byte[rows][cols];
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        bytes[i][j] = (byte) mat.get(i, j)[0];
      }
    }
    return bytes;
  }
  
  public static Mat floatArrayToMat(float[][] arr) {
    int rows = arr.length;
    int cols = arr[0].length;
    Mat mat = new Mat(rows, cols, CvType.CV_32F);
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        mat.put(i, j, arr[i][j]);
      }
    }
    return mat;
  }
  
  public static Mat uint8ArrayToMat(byte[][] arr) {
    int rows = arr.length;
    int cols = arr[0].length;
    Mat mat = new Mat(rows, cols, CvType.CV_8U);
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        mat.put(i, j, arr[i][j]);
      }
    }
    return mat;
  }
  
}
