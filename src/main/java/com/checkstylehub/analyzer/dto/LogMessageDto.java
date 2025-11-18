package com.checkstylehub.analyzer.dto;

/**
 * Data Transfer Object for log messages sent via WebSocket.
 * Contains log level (INFO/ERROR) and message text.
 */
public class LogMessageDto {

    private String level;
    private String message;

    public LogMessageDto(String level, String message) {
        this.level = level;
        this.message = message;
    }

    // Getters and Setters
    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
