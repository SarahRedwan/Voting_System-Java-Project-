package org.example.client.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class PendingSubmissionDAO {

    private PendingSubmissionDAO() {
    }

    public static PendingSubmission findLatestByUsername(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        String sql = "SELECT * FROM pending_submissions WHERE username = ? ORDER BY created_at DESC LIMIT 1";
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapSubmission(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to load pending submission: " + e.getMessage());
        }
        return null;
    }

    public static List<PendingSubmission> findAllPending() {
        List<PendingSubmission> submissions = new ArrayList<>();
        String sql = "SELECT * FROM pending_submissions WHERE status = 'PENDING' ORDER BY created_at ASC";
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                submissions.add(mapSubmission(rs));
            }
        } catch (SQLException e) {
            System.err.println("Failed to load pending submissions: " + e.getMessage());
        }
        return submissions;
    }

    public static long submitForReview(PendingSubmission submission) {
        if (submission == null || submission.getUsername() == null || submission.getUsername().isBlank()) {
            return -1;
        }

        PendingSubmission existingPending = findActivePending(submission.getUsername());
        if (existingPending != null) {
            return updatePending(existingPending.getId(), submission);
        }

        String sql = "INSERT INTO pending_submissions(username, display_name, pending_name, pending_party, pending_description, "
                + "pending_position, pending_image_path, pending_logo_path, pending_pdf_path, pending_video_path, status, admin_message) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', NULL)";
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindSubmissionFields(ps, submission);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to submit for review: " + e.getMessage());
        }
        return -1;
    }

    private static PendingSubmission findActivePending(String username) {
        String sql = "SELECT * FROM pending_submissions WHERE username = ? AND status = 'PENDING' ORDER BY created_at DESC LIMIT 1";
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapSubmission(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to find active pending submission: " + e.getMessage());
        }
        return null;
    }

    private static long updatePending(long id, PendingSubmission submission) {
        String sql = "UPDATE pending_submissions SET display_name = ?, pending_name = ?, pending_party = ?, pending_description = ?, "
                + "pending_position = ?, pending_image_path = COALESCE(?, pending_image_path), "
                + "pending_logo_path = COALESCE(?, pending_logo_path), "
                + "pending_pdf_path = COALESCE(?, pending_pdf_path), "
                + "pending_video_path = COALESCE(?, pending_video_path), status = 'PENDING', admin_message = NULL, reviewed_at = NULL "
                + "WHERE id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, submission.getDisplayName());
            ps.setString(2, submission.getPendingName());
            ps.setString(3, submission.getPendingParty());
            ps.setString(4, submission.getPendingDescription());
            ps.setString(5, submission.getPendingPosition());
            ps.setString(6, submission.getPendingImagePath());
            ps.setString(7, submission.getPendingLogoPath());
            ps.setString(8, submission.getPendingPdfPath());
            ps.setString(9, submission.getPendingVideoPath());
            ps.setLong(10, id);
            ps.executeUpdate();
            return id;
        } catch (SQLException e) {
            System.err.println("Failed to update pending submission: " + e.getMessage());
        }
        return -1;
    }

    public static boolean approve(long submissionId, String adminMessage) {
        PendingSubmission submission = findById(submissionId);
        if (submission == null) {
            return false;
        }

        CandidateProfile profile = CandidateProfileDAO.findByUsername(submission.getUsername());
        if (profile == null) {
            profile = new CandidateProfile(0, submission.getUsername(), submission.getPendingName(),
                    submission.getPendingParty(), submission.getPendingDescription(),
                    submission.getPendingImagePath(), submission.getPendingLogoPath(),
                    submission.getPendingPosition(), LocalDateTime.now(), submission.getPendingPdfPath(),
                    submission.getPendingVideoPath());
        } else {
            if (submission.getPendingName() != null && !submission.getPendingName().isBlank()) {
                profile.setName(submission.getPendingName());
            }
            if (submission.getPendingParty() != null) {
                profile.setParty(submission.getPendingParty());
            }
            if (submission.getPendingDescription() != null) {
                profile.setDescription(submission.getPendingDescription());
            }
            if (submission.getPendingPosition() != null) {
                profile.setPosition(submission.getPendingPosition());
            }
            if (submission.getPendingImagePath() != null) {
                profile.setImageUrl(submission.getPendingImagePath());
            }
            if (submission.getPendingLogoPath() != null) {
                profile.setLogoUrl(submission.getPendingLogoPath());
            }
            if (submission.getPendingPdfPath() != null) {
                profile.setPdfUrl(submission.getPendingPdfPath());
            }
            if (submission.getPendingVideoPath() != null) {
                profile.setVideoUrl(submission.getPendingVideoPath());
            }
        }

        CandidateProfileDAO.saveOrUpdate(profile);
        return updateReviewStatus(submissionId, "APPROVED", adminMessage);
    }

    public static boolean reject(long submissionId, String adminMessage) {
        return updateReviewStatus(submissionId, "REJECTED", adminMessage);
    }

    private static boolean updateReviewStatus(long submissionId, String status, String adminMessage) {
        String sql = "UPDATE pending_submissions SET status = ?, admin_message = ?, reviewed_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, adminMessage);
            ps.setLong(3, submissionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to update submission status: " + e.getMessage());
        }
        return false;
    }

    private static PendingSubmission findById(long id) {
        String sql = "SELECT * FROM pending_submissions WHERE id = ? LIMIT 1";
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapSubmission(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to find submission by id: " + e.getMessage());
        }
        return null;
    }

    private static void bindSubmissionFields(PreparedStatement ps, PendingSubmission submission) throws SQLException {
        ps.setString(1, submission.getUsername());
        ps.setString(2, submission.getDisplayName());
        ps.setString(3, submission.getPendingName());
        ps.setString(4, submission.getPendingParty());
        ps.setString(5, submission.getPendingDescription());
        ps.setString(6, submission.getPendingPosition());
        ps.setString(7, submission.getPendingImagePath());
        ps.setString(8, submission.getPendingLogoPath());
        ps.setString(9, submission.getPendingPdfPath());
        ps.setString(10, submission.getPendingVideoPath());
    }

    private static PendingSubmission mapSubmission(ResultSet rs) throws SQLException {
        Timestamp reviewedAt = rs.getTimestamp("reviewed_at");
        Timestamp createdAt = rs.getTimestamp("created_at");
        return new PendingSubmission(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("display_name"),
                rs.getString("pending_name"),
                rs.getString("pending_party"),
                rs.getString("pending_description"),
                rs.getString("pending_position"),
                rs.getString("pending_image_path"),
                rs.getString("pending_logo_path"),
                rs.getString("pending_pdf_path"),
                rs.getString("pending_video_path"),
                rs.getString("status"),
                rs.getString("admin_message"),
                createdAt != null ? createdAt.toLocalDateTime() : null,
                reviewedAt != null ? reviewedAt.toLocalDateTime() : null
        );
    }
}
