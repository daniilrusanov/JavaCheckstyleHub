package com.checkstylehub.analyzer.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

/**
 * WebSocket controller for handling real-time messages from clients.
 * Currently, not required as logs are sent directly from the service.
 * Can be extended for client-initiated actions (e.g., cancel analysis).
 */
@Controller
public class LogWebsocketController {

    @MessageMapping("/hello")
    @SendTo("/topic/greetings")
    public String greeting(String message) throws Exception {
        Thread.sleep(1000);
        return "Hello! You sent: " + message;
    }
}
