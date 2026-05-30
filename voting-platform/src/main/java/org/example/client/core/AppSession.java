package org.example.client.core;

public final class AppSession {

    private static volatile String username = "guest";
    private static volatile String role = "guest";

    private AppSession() {
    }

    public static String getUsername() {
        return username;
    }

    public static void setUsername(String value) {
        username = value;
    }

    public static String getRole() {
        return role;
    }

    public static void setRole(String value) {
        role = value;
    }

    public static void clear() {
        username = "guest";
        role = "guest";
    }
}