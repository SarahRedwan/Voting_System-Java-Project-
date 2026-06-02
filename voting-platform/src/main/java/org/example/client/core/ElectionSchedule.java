package org.example.client.core;

import java.time.LocalDateTime;

public final class ElectionSchedule {
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final String manualMode;

    public ElectionSchedule(LocalDateTime startTime, LocalDateTime endTime, String manualMode) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.manualMode = manualMode == null ? "SCHEDULED" : manualMode;
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
}
