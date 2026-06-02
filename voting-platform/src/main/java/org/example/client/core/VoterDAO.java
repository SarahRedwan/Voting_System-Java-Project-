package org.example.client.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

public final class VoterDAO {

    private VoterDAO() {
    }

    public static boolean phoneExists(String phone) {
        String sql = "SELECT 1 FROM voters WHERE phone_number = ? LIMIT 1";
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, phone);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Failed to check phone: " + e.getMessage());
        }
        return false;
    }

    public static boolean fanExists(String fan) {
        String sql = "SELECT 1 FROM voters WHERE fan_number = ? LIMIT 1";
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, fan);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Failed to check FAN in voters: " + e.getMessage());
        }
        return false;
    }

    public static String generateVoterId(Connection connection) throws SQLException {
        int year = java.time.Year.now().getValue();
        String prefix = "V" + year;
        String sql = "SELECT voter_id FROM voters WHERE voter_id LIKE ? ORDER BY voter_id DESC LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, prefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                int next = 1;
                if (rs.next()) {
                    String last = rs.getString("voter_id");
                    String numeric = last.substring(prefix.length());
                    next = Integer.parseInt(numeric) + 1;
                }
                return prefix + String.format("%06d", next);
            }
        }
    }

    public static void insertVoter(Connection connection, String username, String voterId, String fullName,
                                   String phone, String fan, LocalDate dob, String address,
                                   String idVerification) throws SQLException {
        String sql = "INSERT INTO voters(username, voter_id, full_name, phone_number, fan_number, date_of_birth, address, id_verification, approval_status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'APPROVED')";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, voterId);
            ps.setString(3, fullName);
            ps.setString(4, phone);
            ps.setString(5, fan);
            ps.setDate(6, java.sql.Date.valueOf(dob));
            ps.setString(7, address);
            ps.setString(8, idVerification == null || idVerification.isBlank() ? null : idVerification);
            ps.executeUpdate();
        }
    }

    public static Voter findByVoterId(String voterId) {
        if (voterId == null || voterId.isBlank()) {
            return null;
        }
        String sql = "SELECT * FROM voters WHERE voter_id = ? LIMIT 1";
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, voterId.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapVoter(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to find voter: " + e.getMessage());
        }
        return null;
    }

    public static String findUsernameByPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String sql = "SELECT username FROM voters WHERE phone_number = ? LIMIT 1";
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, phone);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("username");
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to lookup voter by phone: " + e.getMessage());
        }
        return null;
    }

    public static String findUsernameByFan(String fan) {
        if (fan == null || fan.isBlank()) {
            return null;
        }
        String sql = "SELECT username FROM voters WHERE fan_number = ? LIMIT 1";
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, fan);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("username");
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to lookup voter by FAN: " + e.getMessage());
        }
        return null;
    }

    private static Voter mapVoter(ResultSet rs) throws SQLException {
        java.sql.Date dob = rs.getDate("date_of_birth");
        return new Voter(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("voter_id"),
                rs.getString("full_name"),
                rs.getString("phone_number"),
                rs.getString("fan_number"),
                dob != null ? dob.toLocalDate() : null,
                rs.getString("address"),
                rs.getString("id_verification"),
                rs.getString("approval_status")
        );
    }
}
