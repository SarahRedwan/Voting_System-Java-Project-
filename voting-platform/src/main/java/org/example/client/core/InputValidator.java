package org.example.client.core;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

public final class InputValidator {

    private static final Pattern SAFE_TEXT = Pattern.compile("^[\\p{L}\\p{N}\\s.,'\\-/()#]{1,255}$");
    private static final Pattern PHONE = Pattern.compile("^[0-9]{9,15}$");
    private static final Pattern FAN = Pattern.compile("^[A-Za-z0-9]{10,20}$");
    private static final Pattern PASSWORD = Pattern.compile("^.{4,128}$");

    private InputValidator() {
    }

    public static String sanitizeText(String input, int maxLength) {
        if (input == null) {
            return "";
        }
        String cleaned = input.trim()
                .replaceAll("<", "")
                .replaceAll(">", "")
                .replaceAll("\"", "")
                .replaceAll("'", "")
                .replaceAll(";", "")
                .replaceAll("--", "");
        if (cleaned.length() > maxLength) {
            cleaned = cleaned.substring(0, maxLength);
        }
        return cleaned;
    }

    public static String normalizePhone(String phone) {
        if (phone == null) {
            return "";
        }
        return phone.replaceAll("[^0-9]", "");
    }

    public static String normalizeFan(String fan) {
        if (fan == null) {
            return "";
        }
        return fan.trim().toUpperCase().replaceAll("[^A-Z0-9]", "");
    }

    public static ValidationResult validateName(String name) {
        String value = sanitizeText(name, 255);
        if (value.isBlank()) {
            return ValidationResult.error("Full name is required.");
        }
        if (!SAFE_TEXT.matcher(value).matches()) {
            return ValidationResult.error("Full name contains invalid characters.");
        }
        return ValidationResult.ok(value);
    }

    public static ValidationResult validatePhone(String phone) {
        String normalized = normalizePhone(phone);
        if (normalized.isBlank()) {
            return ValidationResult.error("Phone number is required.");
        }
        if (!PHONE.matcher(normalized).matches()) {
            return ValidationResult.error("Phone number must be 9-15 digits.");
        }
        return ValidationResult.ok(normalized);
    }

    public static ValidationResult validateFan(String fan) {
        String normalized = normalizeFan(fan);
        if (normalized.isBlank()) {
            return ValidationResult.error("FAN number (Fayda ID) is required.");
        }
        if (!FAN.matcher(normalized).matches()) {
            return ValidationResult.error("FAN number must be 10-20 letters or digits.");
        }
        return ValidationResult.ok(normalized);
    }

    public static ValidationResult validatePassword(String password) {
        if (password == null || !PASSWORD.matcher(password).matches()) {
            return ValidationResult.error("Password must be 4-128 characters.");
        }
        return ValidationResult.ok(password);
    }

    public static ValidationResult validateAddress(String address) {
        String value = sanitizeText(address, 500);
        if (value.isBlank()) {
            return ValidationResult.error("Address is required.");
        }
        return ValidationResult.ok(value);
    }

    public static ValidationResult validateDateOfBirth(String dobText) {
        if (dobText == null || dobText.isBlank()) {
            return ValidationResult.error("Date of birth is required.");
        }
        try {
            LocalDate dob = LocalDate.parse(dobText.trim());
            if (dob.isAfter(LocalDate.now().minusYears(18))) {
                return ValidationResult.error("You must be at least 18 years old to register.");
            }
            if (dob.isBefore(LocalDate.now().minusYears(120))) {
                return ValidationResult.error("Please enter a valid date of birth.");
            }
            return ValidationResult.ok(dob.toString());
        } catch (DateTimeParseException e) {
            return ValidationResult.error("Date of birth must be YYYY-MM-DD format.");
        }
    }

    public static ValidationResult validateOptionalIdVerification(String value) {
        if (value == null || value.isBlank()) {
            return ValidationResult.ok("");
        }
        return ValidationResult.ok(sanitizeText(value, 100));
    }

    public record ValidationResult(boolean valid, String value, String errorMessage) {
        public static ValidationResult ok(String value) {
            return new ValidationResult(true, value, null);
        }

        public static ValidationResult error(String message) {
            return new ValidationResult(false, null, message);
        }
    }
}
