package org.example.client.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public final class ElectionScheduleDAO {

    private ElectionScheduleDAO() {
    }

    public static void ensureTable() {
        String sql = "CREATE TABLE IF NOT EXISTS election_settings ("
                + "id INT PRIMARY KEY, "
                + "start_time TIMESTAMP NULL, "
                + "end_time TIMESTAMP NULL, "
                + "manual_mode VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED', "
                + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
                + ") ENGINE=InnoDB CHARACTER SET=utf8mb4";
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.executeUpdate();
            try (PreparedStatement seed = connection.prepareStatement(
                    "INSERT IGNORE INTO election_settings(id, manual_mode) VALUES (1, 'SCHEDULED')")) {
                seed.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Failed to ensure election_settings: " + e.getMessage());
        }
    }

    public static ElectionSchedule load() {
        String sql = "SELECT start_time, end_time, manual_mode FROM election_settings WHERE id = 1";
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return new ElectionSchedule(
                        toLocalDateTime(rs.getTimestamp("start_time")),
                        toLocalDateTime(rs.getTimestamp("end_time")),
                        rs.getString("manual_mode")
                );
            }
        } catch (SQLException e) {
            System.err.println("Failed to load election schedule: " + e.getMessage());
        }
        return new ElectionSchedule(null, null, "SCHEDULED");
    }

    public static void saveSchedule(LocalDateTime startTime, LocalDateTime endTime) throws SQLException {
        String sql = "UPDATE election_settings SET start_time = ?, end_time = ?, manual_mode = 'SCHEDULED', updated_at = CURRENT_TIMESTAMP WHERE id = 1";
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setTimestamp(1, startTime == null ? null : Timestamp.valueOf(startTime));
            ps.setTimestamp(2, endTime == null ? null : Timestamp.valueOf(endTime));
            ps.executeUpdate();
        }
    }

    public static void setManualMode(String mode, LocalDateTime startTime, LocalDateTime endTime) throws SQLException {
        String sql = "UPDATE election_settings SET manual_mode = ?, start_time = ?, end_time = ?, updated_at = CURRENT_TIMESTAMP WHERE id = 1";
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, mode);
            ps.setTimestamp(2, startTime == null ? null : Timestamp.valueOf(startTime));
            ps.setTimestamp(3, endTime == null ? null : Timestamp.valueOf(endTime));
            ps.executeUpdate();
        }
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
