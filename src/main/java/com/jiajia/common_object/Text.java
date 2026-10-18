 package com.jiajia.common_object;

 public class Text
 {
   private String text;
   
   public String getText() {
     return this.text;
   }
   
   public Text(String text) {
     this.text = text;
   }
   
   public String toString() {
     return "text: " + getText();
   }
 }
