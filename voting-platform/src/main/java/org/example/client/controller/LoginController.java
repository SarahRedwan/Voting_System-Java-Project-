package org.example.client.controller;

import org.example.client.core.AppSession;
import org.example.client.core.CandidateProfileDAO;
import org.example.client.core.Database;
import org.example.client.core.SceneManager;
import org.example.client.core.User;
import org.example.client.core.UserDAO;
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
    public void initialize() {
        Database.initializeSchema();
    }

    @FXML
    private void handleLoginSubmit() {
        String loginId = voterIdField.getText().trim();
        String password = passwordField.getText();

        if (loginId.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please fill out all credential fields.");
            return;
        }

        User user = UserDAO.authenticateByLoginId(loginId, password);
        if (user == null) {
            errorLabel.setText("Invalid username, ID, phone, or password. Try again.");
            return;
        }

        if (!user.isActive()) {
            String status = CandidateProfileDAO.getApprovalStatus(user.getUsername());
            if ("PENDING".equals(status)) {
                errorLabel.setText("Your candidate application is pending admin approval.");
            } else if ("REJECTED".equals(status)) {
                String msg = CandidateProfileDAO.getAdminReviewMessage(user.getUsername());
                errorLabel.setText(msg != null ? msg : "Your candidate application was rejected.");
            } else {
                errorLabel.setText("Your account is not active. Contact the administrator.");
            }
            return;
        }

        if ("CANDIDATE".equals(user.getRole())) {
            String status = CandidateProfileDAO.getApprovalStatus(user.getUsername());
            if ("PENDING".equals(status)) {
                errorLabel.setText("Your candidate application is still pending approval.");
                return;
            }
            if ("REJECTED".equals(status)) {
                String msg = CandidateProfileDAO.getAdminReviewMessage(user.getUsername());
                errorLabel.setText(msg != null ? msg : "Your candidate application was rejected.");
                return;
            }
        }

        AppSession.setUsername(user.getUsername());
        AppSession.setRole(user.getRole());
        errorLabel.setText("");

        switch (user.getRole()) {
            case "ADMIN" -> SceneManager.switchScene("AdminDashboardView.fxml", "SecureVote - Election Control Panel");
            case "CANDIDATE" -> SceneManager.switchScene("CandidateDashboardView.fxml", "SecureVote - Candidate Dashboard");
            case "VOTER" -> SceneManager.switchScene("DashboardView.fxml", "SecureVote - Voter Dashboard");
            default -> errorLabel.setText("Unknown role assigned to user. Contact administrator.");
        }
    }

    @FXML
    private void handleBackToWelcome() {
        SceneManager.switchScene("WelcomeView.fxml", "Welcome to SecureVote 2026");
    }

    @FXML
    private void handleGoToRegister() {
        SceneManager.switchScene("RegisterChoiceView.fxml", "SecureVote - Register");
    }
}
