package com.checkstylehub.analyzer.exception;

/**
 * Custom exception for Git repository access errors.
 * Thrown when repository cloning fails (e.g., private repo, invalid URL, network issues).
 */
public class RepositoryAccessException extends RuntimeException {
    public RepositoryAccessException(String message) {
        super(message);
    }

    public RepositoryAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
