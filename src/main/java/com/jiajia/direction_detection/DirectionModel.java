 package com.jiajia.direction_detection;
 
 import ai.onnxruntime.OrtException;
 import com.jiajia.common_onnx.Model;

 public class DirectionModel extends Model
 {
     public DirectionModel(byte[] clsBytes) throws OrtException {
         super(clsBytes);
     }
 }
