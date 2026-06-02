package org.example.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.example.client.core.SceneManager;

public class RegistrationSuccessController {

    @FXML private Label messageLabel;
    @FXML private Label loginIdLabel;

    @FXML
    public void initialize() {
        String data = SceneManager.consumeRegistrationData();
        if (data == null || data.isBlank()) {
            messageLabel.setText("Your account has been created.");
            return;
        }

        String loginId = data;
        boolean isCandidate = data.contains("|CANDIDATE");
        if (isCandidate) {
            loginId = data.replace("|CANDIDATE", "");
            messageLabel.setText("Your candidate application is PENDING admin approval. You will be notified after review.");
            loginIdLabel.setText("Candidate ID: " + loginId);
        } else {
            messageLabel.setText("You are automatically approved and can vote immediately.");
            loginIdLabel.setText("Voter ID: " + loginId);
        }
    }

    @FXML
    private void handleGoToLogin() {
        SceneManager.switchScene("LoginView.fxml", "SecureVote - Sign In");
    }
}
