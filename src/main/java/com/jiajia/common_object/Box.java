package com.jiajia.common_object;

public class Box
{
  private float[] linePosition;
  private float score;
  
  public float[] getLinePosition() {
     return this.linePosition;
  }
  
  public float getScore() {
     return this.score;
  }
  
  public void setLinePosition(float[] linePosition) {
     this.linePosition = linePosition;
  }
  
  public Box(float[] linePosition, float score) {
     this.linePosition = linePosition;
     this.score = score;
  }

  public String toString() {
     return "lineposition: " + this.linePosition[0] + "," + this.linePosition[1] + "," + this.linePosition[2] + "," + this.linePosition[3] + "," + this.linePosition[4] + "," + this.linePosition[5] + "," + this.linePosition[6] + "," + this.linePosition[7]
             + ", score: " + getScore();
  }
}
