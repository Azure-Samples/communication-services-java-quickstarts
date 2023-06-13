package com.acsrecording.api.Models;

public class RecordingInfo {
  private String contentType;
  private String channelType;
  private String format;
  AudioConfiguration AudioConfigurationObject;
  VideoConfiguration VideoConfigurationObject;

  public String getContentType() {
    return contentType;
  }

  public String getChannelType() {
    return channelType;
  }

  public String getFormat() {
    return format;
  }

  public AudioConfiguration getAudioConfiguration() {
    return AudioConfigurationObject;
  }

  public VideoConfiguration getVideoConfiguration() {
    return VideoConfigurationObject;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public void setChannelType(String channelType) {
    this.channelType = channelType;
  }

  public void setFormat(String format) {
    this.format = format;
  }

  public void setAudioConfiguration(AudioConfiguration audioConfigurationObject) {
    this.AudioConfigurationObject = audioConfigurationObject;
  }

  public void setVideoConfiguration(VideoConfiguration videoConfigurationObject) {
    this.VideoConfigurationObject = videoConfigurationObject;
  }
}
