package org.example.client.core;

import java.io.File;

public class PendingMaterial {
    private String candidateName;
    private File pdfFile;
    private File videoFile;
    private String videoUrl;
    private String status; // "PENDING", "APPROVED", "REJECTED"

    public PendingMaterial(String candidateName, File pdfFile, File videoFile, String videoUrl) {
        this.candidateName = candidateName;
        this.pdfFile = pdfFile;
        this.videoFile = videoFile;
        this.videoUrl = videoUrl;
        this.status = "PENDING";
    }

    // Getters and Setters
    public String getCandidateName() { return candidateName; }
    public File getPdfFile() { return pdfFile; }
    public File getVideoFile() { return videoFile; }
    public String getVideoUrl() { return videoUrl; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "Candidate: " + candidateName + " | Status: " + status +
                " (PDF: " + (pdfFile != null ? pdfFile.getName() : "None") +
                ", Video File: " + (videoFile != null ? videoFile.getName() : "None") +
                ", Video URL: " + (videoUrl != null && !videoUrl.isEmpty() ? videoUrl : "None") + ")";
    }
}