package org.example.client.core;

public class User {
    private final int id;
    private final String username;
    private final String fullName;
    private final String role;
    private final String office;

    public User(int id, String username, String fullName, String role, String office) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.office = office;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRole() {
        return role;
    }

    public String getOffice() {
        return office;
    }
}
