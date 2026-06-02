package org.example.client.controller;

import org.example.client.core.AppSession;
import org.example.client.core.SceneManager;
import javafx.fxml.FXML;

public class RoleSelectionController {

    @FXML
    private void handleSelectVoter() {
        AppSession.setPendingRole("voter");
        SceneManager.switchScene("RegisterView.fxml", "SecureVote - Register as Voter");
    }

    @FXML
    private void handleSelectCandidate() {
        AppSession.setPendingRole("candidate");
        SceneManager.switchScene("RegisterView.fxml", "SecureVote - Register as Candidate");
    }

    @FXML
    private void handleBack() {
        AppSession.clear();
        SceneManager.switchScene("WelcomeView.fxml", "Welcome to SecureVote 2026");
    }
}
