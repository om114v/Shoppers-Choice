package com.shopper.services;

import com.shopper.exceptions.ValidationException;
import com.shopper.utils.InputSanitizer;
import com.shopper.utils.Logger;

import java.util.ArrayList;
import java.util.List;

public class ValidationService {
    private static final Logger logger = Logger.getInstance();

    public static List<String> validateItemName(String name) throws ValidationException {
            try {
                List<String> errors = new ArrayList<>();

                // First sanitize the input
                name = InputSanitizer.sanitizeName(name);

                if (name == null || name.trim().isEmpty()) {
                    errors.add("Item name is required");
                } else if (name.length() < 2 || name.length() > 100) {
                    errors.add("Item name must be between 2 and 100 characters");
                }
                return errors;
            } catch (IllegalArgumentException e) {
                List<String> errors = new ArrayList<>();
                errors.add("Invalid item name: " + e.getMessage());
                return errors;
            } catch (Exception e) {
                logger.error(ValidationService.class.getSimpleName(), "Error validating item name", e);
                throw new ValidationException("Invalid input data: item name validation failed", e);
            }
        }

    public static List<String> validateSupplierName(String supplier) throws ValidationException {
        try {
            List<String> errors = new ArrayList<>();

            // Sanitize the input if provided
            if (supplier != null && !supplier.trim().isEmpty()) {
                supplier = InputSanitizer.sanitizeName(supplier);
                if (supplier.length() > 100) {
                    errors.add("Supplier name must not exceed 100 characters");
                }
            }
            return errors;
        } catch (IllegalArgumentException e) {
            List<String> errors = new ArrayList<>();
            errors.add("Invalid supplier name: " + e.getMessage());
            return errors;
        } catch (Exception e) {
            logger.error(ValidationService.class.getSimpleName(), "Error validating supplier name", e);
            throw new ValidationException("Invalid input data: supplier name validation failed", e);
        }
    }

    public static List<String> validatePrice(String priceStr) throws ValidationException {
        try {
            List<String> errors = new ArrayList<>();
            if (priceStr == null || priceStr.trim().isEmpty()) {
                errors.add("Price is required");
                return errors;
            }

            // Sanitize the input first
            priceStr = InputSanitizer.sanitizeText(priceStr, 20);

            try {
                double price = Double.parseDouble(priceStr);
                if (price <= 0) {
                    errors.add("Price must be positive");
                } else if (price > 999999.99) {
                    errors.add("Price cannot exceed 999,999.99");
                }
                // Check decimal places
                String[] parts = priceStr.split("\\.");
                if (parts.length > 1 && parts[1].length() > 2) {
                    errors.add("Price can have at most 2 decimal places");
                }
            } catch (NumberFormatException e) {
                errors.add("Price must be a valid number");
            }
            return errors;
        } catch (IllegalArgumentException e) {
            List<String> errors = new ArrayList<>();
            errors.add("Invalid price: " + e.getMessage());
            return errors;
        } catch (Exception e) {
            logger.error(ValidationService.class.getSimpleName(), "Error validating price", e);
            throw new ValidationException("Invalid input data: price validation failed", e);
        }
    }

    public static List<String> validateNumberOfStickers(String numStr) throws ValidationException {
        try {
            List<String> errors = new ArrayList<>();
            if (numStr == null || numStr.trim().isEmpty()) {
                errors.add("Number of stickers is required");
                return errors;
            }

            // Sanitize the input first
            numStr = InputSanitizer.sanitizeText(numStr, 10);

            try {
                int num = Integer.parseInt(numStr);
                if (num < 1 || num > 1000) {
                    errors.add("Number of stickers must be between 1 and 1000");
                }
            } catch (NumberFormatException e) {
                errors.add("Number of stickers must be a valid integer");
            }
            return errors;
        } catch (IllegalArgumentException e) {
            List<String> errors = new ArrayList<>();
            errors.add("Invalid number of stickers: " + e.getMessage());
            return errors;
        } catch (Exception e) {
            logger.error(ValidationService.class.getSimpleName(), "Error validating number of stickers", e);
            throw new ValidationException("Invalid input data: number of stickers validation failed", e);
        }
    }

    public static List<String> validateGSTNumber(String gst) throws ValidationException {
        try {
            List<String> errors = new ArrayList<>();
            if (gst != null && !gst.trim().isEmpty()) {
                // Sanitize the GST number first
                gst = InputSanitizer.sanitizeGstNumber(gst);

                // GST format: 15 characters, pattern: 2 digits, 5 letters, 4 digits, 1 letter, 1 digit, 1 letter, 1 digit
                String gstPattern = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[0-9]{1}[A-Z]{1}[0-9]{1}$";
                if (!gst.matches(gstPattern)) {
                    errors.add("GST number must be in valid format (e.g., 22AAAAA0000A1Z5)");
                }
            }
            return errors;
        } catch (IllegalArgumentException e) {
            List<String> errors = new ArrayList<>();
            errors.add("Invalid GST number: " + e.getMessage());
            return errors;
        } catch (Exception e) {
            logger.error(ValidationService.class.getSimpleName(), "Error validating GST number", e);
            throw new ValidationException("Invalid input data: GST number validation failed", e);
        }
    }

    public static List<String> validatePhone(String phone) throws ValidationException {
        try {
            List<String> errors = new ArrayList<>();
            if (phone != null && !phone.trim().isEmpty()) {
                // Sanitize the phone number first
                phone = InputSanitizer.sanitizePhone(phone);

                if (!phone.matches("^\\+?[0-9]{10,15}$")) {
                    errors.add("Phone must be 10-15 digits (with optional + prefix)");
                }
            }
            return errors;
        } catch (IllegalArgumentException e) {
            List<String> errors = new ArrayList<>();
            errors.add("Invalid phone number: " + e.getMessage());
            return errors;
        } catch (Exception e) {
            logger.error(ValidationService.class.getSimpleName(), "Error validating phone", e);
            throw new ValidationException("Invalid input data: phone validation failed", e);
        }
    }

    public static List<String> validateEmail(String email) throws ValidationException {
        try {
            List<String> errors = new ArrayList<>();
            if (email != null && !email.trim().isEmpty()) {
                // Sanitize the email first
                email = InputSanitizer.sanitizeEmail(email);

                String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
                if (!email.matches(emailPattern)) {
                    errors.add("Email must be in valid format");
                }
            }
            return errors;
        } catch (IllegalArgumentException e) {
            List<String> errors = new ArrayList<>();
            errors.add("Invalid email: " + e.getMessage());
            return errors;
        } catch (Exception e) {
            logger.error(ValidationService.class.getSimpleName(), "Error validating email", e);
            throw new ValidationException("Invalid input data: email validation failed", e);
        }
    }
}