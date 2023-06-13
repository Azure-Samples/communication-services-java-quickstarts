package com.acsrecording.api.Models;

public class AudioConfiguration {
  private float sampleRate;
  private float bitRate;
  private float channels;

  public float getSampleRate() {
    return sampleRate;
  }

  public float getBitRate() {
    return bitRate;
  }

  public float getChannels() {
    return channels;
  }

  public void setSampleRate(float sampleRate) {
    this.sampleRate = sampleRate;
  }

  public void setBitRate(float bitRate) {
    this.bitRate = bitRate;
  }

  public void setChannels(float channels) {
    this.channels = channels;
  }
}
