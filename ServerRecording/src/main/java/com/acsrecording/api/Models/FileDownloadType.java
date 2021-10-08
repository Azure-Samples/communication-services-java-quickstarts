package com.acsrecording.api.Models;

public class FileDownloadType {
    private final static String recording = "recording";
    private final static String metadata = "metadata";

    public static String getRecording() {
        return recording;
    }

    public static String getMetadata() {
        return metadata;
    }
}
