package org.example.client.controller;

import javafx.fxml.FXML;
import org.example.client.core.Database;
import org.example.client.core.SceneManager;

public class RegisterChoiceController {

    @FXML
    public void initialize() {
        Database.initializeSchema();
    }

    @FXML
    private void handleRegisterVoter() {
        SceneManager.switchScene("VoterRegisterView.fxml", "SecureVote - Voter Registration");
    }

    @FXML
    private void handleRegisterCandidate() {
        SceneManager.switchScene("CandidateRegisterView.fxml", "SecureVote - Candidate Registration");
    }

    @FXML
    private void handleBack() {
        SceneManager.switchScene("WelcomeView.fxml", "Welcome to SecureVote 2026");
    }
}
