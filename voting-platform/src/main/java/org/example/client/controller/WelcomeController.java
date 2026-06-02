package org.example.client.controller;

import org.example.client.core.SceneManager;
import javafx.fxml.FXML;

public class WelcomeController {

    @FXML
    private void handleVoterLoginNavigation() {
        SceneManager.switchScene("LoginView.fxml", "SecureVote - Sign In");
    }

    @FXML
    private void handleRegisterNavigation() {
        SceneManager.switchScene("RegisterChoiceView.fxml", "SecureVote - Register");
    }
}