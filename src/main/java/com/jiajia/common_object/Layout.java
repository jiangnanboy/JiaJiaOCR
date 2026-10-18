 package com.jiajia.common_object;

 
 public class Layout
 {
   private String label;
   private int labelIndex;
   private float[] bbox;
   private float confidence;
   
   public String getLabel() {
     return this.label;
   }
   
   public int getLabelIndex() {
     return this.labelIndex;
   }
   public float[] getBbox() {
     return this.bbox;
   }
   
   public float getConfidence() {
     return this.confidence;
   }
   
   public Layout(String label, int labelIndex, float[] bbox, float confidence) {
     this.label = label;
     this.labelIndex = labelIndex;
     this.bbox = bbox;
     this.confidence = confidence;
   }
 }
