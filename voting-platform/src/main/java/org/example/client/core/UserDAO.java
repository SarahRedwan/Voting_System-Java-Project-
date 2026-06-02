package org.example.client.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class UserDAO {

    private UserDAO() {
    }

    public static User authenticate(String username, String password) {
        String resolved = resolveUsername(username);
        if (resolved == null || password == null) {
            return null;
        }
        return authenticateResolved(resolved, password);
    }

    /**
     * Sign in using username, voter/candidate ID, phone, or FAN.
     */
    public static User authenticateByLoginId(String loginId, String password) {
        return authenticate(loginId, password);
    }

    private static User authenticateResolved(String username, String password) {
        String sql = "SELECT id, username, password_hash, role, full_name, is_active FROM users WHERE username = ? LIMIT 1";
        try (Connection connection = Database.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    if (storedHash != null && storedHash.equals(PasswordUtil.hashPassword(password))) {
                        return new User(
                                rs.getLong("id"),
                                rs.getString("username"),
                                storedHash,
                                rs.getString("role"),
                                rs.getString("full_name"),
                                rs.getBoolean("is_active")
                        );
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("User authentication failure: " + e.getMessage());
        }
        return null;
    }

    private static String resolveUsername(String loginId) {
        if (loginId == null || loginId.isBlank()) {
            return null;
        }

        String trimmed = loginId.trim();
        if (findByUsername(trimmed) != null) {
            return trimmed;
        }

        String lower = trimmed.toLowerCase();
        if (!lower.equals(trimmed) && findByUsername(lower) != null) {
            return lower;
        }

        String upper = trimmed.toUpperCase();
        if (!upper.equals(trimmed) && findByUsername(upper) != null) {
            return upper;
        }

        Voter voter = VoterDAO.findByVoterId(trimmed);
        if (voter == null && !upper.equals(trimmed)) {
            voter = VoterDAO.findByVoterId(upper);
        }
        if (voter != null) {
            return voter.getUsername();
        }

        String phone = InputValidator.normalizePhone(trimmed);
        if (!phone.isBlank()) {
            String fromVoterPhone = VoterDAO.findUsernameByPhone(phone);
            if (fromVoterPhone != null) {
                return fromVoterPhone;
            }
            String fromCandidatePhone = CandidateProfileDAO.findUsernameByPhone(phone);
            if (fromCandidatePhone != null) {
                return fromCandidatePhone;
            }
        }

        String fan = InputValidator.normalizeFan(trimmed);
        if (!fan.isBlank()) {
            String fromVoterFan = VoterDAO.findUsernameByFan(fan);
            if (fromVoterFan != null) {
                return fromVoterFan;
            }
            String fromCandidateFan = CandidateProfileDAO.findUsernameByFan(fan);
            if (fromCandidateFan != null) {
                return fromCandidateFan;
            }
        }

        return null;
    }

    public static boolean usernameExists(String username) {
        return findByUsername(username) != null;
    }

    public static void insertUser(Connection connection, String username, String password, String role,
                                  String fullName, boolean active) throws SQLException {
        String sql = "INSERT INTO users(username, password_hash, role, full_name, is_active) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, PasswordUtil.hashPassword(password));
            ps.setString(3, role);
            ps.setString(4, fullName);
            ps.setBoolean(5, active);
            ps.executeUpdate();
        }
    }

    public static boolean setUserActive(String username, boolean active) {
        if (username == null || username.isBlank()) {
            return false;
        }
        String sql = "UPDATE users SET is_active = ? WHERE username = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setString(2, username);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to update user active status: " + e.getMessage());
        }
        return false;
    }

    public static User findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        String sql = "SELECT id, username, password_hash, role, full_name, is_active FROM users WHERE username = ? LIMIT 1";
        try (Connection connection = Database.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getLong("id"),
                            rs.getString("username"),
                            rs.getString("password_hash"),
                            rs.getString("role"),
                            rs.getString("full_name"),
                            rs.getBoolean("is_active")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to lookup user: " + e.getMessage());
        }
        return null;
    }

    public static void ensureSampleUsers() {
        try (Connection connection = Database.getConnection()) {
            if (countUsers(connection) == 0) {
                String insert = "INSERT INTO users(username, password_hash, role, full_name, is_active) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement ps = connection.prepareStatement(insert)) {
                    ps.setString(1, "admin");
                    ps.setString(2, PasswordUtil.hashPassword("password"));
                    ps.setString(3, "ADMIN");
                    ps.setString(4, "Election Administrator");
                    ps.setBoolean(5, true);
                    ps.executeUpdate();

                    ps.setString(1, "candidate_alpha");
                    ps.setString(2, PasswordUtil.hashPassword("password"));
                    ps.setString(3, "CANDIDATE");
                    ps.setString(4, "Candidate Alpha");
                    ps.setBoolean(5, true);
                    ps.executeUpdate();

                    ps.setString(1, "candidate_bravo");
                    ps.setString(2, PasswordUtil.hashPassword("password"));
                    ps.setString(3, "CANDIDATE");
                    ps.setString(4, "Candidate Bravo");
                    ps.setBoolean(5, true);
                    ps.executeUpdate();

                    ps.setString(1, "voter123");
                    ps.setString(2, PasswordUtil.hashPassword("password"));
                    ps.setString(3, "VOTER");
                    ps.setString(4, "Verified Voter");
                    ps.setBoolean(5, true);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to seed users: " + e.getMessage());
        }
    }

    private static int countUsers(Connection connection) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users";
        try (PreparedStatement ps = connection.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }
}
