package org.example.client.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class CandidateProfileDAO {

    private CandidateProfileDAO() {
    }

    public static CandidateProfile findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        String sql = "SELECT id, username, name, party, description, image_url, logo_url, position, pdf_url, video_url, last_updated FROM candidate_profiles WHERE username = ? LIMIT 1";
        try (Connection connection = Database.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapCandidate(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to load candidate profile: " + e.getMessage());
        }
        return null;
    }

    public static List<CandidateProfile> findApproved() {
        return findByApprovalStatus("APPROVED");
    }

    public static List<CandidateProfile> findPendingApplications() {
        return findByApprovalStatus("PENDING");
    }

    public static List<CandidateApplication> findPendingApplicationRecords() {
        List<CandidateApplication> applications = new ArrayList<>();
        String sql = "SELECT username, name, party, position, phone_number, fan_number FROM candidate_profiles "
                + "WHERE COALESCE(approval_status, 'APPROVED') = 'PENDING' ORDER BY last_updated DESC";
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                applications.add(new CandidateApplication(
                        rs.getString("username"),
                        rs.getString("name"),
                        rs.getString("party"),
                        rs.getString("position"),
                        rs.getString("phone_number"),
                        rs.getString("fan_number")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Failed to load pending applications: " + e.getMessage());
        }
        return applications;
    }

    public static String getApprovalStatus(String username) {
        String sql = "SELECT approval_status FROM candidate_profiles WHERE username = ? LIMIT 1";
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String status = rs.getString("approval_status");
                    return status == null ? "APPROVED" : status;
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to load approval status: " + e.getMessage());
        }
        return null;
    }

    public static String getAdminReviewMessage(String username) {
        String sql = "SELECT admin_review_message FROM candidate_profiles WHERE username = ? LIMIT 1";
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("admin_review_message");
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to load review message: " + e.getMessage());
        }
        return null;
    }

    private static List<CandidateProfile> findByApprovalStatus(String status) {
        List<CandidateProfile> profiles = new ArrayList<>();
        String sql = "SELECT id, username, name, party, description, image_url, logo_url, position, pdf_url, video_url, last_updated "
                + "FROM candidate_profiles WHERE COALESCE(approval_status, 'APPROVED') = ? ORDER BY name";
        try (Connection connection = Database.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    profiles.add(mapCandidate(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to load candidates by status: " + e.getMessage());
        }
        return profiles;
    }

    public static boolean phoneExists(String phone) {
        if (phone == null || phone.isBlank()) {
            return false;
        }
        String sql = "SELECT 1 FROM candidate_profiles WHERE phone_number = ? LIMIT 1";
        try (Connection connection = Database.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, phone);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Failed to check candidate phone: " + e.getMessage());
        }
        return false;
    }

    public static boolean fanExists(String fan) {
        if (fan == null || fan.isBlank()) {
            return false;
        }
        String sql = "SELECT 1 FROM candidate_profiles WHERE fan_number = ? LIMIT 1";
        try (Connection connection = Database.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, fan);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Failed to check candidate FAN: " + e.getMessage());
        }
        return false;
    }

    public static String findUsernameByPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String sql = "SELECT username FROM candidate_profiles WHERE phone_number = ? LIMIT 1";
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, phone);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("username");
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to lookup candidate by phone: " + e.getMessage());
        }
        return null;
    }

    public static String findUsernameByFan(String fan) {
        if (fan == null || fan.isBlank()) {
            return null;
        }
        String sql = "SELECT username FROM candidate_profiles WHERE fan_number = ? LIMIT 1";
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, fan);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("username");
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to lookup candidate by FAN: " + e.getMessage());
        }
        return null;
    }

    public static boolean approveApplication(String username, String adminMessage) {
        return updateApplicationStatus(username, "APPROVED", adminMessage, true);
    }

    public static boolean rejectApplication(String username, String adminMessage) {
        return updateApplicationStatus(username, "REJECTED", adminMessage, false);
    }

    private static boolean updateApplicationStatus(String username, String status, String adminMessage, boolean activateUser) {
        String sql = "UPDATE candidate_profiles SET approval_status = ?, admin_review_message = ?, last_updated = CURRENT_TIMESTAMP WHERE username = ?";
        try (Connection connection = Database.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, adminMessage);
            ps.setString(3, username);
            int updated = ps.executeUpdate();
            if (updated > 0) {
                UserDAO.setUserActive(username, activateUser);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Failed to update application status: " + e.getMessage());
        }
        return false;
    }

    public static List<CandidateProfile> findAll() {
        List<CandidateProfile> profiles = new ArrayList<>();
        String sql = "SELECT id, username, name, party, description, image_url, logo_url, position, pdf_url, video_url, last_updated FROM candidate_profiles ORDER BY name";
        try (Connection connection = Database.getConnection(); PreparedStatement ps = connection.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                profiles.add(mapCandidate(rs));
            }
        } catch (SQLException e) {
            System.err.println("Failed to load candidate profiles: " + e.getMessage());
        }
        return profiles;
    }

    public static void saveOrUpdate(CandidateProfile profile) {
        if (profile == null || profile.getUsername() == null || profile.getUsername().isBlank()) {
            return;
        }
        try (Connection connection = Database.getConnection()) {
            saveOrUpdate(connection, profile);
        } catch (SQLException e) {
            System.err.println("Failed to save candidate profile: " + e.getMessage());
        }
    }

    public static void saveOrUpdate(Connection connection, CandidateProfile profile) throws SQLException {
        if (profile == null || profile.getUsername() == null || profile.getUsername().isBlank()) {
            return;
        }
        String sql = "INSERT INTO candidate_profiles(username, name, party, description, image_url, logo_url, position, pdf_url, video_url, last_updated) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP) "
                + "ON DUPLICATE KEY UPDATE name = VALUES(name), party = VALUES(party), description = VALUES(description), "
                + "image_url = VALUES(image_url), logo_url = VALUES(logo_url), position = VALUES(position), "
                + "pdf_url = VALUES(pdf_url), video_url = VALUES(video_url), last_updated = CURRENT_TIMESTAMP";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, profile.getUsername());
            ps.setString(2, profile.getName());
            ps.setString(3, profile.getParty());
            ps.setString(4, profile.getDescription());
            ps.setString(5, profile.getImageUrl());
            ps.setString(6, profile.getLogoUrl());
            ps.setString(7, profile.getPosition());
            ps.setString(8, profile.getPdfUrl());
            ps.setString(9, profile.getVideoUrl());
            ps.executeUpdate();
        }
    }

    public static void ensureSampleCandidates() {
        try (Connection connection = Database.getConnection()) {
            if (countCandidates(connection) == 0) {
                saveOrUpdate(new CandidateProfile(0, "candidate_alpha", "Candidate Alpha", "Democratic Party",
                        "Experienced policy leader focused on education and infrastructure.", null, null, "Presidential Seat", LocalDateTime.now()));
                saveOrUpdate(new CandidateProfile(0, "candidate_bravo", "Candidate Bravo", "Republican Party",
                        "Focused on fiscal responsibility and public safety.", null, null, "Presidential Seat", LocalDateTime.now()));
                saveOrUpdate(new CandidateProfile(0, "candidate_charlie", "Candidate Charlie", "Independent Bloc",
                        "A community-first candidate advocating transparency and reform.", null, null, "Gubernatorial Seat", LocalDateTime.now()));
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE candidate_profiles SET approval_status = 'APPROVED' WHERE approval_status IS NULL")) {
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Failed to seed candidate profiles: " + e.getMessage());
        }
    }

    private static int countCandidates(Connection connection) throws SQLException {
        String sql = "SELECT COUNT(*) FROM candidate_profiles";
        try (PreparedStatement ps = connection.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    private static CandidateProfile mapCandidate(ResultSet rs) throws SQLException {
        return new CandidateProfile(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("name"),
                rs.getString("party"),
                rs.getString("description"),
                rs.getString("image_url"),
                rs.getString("logo_url"),
                rs.getString("position"),
                rs.getTimestamp("last_updated") != null ? rs.getTimestamp("last_updated").toLocalDateTime() : null,
                rs.getString("pdf_url"),
                rs.getString("video_url")
        );
    }
}
