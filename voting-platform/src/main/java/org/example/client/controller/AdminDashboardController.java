package org.example.client.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.example.client.core.Candidate;
import org.example.client.core.DataManager;
import org.example.client.core.MaterialQueue;
import org.example.client.core.PendingMaterial;
import org.example.client.core.SceneManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class AdminDashboardController {

    // View Panel Workspace Selectors
    @FXML private VBox paneApprovals;
    @FXML private VBox paneAnalytics;
    @FXML private VBox paneCandidates;
    @FXML private VBox paneVoteProgress;

    // Sidebar Navigation Buttons
    @FXML private Button btnNavApprovals;
    @FXML private Button btnNavAnalytics;
    @FXML private Button btnNavCandidates;
    @FXML private Button btnNavVoteProgress;

    // Module 1 Elements (Approvals)
    @FXML private ListView<PendingMaterial> approvalQueueListView;
    @FXML private Label moderationStatusLabel;

    // Module 2 Elements (Analytics Engine Charts)
    @FXML private PieChart ballotPieChart;
    @FXML private BarChart<String, Number> turnoutBarChart;

    // Module 3 Elements (Candidate Management Registry Table)
    @FXML private TableView<SystemCandidateRecord> candidateTableView;
    @FXML private TableColumn<SystemCandidateRecord, String> colName;
    @FXML private TableColumn<SystemCandidateRecord, String> colOffice;
    @FXML private TableColumn<SystemCandidateRecord, String> colStatus;
    @FXML private TextField candidateNameField;
    @FXML private TextField candidateOfficeField;
    @FXML private Label candidateActionStatusLabel;

    // Module 4 Elements (Vote Progress)
    @FXML private Label statTotalVotes;
    @FXML private Label statTotalCandidates;
    @FXML private Label statLeader;
    @FXML private Label statLeaderVotes;
    @FXML private TableView<VoteProgressRecord> voteProgressTable;
    @FXML private TableColumn<VoteProgressRecord, String> colVpRank;
    @FXML private TableColumn<VoteProgressRecord, String> colVpName;
    @FXML private TableColumn<VoteProgressRecord, String> colVpOffice;
    @FXML private TableColumn<VoteProgressRecord, String> colVpVotes;
    @FXML private TableColumn<VoteProgressRecord, String> colVpPercent;
    @FXML private TableColumn<VoteProgressRecord, String> colVpStatus;
    @FXML private Label voteProgressStatusLabel;

    private final ObservableList<SystemCandidateRecord> localRegistry = FXCollections.observableArrayList();
    private final ObservableList<VoteProgressRecord> voteProgressList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        approvalQueueListView.setItems(MaterialQueue.pendingList);
        colName.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        colOffice.setCellValueFactory(cellData -> cellData.getValue().officeProperty());
        colStatus.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

        // Vote progress table columns
        colVpRank.setCellValueFactory(cellData -> cellData.getValue().rankProperty());
        colVpName.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        colVpOffice.setCellValueFactory(cellData -> cellData.getValue().officeProperty());
        colVpVotes.setCellValueFactory(cellData -> cellData.getValue().votesProperty());
        colVpPercent.setCellValueFactory(cellData -> cellData.getValue().percentProperty());
        colVpStatus.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

        refreshCandidateTable();
        setupChartTelemetry();
        refreshVoteProgress();
    }

    // --- SIDEBAR SWITCH NAVIGATION LOGIC ---
    @FXML
    private void showApprovalsView() {
        setPaneVisibility(true, false, false, false);
        setButtonHighlight(btnNavApprovals, btnNavAnalytics, btnNavCandidates, btnNavVoteProgress);
    }

    @FXML
    private void showAnalyticsView() {
        setPaneVisibility(false, true, false, false);
        setButtonHighlight(btnNavAnalytics, btnNavApprovals, btnNavCandidates, btnNavVoteProgress);
    }

    @FXML
    private void showCandidatesView() {
        setPaneVisibility(false, false, true, false);
        setButtonHighlight(btnNavCandidates, btnNavApprovals, btnNavAnalytics, btnNavVoteProgress);
    }

    @FXML
    private void showVoteProgressView() {
        refreshVoteProgress();
        setPaneVisibility(false, false, false, true);
        setButtonHighlight(btnNavVoteProgress, btnNavApprovals, btnNavAnalytics, btnNavCandidates);
    }

    private void setPaneVisibility(boolean app, boolean ana, boolean cand, boolean vp) {
        paneApprovals.setVisible(app);
        paneAnalytics.setVisible(ana);
        paneCandidates.setVisible(cand);
        paneVoteProgress.setVisible(vp);
    }

    private void setButtonHighlight(Button active, Button b2, Button b3, Button b4) {
        active.setStyle("-fx-background-color: #34495e; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12;");
        b2.setStyle("-fx-background-color: transparent; -fx-text-fill: #bdc3c7; -fx-padding: 12;");
        b3.setStyle("-fx-background-color: transparent; -fx-text-fill: #bdc3c7; -fx-padding: 12;");
        b4.setStyle("-fx-background-color: transparent; -fx-text-fill: #bdc3c7; -fx-padding: 12;");
    }

    // --- MODULE 1 BUSINESS LOGIC: MATERIALS APPROVAL ---
    @FXML
    private void handleApproveMaterial() {
        PendingMaterial selectedItem = approvalQueueListView.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            updateStatusText("❌ Select a submission entry row first!", "#e74c3c");
            return;
        }

        try {
            File storageDir = new File("src/main/resources/uploads/");
            if (!storageDir.exists()) storageDir.mkdirs();

            if (selectedItem.getPdfFile() != null) {
                Files.copy(selectedItem.getPdfFile().toPath(), new File(storageDir, selectedItem.getPdfFile().getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            if (selectedItem.getVideoFile() != null) {
                Files.copy(selectedItem.getVideoFile().toPath(), new File(storageDir, selectedItem.getVideoFile().getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            updateStatusText("✅ Materials verified and saved successfully to public node repository.", "#2ecc71");
            MaterialQueue.pendingList.remove(selectedItem);

        } catch (IOException e) {
            updateStatusText("❌ Fatal file system write failure.", "#e74c3c");
        }
    }

    @FXML
    private void handleRejectMaterial() {
        PendingMaterial selectedItem = approvalQueueListView.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            MaterialQueue.pendingList.remove(selectedItem);
            updateStatusText("🛑 Candidate assets rejected and purged from transient ledger.", "#c0392b");
        }
    }

    @FXML
    private void handleAddCandidate() {
        String name = candidateNameField.getText().trim();
        String office = candidateOfficeField.getText().trim();

        if (name.isEmpty() || office.isEmpty()) {
            candidateActionStatusLabel.setText("❌ Please enter candidate name and office.");
            candidateActionStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
            return;
        }

        boolean added = DataManager.addCandidate(name, office);
        if (added) {
            candidateActionStatusLabel.setText("✅ Candidate added successfully.");
            candidateActionStatusLabel.setStyle("-fx-text-fill: #2ecc71;");
            candidateNameField.clear();
            candidateOfficeField.clear();
            refreshCandidateTable();
            setupChartTelemetry();
        } else {
            candidateActionStatusLabel.setText("❌ Candidate could not be added. Name may already exist.");
            candidateActionStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
        }
    }

    @FXML
    private void handleRemoveCandidate() {
        SystemCandidateRecord selected = candidateTableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            candidateActionStatusLabel.setText("❌ Select a candidate row first.");
            candidateActionStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
            return;
        }

        boolean removed = DataManager.removeCandidate(selected.getName());
        if (removed) {
            candidateActionStatusLabel.setText("✅ Candidate removed successfully.");
            candidateActionStatusLabel.setStyle("-fx-text-fill: #2ecc71;");
            refreshCandidateTable();
            setupChartTelemetry();
        } else {
            candidateActionStatusLabel.setText("❌ Candidate could not be removed.");
            candidateActionStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
        }
    }

    // --- MODULE 2 BUSINESS LOGIC: LIVE CHART GENERATOR ---
    private void setupChartTelemetry() {
        List<Candidate> candidates = DataManager.getRankedCandidates();
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        XYChart.Series<String, Number> series1 = new XYChart.Series<>();
        series1.setName("Vote Counts");

        int totalVotes = 0;
        for (Candidate candidate : candidates) {
            pieData.add(new PieChart.Data(candidate.getName(), candidate.getVotes()));
            series1.getData().add(new XYChart.Data<>(candidate.getName(), candidate.getVotes()));
            totalVotes += candidate.getVotes();
        }

        if (pieData.isEmpty()) {
            pieData.addAll(
                    new PieChart.Data("Candidate Alpha", 0),
                    new PieChart.Data("Candidate Bravo", 0),
                    new PieChart.Data("Candidate Charlie", 0)
            );
        }

        if (series1.getData().isEmpty()) {
            series1.getData().add(new XYChart.Data<>("Candidate Alpha", 0));
            series1.getData().add(new XYChart.Data<>("Candidate Bravo", 0));
            series1.getData().add(new XYChart.Data<>("Candidate Charlie", 0));
        }

        ballotPieChart.setData(pieData);
        turnoutBarChart.getData().clear();
        turnoutBarChart.getData().add(series1);
    }

    @FXML
    private void handleExportLedger() {
        try {
            Path csv = DataManager.exportResults();
            updateStatusText("✅ Results exported to: " + csv.toAbsolutePath(), "#2ecc71");
        } catch (IOException e) {
            updateStatusText("❌ Export failed: " + e.getMessage(), "#e74c3c");
        }
    }

    // --- MODULE 3 BUSINESS LOGIC: REGISTRY SECURITY INTERCEPTS ---
    @FXML
    private void handleBlockCandidate() {
        SystemCandidateRecord selected = candidateTableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            if (DataManager.setCandidateStatus(selected.getName(), "🛑 TERMINATED / BLOCKED")) {
                selected.setStatus("🛑 TERMINATED / BLOCKED");
                candidateTableView.refresh();
                refreshCandidateTable();
                setupChartTelemetry();
                updateStatusText("🛑 Candidate blocked and persisted to database.", "#c0392b");
            } else {
                updateStatusText("❌ Unable to block candidate.", "#e74c3c");
            }
        }
    }

    @FXML
    private void handleUnbanCandidate() {
        SystemCandidateRecord selected = candidateTableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            if (DataManager.setCandidateStatus(selected.getName(), "ACTIVE / AUTHORIZED")) {
                selected.setStatus("ACTIVE / AUTHORIZED");
                candidateTableView.refresh();
                refreshCandidateTable();
                setupChartTelemetry();
                updateStatusText("✅ Candidate unbanned and reactivated.", "#2ecc71");
            } else {
                updateStatusText("❌ Unable to unban candidate.", "#e74c3c");
            }
        }
    }

    // --- MODULE 4 BUSINESS LOGIC: VOTE PROGRESS TRACKER ---
    @FXML
    private void handleRefreshVoteProgress() {
        refreshVoteProgress();
        voteProgressStatusLabel.setStyle("-fx-text-fill: #27ae60;");
        voteProgressStatusLabel.setText("✅ Data refreshed successfully.");
    }

    private void refreshVoteProgress() {
        List<Candidate> candidates = DataManager.getRankedCandidates();
        voteProgressList.clear();

        int totalVotes = candidates.stream().mapToInt(Candidate::getVotes).sum();
        int totalCandidates = candidates.size();

        // Update stat cards
        statTotalVotes.setText(String.valueOf(totalVotes));
        statTotalCandidates.setText(String.valueOf(totalCandidates));

        if (!candidates.isEmpty()) {
            Candidate leader = candidates.get(0); // already sorted by votes DESC
            statLeader.setText(leader.getName());
            statLeaderVotes.setText(leader.getVotes() + " votes");
        } else {
            statLeader.setText("—");
            statLeaderVotes.setText("0 votes");
        }

        // Build ranked rows
        int rank = 1;
        for (Candidate c : candidates) {
            double pct = totalVotes > 0 ? (c.getVotes() * 100.0 / totalVotes) : 0.0;
            String pctStr = String.format("%.1f%%", pct);
            String bar = buildProgressBar(pct);
            voteProgressList.add(new VoteProgressRecord(
                    String.valueOf(rank++),
                    c.getName(),
                    c.getOffice(),
                    String.valueOf(c.getVotes()),
                    bar + " " + pctStr,
                    c.getStatus()
            ));
        }

        voteProgressTable.setItems(voteProgressList);
        setupChartTelemetry(); // keep charts in sync
    }

    /** Builds a simple ASCII progress bar for the percentage column. */
    private String buildProgressBar(double pct) {
        int filled = (int) Math.round(pct / 5); // 20 blocks = 100%
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < 20; i++) {
            sb.append(i < filled ? "█" : "░");
        }
        sb.append("]");
        return sb.toString();
    }

    // --- GLOBAL PLATFORM CONTROLS ---
    @FXML
    private void handleSystemFreeze() {
        System.out.println("🚨 CRITICAL CORE INTERCEPT ALERT: GENERAL RUNTIME SYSTEM LOCKING SEQUENCE INITIALIZED.");
    }

    @FXML
    private void handleLogout() {
        SceneManager.switchScene("WelcomeView.fxml", "Welcome to SecureVote 2026");
    }

    private void refreshCandidateTable() {
        localRegistry.clear();
        List<Candidate> candidates = DataManager.getRankedCandidates();
        if (candidates.isEmpty()) {
            localRegistry.addAll(
                    new SystemCandidateRecord("Candidate Alpha", "Presidential Seat", "ACTIVE / AUTHORIZED"),
                    new SystemCandidateRecord("Candidate Bravo", "Presidential Seat", "ACTIVE / AUTHORIZED"),
                    new SystemCandidateRecord("Candidate Charlie", "Gubernatorial Seat", "ACTIVE / AUTHORIZED")
            );
        } else {
            for (Candidate candidate : candidates) {
                localRegistry.add(new SystemCandidateRecord(candidate.getName(), candidate.getOffice(), candidate.getStatus()));
            }
        }
        candidateTableView.setItems(localRegistry);
    }

    private void updateStatusText(String text, String hexColor) {
        moderationStatusLabel.setText(text);
        moderationStatusLabel.setStyle("-fx-text-fill: " + hexColor + ";");
    }

    // Inner class helper to represent clean candidate records inside the TableView
    public static class SystemCandidateRecord {
        private final SimpleStringProperty name;
        private final SimpleStringProperty office;
        private final SimpleStringProperty status;

        public SystemCandidateRecord(String name, String office, String status) {
            this.name = new SimpleStringProperty(name);
            this.office = new SimpleStringProperty(office);
            this.status = new SimpleStringProperty(status);
        }

        public String getName() { return name.get(); }
        public SimpleStringProperty nameProperty() { return name; }
        public String getOffice() { return office.get(); }
        public SimpleStringProperty officeProperty() { return office; }
        public String getStatus() { return status.get(); }
        public void setStatus(String value) { this.status.set(value); }
        public SimpleStringProperty statusProperty() { return status; }
    }

    // Inner class for vote progress table rows
    public static class VoteProgressRecord {
        private final SimpleStringProperty rank;
        private final SimpleStringProperty name;
        private final SimpleStringProperty office;
        private final SimpleStringProperty votes;
        private final SimpleStringProperty percent;
        private final SimpleStringProperty status;

        public VoteProgressRecord(String rank, String name, String office, String votes, String percent, String status) {
            this.rank    = new SimpleStringProperty(rank);
            this.name    = new SimpleStringProperty(name);
            this.office  = new SimpleStringProperty(office);
            this.votes   = new SimpleStringProperty(votes);
            this.percent = new SimpleStringProperty(percent);
            this.status  = new SimpleStringProperty(status);
        }

        public SimpleStringProperty rankProperty()    { return rank; }
        public SimpleStringProperty nameProperty()    { return name; }
        public SimpleStringProperty officeProperty()  { return office; }
        public SimpleStringProperty votesProperty()   { return votes; }
        public SimpleStringProperty percentProperty() { return percent; }
        public SimpleStringProperty statusProperty()  { return status; }
    }
}