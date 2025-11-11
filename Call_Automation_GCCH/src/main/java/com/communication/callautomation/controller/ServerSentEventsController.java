package com.communication.callautomation.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.Set;
import java.io.IOException;

@RestController
public class ServerSentEventsController {

    private final Set<SseEmitter> emitters = new CopyOnWriteArraySet<>();

    @GetMapping("/api/logs/stream")
    public SseEmitter streamLogs() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((ex) -> emitters.remove(emitter));
        
        emitters.add(emitter);
        
        try {
            emitter.send(SseEmitter.event()
                .name("connection")
                .data("Server-Sent Events connection established"));
        } catch (IOException e) {
            emitters.remove(emitter);
        }
        
        return emitter;
    }
    
    public void broadcastLog(String logMessage) {
        emitters.removeIf(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                    .name("log")
                    .data(logMessage));
                return false;
            } catch (IOException e) {
                return true;
            }
        });
    }
}