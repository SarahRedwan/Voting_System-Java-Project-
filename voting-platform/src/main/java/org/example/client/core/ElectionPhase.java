package org.example.client.core;

public enum ElectionPhase {
    NOT_STARTED,
    ACTIVE,
    ENDED;

    public static ElectionPhase fromString(String value) {
        if (value == null) {
            return NOT_STARTED;
        }
        try {
            return ElectionPhase.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return NOT_STARTED;
        }
    }
}
