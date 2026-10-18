 package com.jiajia.text_detection.dbnet;
 
 import ai.onnxruntime.OrtException;
 import com.jiajia.common_onnx.Model;
 
 public class DbDetModel extends Model
 {
     public DbDetModel(byte[] detBytes) throws OrtException {
         super(detBytes);
     }
 }
