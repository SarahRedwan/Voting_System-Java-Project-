package org.example.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.client.core.MaterialQueue;
import org.example.client.core.PendingMaterial;
import org.example.client.core.SceneManager;

import java.io.File;

public class CandidateDashboardController {

    @FXML private Label pdfPathLabel;
    @FXML private Label videoPathLabel;
    @FXML private Label uploadStatusLabel;
    @FXML private Label queueStatusLabel;

    private File selectedPDF;
    private File selectedVideo;

    @FXML
    public void initialize() {
        updateStatusDisplay();
    }

    @FXML
    private void handleUploadPDF() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Manifesto PDF");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files (*.pdf)", "*.pdf")
        );

        // Grab the window window stage reference from an existing element
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
    private void handleSubmitMaterial() {
        if (selectedPDF == null && selectedVideo == null) {
            uploadStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
            uploadStatusLabel.setText("❌ Please select a file before submitting.");
            return;
        }

        // Add this item to our static global memory tracker queue
        PendingMaterial submission = new PendingMaterial("Candidate Alpha", selectedPDF, selectedVideo);
        MaterialQueue.pendingList.add(submission);

        uploadStatusLabel.setStyle("-fx-text-fill: #2ecc71;");
        uploadStatusLabel.setText("✅ Materials submitted! Awaiting administrator approval.");

        // Clear choices out of memory
        selectedPDF = null;
        selectedVideo = null;
        pdfPathLabel.setText("No file selected");
        videoPathLabel.setText("No file selected");

        updateStatusDisplay();
    }

    private void updateStatusDisplay() {
        // Look inside the global shared list to see if our name is inside the moderation queue
        boolean isPending = MaterialQueue.pendingList.stream()
                .anyMatch(material -> material.getCandidateName().equals("Candidate Alpha"));

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
        SceneManager.switchScene("LoginView.fxml", "SecureVote - Sign In");
    }
}