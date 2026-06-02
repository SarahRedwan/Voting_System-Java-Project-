package org.example.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.example.client.core.RegistrationService;
import org.example.client.core.SceneManager;

public class CandidateRegisterController {

    @FXML private TextField fullNameField;
    @FXML private TextField phoneField;
    @FXML private TextField fanField;
    @FXML private TextField dobField;
    @FXML private TextField partyField;
    @FXML private TextField positionField;
    @FXML private TextArea bioField;
    @FXML private TextArea addressField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label statusLabel;

    @FXML
    private void handleRegister() {
        if (!passwordField.getText().equals(confirmPasswordField.getText())) {
            showError("Passwords do not match.");
            return;
        }

        RegistrationService.RegistrationResult result = RegistrationService.registerCandidate(
                fullNameField.getText(),
                phoneField.getText(),
                fanField.getText(),
                dobField.getText(),
                partyField.getText(),
                positionField.getText(),
                bioField.getText(),
                addressField.getText(),
                passwordField.getText()
        );

        if (result.success()) {
            SceneManager.switchSceneWithData(
                    "RegistrationSuccessView.fxml",
                    "Application Submitted",
                    result.loginId() + "|CANDIDATE"
            );
        } else {
            showError(result.message());
        }
    }

    @FXML
    private void handleCancel() {
        SceneManager.switchScene("RegisterChoiceView.fxml", "SecureVote - Register");
    }

    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: #dc2626;");
    }
}
