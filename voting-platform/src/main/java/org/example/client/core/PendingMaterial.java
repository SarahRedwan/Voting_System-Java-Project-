package org.example.client.core;

import java.io.File;

public class PendingMaterial {
    private final long submissionId;
    private final String username;
    private final String candidateName;
    private final String pendingName;
    private final String pendingParty;
    private final String pendingPosition;
    private final String pendingDescription;
    private final File pdfFile;
    private final File videoFile;
    private final File profileImageFile;
    private final File logoImageFile;
    private String status;
    private String adminMessage;

    public PendingMaterial(PendingSubmission submission) {
        this.submissionId = submission.getId();
        this.username = submission.getUsername();
        this.candidateName = submission.getDisplayName() != null ? submission.getDisplayName() : submission.getUsername();
        this.pendingName = submission.getPendingName();
        this.pendingParty = submission.getPendingParty();
        this.pendingPosition = submission.getPendingPosition();
        this.pendingDescription = submission.getPendingDescription();
        this.pdfFile = UploadStorage.resolveFile(submission.getPendingPdfPath());
        this.videoFile = UploadStorage.resolveFile(submission.getPendingVideoPath());
        this.profileImageFile = UploadStorage.resolveFile(submission.getPendingImagePath());
        this.logoImageFile = UploadStorage.resolveFile(submission.getPendingLogoPath());
        this.status = submission.getStatus();
        this.adminMessage = submission.getAdminMessage();
    }

    public long getSubmissionId() {
        return submissionId;
    }

    public String getUsername() {
        return username;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public String getPendingName() {
        return pendingName;
    }

    public String getPendingParty() {
        return pendingParty;
    }

    public String getPendingPosition() {
        return pendingPosition;
    }

    public String getPendingDescription() {
        return pendingDescription;
    }

    public File getPdfFile() {
        return pdfFile;
    }

    public File getVideoFile() {
        return videoFile;
    }

    public File getProfileImageFile() {
        return profileImageFile;
    }

    public File getLogoImageFile() {
        return logoImageFile;
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

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(candidateName).append(" | ").append(status);
        if (pendingName != null && !pendingName.isBlank()) {
            builder.append(" | Name: ").append(pendingName);
        }
        if (pdfFile != null) {
            builder.append(" | PDF: ").append(pdfFile.getName());
        }
        if (videoFile != null) {
            builder.append(" | Video: ").append(videoFile.getName());
        }
        if (profileImageFile != null) {
            builder.append(" | Photo: ").append(profileImageFile.getName());
        }
        if (logoImageFile != null) {
            builder.append(" | Logo: ").append(logoImageFile.getName());
        }
        return builder.toString();
    }
}
