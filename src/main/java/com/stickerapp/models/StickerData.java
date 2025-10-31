package com.stickerapp.models;

import com.stickerapp.exceptions.ValidationException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Represents the sticker data entity (sticker_history table) with comprehensive input validation.
 */
public class StickerData {
    private static final Pattern ITEM_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s&.-]{2,100}$");
    private static final Pattern SUPPLIER_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s&.-]{0,100}$");

    private int id;
    private String itemName;
    private String supplierName;
    private BigDecimal price;
    private int numberOfStickers;
    private LocalDateTime printedAt;

    // Constructors
    public StickerData() {}

    public StickerData(String itemName, String supplierName, BigDecimal price, int numberOfStickers) throws ValidationException {
        setItemName(itemName);
        setSupplierName(supplierName);
        setPrice(price);
        setNumberOfStickers(numberOfStickers);
    }

    // Getters and Setters with validation
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) throws ValidationException {
        if (itemName == null || itemName.trim().isEmpty()) {
            throw new ValidationException("Item name cannot be null or empty");
        }
        itemName = itemName.trim();
        if (!ITEM_NAME_PATTERN.matcher(itemName).matches()) {
            throw new ValidationException("Item name contains invalid characters or length");
        }
        this.itemName = itemName;
    }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) throws ValidationException {
        if (supplierName != null && !supplierName.trim().isEmpty()) {
            supplierName = supplierName.trim();
            if (!SUPPLIER_NAME_PATTERN.matcher(supplierName).matches()) {
                throw new ValidationException("Supplier name contains invalid characters");
            }
        }
        this.supplierName = supplierName;
    }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) throws ValidationException {
        if (price == null) {
            throw new ValidationException("Price cannot be null");
        }
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Price cannot be negative");
        }
        if (price.compareTo(new BigDecimal("999999.99")) > 0) {
            throw new ValidationException("Price cannot exceed 999,999.99");
        }
        // Check decimal places
        if (price.scale() > 2) {
            throw new ValidationException("Price can have at most 2 decimal places");
        }
        this.price = price;
    }

    public int getNumberOfStickers() { return numberOfStickers; }
    public void setNumberOfStickers(int numberOfStickers) throws ValidationException {
        if (numberOfStickers <= 0) {
            throw new ValidationException("Number of stickers must be positive");
        }
        if (numberOfStickers > 1000) {
            throw new ValidationException("Number of stickers cannot exceed 1000");
        }
        this.numberOfStickers = numberOfStickers;
    }

    // Alias for getNumberOfStickers for database compatibility
    public int getQuantity() { return numberOfStickers; }
    public void setQuantity(int quantity) throws ValidationException {
        setNumberOfStickers(quantity);
    }

    public LocalDateTime getPrintedAt() { return printedAt; }
    public void setPrintedAt(LocalDateTime printedAt) { this.printedAt = printedAt; }

    /**
     * Validates all fields of the sticker data.
     * @return list of validation errors, empty if valid
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();

        try {
            // Validate item name
            if (itemName == null || itemName.trim().isEmpty()) {
                errors.add("Item name is required");
            } else if (!ITEM_NAME_PATTERN.matcher(itemName).matches()) {
                errors.add("Item name contains invalid characters or length");
            }

            // Validate supplier name if provided
            if (supplierName != null && !supplierName.trim().isEmpty() &&
                !SUPPLIER_NAME_PATTERN.matcher(supplierName).matches()) {
                errors.add("Supplier name contains invalid characters");
            }

            // Validate price
            if (price == null) {
                errors.add("Price is required");
            } else {
                if (price.compareTo(BigDecimal.ZERO) < 0) {
                    errors.add("Price cannot be negative");
                }
                if (price.compareTo(new BigDecimal("999999.99")) > 0) {
                    errors.add("Price cannot exceed 999,999.99");
                }
                if (price.scale() > 2) {
                    errors.add("Price can have at most 2 decimal places");
                }
            }

            // Validate quantity
            if (numberOfStickers <= 0) {
                errors.add("Number of stickers must be positive");
            } else if (numberOfStickers > 1000) {
                errors.add("Number of stickers cannot exceed 1000");
            }

        } catch (Exception e) {
            errors.add("Validation error: " + e.getMessage());
        }

        return errors;
    }

    @Override
    public String toString() {
        return "StickerData{" +
                "itemName='" + itemName + '\'' +
                ", supplierName='" + supplierName + '\'' +
                ", price=" + price +
                ", numberOfStickers=" + numberOfStickers +
                '}';
    }
}