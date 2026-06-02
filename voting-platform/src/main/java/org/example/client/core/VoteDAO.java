package org.example.client.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public final class VoteDAO {

    private VoteDAO() {}

    public static void recordVote(String voterUsername, String candidateUsername) {
        if (voterUsername == null || voterUsername.isBlank() || candidateUsername == null || candidateUsername.isBlank()) {
            return;
        }

        String sql = "INSERT INTO votes(voter_username, candidate_username) VALUES(?, ?) "
                + "ON DUPLICATE KEY UPDATE candidate_username = VALUES(candidate_username), created_at = CURRENT_TIMESTAMP";
        try (Connection connection = Database.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, voterUsername);
            ps.setString(2, candidateUsername);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to record vote: " + e.getMessage());
        }
    }

    public static Map<String, Integer> getResults() {
        Map<String, Integer> results = new HashMap<>();
        String sql = "SELECT c.name AS candidate_name, COUNT(v.id) AS cnt "
                + "FROM votes v "
                + "JOIN candidate_profiles c ON v.candidate_username = c.username "
                + "WHERE COALESCE(c.approval_status, 'APPROVED') = 'APPROVED' "
                + "GROUP BY c.name";
        try (Connection connection = Database.getConnection(); PreparedStatement ps = connection.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                results.put(rs.getString("candidate_name"), rs.getInt("cnt"));
            }
        } catch (SQLException e) {
            System.err.println("Failed to load results: " + e.getMessage());
        }
        return results;
    }

    public static boolean hasVoted(String voterUsername) {
        if (voterUsername == null || voterUsername.isBlank()) {
            return false;
        }
        String sql = "SELECT 1 FROM votes WHERE voter_username = ? LIMIT 1";
        try (Connection connection = Database.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, voterUsername);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Failed to check vote status: " + e.getMessage());
            return false;
        }
    }

    public static String getVoterVote(String voterUsername) {
        if (voterUsername == null || voterUsername.isBlank()) {
            return null;
        }
        String sql = "SELECT c.name AS candidate_name FROM votes v "
                + "JOIN candidate_profiles c ON v.candidate_username = c.username "
                + "WHERE v.voter_username = ? LIMIT 1";
        try (Connection connection = Database.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, voterUsername);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("candidate_name");
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to load voter vote: " + e.getMessage());
        }
        return null;
    }

    public static void resetVotes() {
        String sql = "DELETE FROM votes";
        try (Connection connection = Database.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to reset votes: " + e.getMessage());
        }
    }
}
