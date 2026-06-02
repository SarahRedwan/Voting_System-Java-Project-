package org.example.client.core;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

public final class RegistrationRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 15 * 60 * 1000L;
    private static final ConcurrentHashMap<String, Deque<Long>> ATTEMPTS = new ConcurrentHashMap<>();

    private RegistrationRateLimiter() {
    }

    public static boolean allowAttempt(String key) {
        String normalizedKey = key == null ? "global" : key.trim().toLowerCase();
        long now = System.currentTimeMillis();
        Deque<Long> timestamps = ATTEMPTS.computeIfAbsent(normalizedKey, ignored -> new ArrayDeque<>());

        synchronized (timestamps) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > WINDOW_MS) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= MAX_ATTEMPTS) {
                return false;
            }
            timestamps.addLast(now);
            return true;
        }
    }

    public static String blockedMessage() {
        return "Too many registration attempts. Please wait 15 minutes and try again.";
    }
}
