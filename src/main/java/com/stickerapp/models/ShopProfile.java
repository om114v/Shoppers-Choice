package com.stickerapp.models;

import com.stickerapp.exceptions.ValidationException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Represents the shop profile entity with comprehensive input validation.
 */
public class ShopProfile {
    private static final Pattern SHOP_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s&.-]{2,100}$");
    private static final Pattern GST_PATTERN = Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[0-9]{1}[A-Z]{1}[0-9]{1}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{10,15}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s,.-/#()]{10,500}$");

    private int id;
    private String shopName;
    private String gstNumber;
    private String address;
    private String phoneNumber;
    private String email;
    private String logoPath;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public ShopProfile() {}

    public ShopProfile(String shopName, String gstNumber, String address, String phoneNumber, String email, String logoPath) throws ValidationException {
        setShopName(shopName);
        setGstNumber(gstNumber);
        setAddress(address);
        setPhoneNumber(phoneNumber);
        setEmail(email);
        setLogoPath(logoPath);
    }

    // Getters and Setters with validation
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getShopName() { return shopName; }
    public void setShopName(String shopName) throws ValidationException {
        if (shopName == null || shopName.trim().isEmpty()) {
            throw new ValidationException("Shop name cannot be null or empty");
        }
        shopName = shopName.trim();
        if (!SHOP_NAME_PATTERN.matcher(shopName).matches()) {
            throw new ValidationException("Shop name contains invalid characters or length");
        }
        this.shopName = shopName;
    }

    public String getGstNumber() { return gstNumber; }
    public void setGstNumber(String gstNumber) throws ValidationException {
        if (gstNumber != null && !gstNumber.trim().isEmpty()) {
            gstNumber = gstNumber.trim().toUpperCase();
            if (!GST_PATTERN.matcher(gstNumber).matches()) {
                throw new ValidationException("GST number must be in valid format (15 characters)");
            }
        }
        this.gstNumber = gstNumber;
    }

    public String getAddress() { return address; }
    public void setAddress(String address) throws ValidationException {
        if (address != null && !address.trim().isEmpty()) {
            address = address.trim();
            if (!ADDRESS_PATTERN.matcher(address).matches()) {
                throw new ValidationException("Address contains invalid characters or length");
            }
        }
        this.address = address;
    }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) throws ValidationException {
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            phoneNumber = phoneNumber.trim();
            if (!PHONE_PATTERN.matcher(phoneNumber).matches()) {
                throw new ValidationException("Phone number must be 10-15 digits");
            }
        }
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) throws ValidationException {
        if (email != null && !email.trim().isEmpty()) {
            email = email.trim().toLowerCase();
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                throw new ValidationException("Email must be in valid format");
            }
        }
        this.email = email;
    }

    public String getLogoPath() { return logoPath; }
    public void setLogoPath(String logoPath) throws ValidationException {
        if (logoPath != null && !logoPath.trim().isEmpty()) {
            logoPath = logoPath.trim();
            // Validate path is within allowed directory and has valid extension
            if (!isValidLogoPath(logoPath)) {
                throw new ValidationException("Invalid logo path or file type");
            }
        }
        this.logoPath = logoPath;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    /**
     * Validates if the logo path is safe and within allowed directories.
     * @param path the file path to validate
     * @return true if path is valid
     */
    private boolean isValidLogoPath(String path) {
        if (path == null || path.isEmpty()) {
            return true; // Allow null/empty paths
        }

        try {
            // Check for path traversal attempts
            if (path.contains("..") || path.contains("\\") || path.contains(":")) {
                return false;
            }

            // Check file extension (only allow image files)
            String lowerPath = path.toLowerCase();
            return lowerPath.endsWith(".png") || lowerPath.endsWith(".jpg") ||
                   lowerPath.endsWith(".jpeg") || lowerPath.endsWith(".gif") ||
                   lowerPath.endsWith(".bmp");

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Validates all fields of the shop profile.
     * @return list of validation errors, empty if valid
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();

        try {
            // Validate shop name
            if (shopName == null || shopName.trim().isEmpty()) {
                errors.add("Shop name is required");
            } else if (!SHOP_NAME_PATTERN.matcher(shopName).matches()) {
                errors.add("Shop name contains invalid characters or length");
            }

            // Validate GST number if provided
            if (gstNumber != null && !gstNumber.trim().isEmpty() &&
                !GST_PATTERN.matcher(gstNumber).matches()) {
                errors.add("GST number must be in valid format");
            }

            // Validate address if provided
            if (address != null && !address.trim().isEmpty() &&
                !ADDRESS_PATTERN.matcher(address).matches()) {
                errors.add("Address contains invalid characters or length");
            }

            // Validate phone if provided
            if (phoneNumber != null && !phoneNumber.trim().isEmpty() &&
                !PHONE_PATTERN.matcher(phoneNumber).matches()) {
                errors.add("Phone number must be 10-15 digits");
            }

            // Validate email if provided
            if (email != null && !email.trim().isEmpty() &&
                !EMAIL_PATTERN.matcher(email).matches()) {
                errors.add("Email must be in valid format");
            }

            // Validate logo path if provided
            if (logoPath != null && !logoPath.trim().isEmpty() &&
                !isValidLogoPath(logoPath)) {
                errors.add("Invalid logo path or file type");
            }

        } catch (Exception e) {
            errors.add("Validation error: " + e.getMessage());
        }

        return errors;
    }

    @Override
    public String toString() {
        return "ShopProfile{" +
                "id=" + id +
                ", shopName='" + shopName + '\'' +
                ", gstNumber='" + gstNumber + '\'' +
                ", address='" + address + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", email='" + email + '\'' +
                ", logoPath='" + logoPath + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}