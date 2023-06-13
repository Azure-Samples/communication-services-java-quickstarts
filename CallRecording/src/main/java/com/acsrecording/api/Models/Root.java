package com.acsrecording.api.Models;

import java.util.*;

public class Root {
  private String resourceId;
  private String callId;
  private String chunkDocumentId;
  private float chunkIndex;
  private String chunkStartTime;
  private float chunkDuration;
  ArrayList<Object> pauseResumeIntervals = new ArrayList<Object>();
  RecordingInfo recordingInfo;
  ArrayList<Object> participants = new ArrayList<Object>();

  public String getResourceId() {
    return resourceId;
  }

  public String getCallId() {
    return callId;
  }

  public String getChunkDocumentId() {
    return chunkDocumentId;
  }

  public float getChunkIndex() {
    return chunkIndex;
  }

  public String getChunkStartTime() {
    return chunkStartTime;
  }

  public float getChunkDuration() {
    return chunkDuration;
  }

  public RecordingInfo getRecordingInfo() {
    return recordingInfo;
  }

  public void setResourceId(String resourceId) {
    this.resourceId = resourceId;
  }

  public void setCallId(String callId) {
    this.callId = callId;
  }

  public void setChunkDocumentId(String chunkDocumentId) {
    this.chunkDocumentId = chunkDocumentId;
  }

  public void setChunkIndex(float chunkIndex) {
    this.chunkIndex = chunkIndex;
  }

  public void setChunkStartTime(String chunkStartTime) {
    this.chunkStartTime = chunkStartTime;
  }

  public void setChunkDuration(float chunkDuration) {
    this.chunkDuration = chunkDuration;
  }

  public void setRecordingInfo(RecordingInfo recordingInfo) {
    this.recordingInfo = recordingInfo;
  }
}
