package com.shopper.utils;

import java.util.regex.Pattern;

/**
 * Utility class for comprehensive input sanitization and validation.
 * Provides methods to clean and validate user inputs to prevent injection
 * attacks
 * and ensure data integrity.
 */
public class InputSanitizer {
    private static final AppLogger logger = AppLogger.getInstance();

    // Patterns for dangerous content
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            "(?i)(union|select|insert|update|delete|drop|create|alter|exec|execute|script|javascript|vbscript|onload|onerror)");

    private static final Pattern XSS_PATTERN = Pattern.compile(
            "(?i)(<script|<iframe|<object|<embed|<form|<input|<meta|<link|<style)");

    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(
            "(\\.\\./|/\\.\\.|\\\\\\.\\.|\\.\\\\)");

    private static final Pattern CONTROL_CHARACTERS_PATTERN = Pattern.compile(
            "[\\x00-\\x1F\\x7F-\\x9F]");

    // Maximum lengths for different input types
    private static final int MAX_TEXT_LENGTH = 10000;
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_EMAIL_LENGTH = 254;
    private static final int MAX_PHONE_LENGTH = 20;
    private static final int MAX_ADDRESS_LENGTH = 500;
    private static final int MAX_PATH_LENGTH = 255;

    /**
     * Sanitizes general text input by removing dangerous characters and limiting
     * length.
     * 
     * @param input the input text to sanitize
     * @return sanitized text or null if input is null
     */
    public static String sanitizeText(String input) {
        return sanitizeText(input, MAX_TEXT_LENGTH);
    }

    /**
     * Sanitizes general text input with custom length limit.
     * 
     * @param input     the input text to sanitize
     * @param maxLength maximum allowed length
     * @return sanitized text or null if input is null
     */
    public static String sanitizeText(String input, int maxLength) {
        if (input == null) {
            return null;
        }

        String sanitized = input.trim();

        // Remove control characters
        sanitized = CONTROL_CHARACTERS_PATTERN.matcher(sanitized).replaceAll("");

        // Check for dangerous patterns
        if (SQL_INJECTION_PATTERN.matcher(sanitized).find()) {
            logger.warn("Potential SQL injection detected in input: "
                    + sanitized.substring(0, Math.min(50, sanitized.length())));
            throw new IllegalArgumentException("Input contains potentially dangerous SQL patterns");
        }

        if (XSS_PATTERN.matcher(sanitized).find()) {
            logger.warn("Potential XSS attack detected in input: "
                    + sanitized.substring(0, Math.min(50, sanitized.length())));
            throw new IllegalArgumentException("Input contains potentially dangerous HTML/script patterns");
        }

        // Limit length
        if (sanitized.length() > maxLength) {
            sanitized = sanitized.substring(0, maxLength);
            logger.debug("Input truncated to maximum length: " + maxLength);
        }

        return sanitized;
    }

    /**
     * Sanitizes name input (for item names, shop names, etc.).
     * 
     * @param name the name to sanitize
     * @return sanitized name
     */
    public static String sanitizeName(String name) {
        if (name == null) {
            return null;
        }

        String sanitized = sanitizeText(name, MAX_NAME_LENGTH);

        // Allow only alphanumeric characters, spaces, and common punctuation
        if (sanitized != null && !sanitized.matches("^[a-zA-Z0-9\\s&.,'-]+$")) {
            logger.warn("Invalid characters in name: " + sanitized);
            throw new IllegalArgumentException("Name contains invalid characters");
        }

        return sanitized;
    }

    /**
     * Sanitizes email input.
     * 
     * @param email the email to sanitize
     * @return sanitized email
     */
    public static String sanitizeEmail(String email) {
        if (email == null) {
            return null;
        }

        String sanitized = sanitizeText(email.toLowerCase(), MAX_EMAIL_LENGTH);

        // Basic email pattern validation
        if (sanitized != null && !sanitized.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            logger.warn("Invalid email format: " + sanitized);
            throw new IllegalArgumentException("Invalid email format");
        }

        return sanitized;
    }

    /**
     * Sanitizes phone number input.
     * 
     * @param phone the phone number to sanitize
     * @return sanitized phone number
     */
    public static String sanitizePhone(String phone) {
        if (phone == null) {
            return null;
        }

        String sanitized = sanitizeText(phone, MAX_PHONE_LENGTH);

        // Remove all non-digit and non-plus characters
        if (sanitized != null) {
            sanitized = sanitized.replaceAll("[^0-9+]", "");
            if (!sanitized.matches("^\\+?[0-9]{10,15}$")) {
                logger.warn("Invalid phone format: " + sanitized);
                throw new IllegalArgumentException("Invalid phone number format");
            }
        }

        return sanitized;
    }

    /**
     * Sanitizes address input.
     * 
     * @param address the address to sanitize
     * @return sanitized address
     */
    public static String sanitizeAddress(String address) {
        if (address == null) {
            return null;
        }

        String sanitized = sanitizeText(address, MAX_ADDRESS_LENGTH);

        // Allow alphanumeric characters and common address punctuation
        if (sanitized != null && !sanitized.matches("^[a-zA-Z0-9\\s,./#()-]+$")) {
            logger.warn("Invalid characters in address: " + sanitized);
            throw new IllegalArgumentException("Address contains invalid characters");
        }

        return sanitized;
    }

    /**
     * Sanitizes file path input.
     * 
     * @param path the path to sanitize
     * @return sanitized path
     */
    public static String sanitizePath(String path) {
        if (path == null) {
            return null;
        }

        String sanitized = sanitizeText(path, MAX_PATH_LENGTH);

        // Check for path traversal
        if (sanitized != null && PATH_TRAVERSAL_PATTERN.matcher(sanitized).find()) {
            logger.warn("Path traversal detected: " + sanitized);
            throw new IllegalArgumentException("Path traversal not allowed");
        }

        return sanitized;
    }

    /**
     * Sanitizes numeric input and converts to integer.
     * 
     * @param input    the numeric string to sanitize
     * @param minValue minimum allowed value
     * @param maxValue maximum allowed value
     * @return sanitized integer
     */
    public static int sanitizeInt(String input, int minValue, int maxValue) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Input cannot be null or empty");
        }

        try {
            int value = Integer.parseInt(input.trim());
            if (value < minValue || value > maxValue) {
                throw new IllegalArgumentException("Value must be between " + minValue + " and " + maxValue);
            }
            return value;
        } catch (NumberFormatException e) {
            logger.warn("Invalid integer format: " + input);
            throw new IllegalArgumentException("Invalid number format");
        }
    }

    /**
     * Sanitizes numeric input and converts to double.
     * 
     * @param input    the numeric string to sanitize
     * @param minValue minimum allowed value
     * @param maxValue maximum allowed value
     * @return sanitized double
     */
    public static double sanitizeDouble(String input, double minValue, double maxValue) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Input cannot be null or empty");
        }

        try {
            double value = Double.parseDouble(input.trim());
            if (value < minValue || value > maxValue) {
                throw new IllegalArgumentException("Value must be between " + minValue + " and " + maxValue);
            }
            return value;
        } catch (NumberFormatException e) {
            logger.warn("Invalid double format: " + input);
            throw new IllegalArgumentException("Invalid number format");
        }
    }

    /**
     * Checks if input contains potentially dangerous patterns.
     * 
     * @param input the input to check
     * @return true if input appears safe
     */
    public static boolean isSafeInput(String input) {
        if (input == null) {
            return true;
        }

        return !SQL_INJECTION_PATTERN.matcher(input).find() &&
                !XSS_PATTERN.matcher(input).find() &&
                !PATH_TRAVERSAL_PATTERN.matcher(input).find();
    }

    /**
     * Escapes special characters for safe display in HTML.
     * 
     * @param input the input to escape
     * @return HTML-safe string
     */
    public static String escapeHtml(String input) {
        if (input == null) {
            return null;
        }

        return input.replace("&", "&")
                .replace("<", "<")
                .replace(">", ">")
                .replace("\"", "\"")
                .replace("'", "&#x27;")
                .replace("/", "&#x2F;");
    }

    /**
     * Validates GST number format.
     * 
     * @param gst the GST number to validate
     * @return true if valid GST format
     */
    public static boolean isValidGstNumber(String gst) {
        if (gst == null || gst.trim().isEmpty()) {
            return true; // Optional field
        }

        gst = gst.trim().toUpperCase();
        return gst.matches("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[0-9]{1}[A-Z]{1}[0-9]{1}$");
    }

    /**
     * Sanitizes GST number input.
     * 
     * @param gst the GST number to sanitize
     * @return sanitized GST number
     */
    public static String sanitizeGstNumber(String gst) {
        if (gst == null) {
            return null;
        }

        String sanitized = sanitizeText(gst.toUpperCase(), 15);

        if (sanitized != null && !isValidGstNumber(sanitized)) {
            logger.warn("Invalid GST format: " + sanitized);
            throw new IllegalArgumentException("Invalid GST number format");
        }

        return sanitized;
    }
}