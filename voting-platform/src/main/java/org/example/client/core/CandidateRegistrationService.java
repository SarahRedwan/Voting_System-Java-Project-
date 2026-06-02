package org.example.client.core;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

public final class CandidateRegistrationService {

    public record CreateCandidateResult(boolean success, String message, String username) {
    }

    private CandidateRegistrationService() {
    }

    public static CreateCandidateResult createCandidate(String username, String password, String displayName,
                                                        String party, String position) {
        String normalizedUsername = normalizeUsername(username);
        if (normalizedUsername.isBlank()) {
            return new CreateCandidateResult(false, "Login ID is required.", null);
        }
        if (!normalizedUsername.matches("[a-z0-9_]{3,50}")) {
            return new CreateCandidateResult(false, "Login ID must be 3-50 characters (letters, numbers, underscore only).", null);
        }
        if (password == null || password.length() < 4) {
            return new CreateCandidateResult(false, "Password must be at least 4 characters.", null);
        }
        if (displayName == null || displayName.isBlank()) {
            return new CreateCandidateResult(false, "Candidate display name is required.", null);
        }

        if (UserDAO.usernameExists(normalizedUsername)) {
            return new CreateCandidateResult(false, "Login ID already exists. Choose a different username.", null);
        }

        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                UserDAO.insertUser(connection, normalizedUsername, password, "CANDIDATE", displayName.trim(), true);

                CandidateProfile profile = new CandidateProfile(
                        0,
                        normalizedUsername,
                        displayName.trim(),
                        party == null ? "" : party.trim(),
                        "",
                        null,
                        null,
                        position == null ? "" : position.trim(),
                        LocalDateTime.now()
                );
                CandidateProfileDAO.saveOrUpdate(connection, profile);
                try (java.sql.PreparedStatement ps = connection.prepareStatement(
                        "UPDATE candidate_profiles SET approval_status = 'APPROVED' WHERE username = ?")) {
                    ps.setString(1, normalizedUsername);
                    ps.executeUpdate();
                }

                connection.commit();
                return new CreateCandidateResult(
                        true,
                        "Candidate created. Login ID: " + normalizedUsername,
                        normalizedUsername
                );
            } catch (SQLException e) {
                connection.rollback();
                return new CreateCandidateResult(false, "Failed to create candidate: " + e.getMessage(), null);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            return new CreateCandidateResult(false, "Database error: " + e.getMessage(), null);
        }
    }

    private static String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }
}
