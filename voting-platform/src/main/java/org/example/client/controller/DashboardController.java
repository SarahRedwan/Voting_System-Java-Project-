package org.example.client.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.example.client.core.Candidate;
import org.example.client.core.DataManager;
import org.example.client.core.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class DashboardController {

    @FXML private Label timerLabel;
    @FXML private ListView<String> candidateListView;
    @FXML private Label candidateNameLabel;
    @FXML private Label partyLabel;
    @FXML private Label manifestoSummaryLabel;
    @FXML private Hyperlink manifestoLink;
    @FXML private Label videoSummaryLabel;
    @FXML private Hyperlink videoLink;
    @FXML private Label voterSessionLabel;

    private List<Candidate> candidateList;
    private Candidate selectedCandidate;

    @FXML
    private void handleGoToVoting() {
        System.out.println("Redirecting user session to digital voting booth window...");
        // Perfect flat path location with a clean application header title string
        SceneManager.switchScene("VotingView.fxml", "Voter Dashboard Booth");
    }

    @FXML
    public void initialize() {
        timerLabel.setText("02h : 45m : 12s");
        voterSessionLabel.setText("Authenticated Voter Session Active");

        candidateList = DataManager.getRankedCandidates();
        ObservableList<String> candidates = FXCollections.observableArrayList();
        for (Candidate candidate : candidateList) {
            candidates.add(candidate.getName() + " (" + candidate.getOffice() + ")");
        }
        candidateListView.setItems(candidates);

        candidateListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                updateCampaignZone(newValue);
            }
        });
    }

    private void updateCampaignZone(String candidateInfo) {
        String candidateName = candidateInfo.split(" \\(")[0];
        selectedCandidate = candidateList.stream()
                .filter(candidate -> candidate.getName().equals(candidateName))
                .findFirst()
                .orElse(null);

        if (selectedCandidate == null) {
            return;
        }

        candidateNameLabel.setText(selectedCandidate.getName());
        partyLabel.setText("Running for: " + selectedCandidate.getOffice());

        if (selectedCandidate.getManifestoUrl() != null && !selectedCandidate.getManifestoUrl().isBlank()) {
            manifestoLink.setText("Open manifesto link");
            manifestoLink.setDisable(false);
            manifestoSummaryLabel.setText("Click the link below to view this candidate's manifesto.");
        } else if (selectedCandidate.getManifestoPdfPath() != null && !selectedCandidate.getManifestoPdfPath().isBlank()) {
            manifestoLink.setText("Open manifesto PDF");
            manifestoLink.setDisable(false);
            manifestoSummaryLabel.setText("Click the link below to open the manifesto PDF.");
        } else {
            manifestoLink.setText("No manifesto available");
            manifestoLink.setDisable(true);
            manifestoSummaryLabel.setText("This candidate has not provided a manifesto link yet.");
        }

        if (selectedCandidate.getVideoUrl() != null && !selectedCandidate.getVideoUrl().isBlank()) {
            videoLink.setText("Watch campaign video");
            videoLink.setDisable(false);
            videoSummaryLabel.setText("Click the link below to watch the campaign video.");
        } else {
            videoLink.setText("No video available");
            videoLink.setDisable(true);
            videoSummaryLabel.setText("This candidate has not provided a video link yet.");
        }
    }

    @FXML
    private void handleOpenManifesto() {
        if (selectedCandidate == null) {
            return;
        }
        String manifestoUrl = selectedCandidate.getManifestoUrl();
        if (manifestoUrl != null && !manifestoUrl.isBlank()) {
            openLink(manifestoUrl);
            return;
        }
        String manifestoPdfPath = selectedCandidate.getManifestoPdfPath();
        if (manifestoPdfPath != null && !manifestoPdfPath.isBlank()) {
            openLink(new File(manifestoPdfPath).toURI().toString());
        }
    }

    @FXML
    private void handleOpenVideo() {
        if (selectedCandidate == null) {
            return;
        }
        String videoUrl = selectedCandidate.getVideoUrl();
        if (videoUrl != null && !videoUrl.isBlank()) {
            openLink(videoUrl);
        }
    }

    private void openLink(String link) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(link));
            }
        } catch (IOException | URISyntaxException e) {
            System.err.println("[DashboardController] Failed to open link: " + e.getMessage());
        }
    }
}