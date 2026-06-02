package org.example.client.core;

import java.time.LocalDateTime;

public final class CandidateProfile {
    private final long id;
    private final String username;
    private String name;
    private String party;
    private String description;
    private String imageUrl;
    private String logoUrl;
    private String position;
    private String pdfUrl;
    private String videoUrl;
    private LocalDateTime lastUpdated;

    public CandidateProfile(long id, String username, String name, String party, String description,
                            String imageUrl, String logoUrl, String position, LocalDateTime lastUpdated) {
        this(id, username, name, party, description, imageUrl, logoUrl, position, lastUpdated, null, null);
    }

    public CandidateProfile(long id, String username, String name, String party, String description,
                            String imageUrl, String logoUrl, String position, LocalDateTime lastUpdated,
                            String pdfUrl, String videoUrl) {
        this.id = id;
        this.username = username;
        this.name = name;
        this.party = party;
        this.description = description;
        this.imageUrl = imageUrl;
        this.logoUrl = logoUrl;
        this.position = position;
        this.pdfUrl = pdfUrl;
        this.videoUrl = videoUrl;
        this.lastUpdated = lastUpdated;
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParty() {
        return party;
    }

    public void setParty(String party) {
        this.party = party;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }
}
