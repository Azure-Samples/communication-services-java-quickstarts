package com.acsrecording.api.Models;

import java.util.Map;
import java.util.HashMap;
import com.azure.communication.callautomation.models.RecordingContent;
import com.azure.communication.callautomation.models.RecordingChannel;
import com.azure.communication.callautomation.models.RecordingFormat;

public class Mapper {
    static Map<String, RecordingContent> recContentMap = new HashMap<String, RecordingContent>() {
        {
            put("audiovideo", RecordingContent.AUDIO_VIDEO);
            put("audio", RecordingContent.AUDIO);
        }
    };

    static Map<String, RecordingChannel> recChannelMap = new HashMap<String, RecordingChannel>() {
        {
            put("mixed", RecordingChannel.MIXED);
            put("unmixed", RecordingChannel.UNMIXED);
        }
    };

    static Map<String, RecordingFormat> recFormatMap = new HashMap<String, RecordingFormat>() {
        {
            put("mp3", RecordingFormat.MP3);
            put("mp4", RecordingFormat.MP4);
            put("wav", RecordingFormat.WAV);
        }
    };

    public static Map<String, RecordingContent> getRecordingContentMap() {
        return recContentMap;
    }

    public static Map<String, RecordingChannel> getRecordingChannelMap() {
        return recChannelMap;
    }

    public static Map<String, RecordingFormat> getRecordingFormatMap() {
        return recFormatMap;
    }
}