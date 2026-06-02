package org.example.client.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.client.core.AppSession;
import org.example.client.core.CandidateProfile;
import org.example.client.core.CandidateProfileDAO;
import org.example.client.core.Database;
import org.example.client.core.MaterialQueue;
import org.example.client.core.PendingSubmission;
import org.example.client.core.PendingSubmissionDAO;
import org.example.client.core.SceneManager;
import org.example.client.core.UploadStorage;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;

public class CandidateDashboardController {

    @FXML private TextField candidateNameField;
    @FXML private TextField partyField;
    @FXML private TextField positionField;
    @FXML private TextArea bioTextArea;
    @FXML private Label profileImageLabel;
    @FXML private Label logoImageLabel;
    @FXML private Label uploadStatusLabel;
    @FXML private Label queueStatusLabel;
    @FXML private Label profileSaveStatusLabel;
    @FXML private Label pdfPathLabel;
    @FXML private Label videoPathLabel;
    @FXML private MediaView campaignVideoView;
    @FXML private Button playVideoButton;
    @FXML private Button pauseVideoButton;

    private File selectedPDF;
    private File selectedVideo;
    private File selectedProfileImage;
    private File selectedLogoImage;
    private CandidateProfile profile;
    private MediaPlayer mediaPlayer;
    private Timeline statusTimeline;
    private String lastKnownSubmissionStatus;

    @FXML
    public void initialize() {
        Database.initializeSchema();
        loadCandidateProfile();
        updateStatusDisplay();
        MaterialQueue.refreshFromDatabase();

        statusTimeline = new Timeline(new KeyFrame(Duration.seconds(2), event -> updateStatusDisplay()));
        statusTimeline.setCycleCount(Timeline.INDEFINITE);
        statusTimeline.play();
    }

    private void loadCandidateProfile() {
        profile = CandidateProfileDAO.findByUsername(AppSession.getUsername());
        if (profile == null) {
            profile = new CandidateProfile(0, AppSession.getUsername(), "", "", "", null, null, "", LocalDateTime.now());
            CandidateProfileDAO.saveOrUpdate(profile);
        }

        candidateNameField.setText(profile.getName());
        partyField.setText(profile.getParty());
        positionField.setText(profile.getPosition());
        bioTextArea.setText(profile.getDescription());
        profileImageLabel.setText(fileLabel(profile.getImageUrl()));
        logoImageLabel.setText(fileLabel(profile.getLogoUrl()));
        pdfPathLabel.setText(fileLabel(profile.getPdfUrl()));
        videoPathLabel.setText(fileLabel(profile.getVideoUrl()));
    }

    private String fileLabel(String path) {
        if (path == null || path.isBlank()) {
            return "No file selected";
        }
        return new File(path).getName();
    }

    @FXML
    private void handleUploadPDF() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Manifesto PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files (*.pdf)", "*.pdf"));

        Stage stage = (Stage) uploadStatusLabel.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            selectedPDF = file;
            pdfPathLabel.setText(file.getName());
            uploadStatusLabel.setText("Selected manifesto: " + file.getName());
            uploadStatusLabel.setStyle("-fx-text-fill: #059669;");
        }
    }

    @FXML
    private void handleUploadVideo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Campaign Video Ad");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("MP4 Video Files (*.mp4)", "*.mp4"));

        Stage stage = (Stage) uploadStatusLabel.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            selectedVideo = file;
            videoPathLabel.setText(file.getName());
            uploadStatusLabel.setText("Selected media: " + file.getName());
            uploadStatusLabel.setStyle("-fx-text-fill: #059669;");
        }
    }

    @FXML
    private void handleUploadProfileImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Profile Picture");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));

        Stage stage = (Stage) profileImageLabel.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            selectedProfileImage = file;
            profileImageLabel.setText(file.getName());
        }
    }

    @FXML
    private void handleUploadLogo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Campaign Logo");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));

        Stage stage = (Stage) logoImageLabel.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            selectedLogoImage = file;
            logoImageLabel.setText(file.getName());
        }
    }

    @FXML
    private void handleSaveProfile() {
        submitChangesForApproval(false);
    }

    @FXML
    private void handleSubmitMaterial() {
        if (selectedPDF == null && selectedVideo == null) {
            uploadStatusLabel.setStyle("-fx-text-fill: #dc2626;");
            uploadStatusLabel.setText("Please select a PDF or video before submitting materials.");
            return;
        }
        submitChangesForApproval(true);
    }

    private void submitChangesForApproval(boolean materialsOnly) {
        if (profile == null) {
            profile = new CandidateProfile(0, AppSession.getUsername(), "", "", "", null, null, "", LocalDateTime.now());
        }

        String name = candidateNameField.getText().trim();
        if (!materialsOnly && name.isBlank()) {
            profileSaveStatusLabel.setText("Candidate name is required.");
            profileSaveStatusLabel.setStyle("-fx-text-fill: #dc2626;");
            return;
        }

        try {
            PendingSubmission existingPending = PendingSubmissionDAO.findLatestByUsername(AppSession.getUsername());
            String imagePath = existingPending != null && "PENDING".equals(existingPending.getStatus())
                    ? existingPending.getPendingImagePath() : profile.getImageUrl();
            String logoPath = existingPending != null && "PENDING".equals(existingPending.getStatus())
                    ? existingPending.getPendingLogoPath() : profile.getLogoUrl();
            String pdfPath = existingPending != null && "PENDING".equals(existingPending.getStatus())
                    ? existingPending.getPendingPdfPath() : profile.getPdfUrl();
            String videoPath = existingPending != null && "PENDING".equals(existingPending.getStatus())
                    ? existingPending.getPendingVideoPath() : profile.getVideoUrl();

            if (selectedProfileImage != null) {
                imagePath = UploadStorage.copyToUploads(selectedProfileImage, AppSession.getUsername(), "profile");
                selectedProfileImage = null;
            }
            if (selectedLogoImage != null) {
                logoPath = UploadStorage.copyToUploads(selectedLogoImage, AppSession.getUsername(), "logo");
                selectedLogoImage = null;
            }
            if (selectedPDF != null) {
                pdfPath = UploadStorage.copyToUploads(selectedPDF, AppSession.getUsername(), "pdf");
                selectedPDF = null;
            }
            if (selectedVideo != null) {
                videoPath = UploadStorage.copyToUploads(selectedVideo, AppSession.getUsername(), "video");
                selectedVideo = null;
            }

            PendingSubmission submission = new PendingSubmission(
                    0,
                    AppSession.getUsername(),
                    name.isBlank() ? profile.getName() : name,
                    materialsOnly ? profile.getName() : name,
                    materialsOnly ? profile.getParty() : partyField.getText().trim(),
                    materialsOnly ? profile.getDescription() : bioTextArea.getText().trim(),
                    materialsOnly ? profile.getPosition() : positionField.getText().trim(),
                    imagePath,
                    logoPath,
                    pdfPath,
                    videoPath,
                    "PENDING",
                    null,
                    LocalDateTime.now(),
                    null
            );

            PendingSubmissionDAO.submitForReview(submission);
            MaterialQueue.refreshFromDatabase();

            profileSaveStatusLabel.setText("Changes submitted for administrator approval.");
            profileSaveStatusLabel.setStyle("-fx-text-fill: #d97706;");
            uploadStatusLabel.setText("Your profile and materials are waiting for admin review.");
            uploadStatusLabel.setStyle("-fx-text-fill: #d97706;");

            pdfPathLabel.setText(fileLabel(pdfPath));
            videoPathLabel.setText(fileLabel(videoPath));
            updateStatusDisplay();
        } catch (IOException e) {
            profileSaveStatusLabel.setText("Failed to stage uploaded files: " + e.getMessage());
            profileSaveStatusLabel.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    @FXML
    private void handlePlayVideo() {
        File videoFile = selectedVideo;
        if (videoFile == null && profile != null) {
            videoFile = UploadStorage.resolveFile(profile.getVideoUrl());
        }
        PendingSubmission latest = PendingSubmissionDAO.findLatestByUsername(AppSession.getUsername());
        if (videoFile == null && latest != null) {
            videoFile = UploadStorage.resolveFile(latest.getPendingVideoPath());
        }

        if (videoFile == null || !videoFile.exists()) {
            uploadStatusLabel.setText("No playable video file is available yet.");
            uploadStatusLabel.setStyle("-fx-text-fill: #dc2626;");
            return;
        }

        stopMediaPlayer();
        Media media = new Media(videoFile.toURI().toString());
        mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setOnError(() -> {
            uploadStatusLabel.setText("Unable to play video: " + mediaPlayer.getError().getMessage());
            uploadStatusLabel.setStyle("-fx-text-fill: #dc2626;");
        });
        campaignVideoView.setMediaPlayer(mediaPlayer);
        mediaPlayer.play();
        uploadStatusLabel.setText("Playing: " + videoFile.getName());
        uploadStatusLabel.setStyle("-fx-text-fill: #059669;");
    }

    @FXML
    private void handlePauseVideo() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }
    }

    private void stopMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
        campaignVideoView.setMediaPlayer(null);
    }

    private void updateStatusDisplay() {
        PendingSubmission latest = PendingSubmissionDAO.findLatestByUsername(AppSession.getUsername());
        if (latest == null) {
            queueStatusLabel.setText("STANDBY / APPROVED");
            queueStatusLabel.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");
            return;
        }

        switch (latest.getStatus()) {
            case "PENDING" -> {
                queueStatusLabel.setText("PENDING ADMIN REVIEW");
                queueStatusLabel.setStyle("-fx-text-fill: #d97706; -fx-font-weight: bold;");
                uploadStatusLabel.setText("Waiting for administrator approval.");
                uploadStatusLabel.setStyle("-fx-text-fill: #d97706;");
            }
            case "APPROVED" -> {
                queueStatusLabel.setText("APPROVED");
                queueStatusLabel.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");
                String message = latest.getAdminMessage() != null && !latest.getAdminMessage().isBlank()
                        ? latest.getAdminMessage()
                        : "Your changes were approved and are now live.";
                uploadStatusLabel.setText(message);
                uploadStatusLabel.setStyle("-fx-text-fill: #059669;");
                profileSaveStatusLabel.setText(message);
                profileSaveStatusLabel.setStyle("-fx-text-fill: #059669;");
                if (!"APPROVED".equals(lastKnownSubmissionStatus)) {
                    loadCandidateProfile();
                }
            }
            case "REJECTED" -> {
                queueStatusLabel.setText("REJECTED");
                queueStatusLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                String message = latest.getAdminMessage() != null && !latest.getAdminMessage().isBlank()
                        ? latest.getAdminMessage()
                        : "Your submission was rejected. Please revise and submit again.";
                uploadStatusLabel.setText(message);
                uploadStatusLabel.setStyle("-fx-text-fill: #dc2626;");
                profileSaveStatusLabel.setText(message);
                profileSaveStatusLabel.setStyle("-fx-text-fill: #dc2626;");
            }
            default -> {
                queueStatusLabel.setText("STANDBY / APPROVED");
                queueStatusLabel.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");
            }
        }
        lastKnownSubmissionStatus = latest.getStatus();
    }

    @FXML
    private void handleLogout() {
        if (statusTimeline != null) {
            statusTimeline.stop();
        }
        stopMediaPlayer();
        SceneManager.switchScene("LoginView.fxml", "SecureVote - Sign In");
    }
}
