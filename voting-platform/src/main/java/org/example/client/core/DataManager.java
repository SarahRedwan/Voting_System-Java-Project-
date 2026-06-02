package org.example.client.core;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class DataManager {

    private static final String DATABASE_NAME = "securevote";
    private static final String JDBC_URL_BASE = "jdbc:mysql://localhost:3306/";
    private static final String JDBC_PARAMS = "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String JDBC_USER = "root";
    private static final String JDBC_PASSWORD = "asmkr1221#A";
    private static final Path LOG_FILE = Paths.get("vote_history.log");
    private static final Path EXPORT_DIR = Paths.get("exports");
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static {
        initialize();
    }

    private DataManager() {
    }

    public static void initialize() {
        try {
            ensureDatabaseExists();
            try (Connection connection = getConnection()) {
                createSchema(connection);
                seedCandidates(connection);
                seedUsers(connection);
               
            }
        } catch (SQLException e) {
            System.err.println("[DataManager] Initialization error: " + e.getMessage());
        }
    }

    private static void ensureDatabaseExists() throws SQLException {
        try (Connection connection = DriverManager.getConnection(JDBC_URL_BASE + JDBC_PARAMS, JDBC_USER, JDBC_PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DATABASE_NAME);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL_BASE + DATABASE_NAME + JDBC_PARAMS, JDBC_USER, JDBC_PASSWORD);
    }

    private static void createSchema(Connection connection) throws SQLException {
        String createCandidates = "CREATE TABLE IF NOT EXISTS candidates (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "name VARCHAR(100) NOT NULL UNIQUE, " +
                "office VARCHAR(120) NOT NULL, " +
                "status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE / AUTHORIZED', " +
                "votes INT NOT NULL DEFAULT 0, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ") ENGINE=InnoDB";

        String createVotes = "CREATE TABLE IF NOT EXISTS votes (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "voter_id VARCHAR(100) NOT NULL, " +
                "candidate_id INT NOT NULL, " +
                "voted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "UNIQUE KEY unique_vote_per_voter (voter_id), " +
                "FOREIGN KEY (candidate_id) REFERENCES candidates(id) " +
                "ON UPDATE CASCADE ON DELETE RESTRICT" +
                ") ENGINE=InnoDB";

        // Users table for registered voters and candidates
        String createUsers = "CREATE TABLE IF NOT EXISTS users (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "username VARCHAR(100) NOT NULL UNIQUE, " +
                "full_name VARCHAR(150) NOT NULL, " +
                "password_hash VARCHAR(255) NOT NULL, " +
                "role VARCHAR(20) NOT NULL DEFAULT 'voter', " +
                "office VARCHAR(120), " +
                "manifesto_url VARCHAR(500), " +
                "manifesto_pdf_path VARCHAR(500), " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ") ENGINE=InnoDB";

        try (Statement statement = connection.createStatement()) {
            statement.execute(createCandidates);
            statement.execute(createVotes);
            statement.execute(createUsers);
        }

        ensureColumnExists(connection, "users", "manifesto_url", "VARCHAR(500)");
        ensureColumnExists(connection, "users", "manifesto_pdf_path", "VARCHAR(500)");
        ensureColumnExists(connection, "users", "video_url", "VARCHAR(500)");
    }

    private static void ensureColumnExists(Connection connection, String tableName, String columnName, String columnDefinition) throws SQLException {
        String query = "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                       "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, DATABASE_NAME);
            stmt.setString(2, tableName);
            stmt.setString(3, columnName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) {
                    try (Statement alter = connection.createStatement()) {
                        alter.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition);
                    }
                }
            }
        }
    }

    private static void seedCandidates(Connection connection) throws SQLException {
        try (PreparedStatement countStmt = connection.prepareStatement("SELECT COUNT(*) FROM candidates");
             ResultSet resultSet = countStmt.executeQuery()) {
            if (resultSet.next() && resultSet.getInt(1) == 0) {
                String insert = "INSERT INTO candidates (name, office, status) VALUES (?, ?, ?)";
                try (PreparedStatement insertStmt = connection.prepareStatement(insert)) {
                    addSeedCandidate(insertStmt, "Candidate Alpha", "Presidential Seat", "ACTIVE / AUTHORIZED");
                    addSeedCandidate(insertStmt, "Candidate Bravo", "Presidential Seat", "ACTIVE / AUTHORIZED");
                    addSeedCandidate(insertStmt, "Candidate Charlie", "Gubernatorial Seat", "ACTIVE / AUTHORIZED");
                    addSeedCandidate(insertStmt, "Belstgena", "Presidential Seat", "ACTIVE / AUTHORIZED");
                }
            }
        }
        ensureCandidateExists(connection, "Belstgena", "Presidential Seat", "ACTIVE / AUTHORIZED");
    }

    private static void ensureCandidateExists(Connection connection, String name, String office, String status) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT id FROM candidates WHERE LOWER(name) = LOWER(?)")) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        }

        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO candidates (name, office, status) VALUES (?, ?, ?)")) {
            stmt.setString(1, name);
            stmt.setString(2, office);
            stmt.setString(3, status);
            stmt.executeUpdate();
        }
    }

    private static void addSeedCandidate(PreparedStatement insertStmt, String name, String office, String status) throws SQLException {
        insertStmt.setString(1, name);
        insertStmt.setString(2, office);
        insertStmt.setString(3, status);
        insertStmt.executeUpdate();
    }

    private static void seedUsers(Connection connection) throws SQLException {
        try (PreparedStatement countStmt = connection.prepareStatement("SELECT COUNT(*) FROM users");
             ResultSet resultSet = countStmt.executeQuery()) {
            if (resultSet.next() && resultSet.getInt(1) == 0) {
                String insert = "INSERT INTO users (username, full_name, password_hash, role, office) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement insertStmt = connection.prepareStatement(insert)) {
                    addSeedUser(insertStmt, "admin", "Election Administrator", "password", "admin", null);
                    addSeedUser(insertStmt, "candidate", "Candidate Alpha", "password", "candidate", "Presidential Seat");
                    addSeedUser(insertStmt, "belstgena", "Belstgena", "password", "candidate", "Presidential Seat");
                    addSeedUser(insertStmt, "voter123", "Registered Voter", "password", "voter", null);
                }
            }
        }
        ensureUserExists(connection, "belstgena", "Belstgena", "password", "candidate", "Presidential Seat");
    }

    private static void ensureUserExists(Connection connection, String username, String fullName, String password, String role, String office) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT id FROM users WHERE LOWER(username) = LOWER(?)")) {
            stmt.setString(1, username.toLowerCase());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        }

        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO users (username, full_name, password_hash, role, office) VALUES (?, ?, ?, ?, ?)") ) {
            stmt.setString(1, username.toLowerCase());
            stmt.setString(2, fullName);
            stmt.setString(3, hashPassword(password));
            stmt.setString(4, role);
            stmt.setString(5, office);
            stmt.executeUpdate();
        }
    }

    private static void addSeedUser(PreparedStatement insertStmt, String username, String fullName, String password, String role, String office) throws SQLException {
        insertStmt.setString(1, username);
        insertStmt.setString(2, fullName);
        insertStmt.setString(3, hashPassword(password));
        insertStmt.setString(4, role);
        insertStmt.setString(5, office);
        insertStmt.executeUpdate();
    }

    public static Optional<User> authenticateUser(String username, String password) {
        String query = "SELECT id, username, full_name, role, office FROM users WHERE username = ? AND password_hash = ?";
        try (Connection connection = getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username.toLowerCase());
            stmt.setString(2, hashPassword(password));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("full_name"),
                            rs.getString("role"),
                            rs.getString("office")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DataManager] Authentication failed: " + e.getMessage());
        }
        return Optional.empty();
    }

    public static Optional<User> findUserByUsername(String username) {
        String query = "SELECT id, username, full_name, role, office FROM users WHERE username = ?";
        try (Connection connection = getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username.toLowerCase());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("full_name"),
                            rs.getString("role"),
                            rs.getString("office")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DataManager] Failed to find user: " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Registers a new user (voter or candidate).
     * Passwords are stored as a SHA-256 hex hash.
     *
     * @param username       unique login name
     * @param fullName       display name
     * @param password       plain-text password (will be hashed)
     * @param role           "voter" or "candidate"
     * @param office         office the candidate is running for (null for voters)
     * @param manifestoUrl   URL link to manifesto (candidates only, null if PDF used)
     * @param manifestoPath  local PDF path (candidates only, null if URL used)
     * @return true if registration succeeded, false if username is already taken
     */
    public static boolean registerUser(String username, String fullName, String password,
                                       String role, String office,
                                       String manifestoUrl, String manifestoPath,
                                       String videoUrl) {
        String hash = hashPassword(password);
        String query = "INSERT INTO users (username, full_name, password_hash, role, office, manifesto_url, manifesto_pdf_path, video_url) " +
                       "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username.toLowerCase());
            stmt.setString(2, fullName);
            stmt.setString(3, hash);
            stmt.setString(4, role);
            stmt.setString(5, office);
            stmt.setString(6, manifestoUrl);
            stmt.setString(7, manifestoPath);
            stmt.setString(8, videoUrl);
            stmt.executeUpdate();

            // Auto-add candidate to the candidates table so they appear in the election
            if ("candidate".equals(role) && office != null && !office.isBlank()) {
                addCandidate(fullName, office);
            }

            return true;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                return false; // duplicate username
            }
            System.err.println("[DataManager] Registration failed: " + e.getMessage());
            return false;
        }
    }

    public static boolean registerUser(String username, String fullName, String password, String role, String office) {
        return registerUser(username, fullName, password, role, office, null, null, null);
    }

    /**
     * Withdraws a candidate from the election.
     * Removes them from the candidates table and marks their user role as 'withdrawn'.
     *
     * @param username the candidate's username
     * @return true if withdrawal succeeded
     */
    public static boolean withdrawCandidate(String username) {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);

            // Get the candidate's full name to remove from candidates table
            String fullName = null;
            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT full_name FROM users WHERE username = ? AND role = 'candidate'")) {
                stmt.setString(1, username.toLowerCase());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        fullName = rs.getString("full_name");
                    }
                }
            }

            if (fullName == null) {
                return false; // not a candidate
            }

            // Remove from candidates table
            try (PreparedStatement stmt = connection.prepareStatement(
                    "DELETE FROM candidates WHERE name = ?")) {
                stmt.setString(1, fullName);
                stmt.executeUpdate();
            }

            // Mark user role as withdrawn
            try (PreparedStatement stmt = connection.prepareStatement(
                    "UPDATE users SET role = 'withdrawn' WHERE username = ?")) {
                stmt.setString(1, username.toLowerCase());
                stmt.executeUpdate();
            }

            connection.commit();
            System.out.println("[DataManager] Candidate '" + fullName + "' has withdrawn from the election.");
            return true;
        } catch (SQLException e) {
            System.err.println("[DataManager] Withdrawal failed: " + e.getMessage());
            return false;
        }
    }

    /** SHA-256 hex hash of the given plain-text password. */
    private static String hashPassword(String password) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public static boolean recordVote(String voterId, String rawCandidateText) {
        if (voterId == null || voterId.isBlank()) {
            voterId = "anonymous";
        }
        String candidateName = mapCandidateName(rawCandidateText);
        // Ensure the acting user exists and is a voter (candidates/admins cannot cast votes)
        try {
            Optional<User> userOpt = findUserByUsername(voterId);
            if (userOpt.isEmpty() || !"voter".equals(userOpt.get().getRole())) {
                return false;
            }
        } catch (Exception ignored) {
            return false;
        }

        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            int candidateId = getCandidateId(connection, candidateName);
            if (candidateId < 0) {
                return false;
            }
            if (!isCandidateActive(connection, candidateId)) {
                return false;
            }
            if (hasVoted(connection, voterId)) {
                return false;
            }
            try (PreparedStatement voteStmt = connection.prepareStatement("INSERT INTO votes (voter_id, candidate_id) VALUES (?, ?)");
                 PreparedStatement updateStmt = connection.prepareStatement("UPDATE candidates SET votes = votes + 1 WHERE id = ?")) {
                voteStmt.setString(1, voterId);
                voteStmt.setInt(2, candidateId);
                voteStmt.executeUpdate();

                updateStmt.setInt(1, candidateId);
                updateStmt.executeUpdate();
            }
            connection.commit();
            writeAuditLog(String.format("[%s] Voter '%s' cast vote for '%s'", now(), voterId, candidateName));
            return true;
        } catch (SQLException | IOException e) {
            System.err.println("[DataManager] Vote record failed: " + e.getMessage());
            return false;
        }
    }

    private static boolean hasVoted(Connection connection, String voterId) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT 1 FROM votes WHERE voter_id = ?")) {
            stmt.setString(1, voterId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean isCandidateActive(Connection connection, int candidateId) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT status FROM candidates WHERE id = ?")) {
            stmt.setInt(1, candidateId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String status = rs.getString("status");
                    return status != null && status.toUpperCase().contains("ACTIVE");
                }
                return false;
            }
        }
    }

    private static int getCandidateId(Connection connection, String candidateName) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT id FROM candidates WHERE name = ?")) {
            stmt.setString(1, candidateName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        return -1;
    }

    public static List<Candidate> getRankedCandidates() {
        List<Candidate> results = new ArrayList<>();
        String query = "SELECT c.id, c.name, c.office, c.status, c.votes, " +
                       "u.manifesto_url, u.manifesto_pdf_path, u.video_url " +
                       "FROM candidates c " +
                       "LEFT JOIN users u ON LOWER(c.name) = LOWER(u.full_name) " +
                       "ORDER BY c.votes DESC, c.name ASC";
        try (Connection connection = getConnection();
             PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                results.add(new Candidate(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("office"),
                        rs.getString("status"),
                        rs.getInt("votes"),
                        rs.getString("manifesto_url"),
                        rs.getString("manifesto_pdf_path"),
                        rs.getString("video_url")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[DataManager] Failed to fetch ranked candidates: " + e.getMessage());
        }
        return results;
    }

    public static Optional<Candidate> findCandidateByName(String candidateText) {
        String candidateName = mapCandidateName(candidateText);
        String query = "SELECT c.id, c.name, c.office, c.status, c.votes, " +
                       "u.manifesto_url, u.manifesto_pdf_path, u.video_url " +
                       "FROM candidates c " +
                       "LEFT JOIN users u ON LOWER(c.name) = LOWER(u.full_name) " +
                       "WHERE c.name = ?";
        try (Connection connection = getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, candidateName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Candidate(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("office"),
                            rs.getString("status"),
                            rs.getInt("votes"),
                            rs.getString("manifesto_url"),
                            rs.getString("manifesto_pdf_path"),
                            rs.getString("video_url")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DataManager] Failed to find candidate: " + e.getMessage());
        }
        return Optional.empty();
    }

    public static boolean setCandidateStatus(String candidateName, String status) {
        String query = "UPDATE candidates SET status = ? WHERE name = ?";
        try (Connection connection = getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, status);
            stmt.setString(2, mapCandidateName(candidateName));
            int updated = stmt.executeUpdate();
            return updated > 0;
        } catch (SQLException e) {
            System.err.println("[DataManager] Failed to update candidate status: " + e.getMessage());
            return false;
        }
    }

    public static boolean addCandidate(String name, String office) {
        String query = "INSERT INTO candidates (name, office, status) VALUES (?, ?, 'ACTIVE / AUTHORIZED')";
        try (Connection connection = getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, name);
            stmt.setString(2, office);
            int inserted = stmt.executeUpdate();
            return inserted > 0;
        } catch (SQLException e) {
            System.err.println("[DataManager] Failed to add candidate: " + e.getMessage());
            return false;
        }
    }

    public static boolean removeCandidate(String name) {
        String query = "DELETE FROM candidates WHERE name = ?";
        try (Connection connection = getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, mapCandidateName(name));
            int removed = stmt.executeUpdate();
            return removed > 0;
        } catch (SQLException e) {
            System.err.println("[DataManager] Failed to remove candidate: " + e.getMessage());
            return false;
        }
    }

    public static Path exportResults() throws IOException {
        if (!Files.exists(EXPORT_DIR)) {
            Files.createDirectories(EXPORT_DIR);
        }

        Path csv = EXPORT_DIR.resolve("securevote-results.csv");
        Path txt = EXPORT_DIR.resolve("securevote-results.txt");
        List<Candidate> results = getRankedCandidates();

        try (BufferedWriter writer = Files.newBufferedWriter(csv, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write("Name,Office,Status,Votes");
            writer.newLine();
            for (Candidate candidate : results) {
                writer.write(String.format("%s,%s,%s,%d", candidate.getName(), candidate.getOffice(), candidate.getStatus(), candidate.getVotes()));
                writer.newLine();
            }
        }

        try (BufferedWriter writer = Files.newBufferedWriter(txt, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write("SECUREVOTE RESULTS EXPORT\n");
            writer.write("==========================\n");
            for (Candidate candidate : results) {
                writer.write(String.format("%s (%s) - %d votes - %s\n", candidate.getName(), candidate.getOffice(), candidate.getVotes(), candidate.getStatus()));
            }
        }

        writeAuditLog(String.format("[%s] Exported results to %s and %s", now(), csv.toAbsolutePath(), txt.toAbsolutePath()));
        return csv;
    }

    private static void writeAuditLog(String message) throws IOException {
        String line = String.format("%s %s", now(), message);
        try (BufferedWriter writer = Files.newBufferedWriter(LOG_FILE, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            writer.write(line);
            writer.newLine();
        }
    }

    private static String now() {
        return LocalDateTime.now().format(TIMESTAMP_FORMATTER);
    }

    private static String mapCandidateName(String candidateText) {
        if (candidateText == null) {
            return "Candidate Alpha";
        }
        String normalized = candidateText.toLowerCase();
        if (normalized.contains("alpha") || normalized.contains("candidate a")) {
            return "Candidate Alpha";
        }
        if (normalized.contains("bravo") || normalized.contains("candidate b")) {
            return "Candidate Bravo";
        }
        if (normalized.contains("charlie") || normalized.contains("candidate c")) {
            return "Candidate Charlie";
        }
        return candidateText.trim();
    }
}
