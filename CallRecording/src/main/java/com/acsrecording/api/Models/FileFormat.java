package com.acsrecording.api.Models;

public class FileFormat {
    private final static String json = "json";
    private final static String mp4 = "mp4";
    private final static String mp3 = "mp3";
    private final static String wav = "wav";

    public static String getJson() {
        return json;
    }

    public static String getMp4() {
        return mp4;
    }

    public static String getMp3() {
        return mp3;
    }

    public static String getWav() {
        return wav;
    }
}
