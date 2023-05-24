package com.communication.callautomation.controller;

import com.azure.messaging.eventgrid.EventGridEvent;
import com.communication.callautomation.exceptions.InvalidEventPayloadException;
import com.communication.callautomation.exceptions.MediaLoadingException;
import com.communication.callautomation.handler.EventsHandler;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONException;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

@RestController
@RequestMapping(ApiVersion.CURRENT + "/calls")
@Slf4j
public class CallController {
    private EventsHandler eventsHandler;

    public CallController(final EventsHandler eventsHandler) { this.eventsHandler = eventsHandler; }

    @PostMapping(path = "/incoming", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> incomingCallEndpoint(@RequestBody final String reqBody) {
        List<EventGridEvent> events;
        try{
            events = EventGridEvent.fromString(reqBody);
        } catch (IllegalArgumentException ex) {
            throw new InvalidEventPayloadException(ex.getMessage(), ex);
        }
        log.trace("Request received at CallController");
        return eventsHandler.handleIncomingEvents(events);
    }

    @PostMapping(path = "/ongoing/{contextId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> ongoingCallEndpoint(@RequestBody final String reqBody,
                                                      @PathVariable final String contextId,
                                                      @RequestParam(name = "callerId") String callerId) {
        JSONArray events;
        try{
            events = new JSONArray(reqBody);
        } catch (JSONException ex) {
            throw new InvalidEventPayloadException(ex.getMessage(), ex);
        }
        log.trace("Ongoing events received at CallController");
        return eventsHandler.handleOngoingEvents(reqBody);
    }

    @GetMapping(path = "/media/{filename}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> mediaLoadingEndpoint(@PathVariable final String filename) {
        log.trace("Media loading events received at CallController");
        return eventsHandler.handleMediaLoadingEvents(filename);
    }
}
