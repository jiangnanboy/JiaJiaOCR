 package com.jiajia.common_object;

 import java.util.List;

 public class Boxes
 {
   private List<Box> boxes;
   
   public List<Box> getBoxes() {
     return this.boxes;
   }
   
   public void setBoxes(List<Box> boxes) {
     this.boxes = boxes;
   }
   
   public Boxes(List<Box> boxes) {
     this.boxes = boxes;
   }

   public String toString() {
     return "boxes:" + getBoxes();
   }
 }
