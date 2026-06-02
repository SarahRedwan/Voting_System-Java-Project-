package org.example.client.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import org.example.client.core.AppSession;
import org.example.client.core.CandidateProfile;
import org.example.client.core.CandidateProfileDAO;
import org.example.client.core.ElectionPhase;
import org.example.client.core.ElectionStateSnapshot;
import org.example.client.core.SceneManager;
import org.example.client.core.VotingSocketClient;

import java.util.List;
import java.util.Optional;

public class VotingController {

    @FXML private ToggleGroup candidateGroup;
    @FXML private RadioButton candidate1;
    @FXML private RadioButton candidate2;
    @FXML private RadioButton candidate3;
    @FXML private Label statusLabel;
    @FXML private Label phaseBannerLabel;
    @FXML private Label timerPrefixLabel;
    @FXML private Label boothTimerLabel;
    @FXML private VBox ballotPanel;
    @FXML private Button submitVoteButton;

    private boolean hasAlreadyVoted = false;
    private ElectionPhase currentPhase = ElectionPhase.NOT_STARTED;
    private volatile boolean phasePollRunning = false;
    private Thread phasePollThread;

    @FXML
    public void initialize() {
        VotingSocketClient client = VotingSocketClient.getInstance();
        if (!client.connect(AppSession.getUsername())) {
            phaseBannerLabel.setText("Cannot connect to election server.");
            phaseBannerLabel.setStyle("-fx-text-fill: #dc2626;");
            ballotPanel.setDisable(true);
            submitVoteButton.setDisable(true);
            return;
        }
        client.addListener(this::handleServerMessage);

        client.requestVoterStatus(AppSession.getUsername());
        client.requestElectionPhase();
        populateCandidateOptions();
        applyPhaseUi();
        startPhasePolling(client);
    }

    private void startPhasePolling(VotingSocketClient client) {
        phasePollRunning = true;
        phasePollThread = new Thread(() -> {
            while (phasePollRunning) {
                try {
                    Thread.sleep(1000);
                    client.requestElectionPhase();
                } catch (InterruptedException ignored) {
                    break;
                }
            }
        }, "booth-phase-poll");
        phasePollThread.setDaemon(true);
        phasePollThread.start();
    }

    private void populateCandidateOptions() {
        List<CandidateProfile> profiles = CandidateProfileDAO.findApproved();
        RadioButton[] options = {candidate1, candidate2, candidate3};

        for (int i = 0; i < options.length; i++) {
            if (i < profiles.size()) {
                CandidateProfile profile = profiles.get(i);
                RadioButton button = options[i];
                button.setVisible(true);
                button.setDisable(currentPhase != ElectionPhase.ACTIVE);
                button.setText(profile.getName() + " (" + (profile.getParty() == null ? "Independent" : profile.getParty()) + ")");
                button.setUserData(profile.getUsername());
            } else {
                options[i].setVisible(false);
            }
        }
    }

    private void handleServerMessage(String message) {
        Platform.runLater(() -> {
            if (message.startsWith("ELECTION_PHASE|")) {
                currentPhase = ElectionStateSnapshot.parse(message).getPhase();
                applyElectionState(ElectionStateSnapshot.parse(message));
            } else if (message.startsWith("VOTER_STATUS|")) {
                parseVoterStatus(message);
            } else if (message.startsWith("SYSTEM|REJECTED_VOTE|")) {
                String reason = message.substring("SYSTEM|REJECTED_VOTE|".length());
                statusLabel.setText(reason);
                statusLabel.setStyle("-fx-text-fill: #dc2626;");
            }
        });
    }

    private void applyElectionState(ElectionStateSnapshot state) {
        currentPhase = state.getPhase();
        boothTimerLabel.setText(formatSeconds(state.getCountdownSeconds()));
        applyPhaseUi();

        switch (currentPhase) {
            case NOT_STARTED -> {
                timerPrefixLabel.setText("Opens in:");
                phaseBannerLabel.setText("Voting has not started yet. Please wait.");
                phaseBannerLabel.setStyle("-fx-text-fill: #d97706;");
            }
            case ACTIVE -> {
                timerPrefixLabel.setText("Time remaining:");
                phaseBannerLabel.setText("Voting is open. Select a candidate and submit your ballot.");
                phaseBannerLabel.setStyle("-fx-text-fill: #059669;");
            }
            case ENDED -> {
                timerPrefixLabel.setText("Status:");
                boothTimerLabel.setText("ENDED");
                phaseBannerLabel.setText("Voting has ended.");
                phaseBannerLabel.setStyle("-fx-text-fill: #dc2626;");
            }
        }
    }

    private void applyPhaseUi() {
        boolean votingOpen = currentPhase == ElectionPhase.ACTIVE;
        ballotPanel.setDisable(!votingOpen);
        submitVoteButton.setDisable(!votingOpen);
        for (RadioButton option : new RadioButton[]{candidate1, candidate2, candidate3}) {
            if (option.isVisible()) {
                option.setDisable(!votingOpen);
            }
        }
    }

    private void parseVoterStatus(String message) {
        String[] parts = message.split("\\|");
        for (String part : parts) {
            if (part.startsWith("hasVoted=")) {
                hasAlreadyVoted = Boolean.parseBoolean(part.substring("hasVoted=".length()));
                if (hasAlreadyVoted) {
                    statusLabel.setText("You have already voted. You may change your vote while voting is active.");
                    statusLabel.setStyle("-fx-text-fill: #d97706;");
                }
                break;
            }
        }
    }

    @FXML
    public void handleSubmitVote() {
        if (currentPhase != ElectionPhase.ACTIVE) {
            statusLabel.setText(currentPhase == ElectionPhase.ENDED
                    ? "Voting has ended."
                    : "Voting has not started yet. Please wait.");
            statusLabel.setStyle("-fx-text-fill: #dc2626;");
            return;
        }

        RadioButton selectedRadio = (RadioButton) candidateGroup.getSelectedToggle();
        if (selectedRadio == null) {
            statusLabel.setStyle("-fx-text-fill: #dc2626;");
            statusLabel.setText("Please select a candidate before submitting.");
            return;
        }

        if (hasAlreadyVoted) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Change Vote?");
            confirm.setHeaderText("You have already voted");
            confirm.setContentText("Change your vote to " + selectedRadio.getText() + "?");
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK) {
                return;
            }
        }

        VotingSocketClient client = VotingSocketClient.getInstance();
        if (!client.connect(AppSession.getUsername())) {
            statusLabel.setStyle("-fx-text-fill: #dc2626;");
            statusLabel.setText("Could not connect to voting server.");
            return;
        }

        String candidateUsername = selectedRadio.getUserData() instanceof String
                ? (String) selectedRadio.getUserData()
                : selectedRadio.getText();
        client.sendVote(candidateUsername);

        statusLabel.setStyle("-fx-text-fill: #047857;");
        statusLabel.setText("Vote submitted. Returning to dashboard...");
        hasAlreadyVoted = true;

        Platform.runLater(() -> {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException ignored) {
            }
            SceneManager.switchScene("ConfirmationView.fxml", "Vote Confirmation");
        });
    }

    @FXML
    public void handleCancel() {
        SceneManager.switchScene("DashboardView.fxml", "SecureVote - Voter Dashboard");
    }

    private String formatSeconds(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }
}
