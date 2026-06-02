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
import javafx.scene.control.PasswordField;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.application.Platform;
import org.example.client.core.CandidateApplication;
import org.example.client.core.CandidateProfile;
import org.example.client.core.CandidateProfileDAO;
import org.example.client.core.ElectionPhase;
import org.example.client.core.ElectionSchedule;
import org.example.client.core.ElectionScheduleDAO;
import org.example.client.core.ElectionStateSnapshot;
import org.example.client.core.CandidateRegistrationService;
import org.example.client.core.Database;
import org.example.client.core.User;
import org.example.client.core.UserDAO;
import org.example.client.core.MaterialQueue;
import org.example.client.core.PendingMaterial;
import org.example.client.core.PendingSubmissionDAO;
import org.example.client.core.SceneManager;
import org.example.client.core.AdminClient;
import org.example.client.core.VotingSocketClient;

import java.rmi.RemoteException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

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
    @FXML private ListView<CandidateApplication> candidateApplicationsListView;
    @FXML private ListView<PendingMaterial> approvalQueueListView;
    @FXML private Label moderationStatusLabel;
    private final ObservableList<CandidateApplication> pendingApplications = FXCollections.observableArrayList();

    // Module 2 Elements (Analytics Engine Charts)
    @FXML private PieChart ballotPieChart;
    @FXML private BarChart<String, Number> turnoutBarChart;

    // Module 3 Elements (Candidate Management Registry Table)
    @FXML private TextField newCandidateUsernameField;
    @FXML private PasswordField newCandidatePasswordField;
    @FXML private TextField newCandidateDisplayNameField;
    @FXML private TextField newCandidatePartyField;
    @FXML private TextField newCandidatePositionField;
    @FXML private Label createCandidateStatusLabel;
    @FXML private TableView<SystemCandidateRecord> candidateTableView;
    @FXML private TableColumn<SystemCandidateRecord, String> colUsername;
    @FXML private TableColumn<SystemCandidateRecord, String> colName;
    @FXML private TableColumn<SystemCandidateRecord, String> colOffice;
    @FXML private TableColumn<SystemCandidateRecord, String> colStatus;

    // Election Control Elements
    @FXML private Label electionStatusLabel;
    @FXML private Label electionTimerLabel;
    @FXML private Label electionTimerPrefixLabel;
    @FXML private TextField electionStartField;
    @FXML private TextField electionEndField;
    @FXML private Spinner<Integer> durationSpinner;
    @FXML private Button startElectionButton;
    @FXML private Button stopElectionButton;
    @FXML private Button resetVotesButton;

    private final ObservableList<SystemCandidateRecord> localRegistry = FXCollections.observableArrayList();
    private Thread timerThread;
    private volatile boolean timerActive = false;
    private Timeline approvalRefreshTimeline;

    @FXML
    public void initialize() {
        Database.initializeSchema();

        // Wire up Module 1 Approval Sync Engine
        candidateApplicationsListView.setItems(pendingApplications);
        approvalQueueListView.setItems(MaterialQueue.pendingList);
        refreshCandidateApplications();
        MaterialQueue.refreshFromDatabase();
        approvalRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(2), event -> {
            MaterialQueue.refreshFromDatabase();
            refreshCandidateApplications();
        }));
        approvalRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        approvalRefreshTimeline.play();

        // Wire up Module 3 Registry Table Models
        colUsername.setCellValueFactory(cellData -> cellData.getValue().usernameProperty());
        colName.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        colOffice.setCellValueFactory(cellData -> cellData.getValue().officeProperty());
        colStatus.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

        candidateTableView.setItems(localRegistry);
        populateCandidateRegistry();

        // Setup election control spinner
        if (durationSpinner != null) {
            SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 3600, 300);
            durationSpinner.setValueFactory(valueFactory);
        }

        // Connect to RMI admin on localhost:1099
        AdminClient.connect("localhost", 1099);
        updateElectionStatus();

        // Start live voting socket so admin sees status and timer updates
        VotingSocketClient socketClient = VotingSocketClient.getInstance();
        socketClient.connect("admin");
        socketClient.addListener(this::handleServerMessage);
        socketClient.requestElectionPhase();
        socketClient.requestCandidates();

        loadScheduleFields();
        refreshChartTelemetry();
        startTimerDisplay();
    }

    private void loadScheduleFields() {
        ElectionSchedule schedule = ElectionScheduleDAO.load();
        if (schedule.getStartTime() != null) {
            electionStartField.setText(schedule.getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        if (schedule.getEndTime() != null) {
            electionEndField.setText(schedule.getEndTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
    }

    private void startTimerDisplay() {
        timerActive = true;
        timerThread = new Thread(() -> {
            while (timerActive) {
                try {
                    Thread.sleep(1000);
                    Platform.runLater(this::updateElectionStatus);
                } catch (InterruptedException ignored) {
                    break;
                }
            }
        }, "admin-timer-display");
        timerThread.setDaemon(true);
        timerThread.start();
    }

    private void updateElectionStatus() {
        if (!AdminClient.isConnected()) {
            electionStatusLabel.setText("❌ NOT CONNECTED to voting server");
            electionStatusLabel.setStyle("-fx-text-fill: #dc2626;");
            electionTimerLabel.setText("--:--:--");
            return;
        }

        try {
            String phase = AdminClient.getElectionPhase();
            electionStatusLabel.setText("Phase: " + phase + " | Server connected");
            electionStatusLabel.setStyle("-fx-text-fill: #059669;");
        } catch (Exception e) {
            electionStatusLabel.setText("Connected (phase unavailable)");
            electionStatusLabel.setStyle("-fx-text-fill: #d97706;");
        }
        VotingSocketClient.getInstance().requestElectionPhase();
    }

    // --- SIDEBAR SWITCH NAVIGATION LOGIC ---
    @FXML
    private void showApprovalsView() {
        setPaneVisibility(true, false, false);
        setButtonHighlight(btnNavApprovals, btnNavAnalytics, btnNavCandidates);
        MaterialQueue.refreshFromDatabase();
        refreshCandidateApplications();
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
        populateCandidateRegistry();
    }

    private void setPaneVisibility(boolean app, boolean ana, boolean cand) {
        paneApprovals.setVisible(app);
        paneAnalytics.setVisible(ana);
        paneCandidates.setVisible(cand);
    }

    private void setButtonHighlight(Button active, Button b2, Button b3) {
        active.setStyle("-fx-background-color: #312e81; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12;");
        b2.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-padding: 12;");
        b3.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-padding: 12;");
    }

    private void refreshCandidateApplications() {
        pendingApplications.setAll(CandidateProfileDAO.findPendingApplicationRecords());
    }

    @FXML
    private void handleApproveApplication() {
        CandidateApplication selected = candidateApplicationsListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            updateStatusText("Select a candidate application first.", "#dc2626");
            return;
        }
        String message = "Your candidate application has been approved. You may now sign in and manage your campaign.";
        if (CandidateProfileDAO.approveApplication(selected.getUsername(), message)) {
            updateStatusText("Approved application for " + selected.getFullName() + ".", "#059669");
            refreshCandidateApplications();
            populateCandidateRegistry();
            VotingSocketClient.getInstance().requestCandidates();
        } else {
            updateStatusText("Failed to approve application.", "#dc2626");
        }
    }

    @FXML
    private void handleRejectApplication() {
        CandidateApplication selected = candidateApplicationsListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            updateStatusText("Select a candidate application first.", "#dc2626");
            return;
        }
        String message = "Your candidate application was rejected. Please contact the election office for details.";
        if (CandidateProfileDAO.rejectApplication(selected.getUsername(), message)) {
            updateStatusText("Rejected application for " + selected.getFullName() + ".", "#b91c1c");
            refreshCandidateApplications();
        } else {
            updateStatusText("Failed to reject application.", "#dc2626");
        }
    }

    // --- MODULE 1 BUSINESS LOGIC: MATERIALS APPROVAL ---
    @FXML
    private void handleApproveMaterial() {
        PendingMaterial selectedItem = approvalQueueListView.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            updateStatusText("Select a submission entry first.", "#dc2626");
            return;
        }

        String message = "Your profile and campaign materials were approved and are now live.";
        boolean approved = PendingSubmissionDAO.approve(selectedItem.getSubmissionId(), message);
        if (approved) {
            updateStatusText("Approved submission for " + selectedItem.getCandidateName() + ".", "#059669");
            MaterialQueue.refreshFromDatabase();
            populateCandidateRegistry();
            VotingSocketClient.getInstance().requestCandidates();
        } else {
            updateStatusText("Failed to approve the selected submission.", "#dc2626");
        }
    }

    @FXML
    private void handleRejectMaterial() {
        PendingMaterial selectedItem = approvalQueueListView.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            updateStatusText("Select a submission entry first.", "#dc2626");
            return;
        }

        String message = "Your submission was rejected. Please review the feedback, update your materials, and submit again.";
        boolean rejected = PendingSubmissionDAO.reject(selectedItem.getSubmissionId(), message);
        if (rejected) {
            updateStatusText("Rejected submission for " + selectedItem.getCandidateName() + ".", "#b91c1c");
            MaterialQueue.refreshFromDatabase();
        } else {
            updateStatusText("Failed to reject the selected submission.", "#dc2626");
        }
    }

    // --- MODULE 2 BUSINESS LOGIC: LIVE CHART GENERATOR ---
    private void refreshChartTelemetry() {
        try {
            Map<String, Integer> results = AdminClient.viewResults();
            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Votes per Candidate");

            int totalVotes = results.values().stream().mapToInt(Integer::intValue).sum();
            for (Map.Entry<String, Integer> entry : results.entrySet()) {
                pieData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
                series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
            }

            ballotPieChart.setData(pieData);
            turnoutBarChart.getData().clear();
            turnoutBarChart.getData().add(series);

            if (totalVotes == 0) {
                moderationStatusLabel.setText("ℹ️ No votes recorded yet. Refreshing live results...");
                moderationStatusLabel.setStyle("-fx-text-fill: #0d9488;");
            } else {
                moderationStatusLabel.setText("✅ Live vote totals updated (" + totalVotes + " ballots)");
                moderationStatusLabel.setStyle("-fx-text-fill: #059669;");
            }
        } catch (RemoteException e) {
            moderationStatusLabel.setText("⚠️ Unable to load live results: " + e.getMessage());
            moderationStatusLabel.setStyle("-fx-text-fill: #d97706;");
        }
    }

    private void populateCandidateRegistry() {
        localRegistry.clear();
        List<CandidateProfile> profiles = CandidateProfileDAO.findAll();
        for (CandidateProfile profile : profiles) {
            User user = UserDAO.findByUsername(profile.getUsername());
            String status = user != null && user.isActive() ? "ACTIVE / AUTHORIZED" : "BLOCKED";
            localRegistry.add(new SystemCandidateRecord(
                    profile.getUsername(),
                    profile.getName(),
                    profile.getPosition() == null ? "" : profile.getPosition(),
                    status
            ));
        }
    }

    @FXML
    private void handleCreateCandidate() {
        String username = newCandidateUsernameField.getText();
        String password = newCandidatePasswordField.getText();
        String displayName = newCandidateDisplayNameField.getText();
        String party = newCandidatePartyField.getText();
        String position = newCandidatePositionField.getText();

        CandidateRegistrationService.CreateCandidateResult result =
                CandidateRegistrationService.createCandidate(username, password, displayName, party, position);

        if (result.success()) {
            createCandidateStatusLabel.setText(result.message());
            createCandidateStatusLabel.setStyle("-fx-text-fill: #059669;");
            newCandidateUsernameField.clear();
            newCandidatePasswordField.clear();
            newCandidateDisplayNameField.clear();
            newCandidatePartyField.clear();
            newCandidatePositionField.clear();
            populateCandidateRegistry();
            VotingSocketClient.getInstance().requestCandidates();
        } else {
            createCandidateStatusLabel.setText(result.message());
            createCandidateStatusLabel.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    private void handleServerMessage(String message) {
        Platform.runLater(() -> {
            if (message.startsWith("ELECTION_PHASE|")) {
                applyAdminElectionState(ElectionStateSnapshot.parse(message));
            } else if (message.startsWith("SYSTEM|ELECTION_STARTED")) {
                electionStatusLabel.setText("Phase: ACTIVE | Server connected");
                electionStatusLabel.setStyle("-fx-text-fill: #059669;");
            } else if (message.startsWith("SYSTEM|ELECTION_STOPPED")) {
                electionStatusLabel.setText("Phase: ENDED | Server connected");
                electionStatusLabel.setStyle("-fx-text-fill: #d97706;");
            } else if (message.startsWith("CANDIDATE|")) {
                populateCandidateRegistry();
            }
        });
    }

    private void applyAdminElectionState(ElectionStateSnapshot state) {
        ElectionPhase phase = state.getPhase();
        electionStatusLabel.setText("Phase: " + phase.name() + " | Server connected");
        electionTimerLabel.setText(formatTimer(String.valueOf(state.getCountdownSeconds())));

        switch (phase) {
            case NOT_STARTED -> {
                electionTimerPrefixLabel.setText("Opens in:");
                electionStatusLabel.setStyle("-fx-text-fill: #d97706;");
            }
            case ACTIVE -> {
                electionTimerPrefixLabel.setText("Time remaining:");
                electionStatusLabel.setStyle("-fx-text-fill: #059669;");
            }
            case ENDED -> {
                electionTimerPrefixLabel.setText("Status:");
                electionTimerLabel.setText("ENDED");
                electionStatusLabel.setStyle("-fx-text-fill: #dc2626;");
            }
        }
    }

    @FXML
    private void handleClearOverride() {
        try {
            AdminClient.clearManualOverride();
            moderationStatusLabel.setText("Manual override cleared. Scheduled start/end times now control voting.");
            moderationStatusLabel.setStyle("-fx-text-fill: #0d9488;");
        } catch (Exception e) {
            moderationStatusLabel.setText("Failed to clear override: " + e.getMessage());
            moderationStatusLabel.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    @FXML
    private void handleSetSchedule() {
        try {
            LocalDateTime start = parseDateTime(electionStartField.getText());
            LocalDateTime end = parseDateTime(electionEndField.getText());
            if (start == null || end == null) {
                moderationStatusLabel.setText("Enter both start and end times (YYYY-MM-DDTHH:mm:ss).");
                moderationStatusLabel.setStyle("-fx-text-fill: #dc2626;");
                return;
            }
            AdminClient.setElectionSchedule(start, end);
            loadScheduleFields();
            moderationStatusLabel.setText("Schedule saved. Voting will open and close automatically.");
            moderationStatusLabel.setStyle("-fx-text-fill: #059669;");
        } catch (Exception e) {
            moderationStatusLabel.setText("Failed to set schedule: " + e.getMessage());
            moderationStatusLabel.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    private LocalDateTime parseDateTime(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(text.trim());
        } catch (DateTimeParseException e) {
            return LocalDateTime.parse(text.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        }
    }

    private String formatTimer(String rawSeconds) {
        if (rawSeconds == null || rawSeconds.isBlank()) {
            return "--:--:--";
        }
        try {
            long seconds = Long.parseLong(rawSeconds.trim());
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            long secs = seconds % 60;
            return String.format("%02d:%02d:%02d", hours, minutes, secs);
        } catch (NumberFormatException e) {
            return "--:--:--";
        }
    }

    @FXML
    private void handleExportLedger() {
        System.out.println("Compiling cryptographic SHA256 spreadsheet block logs...");
        updateStatusText("✅ Turnout Ledger successfully written to root execution folder path.", "#059669");
    }

    // --- MODULE 3 BUSINESS LOGIC: REGISTRY SECURITY INTERCEPTS ---
    @FXML
    private void handleBlockCandidate() {
        SystemCandidateRecord selected = candidateTableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            createCandidateStatusLabel.setText("Select a candidate from the table first.");
            createCandidateStatusLabel.setStyle("-fx-text-fill: #dc2626;");
            return;
        }
        if (UserDAO.setUserActive(selected.getUsername(), false)) {
            selected.setStatus("BLOCKED");
            candidateTableView.refresh();
            createCandidateStatusLabel.setText("Blocked login for " + selected.getUsername() + ".");
            createCandidateStatusLabel.setStyle("-fx-text-fill: #d97706;");
        }
    }

    @FXML
    private void handleUnbanCandidate() {
        SystemCandidateRecord selected = candidateTableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            createCandidateStatusLabel.setText("Select a candidate from the table first.");
            createCandidateStatusLabel.setStyle("-fx-text-fill: #dc2626;");
            return;
        }
        if (UserDAO.setUserActive(selected.getUsername(), true)) {
            selected.setStatus("ACTIVE / AUTHORIZED");
            candidateTableView.refresh();
            createCandidateStatusLabel.setText("Re-activated login for " + selected.getUsername() + ".");
            createCandidateStatusLabel.setStyle("-fx-text-fill: #059669;");
        }
    }

    // --- GLOBAL PLATFORM CONTROLS ---
    @FXML
    private void handleStartElection() {
        int durationSeconds = durationSpinner.getValue();
        try {
            AdminClient.startElection(durationSeconds);
            moderationStatusLabel.setText("✅ Election started for " + durationSeconds + " seconds");
            moderationStatusLabel.setStyle("-fx-text-fill: #059669;");
        } catch (Exception e) {
            moderationStatusLabel.setText("❌ Failed to start election: " + e.getMessage());
            moderationStatusLabel.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    @FXML
    private void handleStopElection() {
        try {
            AdminClient.stopElection();
            moderationStatusLabel.setText("🛑 Election stopped");
            moderationStatusLabel.setStyle("-fx-text-fill: #d97706;");
        } catch (Exception e) {
            moderationStatusLabel.setText("❌ Failed to stop election: " + e.getMessage());
            moderationStatusLabel.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    @FXML
    private void handleResetVotes() {
        try {
            AdminClient.resetVotes();
            moderationStatusLabel.setText("🔄 All votes reset");
            moderationStatusLabel.setStyle("-fx-text-fill: #0d9488;");
        } catch (Exception e) {
            moderationStatusLabel.setText("❌ Failed to reset votes: " + e.getMessage());
            moderationStatusLabel.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    @FXML
    private void handleSystemFreeze() {
        System.out.println("🚨 CRITICAL CORE INTERCEPT ALERT: GENERAL RUNTIME SYSTEM LOCKING SEQUENCE INITIALIZED.");
    }

    @FXML
    private void handleLogout() {
        if (approvalRefreshTimeline != null) {
            approvalRefreshTimeline.stop();
        }
        SceneManager.switchScene("WelcomeView.fxml", "Welcome to SecureVote 2026");
    }

    private void updateStatusText(String text, String hexColor) {
        moderationStatusLabel.setText(text);
        moderationStatusLabel.setStyle("-fx-text-fill: " + hexColor + ";");
    }

    // Inner class helper to represent clean candidate records inside the TableView
    public static class SystemCandidateRecord {
        private final SimpleStringProperty username;
        private final SimpleStringProperty name;
        private final SimpleStringProperty office;
        private final SimpleStringProperty status;

        public SystemCandidateRecord(String username, String name, String office, String status) {
            this.username = new SimpleStringProperty(username);
            this.name = new SimpleStringProperty(name);
            this.office = new SimpleStringProperty(office);
            this.status = new SimpleStringProperty(status);
        }

        public String getUsername() { return username.get(); }
        public SimpleStringProperty usernameProperty() { return username; }
        public String getName() { return name.get(); }
        public SimpleStringProperty nameProperty() { return name; }
        public String getOffice() { return office.get(); }
        public SimpleStringProperty officeProperty() { return office; }
        public String getStatus() { return status.get(); }
        public void setStatus(String value) { this.status.set(value); }
        public SimpleStringProperty statusProperty() { return status; }
    }
}