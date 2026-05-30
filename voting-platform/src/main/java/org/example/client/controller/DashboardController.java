package org.example.client.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.example.client.core.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.scene.media.MediaView;

public class DashboardController {

    @FXML private Label timerLabel;
    @FXML private ListView<String> candidateListView;
    @FXML private Label candidateNameLabel;
    @FXML private Label partyLabel;
    @FXML private ListView<String> documentListView;
    @FXML private MediaView campaignMediaView;
    @FXML private Label voterSessionLabel;
    @FXML
    private void handleGoToVoting() {
        System.out.println("Redirecting user session to digital voting booth window...");
        // Perfect flat path location with a clean application header title string
        SceneManager.switchScene("VotingView.fxml", "Voter Dashboard Booth");
    }

    @FXML
    public void initialize() {
        // Simple placeholder time display string
        timerLabel.setText("02h : 45m : 12s");
        voterSessionLabel.setText("Authenticated Voter Session Active");

        // Generate dummy candidate listings
        ObservableList<String> candidates = FXCollections.observableArrayList(
                "Candidate Alpha (Democratic Party)",
                "Candidate Beta (Republican Party)",
                "Candidate Gamma (Independent Bloc)"
        );
        candidateListView.setItems(candidates);

        // Add a click listener to the list to update our content area dynamically!
        candidateListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                updateCampaignZone(newValue);
            }
        });
    }

    private void updateCampaignZone(String candidateInfo) {
        // Extract basic data to show on screen layout
        candidateNameLabel.setText(candidateInfo.split(" \\(")[0]);
        partyLabel.setText("Affiliation: " + candidateInfo.substring(candidateInfo.indexOf("(")));

        // Inject dummy files depending on selection
        ObservableList<String> files = FXCollections.observableArrayList(
                "official_manifesto_2026.pdf",
                "economic_reform_charter.txt",
                "campaign_press_release.pdf"
        );
        documentListView.setItems(files);
    }
}