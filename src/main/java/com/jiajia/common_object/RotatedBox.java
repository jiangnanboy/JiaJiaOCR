 package com.jiajia.common_object;
 
 public class RotatedBox {
   private float[] box;
   private String text;
   private boolean rotate;
   private double prob;
   
   public RotatedBox(float[] box, boolean rotate, String text, double prob) {
     this.box = box;
     this.rotate = rotate;
     this.text = text;
     this.prob = prob;
   }
   
   public float[] getBox() {
     return this.box;
   }
   
   public void setBox(float[] box) {
     this.box = box;
   }

   public boolean getRotate() {
     return this.rotate;
   }

   public void setRotate(boolean rotate) {
     this.rotate = rotate;
   }

   public String getText() {
     return this.text;
   }
   
   public void setText(String text) {
     this.text = text;
   }
   
   public double getProb() {
     return this.prob;
   }
   
   public void setProb(double prob) {
     this.prob = prob;
   }
   
   public String toString() {
     return String.format("Box: [%f,%f,%f,%f], rotate: %s, angle: %s, prob: %f", new Object[] { Float.valueOf(this.box[0]), Float.valueOf(this.box[1]), Float.valueOf(this.box[2]), Float.valueOf(this.box[3]), this.rotate, this.text, Double.valueOf(this.prob) });
   }
 }
