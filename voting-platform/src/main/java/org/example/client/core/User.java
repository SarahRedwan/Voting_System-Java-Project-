package org.example.client.core;

public final class User {
    private final long id;
    private final String username;
    private final String passwordHash;
    private final String role;
    private final String fullName;
    private final boolean active;

    public User(long id, String username, String passwordHash, String role, String fullName, boolean active) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.fullName = fullName;
        this.active = active;
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getRole() {
        return role;
    }

    public String getFullName() {
        return fullName;
    }

    public boolean isActive() {
        return active;
    }
}
