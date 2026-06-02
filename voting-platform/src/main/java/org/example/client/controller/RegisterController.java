package org.example.client.controller;

import org.example.client.core.AppSession;
import org.example.client.core.DataManager;
import org.example.client.core.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class RegisterController {

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label messageLabel;

    @FXML private TextField fullNameField;
    @FXML private TextField usernameField;
    @FXML private TextField officeField;
    @FXML private VBox officeBox;

    // Manifesto section
    @FXML private VBox manifestoBox;
    @FXML private RadioButton radioLink;
    @FXML private RadioButton radioPdf;
    @FXML private VBox linkBox;
    @FXML private HBox pdfBox;
    @FXML private TextField manifestoLinkField;
    @FXML private TextField videoLinkField;
    @FXML private Label pdfNameLabel;

    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;

    private String role;
    private File selectedPdfFile;

    @FXML
    public void initialize() {
        role = AppSession.getPendingRole();

        if ("candidate".equals(role)) {
            titleLabel.setText("Create Candidate Account");
            subtitleLabel.setText("Register to run for office in SecureVote 2026");
            officeBox.setVisible(true);
            officeBox.setManaged(true);
            manifestoBox.setVisible(true);
            manifestoBox.setManaged(true);
        } else {
            titleLabel.setText("Create Voter Account");
            subtitleLabel.setText("Register to cast your vote in SecureVote 2026");
        }
    }

    /** Switch between URL and PDF input when radio buttons are toggled. */
    @FXML
    private void handleManifestoToggle() {
        boolean isLink = radioLink.isSelected();
        linkBox.setVisible(isLink);
        linkBox.setManaged(isLink);
        pdfBox.setVisible(!isLink);
        pdfBox.setManaged(!isLink);
    }

    @FXML
    private void handlePickPdf() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Manifesto PDF");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files (*.pdf)", "*.pdf")
        );
        Stage stage = (Stage) pdfNameLabel.getScene().getWindow();
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            selectedPdfFile = file;
            pdfNameLabel.setText(file.getName());
            pdfNameLabel.setStyle("-fx-text-fill: #2c3e50;");
        }
    }

    @FXML
    private void handleRegister() {
        String fullName   = fullNameField.getText().trim();
        String username   = usernameField.getText().trim();
        String password   = passwordField.getText();
        String confirm    = confirmPasswordField.getText();
        String office     = officeField.getText().trim();

        // --- Basic validation ---
        if (fullName.isEmpty() || username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            showError("Please fill in all required fields.");
            return;
        }

        if ("candidate".equals(role) && office.isEmpty()) {
            showError("Please enter the office or position you are running for.");
            return;
        }

        if (username.length() < 3) {
            showError("Username must be at least 3 characters.");
            return;
        }

        if (password.length() < 6) {
            showError("Password must be at least 6 characters.");
            return;
        }

        if (!password.equals(confirm)) {
            showError("Passwords do not match.");
            return;
        }

        // --- Manifesto validation (candidates only) ---
        String manifestoUrl  = null;
        String manifestoPath = null;

        if ("candidate".equals(role)) {
            if (radioLink.isSelected()) {
                manifestoUrl = manifestoLinkField.getText().trim();
                if (manifestoUrl.isEmpty()) {
                    showError("Please provide a URL link to your manifesto.");
                    return;
                }
                if (!manifestoUrl.startsWith("http://") && !manifestoUrl.startsWith("https://")) {
                    showError("Manifesto URL must start with http:// or https://");
                    return;
                }
            } else {
                if (selectedPdfFile == null) {
                    showError("Please select a PDF file for your manifesto.");
                    return;
                }
                manifestoPath = selectedPdfFile.getAbsolutePath();
            }
        }

        String videoUrl = null;
        if ("candidate".equals(role)) {
            videoUrl = videoLinkField.getText().trim();
            if (!videoUrl.isEmpty() && !videoUrl.startsWith("http://") && !videoUrl.startsWith("https://")) {
                showError("Video URL must start with http:// or https://");
                return;
            }
        }

        // --- Attempt registration ---
        // Prevent registering an existing username (and avoid role collisions)
        if (DataManager.findUserByUsername(username).isPresent()) {
            showError("Username already taken. Please choose a different one.");
            return;
        }

        boolean success = DataManager.registerUser(
            username, fullName, password, role, office, manifestoUrl, manifestoPath, videoUrl
        );

        if (success) {
            showSuccess("Account created successfully! You can now sign in.");
            clearForm();
        } else {
            showError("Username already taken. Please choose a different one.");
        }
    }

    @FXML
    private void handleBack() {
        SceneManager.switchScene("RoleSelectionView.fxml", "SecureVote - Create Account");
    }

    private void showError(String message) {
        messageLabel.setStyle("-fx-text-fill: #e74c3c;");
        messageLabel.setText(message);
    }

    private void showSuccess(String message) {
        messageLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        messageLabel.setText(message);
    }

    private void clearForm() {
        fullNameField.clear();
        usernameField.clear();
        officeField.clear();
        manifestoLinkField.clear();
        videoLinkField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        selectedPdfFile = null;
        pdfNameLabel.setText("No file selected");
        pdfNameLabel.setStyle("-fx-text-fill: #95a5a6;");
    }
}
