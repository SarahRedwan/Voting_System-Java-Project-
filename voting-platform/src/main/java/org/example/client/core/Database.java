package org.example.client.core;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {

    private static final String HOST = "localhost";
    private static final String PORT = "3306";
    private static final String DB_NAME = "securevote";
    private static final String USER = "root";
    private static final String PASSWORD = "@rahmet0947!";

    private Database() {}

    public static Connection getConnection() throws SQLException {
        String url = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DB_NAME + "?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC";
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ignored) {
        }
        return DriverManager.getConnection(url, USER, PASSWORD);
    }

    public static void initializeSchema() {
        String[] ddl = {
                "CREATE TABLE IF NOT EXISTS users ("
                        + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                        + "username VARCHAR(100) NOT NULL UNIQUE, "
                        + "password_hash VARCHAR(255) NOT NULL, "
                        + "role VARCHAR(20) NOT NULL, "
                        + "full_name VARCHAR(255), "
                        + "is_active BOOLEAN DEFAULT TRUE, "
                        + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                        + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
                        + ") ENGINE=InnoDB CHARACTER SET=utf8mb4",
                "CREATE TABLE IF NOT EXISTS candidate_profiles ("
                        + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                        + "username VARCHAR(100) NOT NULL UNIQUE, "
                        + "name VARCHAR(255) NOT NULL, "
                        + "party VARCHAR(255), "
                        + "description TEXT, "
                        + "image_url VARCHAR(1024), "
                        + "logo_url VARCHAR(1024), "
                        + "position VARCHAR(255), "
                        + "pdf_url VARCHAR(1024), "
                        + "video_url VARCHAR(1024), "
                        + "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
                        + ") ENGINE=InnoDB CHARACTER SET=utf8mb4",
                "CREATE TABLE IF NOT EXISTS pending_submissions ("
                        + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                        + "username VARCHAR(100) NOT NULL, "
                        + "display_name VARCHAR(255), "
                        + "pending_name VARCHAR(255), "
                        + "pending_party VARCHAR(255), "
                        + "pending_description TEXT, "
                        + "pending_position VARCHAR(255), "
                        + "pending_image_path VARCHAR(1024), "
                        + "pending_logo_path VARCHAR(1024), "
                        + "pending_pdf_path VARCHAR(1024), "
                        + "pending_video_path VARCHAR(1024), "
                        + "status VARCHAR(20) NOT NULL DEFAULT 'PENDING', "
                        + "admin_message TEXT, "
                        + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                        + "reviewed_at TIMESTAMP NULL, "
                        + "INDEX idx_pending_username (username), "
                        + "INDEX idx_pending_status (status)"
                        + ") ENGINE=InnoDB CHARACTER SET=utf8mb4",
                "CREATE TABLE IF NOT EXISTS voters ("
                        + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                        + "username VARCHAR(100) NOT NULL UNIQUE, "
                        + "voter_id VARCHAR(20) NOT NULL UNIQUE, "
                        + "full_name VARCHAR(255) NOT NULL, "
                        + "phone_number VARCHAR(20) NOT NULL UNIQUE, "
                        + "fan_number VARCHAR(50) NOT NULL UNIQUE, "
                        + "date_of_birth DATE NOT NULL, "
                        + "address TEXT NOT NULL, "
                        + "id_verification VARCHAR(255), "
                        + "approval_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED', "
                        + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                        + "FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE ON UPDATE CASCADE"
                        + ") ENGINE=InnoDB CHARACTER SET=utf8mb4",
                "CREATE TABLE IF NOT EXISTS votes ("
                        + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                        + "voter_username VARCHAR(100) NOT NULL, "
                        + "candidate_username VARCHAR(100) NOT NULL, "
                        + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                        + "UNIQUE KEY ux_votes_voter (voter_username), "
                        + "INDEX idx_candidate_username (candidate_username), "
                        + "FOREIGN KEY (candidate_username) REFERENCES candidate_profiles(username) ON DELETE CASCADE ON UPDATE CASCADE"
                        + ") ENGINE=InnoDB CHARACTER SET=utf8mb4"
        };

        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            for (String sql : ddl) {
                statement.execute(sql);
            }
            ensureColumn(statement, "candidate_profiles", "pdf_url", "VARCHAR(1024)");
            ensureColumn(statement, "candidate_profiles", "video_url", "VARCHAR(1024)");
            ensureColumn(statement, "candidate_profiles", "phone_number", "VARCHAR(20)");
            ensureColumn(statement, "candidate_profiles", "fan_number", "VARCHAR(50)");
            ensureColumn(statement, "candidate_profiles", "date_of_birth", "DATE");
            ensureColumn(statement, "candidate_profiles", "address", "TEXT");
            ensureColumn(statement, "candidate_profiles", "approval_status", "VARCHAR(20) DEFAULT 'APPROVED'");
            ensureColumn(statement, "candidate_profiles", "admin_review_message", "TEXT");
            ensureUniqueIndex(statement, "candidate_profiles", "ux_candidate_phone", "phone_number");
            ensureUniqueIndex(statement, "candidate_profiles", "ux_candidate_fan", "fan_number");
            ElectionScheduleDAO.ensureTable();
            UserDAO.ensureSampleUsers();
            CandidateProfileDAO.ensureSampleCandidates();
        } catch (SQLException e) {
            System.err.println("Database schema initialization failed: " + e.getMessage());
        }
    }

    private static void ensureColumn(Statement statement, String table, String column, String definition) {
        try {
            statement.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        } catch (SQLException ignored) {
            // Column already exists on upgraded databases.
        }
    }

    private static void ensureUniqueIndex(Statement statement, String table, String indexName, String column) {
        try {
            statement.execute("CREATE UNIQUE INDEX " + indexName + " ON " + table + "(" + column + ")");
        } catch (SQLException ignored) {
            // Index may already exist or column has duplicates on legacy data.
        }
    }
}
