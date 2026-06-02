package org.example.client.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import org.example.client.core.AppSession;
import org.example.client.core.CandidateProfile;
import org.example.client.core.CandidateProfileDAO;
import org.example.client.core.ElectionPhase;
import org.example.client.core.ElectionStateSnapshot;
import org.example.client.core.SceneManager;
import org.example.client.core.UploadStorage;
import org.example.client.core.VoteDAO;
import org.example.client.core.VotingSocketClient;

import java.io.File;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class DashboardController {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private Label timerLabel;
    @FXML private Label timerPrefixLabel;
    @FXML private Label electionBannerLabel;
    @FXML private Label electionScheduleLabel;
    @FXML private Label resultsSummaryLabel;
    @FXML private Label browseHintLabel;
    @FXML private ProgressBar countdownProgressBar;
    @FXML private ListView<String> candidateListView;
    @FXML private Label candidateNameLabel;
    @FXML private Label partyLabel;
    @FXML private ListView<String> documentListView;
    @FXML private MediaView campaignMediaView;
    @FXML private Label voterSessionLabel;
    @FXML private Button proceedToVoteButton;

    private final Map<String, CandidateProfile> candidateMap = new HashMap<>();
    private volatile boolean timerRunning = false;
    private Thread timerThread;
    private MediaPlayer mediaPlayer;
    private CandidateProfile selectedProfile;
    private ElectionPhase currentPhase = ElectionPhase.NOT_STARTED;
    private long lastCountdownTotal = 1;

    @FXML
    private void handleGoToVoting() {
        if (currentPhase != ElectionPhase.ACTIVE) {
            electionBannerLabel.setText(currentPhase == ElectionPhase.ENDED
                    ? "Voting has ended."
                    : "Voting has not started yet. Please wait.");
            return;
        }
        SceneManager.switchScene("VotingView.fxml", "Voter Dashboard Booth");
    }

    @FXML
    public void initialize() {
        voterSessionLabel.setText("Logged in as: " + AppSession.getUsername());

        VotingSocketClient client = VotingSocketClient.getInstance();
        if (!client.connect(AppSession.getUsername())) {
            electionBannerLabel.setText("Cannot connect to election server. Start VotingSocketServer first.");
            electionBannerLabel.setStyle("-fx-text-fill: #dc2626;");
            proceedToVoteButton.setDisable(true);
            return;
        }
        client.addListener(this::handleServerMessage);

        loadCandidateProfiles();
        client.requestCandidates();
        client.requestElectionPhase();
        startLiveTimer(client);
    }

    @FXML
    private void handlePlayCampaignVideo() {
        if (selectedProfile == null) {
            return;
        }
        File videoFile = UploadStorage.resolveFile(selectedProfile.getVideoUrl());
        if (videoFile == null || !videoFile.exists()) {
            voterSessionLabel.setText("No campaign video available for this candidate.");
            return;
        }
        stopMediaPlayer();
        Media media = new Media(videoFile.toURI().toString());
        mediaPlayer = new MediaPlayer(media);
        campaignMediaView.setMediaPlayer(mediaPlayer);
        mediaPlayer.play();
    }

    @FXML
    private void handlePauseCampaignVideo() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }
    }

    private void startLiveTimer(VotingSocketClient client) {
        timerRunning = true;
        timerThread = new Thread(() -> {
            while (timerRunning) {
                try {
                    Thread.sleep(1000);
                    client.requestElectionPhase();
                } catch (InterruptedException ignored) {
                    break;
                }
            }
        }, "dashboard-election-timer");
        timerThread.setDaemon(true);
        timerThread.start();
    }

    private void loadCandidateProfiles() {
        candidateMap.clear();
        ObservableList<String> candidates = FXCollections.observableArrayList();
        for (CandidateProfile profile : CandidateProfileDAO.findApproved()) {
            String label = buildCandidateListLabel(profile);
            candidateMap.put(label, profile);
            candidates.add(label);
        }
        candidateListView.setItems(candidates);
        candidateListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                updateCampaignZone(candidateMap.get(newValue));
            }
        });
    }

    private void handleServerMessage(String message) {
        Platform.runLater(() -> {
            if (message.startsWith("ELECTION_PHASE|")) {
                applyElectionState(ElectionStateSnapshot.parse(message));
            } else if (message.startsWith("SYSTEM|REJECTED_VOTE|")) {
                String reason = message.substring("SYSTEM|REJECTED_VOTE|".length());
                electionBannerLabel.setText(reason);
            } else if (message.startsWith("CANDIDATE|")) {
                loadCandidateProfiles();
            }
        });
    }

    private void applyElectionState(ElectionStateSnapshot state) {
        currentPhase = state.getPhase();
        timerLabel.setText(formatSeconds(state.getCountdownSeconds()));

        StringBuilder scheduleText = new StringBuilder();
        String zone = ZoneId.systemDefault().getId();
        if (state.getStartTime() != null) {
            scheduleText.append("Voting opens at: ").append(state.getStartTime().format(DISPLAY_FORMAT))
                    .append(" (").append(zone).append(")");
        }
        if (state.getEndTime() != null) {
            if (!scheduleText.isEmpty()) {
                scheduleText.append("  |  ");
            }
            scheduleText.append("Voting closes at: ").append(state.getEndTime().format(DISPLAY_FORMAT))
                    .append(" (").append(zone).append(")");
        }
        electionScheduleLabel.setText(scheduleText.toString());

        switch (currentPhase) {
            case NOT_STARTED -> {
                timerPrefixLabel.setText("Opens in:");
                electionBannerLabel.setText("Voting has not started yet. Please wait.");
                electionBannerLabel.setStyle("-fx-text-fill: #d97706;");
                browseHintLabel.setText("Voting is locked. You may browse candidates and read documents.");
                proceedToVoteButton.setDisable(true);
                proceedToVoteButton.setText("PROCEED TO VOTE (locked)");
                proceedToVoteButton.setStyle("-fx-background-color: #94a3b8; -fx-text-fill: white; -fx-font-weight: bold;");
                updateProgress(state.getCountdownSeconds());
            }
            case ACTIVE -> {
                timerPrefixLabel.setText("Time remaining:");
                electionBannerLabel.setText("Voting is now open! Cast your ballot before time runs out.");
                electionBannerLabel.setStyle("-fx-text-fill: #059669;");
                browseHintLabel.setText("Select a candidate, then proceed to the voting booth.");
                proceedToVoteButton.setDisable(false);
                proceedToVoteButton.setText("PROCEED TO VOTE");
                proceedToVoteButton.setStyle("-fx-background-color: #d97706; -fx-text-fill: white; -fx-font-weight: bold;");
                updateProgress(state.getCountdownSeconds());
            }
            case ENDED -> {
                timerPrefixLabel.setText("Status:");
                timerLabel.setText("ENDED");
                electionBannerLabel.setText("Voting has ended.");
                electionBannerLabel.setStyle("-fx-text-fill: #dc2626;");
                browseHintLabel.setText("You may still review candidate materials below.");
                proceedToVoteButton.setDisable(true);
                proceedToVoteButton.setText("VOTING CLOSED");
                proceedToVoteButton.setStyle("-fx-background-color: #94a3b8; -fx-text-fill: white; -fx-font-weight: bold;");
                countdownProgressBar.setProgress(1.0);
                showResultsSummary();
            }
        }
        if (currentPhase != ElectionPhase.ENDED) {
            resultsSummaryLabel.setVisible(false);
            resultsSummaryLabel.setText("");
        }
    }

    private void showResultsSummary() {
        Map<String, Integer> results = VoteDAO.getResults();
        if (results.isEmpty()) {
            resultsSummaryLabel.setText("Results will be published when available.");
        } else {
            String summary = results.entrySet().stream()
                    .map(e -> e.getKey() + ": " + e.getValue() + " vote(s)")
                    .collect(Collectors.joining("  |  "));
            resultsSummaryLabel.setText("Results: " + summary);
        }
        resultsSummaryLabel.setVisible(true);
    }

    private void updateProgress(long secondsRemaining) {
        if (secondsRemaining > lastCountdownTotal) {
            lastCountdownTotal = secondsRemaining;
        }
        if (lastCountdownTotal <= 0) {
            countdownProgressBar.setProgress(0);
            return;
        }
        double progress = 1.0 - ((double) secondsRemaining / (double) lastCountdownTotal);
        countdownProgressBar.setProgress(Math.max(0, Math.min(1, progress)));
    }

    private void updateCampaignZone(CandidateProfile profile) {
        stopMediaPlayer();
        selectedProfile = profile;
        if (profile == null) {
            candidateNameLabel.setText("Select a candidate to view details");
            partyLabel.setText("Party affiliation will appear here.");
            documentListView.setItems(FXCollections.observableArrayList());
            return;
        }

        candidateNameLabel.setText(profile.getName());
        partyLabel.setText("Affiliation: " + (profile.getParty() == null ? "N/A" : profile.getParty()));

        ObservableList<String> documents = FXCollections.observableArrayList(
                "Platform Briefing: " + profile.getName(),
                "Campaign Position: " + (profile.getPosition() == null ? "N/A" : profile.getPosition()),
                "Profile Bio: " + (profile.getDescription() == null ? "No biography available" : profile.getDescription())
        );
        File pdfFile = UploadStorage.resolveFile(profile.getPdfUrl());
        if (pdfFile != null) {
            documents.add("Manifesto PDF: " + pdfFile.getName());
        }
        documentListView.setItems(documents);
    }

    private void stopMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
        if (campaignMediaView != null) {
            campaignMediaView.setMediaPlayer(null);
        }
    }

    private String buildCandidateListLabel(CandidateProfile profile) {
        return profile.getName() + " (" + (profile.getParty() == null ? "Independent" : profile.getParty()) + ")";
    }

    private String formatSeconds(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }
}
