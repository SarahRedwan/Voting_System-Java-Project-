package org.example.client.core;

import java.time.LocalDateTime;

public final class ElectionStateSnapshot {
    private final ElectionPhase phase;
    private final long countdownSeconds;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final String manualMode;

    public ElectionStateSnapshot(ElectionPhase phase, long countdownSeconds,
                                 LocalDateTime startTime, LocalDateTime endTime, String manualMode) {
        this.phase = phase;
        this.countdownSeconds = countdownSeconds;
        this.startTime = startTime;
        this.endTime = endTime;
        this.manualMode = manualMode;
    }

    public ElectionPhase getPhase() {
        return phase;
    }

    public long getCountdownSeconds() {
        return countdownSeconds;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public String getManualMode() {
        return manualMode;
    }

    public String toSocketMessage() {
        StringBuilder builder = new StringBuilder("ELECTION_PHASE|phase=").append(phase.name());
        builder.append("|seconds=").append(countdownSeconds);
        if (startTime != null) {
            builder.append("|startAt=").append(startTime);
        }
        if (endTime != null) {
            builder.append("|endAt=").append(endTime);
        }
        if (manualMode != null) {
            builder.append("|manual=").append(manualMode);
        }
        return builder.toString();
    }

    public static ElectionStateSnapshot parse(String message) {
        if (message == null || !message.startsWith("ELECTION_PHASE|")) {
            return new ElectionStateSnapshot(ElectionPhase.NOT_STARTED, 0, null, null, "SCHEDULED");
        }
        ElectionPhase phase = ElectionPhase.NOT_STARTED;
        long seconds = 0;
        LocalDateTime startAt = null;
        LocalDateTime endAt = null;
        String manual = "SCHEDULED";

        String[] parts = message.split("\\|");
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            if (part.startsWith("phase=")) {
                phase = ElectionPhase.fromString(part.substring("phase=".length()));
            } else if (part.startsWith("seconds=")) {
                try {
                    seconds = Long.parseLong(part.substring("seconds=".length()));
                } catch (NumberFormatException ignored) {
                }
            } else if (part.startsWith("startAt=")) {
                try {
                    startAt = LocalDateTime.parse(part.substring("startAt=".length()));
                } catch (Exception ignored) {
                }
            } else if (part.startsWith("endAt=")) {
                try {
                    endAt = LocalDateTime.parse(part.substring("endAt=".length()));
                } catch (Exception ignored) {
                }
            } else if (part.startsWith("manual=")) {
                manual = part.substring("manual=".length());
            }
        }
        return new ElectionStateSnapshot(phase, seconds, startAt, endAt, manual);
    }
}
