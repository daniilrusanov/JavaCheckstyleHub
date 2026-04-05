package com.checkstylehub.analyzer.dto;

import lombok.Data;
import lombok.AllArgsConstructor;

/**
 * Data Transfer Object for log messages sent via WebSocket.
 * Contains log level (INFO/ERROR) and message text.
 */
@Data
@AllArgsConstructor
public class LogMessageDto {

    private String level;
    private String message;
}
