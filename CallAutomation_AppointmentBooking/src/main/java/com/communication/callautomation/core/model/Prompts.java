package com.communication.callautomation.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public enum Prompts {
    CHOICE1("choice1.wav"),
    CHOICE2("choice2.wav"),
    CHOICE3("choice3.wav"),
    GOODBYE("goodbye.wav"),
    MAINMENU("mainmenu.wav"),
    RECORDINGSTARTED("recordingstarted.wav"),
    RETRY("retry.wav");

    private final String mediafile;
}
