package org.example.client.controller;

import org.example.client.core.SceneManager;
import javafx.fxml.FXML;

public class WelcomeController {

    @FXML
    private void handleVoterLoginNavigation() {
        System.out.println("Opening Unified Authentication Gateway...");
        SceneManager.switchScene("LoginView.fxml", "SecureVote - Sign In");
    }
}