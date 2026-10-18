package com.jiajia.utils.cv;

import org.opencv.core.*;

public class MatNdarray {
  
  public static float[] matToCHW(Mat m) {
    return matToCHW(m, true, null, null);
  }
  
  public static float[] matToCHW(Mat m, boolean normalize) {
    return matToCHW(m, normalize, null, null);
  }
  
  public static float[] matToCHW(Mat m, boolean normalize, float[] mean, float[] std) {
    int height = m.height();
    int width = m.width();
    int channels = m.channels();
    
    Mat mat = new Mat();
    m.convertTo(mat, CvType.CV_32FC3);
    
    if (normalize) {
      Core.multiply(mat, new Scalar(1.0/255.0, 1.0/255.0, 1.0/255.0), mat);
    }
    
    if (mean != null && std != null) {
      Core.subtract(mat, new Scalar(mean[0], mean[1], mean[2]), mat);
      Core.divide(mat, new Scalar(std[0], std[1], std[2]), mat);
    }
    
    float[] chw = new float[channels * height * width];
    float[] hwc = new float[height * width * channels];
    mat.get(0, 0, hwc);
    
    int index = 0;
    for (int ch = 0; ch < channels; ch++) {
      for (int i = ch; i < hwc.length; i += channels) {
        chw[index++] = hwc[i];
      }
    }
    
    mat.release();
    return chw;
  }
  
  public static Mat rotateMat(Mat m) {
    Mat result = new Mat();
    Core.rotate(m, result, Core.ROTATE_90_CLOCKWISE);
    return result;
  }
  
}
