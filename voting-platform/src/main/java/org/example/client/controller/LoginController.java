package org.example.client.controller;

import org.example.client.core.AppSession;
import org.example.client.core.DataManager;
import org.example.client.core.SceneManager;
import org.example.client.core.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.Optional;

public class LoginController {

    @FXML
    private TextField voterIdField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    private void handleLoginSubmit() {
        String voterId = voterIdField.getText().trim().toLowerCase(); // Normalize string case
        String password = passwordField.getText();

        if (voterId.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please fill out all credential fields.");
            return;
        }

        System.out.println("Processing login credentials against database users...");
        Optional<User> authenticated = DataManager.authenticateUser(voterId, password);

        if (authenticated.isEmpty()) {
            errorLabel.setText("Invalid credentials. Register a new account or try again.");
            return;
        }

        User user = authenticated.get();
        AppSession.setUsername(user.getUsername());
        AppSession.setRole(user.getRole());
        errorLabel.setText("");

        switch (user.getRole()) {
            case "admin" -> SceneManager.switchScene("AdminDashboardView.fxml", "SecureVote - Election Control Panel");
            case "candidate" -> SceneManager.switchScene("CandidateDashboardView.fxml", "SecureVote - Candidate Dashboard");
            default -> SceneManager.switchScene("DashboardView.fxml", "SecureVote - Voter Dashboard");
        }
    }

    @FXML
    private void handleOpenRegistration() {
        SceneManager.switchScene("RoleSelectionView.fxml", "SecureVote - Create Account");
    }

    @FXML
    private void handleBackToWelcome() {
        AppSession.clear();
        SceneManager.switchScene("WelcomeView.fxml", "Welcome to SecureVote 2026");
    }
}