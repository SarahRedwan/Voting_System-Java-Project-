package org.example.client.core;

public final class CandidateApplication {
    private final String username;
    private final String fullName;
    private final String party;
    private final String position;
    private final String phoneNumber;
    private final String fanNumber;

    public CandidateApplication(String username, String fullName, String party, String position,
                                String phoneNumber, String fanNumber) {
        this.username = username;
        this.fullName = fullName;
        this.party = party;
        this.position = position;
        this.phoneNumber = phoneNumber;
        this.fanNumber = fanNumber;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getParty() {
        return party;
    }

    public String getPosition() {
        return position;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getFanNumber() {
        return fanNumber;
    }

    @Override
    public String toString() {
        return fullName + " (" + username + ") | " + party + " | " + position
                + " | Phone: " + phoneNumber + " | FAN: " + fanNumber;
    }
}
