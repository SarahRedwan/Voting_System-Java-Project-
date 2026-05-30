package org.example.client.core;

import java.io.File;

public class PendingMaterial {
    private String candidateName;
    private File pdfFile;
    private File videoFile;
    private String status; // "PENDING", "APPROVED", "REJECTED"

    public PendingMaterial(String candidateName, File pdfFile, File videoFile) {
        this.candidateName = candidateName;
        this.pdfFile = pdfFile;
        this.videoFile = videoFile;
        this.status = "PENDING";
    }

    // Getters and Setters
    public String getCandidateName() { return candidateName; }
    public File getPdfFile() { return pdfFile; }
    public File getVideoFile() { return videoFile; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "Candidate: " + candidateName + " | Status: " + status +
                " (PDF: " + (pdfFile != null ? pdfFile.getName() : "None") +
                ", Video: " + (videoFile != null ? videoFile.getName() : "None") + ")";
    }
}