package org.example.client.core;

public class Candidate {
    private final int id;
    private final String name;
    private final String office;
    private final String status;
    private final int votes;
    private final String manifestoUrl;
    private final String manifestoPdfPath;
    private final String videoUrl;

    public Candidate(int id, String name, String office, String status, int votes,
                     String manifestoUrl, String manifestoPdfPath, String videoUrl) {
        this.id = id;
        this.name = name;
        this.office = office;
        this.status = status;
        this.votes = votes;
        this.manifestoUrl = manifestoUrl;
        this.manifestoPdfPath = manifestoPdfPath;
        this.videoUrl = videoUrl;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getOffice() {
        return office;
    }

    public String getStatus() {
        return status;
    }

    public int getVotes() {
        return votes;
    }

    public String getManifestoUrl() {
        return manifestoUrl;
    }

    public String getManifestoPdfPath() {
        return manifestoPdfPath;
    }

    public String getVideoUrl() {
        return videoUrl;
    }
}
