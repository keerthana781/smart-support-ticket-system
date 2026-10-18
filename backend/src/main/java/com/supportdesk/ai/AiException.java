package com.supportdesk.ai;

/** Anything that goes wrong while talking to an AI provider (network, timeout, bad response). */
public class AiException extends RuntimeException {

    public AiException(String message) {
        super(message);
    }

    public AiException(String message, Throwable cause) {
        super(message, cause);
    }
}
