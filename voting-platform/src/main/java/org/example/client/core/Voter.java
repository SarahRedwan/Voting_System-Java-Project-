package org.example.client.core;

import java.time.LocalDate;

public final class Voter {
    private final long id;
    private final String username;
    private final String voterId;
    private final String fullName;
    private final String phoneNumber;
    private final String fanNumber;
    private final LocalDate dateOfBirth;
    private final String address;
    private final String idVerification;
    private final String approvalStatus;

    public Voter(long id, String username, String voterId, String fullName, String phoneNumber,
                 String fanNumber, LocalDate dateOfBirth, String address, String idVerification,
                 String approvalStatus) {
        this.id = id;
        this.username = username;
        this.voterId = voterId;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.fanNumber = fanNumber;
        this.dateOfBirth = dateOfBirth;
        this.address = address;
        this.idVerification = idVerification;
        this.approvalStatus = approvalStatus;
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getVoterId() {
        return voterId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getFanNumber() {
        return fanNumber;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getAddress() {
        return address;
    }

    public String getIdVerification() {
        return idVerification;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }
}
