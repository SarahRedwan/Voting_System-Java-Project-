package org.example.client.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public final class RegistrationService {

    public record RegistrationResult(boolean success, String message, String loginId) {
    }

    private RegistrationService() {
    }

    public static RegistrationResult registerVoter(String fullName, String phone, String fan, String dob,
                                                   String address, String idVerification, String password) {
        if (!RegistrationRateLimiter.allowAttempt(phone)) {
            return new RegistrationResult(false, RegistrationRateLimiter.blockedMessage(), null);
        }

        InputValidator.ValidationResult nameResult = InputValidator.validateName(fullName);
        if (!nameResult.valid()) {
            return new RegistrationResult(false, nameResult.errorMessage(), null);
        }
        InputValidator.ValidationResult phoneResult = InputValidator.validatePhone(phone);
        if (!phoneResult.valid()) {
            return new RegistrationResult(false, phoneResult.errorMessage(), null);
        }
        InputValidator.ValidationResult fanResult = InputValidator.validateFan(fan);
        if (!fanResult.valid()) {
            return new RegistrationResult(false, fanResult.errorMessage(), null);
        }
        InputValidator.ValidationResult dobResult = InputValidator.validateDateOfBirth(dob);
        if (!dobResult.valid()) {
            return new RegistrationResult(false, dobResult.errorMessage(), null);
        }
        InputValidator.ValidationResult addressResult = InputValidator.validateAddress(address);
        if (!addressResult.valid()) {
            return new RegistrationResult(false, addressResult.errorMessage(), null);
        }
        InputValidator.ValidationResult passwordResult = InputValidator.validatePassword(password);
        if (!passwordResult.valid()) {
            return new RegistrationResult(false, passwordResult.errorMessage(), null);
        }
        InputValidator.ValidationResult idResult = InputValidator.validateOptionalIdVerification(idVerification);

        String normalizedPhone = phoneResult.value();
        String normalizedFan = fanResult.value();

        if (VoterDAO.phoneExists(normalizedPhone) || CandidateProfileDAO.phoneExists(normalizedPhone)) {
            return new RegistrationResult(false, "Phone number already registered.", null);
        }
        if (VoterDAO.fanExists(normalizedFan) || CandidateProfileDAO.fanExists(normalizedFan)) {
            return new RegistrationResult(false, "FAN number already registered.", null);
        }

        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String voterId = VoterDAO.generateVoterId(connection);
                UserDAO.insertUser(connection, voterId, password, "VOTER", nameResult.value(), true);
                VoterDAO.insertVoter(
                        connection,
                        voterId,
                        voterId,
                        nameResult.value(),
                        normalizedPhone,
                        normalizedFan,
                        LocalDate.parse(dobResult.value()),
                        addressResult.value(),
                        idResult.value()
                );
                connection.commit();
                return new RegistrationResult(
                        true,
                        "Registration successful! Your username and Voter ID: " + voterId + ". Use either to sign in.",
                        voterId
                );
            } catch (SQLException e) {
                connection.rollback();
                return new RegistrationResult(false, "Registration failed: " + e.getMessage(), null);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            return new RegistrationResult(false, "Database error: " + e.getMessage(), null);
        }
    }

    public static RegistrationResult registerCandidate(String fullName, String phone, String fan, String dob,
                                                       String party, String position, String biography,
                                                       String address, String password) {
        if (!RegistrationRateLimiter.allowAttempt(phone)) {
            return new RegistrationResult(false, RegistrationRateLimiter.blockedMessage(), null);
        }

        InputValidator.ValidationResult nameResult = InputValidator.validateName(fullName);
        if (!nameResult.valid()) {
            return new RegistrationResult(false, nameResult.errorMessage(), null);
        }
        InputValidator.ValidationResult phoneResult = InputValidator.validatePhone(phone);
        if (!phoneResult.valid()) {
            return new RegistrationResult(false, phoneResult.errorMessage(), null);
        }
        InputValidator.ValidationResult fanResult = InputValidator.validateFan(fan);
        if (!fanResult.valid()) {
            return new RegistrationResult(false, fanResult.errorMessage(), null);
        }
        InputValidator.ValidationResult dobResult = InputValidator.validateDateOfBirth(dob);
        if (!dobResult.valid()) {
            return new RegistrationResult(false, dobResult.errorMessage(), null);
        }
        InputValidator.ValidationResult addressResult = InputValidator.validateAddress(address);
        if (!addressResult.valid()) {
            return new RegistrationResult(false, addressResult.errorMessage(), null);
        }
        InputValidator.ValidationResult passwordResult = InputValidator.validatePassword(password);
        if (!passwordResult.valid()) {
            return new RegistrationResult(false, passwordResult.errorMessage(), null);
        }

        String normalizedPhone = phoneResult.value();
        String normalizedFan = fanResult.value();
        String sanitizedParty = InputValidator.sanitizeText(party, 255);
        String sanitizedPosition = InputValidator.sanitizeText(position, 255);
        String sanitizedBio = InputValidator.sanitizeText(biography, 2000);

        if (sanitizedParty.isBlank()) {
            return new RegistrationResult(false, "Political party / affiliation is required.", null);
        }
        if (sanitizedPosition.isBlank()) {
            return new RegistrationResult(false, "Position running for is required.", null);
        }
        if (sanitizedBio.isBlank()) {
            return new RegistrationResult(false, "Biography is required.", null);
        }

        if (VoterDAO.phoneExists(normalizedPhone) || CandidateProfileDAO.phoneExists(normalizedPhone)) {
            return new RegistrationResult(false, "Phone number already registered.", null);
        }
        if (VoterDAO.fanExists(normalizedFan) || CandidateProfileDAO.fanExists(normalizedFan)) {
            return new RegistrationResult(false, "FAN number already registered.", null);
        }

        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String candidateLoginId = generateCandidateLoginId(connection);
                UserDAO.insertUser(connection, candidateLoginId, password, "CANDIDATE", nameResult.value(), false);

                String sql = "INSERT INTO candidate_profiles(username, name, party, description, position, phone_number, fan_number, "
                        + "date_of_birth, address, approval_status, last_updated) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', CURRENT_TIMESTAMP)";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, candidateLoginId);
                    ps.setString(2, nameResult.value());
                    ps.setString(3, sanitizedParty);
                    ps.setString(4, sanitizedBio);
                    ps.setString(5, sanitizedPosition);
                    ps.setString(6, normalizedPhone);
                    ps.setString(7, normalizedFan);
                    ps.setDate(8, java.sql.Date.valueOf(LocalDate.parse(dobResult.value())));
                    ps.setString(9, addressResult.value());
                    ps.executeUpdate();
                }

                connection.commit();
                return new RegistrationResult(
                        true,
                        "Application submitted! Your Candidate ID is: " + candidateLoginId
                                + ". You can sign in after admin approval.",
                        candidateLoginId
                );
            } catch (SQLException e) {
                connection.rollback();
                return new RegistrationResult(false, "Registration failed: " + e.getMessage(), null);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            return new RegistrationResult(false, "Database error: " + e.getMessage(), null);
        }
    }

    private static String generateCandidateLoginId(Connection connection) throws SQLException {
        int year = java.time.Year.now().getValue();
        String prefix = "C" + year;
        String sql = "SELECT username FROM candidate_profiles WHERE username LIKE ? ORDER BY username DESC LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, prefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                int next = 1;
                if (rs.next()) {
                    String last = rs.getString("username");
                    String numeric = last.substring(prefix.length());
                    next = Integer.parseInt(numeric) + 1;
                }
                return prefix + String.format("%06d", next);
            }
        }
    }
}
