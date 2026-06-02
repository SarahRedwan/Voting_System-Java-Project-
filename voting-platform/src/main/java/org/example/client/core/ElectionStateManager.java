package org.example.client.core;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.function.Consumer;

public final class ElectionStateManager {

    private static volatile ElectionPhase lastBroadcastPhase;
    private static volatile boolean schedulerRunning = false;
    private static Consumer<String> broadcaster;

    private ElectionStateManager() {
    }

    public static void initialize(Consumer<String> messageBroadcaster) {
        ElectionScheduleDAO.ensureTable();
        broadcaster = messageBroadcaster;
        if (!schedulerRunning) {
            schedulerRunning = true;
            Thread scheduler = new Thread(ElectionStateManager::schedulerLoop, "election-phase-scheduler");
            scheduler.setDaemon(true);
            scheduler.start();
        }
    }

    public static synchronized ElectionStateSnapshot getCurrentState() {
        ElectionSchedule schedule = ElectionScheduleDAO.load();
        LocalDateTime now = LocalDateTime.now();
        return computeState(schedule, now);
    }

    public static boolean isVotingAllowed() {
        return getCurrentState().getPhase() == ElectionPhase.ACTIVE;
    }

    public static void setSchedule(LocalDateTime startTime, LocalDateTime endTime) throws SQLException {
        if (startTime != null && endTime != null && !endTime.isAfter(startTime)) {
            throw new SQLException("End time must be after start time.");
        }
        ElectionScheduleDAO.saveSchedule(startTime, endTime);
        broadcastNow();
    }

    public static void startNow(long durationSeconds) throws SQLException {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = durationSeconds > 0 ? now.plusSeconds(durationSeconds) : null;
        ElectionScheduleDAO.setManualMode("FORCE_ACTIVE", now, end);
        broadcastNow();
    }

    public static void stopNow() throws SQLException {
        ElectionScheduleDAO.setManualMode("FORCE_STOPPED", null, LocalDateTime.now());
        broadcastNow();
    }

    public static void resetToScheduled() throws SQLException {
        ElectionSchedule schedule = ElectionScheduleDAO.load();
        ElectionScheduleDAO.setManualMode("SCHEDULED", schedule.getStartTime(), schedule.getEndTime());
        broadcastNow();
    }

    private static ElectionStateSnapshot computeState(ElectionSchedule schedule, LocalDateTime now) {
        String manual = schedule.getManualMode();
        LocalDateTime start = schedule.getStartTime();
        LocalDateTime end = schedule.getEndTime();

        if ("FORCE_STOPPED".equals(manual)) {
            return new ElectionStateSnapshot(ElectionPhase.ENDED, 0, start, end, manual);
        }

        if ("FORCE_ACTIVE".equals(manual)) {
            if (end != null && !now.isBefore(end)) {
                return new ElectionStateSnapshot(ElectionPhase.ENDED, 0, start, end, manual);
            }
            long secondsLeft = end == null ? 0 : Math.max(0, java.time.Duration.between(now, end).getSeconds());
            return new ElectionStateSnapshot(ElectionPhase.ACTIVE, secondsLeft, start, end, manual);
        }

        if (start == null && end == null) {
            return new ElectionStateSnapshot(ElectionPhase.NOT_STARTED, 0, null, null, manual);
        }

        if (start != null && now.isBefore(start)) {
            long secondsUntilStart = Math.max(0, java.time.Duration.between(now, start).getSeconds());
            return new ElectionStateSnapshot(ElectionPhase.NOT_STARTED, secondsUntilStart, start, end, manual);
        }

        if (end != null && !now.isBefore(end)) {
            return new ElectionStateSnapshot(ElectionPhase.ENDED, 0, start, end, manual);
        }

        long secondsUntilEnd = end == null ? 0 : Math.max(0, java.time.Duration.between(now, end).getSeconds());
        return new ElectionStateSnapshot(ElectionPhase.ACTIVE, secondsUntilEnd, start, end, manual);
    }

    private static void schedulerLoop() {
        while (schedulerRunning) {
            try {
                ElectionStateSnapshot state = getCurrentState();
                ElectionPhase phase = state.getPhase();

                if (lastBroadcastPhase == null || lastBroadcastPhase != phase) {
                    if (phase == ElectionPhase.ACTIVE) {
                        broadcast("SYSTEM|ELECTION_STARTED");
                    } else if (phase == ElectionPhase.ENDED && lastBroadcastPhase == ElectionPhase.ACTIVE) {
                        broadcast("SYSTEM|ELECTION_STOPPED");
                    }
                    lastBroadcastPhase = phase;
                }

                broadcast(state.toSocketMessage());
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                System.err.println("Election scheduler error: " + e.getMessage());
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {
                    break;
                }
            }
        }
    }

    private static void broadcastNow() {
        ElectionStateSnapshot state = getCurrentState();
        lastBroadcastPhase = null;
        broadcast(state.toSocketMessage());
    }

    private static void broadcast(String message) {
        if (broadcaster != null) {
            broadcaster.accept(message);
        }
    }
}
