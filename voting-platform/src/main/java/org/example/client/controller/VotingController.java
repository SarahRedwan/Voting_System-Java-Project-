package org.example.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import org.example.client.core.AppSession;
import org.example.client.core.DataManager;
import org.example.client.core.SceneManager;

public class VotingController {

    @FXML
    private ToggleGroup candidateGroup;
    @FXML
    private RadioButton candidate1;
    @FXML
    private RadioButton candidate2;
    @FXML
    private RadioButton candidate3;
    @FXML
    private Label statusLabel;

    @FXML
    public void handleSubmitVote() {
        RadioButton selectedRadio = (RadioButton) candidateGroup.getSelectedToggle();

        if (selectedRadio == null) {
            statusLabel.setStyle("-fx-text-fill: #d32f2f;");
            statusLabel.setText("❌ Please select a candidate before submitting!");
            return;
        }

        String chosenCandidate = selectedRadio.getText();
        String voterId = AppSession.getUsername();

        System.out.println("Processing ballot registration for: " + chosenCandidate + " by " + voterId);
        boolean stored = DataManager.recordVote(voterId, chosenCandidate);

        if (!stored) {
            statusLabel.setStyle("-fx-text-fill: #d32f2f;");
            statusLabel.setText("❌ Vote could not be recorded. Either you have already voted or the candidate is inactive.");
            return;
        }

        statusLabel.setStyle("-fx-text-fill: #2e7d32;");
        statusLabel.setText("✅ Vote cast successfully! Returning to Dashboard...");
        SceneManager.switchScene("ConfirmationView.fxml", "Vote Confirmation");
    }

    @FXML
    public void handleCancel() {
        // Match your exact resources/fxml flat folder hierarchy structure
        SceneManager.switchScene("DashboardView.fxml", "voter");
    }
}


