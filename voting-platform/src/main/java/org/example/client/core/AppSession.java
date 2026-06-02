package org.example.client.core;

public final class AppSession {

    private static volatile String username = "guest";
    private static volatile String role = "guest";
    private static volatile String pendingRole = "voter";

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

    /** Holds the role chosen on the RoleSelection screen before registration completes. */
    public static String getPendingRole() {
        return pendingRole;
    }

    public static void setPendingRole(String value) {
        pendingRole = value;
    }

    public static void clear() {
        username = "guest";
        role = "guest";
        pendingRole = "voter";
    }
}