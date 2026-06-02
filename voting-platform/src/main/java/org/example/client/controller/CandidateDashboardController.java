package org.example.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.client.core.AppSession;
import org.example.client.core.DataManager;
import org.example.client.core.MaterialQueue;
import org.example.client.core.PendingMaterial;
import org.example.client.core.SceneManager;

import java.io.File;
import java.util.Optional;

public class CandidateDashboardController {

    @FXML private Label pdfPathLabel;
    @FXML private Label videoPathLabel;
    @FXML private Label uploadStatusLabel;
    @FXML private Label queueStatusLabel;
    @FXML private Label candidateNameLabel;
    @FXML private Label withdrawStatusLabel;
    @FXML private RadioButton videoLinkRadio;
    @FXML private RadioButton videoFileRadio;
    @FXML private TextField videoLinkField;
    @FXML private VBox videoFileBox;

    private File selectedPDF;
    private File selectedVideo;
    private String videoLink;
    private String linkedCandidateName = "Candidate Alpha";

    @FXML
    public void initialize() {
        if (!AppSession.getUsername().equals("guest")) {
            DataManager.findUserByUsername(AppSession.getUsername()).ifPresent(user -> {
                linkedCandidateName = user.getFullName();
                candidateNameLabel.setText(user.getFullName());
            });
        }
        updateStatusDisplay();
    }

    @FXML
    private void handleUploadPDF() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Manifesto PDF");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files (*.pdf)", "*.pdf")
        );
        Stage stage = (Stage) pdfPathLabel.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            selectedPDF = file;
            pdfPathLabel.setText(file.getName());
        }
    }

    @FXML
    private void handleUploadVideo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Campaign Video Ad");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("MP4 Video Files (*.mp4)", "*.mp4")
        );
        Stage stage = (Stage) videoPathLabel.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            selectedVideo = file;
            videoPathLabel.setText(file.getName());
        }
    }

    @FXML
    private void handleVideoToggle() {
        boolean useLink = videoLinkRadio.isSelected();
        videoLinkField.setDisable(!useLink);
        videoLinkField.setVisible(useLink);
        videoFileBox.setVisible(!useLink);
        videoFileBox.setManaged(!useLink);
        if (useLink) {
            videoPathLabel.setText("No file selected");
            selectedVideo = null;
        } else {
            videoLinkField.clear();
        }
    }

    @FXML
    private void handleSubmitMaterial() {
        videoLink = videoLinkField.getText().trim();

        if (selectedPDF == null && selectedVideo == null && (videoLink == null || videoLink.isEmpty())) {
            uploadStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
            uploadStatusLabel.setText("❌ Please submit a manifesto PDF or a video link/file.");
            return;
        }

        if (videoLinkRadio.isSelected() && videoLink != null && !videoLink.isEmpty()) {
            if (!videoLink.startsWith("http://") && !videoLink.startsWith("https://")) {
                uploadStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
                uploadStatusLabel.setText("❌ Video URL must start with http:// or https://");
                return;
            }
            selectedVideo = null;
        }

        if (videoFileRadio.isSelected() && selectedVideo == null && (videoLink == null || videoLink.isEmpty())) {
            uploadStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
            uploadStatusLabel.setText("❌ Please choose a video file before submitting.");
            return;
        }

        PendingMaterial submission = new PendingMaterial(linkedCandidateName, selectedPDF, selectedVideo, videoLink);
        MaterialQueue.pendingList.add(submission);

        uploadStatusLabel.setStyle("-fx-text-fill: #2ecc71;");
        uploadStatusLabel.setText("✅ Materials submitted! Awaiting administrator approval.");

        selectedPDF = null;
        selectedVideo = null;
        videoLink = null;
        pdfPathLabel.setText("No file selected");
        videoPathLabel.setText("No file selected");
        videoLinkField.clear();

        updateStatusDisplay();
    }

    @FXML
    private void handleWithdraw() {
        // Confirmation dialog before withdrawing
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Withdraw Candidacy");
        confirm.setHeaderText("Are you sure you want to withdraw?");
        confirm.setContentText(
                "This will permanently remove you from the election.\n" +
                "Your account will remain but you will no longer appear as a candidate.\n\n" +
                "This action cannot be undone."
        );

        Stage stage = (Stage) withdrawStatusLabel.getScene().getWindow();
        confirm.initOwner(stage);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String username = AppSession.getUsername();
            boolean success = DataManager.withdrawCandidate(username);

            if (success) {
                AppSession.clear();
                // Show brief success alert then go back to welcome
                Alert done = new Alert(Alert.AlertType.INFORMATION);
                done.setTitle("Withdrawal Confirmed");
                done.setHeaderText("You have been removed from the election.");
                done.setContentText("Your candidacy has been withdrawn successfully.");
                done.initOwner(stage);
                done.showAndWait();
                SceneManager.switchScene("WelcomeView.fxml", "Welcome to SecureVote 2026");
            } else {
                withdrawStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
                withdrawStatusLabel.setText("❌ Withdrawal failed. Please try again or contact admin.");
            }
        }
    }

    private void updateStatusDisplay() {
        boolean isPending = MaterialQueue.pendingList.stream()
            .anyMatch(material -> material.getCandidateName().equals(linkedCandidateName));

        if (isPending) {
            queueStatusLabel.setText("⏳ PENDING ADMIN REVIEW");
            queueStatusLabel.setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold;");
        } else {
            queueStatusLabel.setText("✅ STANDBY / APPROVED");
            queueStatusLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
        }
    }

    @FXML
    private void handleLogout() {
        System.out.println("Terminating candidate workspace session...");
        AppSession.clear();
        SceneManager.switchScene("LoginView.fxml", "SecureVote - Sign In");
    }
}