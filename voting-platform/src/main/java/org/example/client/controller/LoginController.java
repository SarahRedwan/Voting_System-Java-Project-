package org.example.client.controller;

import org.example.client.core.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

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

        System.out.println("Processing login credentials against system roles...");

        // 🛡️ ROLE 1: ELECTION ADMINISTRATOR
        if (voterId.equals("admin") && password.equals("password")) {
            System.out.println("Access Cleared: Routing to Admin Panel Desk...");
            errorLabel.setText("");
            SceneManager.switchScene("AdminDashboardView.fxml", "SecureVote - Election Control Panel");
        }
        // 📢 ROLE 2: POLITICAL CANDIDATE
        else if (voterId.equals("candidate") && password.equals("password")) {
            System.out.println("Access Cleared: Routing to Candidate Console...");
            errorLabel.setText("");
            SceneManager.switchScene("CandidateDashboardView.fxml", "SecureVote - Candidate Dashboard");
        }
        // 🗳️ ROLE 3: STANDARD ELIGIBLE VOTER (Using your "123" placeholder contract)
        else if (voterId.equals("123") && password.equals("password")) {
            System.out.println("Access Cleared: Routing to Voter Platform...");
            errorLabel.setText("");
            SceneManager.switchScene("DashboardView.fxml", "SecureVote - Voter Dashboard");
        }
        // ❌ REJECTED
        else {
            errorLabel.setText("Invalid Registration ID or Security PIN. Try again.");
        }
    }

    @FXML
    private void handleBackToWelcome() {
        SceneManager.switchScene("WelcomeView.fxml", "Welcome to SecureVote 2026");
    }
}