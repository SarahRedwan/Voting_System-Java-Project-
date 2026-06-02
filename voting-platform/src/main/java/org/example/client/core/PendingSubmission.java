package org.example.client.core;

import java.time.LocalDateTime;

public final class PendingSubmission {
    private final long id;
    private final String username;
    private String displayName;
    private String pendingName;
    private String pendingParty;
    private String pendingDescription;
    private String pendingPosition;
    private String pendingImagePath;
    private String pendingLogoPath;
    private String pendingPdfPath;
    private String pendingVideoPath;
    private String status;
    private String adminMessage;
    private final LocalDateTime createdAt;
    private LocalDateTime reviewedAt;

    public PendingSubmission(long id, String username, String displayName, String pendingName,
                           String pendingParty, String pendingDescription, String pendingPosition,
                           String pendingImagePath, String pendingLogoPath, String pendingPdfPath,
                           String pendingVideoPath, String status, String adminMessage,
                           LocalDateTime createdAt, LocalDateTime reviewedAt) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.pendingName = pendingName;
        this.pendingParty = pendingParty;
        this.pendingDescription = pendingDescription;
        this.pendingPosition = pendingPosition;
        this.pendingImagePath = pendingImagePath;
        this.pendingLogoPath = pendingLogoPath;
        this.pendingPdfPath = pendingPdfPath;
        this.pendingVideoPath = pendingVideoPath;
        this.status = status;
        this.adminMessage = adminMessage;
        this.createdAt = createdAt;
        this.reviewedAt = reviewedAt;
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPendingName() {
        return pendingName;
    }

    public void setPendingName(String pendingName) {
        this.pendingName = pendingName;
    }

    public String getPendingParty() {
        return pendingParty;
    }

    public void setPendingParty(String pendingParty) {
        this.pendingParty = pendingParty;
    }

    public String getPendingDescription() {
        return pendingDescription;
    }

    public void setPendingDescription(String pendingDescription) {
        this.pendingDescription = pendingDescription;
    }

    public String getPendingPosition() {
        return pendingPosition;
    }

    public void setPendingPosition(String pendingPosition) {
        this.pendingPosition = pendingPosition;
    }

    public String getPendingImagePath() {
        return pendingImagePath;
    }

    public void setPendingImagePath(String pendingImagePath) {
        this.pendingImagePath = pendingImagePath;
    }

    public String getPendingLogoPath() {
        return pendingLogoPath;
    }

    public void setPendingLogoPath(String pendingLogoPath) {
        this.pendingLogoPath = pendingLogoPath;
    }

    public String getPendingPdfPath() {
        return pendingPdfPath;
    }

    public void setPendingPdfPath(String pendingPdfPath) {
        this.pendingPdfPath = pendingPdfPath;
    }

    public String getPendingVideoPath() {
        return pendingVideoPath;
    }

    public void setPendingVideoPath(String pendingVideoPath) {
        this.pendingVideoPath = pendingVideoPath;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAdminMessage() {
        return adminMessage;
    }

    public void setAdminMessage(String adminMessage) {
        this.adminMessage = adminMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
}
