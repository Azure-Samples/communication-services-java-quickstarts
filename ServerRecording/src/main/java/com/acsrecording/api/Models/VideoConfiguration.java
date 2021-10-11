package com.acsrecording.api.Models;

public class VideoConfiguration {
  private float longerSideLength;
  private float shorterSideLength;
  private float framerate;
  private float bitRate;

  public float getLongerSideLength() {
    return longerSideLength;
  }

  public float getShorterSideLength() {
    return shorterSideLength;
  }

  public float getFramerate() {
    return framerate;
  }

  public float getBitRate() {
    return bitRate;
  }

  public void setLongerSideLength(float longerSideLength) {
    this.longerSideLength = longerSideLength;
  }

  public void setShorterSideLength(float shorterSideLength) {
    this.shorterSideLength = shorterSideLength;
  }

  public void setFramerate(float framerate) {
    this.framerate = framerate;
  }

  public void setBitRate(float bitRate) {
    this.bitRate = bitRate;
  }
}
