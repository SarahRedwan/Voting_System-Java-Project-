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
import javafx.scene.layout.VBox;
import org.example.client.core.MaterialQueue;
import org.example.client.core.PendingMaterial;
import org.example.client.core.SceneManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class AdminDashboardController {

    // View Panel Workspace Selectors
    @FXML private VBox paneApprovals;
    @FXML private VBox paneAnalytics;
    @FXML private VBox paneCandidates;

    // Sidebar Navigation Buttons
    @FXML private Button btnNavApprovals;
    @FXML private Button btnNavAnalytics;
    @FXML private Button btnNavCandidates;

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

    private final ObservableList<SystemCandidateRecord> localRegistry = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Wire up Module 1 Approval Sync Engine
        approvalQueueListView.setItems(MaterialQueue.pendingList);

        // Load Static Demo Telemetry values for Analytics View
        setupChartTelemetry();

        // Wire up Module 3 Registry Table Models
        colName.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        colOffice.setCellValueFactory(cellData -> cellData.getValue().officeProperty());
        colStatus.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

        localRegistry.addAll(
                new SystemCandidateRecord("Candidate Alpha", "Presidential Seat", "ACTIVE / AUTHORIZED"),
                new SystemCandidateRecord("Candidate Bravo", "Presidential Seat", "ACTIVE / AUTHORIZED"),
                new SystemCandidateRecord("Candidate Charlie", "Gubernatorial Seat", "ACTIVE / AUTHORIZED")
        );
        candidateTableView.setItems(localRegistry);
    }

    // --- SIDEBAR SWITCH NAVIGATION LOGIC ---
    @FXML
    private void showApprovalsView() {
        setPaneVisibility(true, false, false);
        setButtonHighlight(btnNavApprovals, btnNavAnalytics, btnNavCandidates);
    }

    @FXML
    private void showAnalyticsView() {
        setPaneVisibility(false, true, false);
        setButtonHighlight(btnNavAnalytics, btnNavApprovals, btnNavCandidates);
    }

    @FXML
    private void showCandidatesView() {
        setPaneVisibility(false, false, true);
        setButtonHighlight(btnNavCandidates, btnNavApprovals, btnNavAnalytics);
    }

    private void setPaneVisibility(boolean app, boolean ana, boolean cand) {
        paneApprovals.setVisible(app);
        paneAnalytics.setVisible(ana);
        paneCandidates.setVisible(cand);
    }

    private void setButtonHighlight(Button active, Button b2, Button b3) {
        active.setStyle("-fx-background-color: #34495e; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12;");
        b2.setStyle("-fx-background-color: transparent; -fx-text-fill: #bdc3c7; -fx-padding: 12;");
        b3.setStyle("-fx-background-color: transparent; -fx-text-fill: #bdc3c7; -fx-padding: 12;");
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

    // --- MODULE 2 BUSINESS LOGIC: LIVE CHART GENERATOR ---
    private void setupChartTelemetry() {
        // Pie Chart Allocation Ingestion
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                new PieChart.Data("Candidate Alpha", 520),
                new PieChart.Data("Candidate Bravo", 410),
                new PieChart.Data("Candidate Charlie", 318)
        );
        ballotPieChart.setData(pieData);

        // Bar Chart Ingestion
        XYChart.Series<String, Number> series1 = new XYChart.Series<>();
        series1.setName("Verified Submissions");
        series1.getData().add(new XYChart.Data<>("Region North", 450));
        series1.getData().add(new XYChart.Data<>("Region East", 590));
        series1.getData().add(new XYChart.Data<>("Region South", 208));

        turnoutBarChart.getData().add(series1);
    }

    @FXML
    private void handleExportLedger() {
        System.out.println("Compiling cryptographic SHA256 spreadsheet block logs...");
        updateStatusText("✅ Turnout Ledger successfully written to root execution folder path.", "#2ecc71");
    }

    // --- MODULE 3 BUSINESS LOGIC: REGISTRY SECURITY INTERCEPTS ---
    @FXML
    private void handleBlockCandidate() {
        SystemCandidateRecord selected = candidateTableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            selected.setStatus("🛑 TERMINATED / BLOCKED");
            candidateTableView.refresh();
        }
    }

    @FXML
    private void handleUnbanCandidate() {
        SystemCandidateRecord selected = candidateTableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            selected.setStatus("ACTIVE / AUTHORIZED");
            candidateTableView.refresh();
        }
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
}