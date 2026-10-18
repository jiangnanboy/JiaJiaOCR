 package com.jiajia.common_object;
 
 public class Direction {
   private String name;
   private float prob;
   
   public Direction(String name, float prob) {
     this.name = name;
     this.prob = prob;
   }
   
   public String getName() {
     return this.name;
   }
   
   public void setName(String name) {
     this.name = name;
   }
   
   public float getProb() {
     return this.prob;
   }
   
   public void setProb(float prob) {
     this.prob = prob;
   }
   
   public String toString() {
     return String.format("name: %s, prob: %f", new Object[] { this.name, Float.valueOf(this.prob) });
   }
 }
