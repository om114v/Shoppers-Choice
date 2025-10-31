package com.shopper.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.shopper.exceptions.ValidationException;
import com.shopper.services.ValidationService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidationServiceTest {

    @Test
    @DisplayName("validateItemName - Valid name")
    void testValidateItemName_Valid() throws ValidationException {
        List<String> errors = ValidationService.validateItemName("Valid Item");
        assertTrue(errors.isEmpty(), "Should have no errors for valid item name");
    }

    @Test
    @DisplayName("validateItemName - Null name")
    void testValidateItemName_Null() throws ValidationException {
        List<String> errors = ValidationService.validateItemName(null);
        assertEquals(1, errors.size(), "Should have one error for null name");
        assertEquals("Item name is required", errors.get(0));
    }

    @Test
    @DisplayName("validateItemName - Empty name")
    void testValidateItemName_Empty() throws ValidationException {
        List<String> errors = ValidationService.validateItemName("");
        assertEquals(1, errors.size(), "Should have one error for empty name");
        assertEquals("Item name is required", errors.get(0));
    }

    @Test
    @DisplayName("validateItemName - Whitespace only")
    void testValidateItemName_WhitespaceOnly() throws ValidationException {
        List<String> errors = ValidationService.validateItemName("   ");
        assertEquals(1, errors.size(), "Should have one error for whitespace only");
        assertEquals("Item name is required", errors.get(0));
    }

    @Test
    @DisplayName("validateItemName - Too short")
    void testValidateItemName_TooShort() throws ValidationException {
        List<String> errors = ValidationService.validateItemName("A");
        assertEquals(1, errors.size(), "Should have one error for name too short");
        assertEquals("Item name must be between 2 and 100 characters", errors.get(0));
    }

    @Test
    @DisplayName("validateItemName - Too long")
    void testValidateItemName_TooLong() throws ValidationException {
        String longName = "A".repeat(101);
        List<String> errors = ValidationService.validateItemName(longName);
        assertEquals(1, errors.size(), "Should have one error for name too long");
        assertEquals("Item name must be between 2 and 100 characters", errors.get(0));
    }

    @Test
    @DisplayName("validateItemName - Minimum length")
    void testValidateItemName_MinLength() throws ValidationException {
        List<String> errors = ValidationService.validateItemName("AB");
        assertTrue(errors.isEmpty(), "Should have no errors for minimum length");
    }

    @Test
    @DisplayName("validateItemName - Maximum length")
    void testValidateItemName_MaxLength() throws ValidationException {
        String maxName = "A".repeat(100);
        List<String> errors = ValidationService.validateItemName(maxName);
        assertTrue(errors.isEmpty(), "Should have no errors for maximum length");
    }

    @Test
    @DisplayName("validateSupplierName - Valid supplier")
    void testValidateSupplierName_Valid() throws ValidationException {
        List<String> errors = ValidationService.validateSupplierName("Valid Supplier");
        assertTrue(errors.isEmpty(), "Should have no errors for valid supplier name");
    }

    @Test
    @DisplayName("validateSupplierName - Null supplier")
    void testValidateSupplierName_Null() throws ValidationException {
        List<String> errors = ValidationService.validateSupplierName(null);
        assertTrue(errors.isEmpty(), "Should have no errors for null supplier name");
    }

    @Test
    @DisplayName("validateSupplierName - Empty supplier")
    void testValidateSupplierName_Empty() throws ValidationException {
        List<String> errors = ValidationService.validateSupplierName("");
        assertTrue(errors.isEmpty(), "Should have no errors for empty supplier name");
    }

    @Test
    @DisplayName("validateSupplierName - Too long")
    void testValidateSupplierName_TooLong() throws ValidationException {
        String longSupplier = "A".repeat(101);
        List<String> errors = ValidationService.validateSupplierName(longSupplier);
        assertEquals(1, errors.size(), "Should have one error for supplier name too long");
        assertEquals("Supplier name must not exceed 100 characters", errors.get(0));
    }

    @Test
    @DisplayName("validateSupplierName - Maximum length")
    void testValidateSupplierName_MaxLength() throws ValidationException {
        String maxSupplier = "A".repeat(100);
        List<String> errors = ValidationService.validateSupplierName(maxSupplier);
        assertTrue(errors.isEmpty(), "Should have no errors for maximum length supplier name");
    }

    @Test
    @DisplayName("validatePrice - Valid price")
    void testValidatePrice_Valid() throws ValidationException {
        List<String> errors = ValidationService.validatePrice("10.50");
        assertTrue(errors.isEmpty(), "Should have no errors for valid price");
    }

    @Test
    @DisplayName("validatePrice - Null price")
    void testValidatePrice_Null() throws ValidationException {
        List<String> errors = ValidationService.validatePrice(null);
        assertEquals(1, errors.size(), "Should have one error for null price");
        assertEquals("Price is required", errors.get(0));
    }

    @Test
    @DisplayName("validatePrice - Empty price")
    void testValidatePrice_Empty() throws ValidationException {
        List<String> errors = ValidationService.validatePrice("");
        assertEquals(1, errors.size(), "Should have one error for empty price");
        assertEquals("Price is required", errors.get(0));
    }

    @Test
    @DisplayName("validatePrice - Whitespace only")
    void testValidatePrice_WhitespaceOnly() throws ValidationException {
        List<String> errors = ValidationService.validatePrice("   ");
        assertEquals(1, errors.size(), "Should have one error for whitespace only price");
        assertEquals("Price is required", errors.get(0));
    }

    @Test
    @DisplayName("validatePrice - Zero price")
    void testValidatePrice_Zero() throws ValidationException {
        List<String> errors = ValidationService.validatePrice("0");
        assertEquals(1, errors.size(), "Should have one error for zero price");
        assertEquals("Price must be positive", errors.get(0));
    }

    @Test
    @DisplayName("validatePrice - Negative price")
    void testValidatePrice_Negative() throws ValidationException {
        List<String> errors = ValidationService.validatePrice("-5.00");
        assertEquals(1, errors.size(), "Should have one error for negative price");
        assertEquals("Price must be positive", errors.get(0));
    }

    @Test
    @DisplayName("validatePrice - Too many decimal places")
    void testValidatePrice_TooManyDecimals() throws ValidationException {
        List<String> errors = ValidationService.validatePrice("10.123");
        assertEquals(1, errors.size(), "Should have one error for too many decimal places");
        assertEquals("Price can have at most 2 decimal places", errors.get(0));
    }

    @Test
    @DisplayName("validatePrice - Valid with two decimals")
    void testValidatePrice_ValidTwoDecimals() throws ValidationException {
        List<String> errors = ValidationService.validatePrice("10.12");
        assertTrue(errors.isEmpty(), "Should have no errors for valid price with two decimals");
    }

    @Test
    @DisplayName("validatePrice - Valid integer")
    void testValidatePrice_ValidInteger() throws ValidationException {
        List<String> errors = ValidationService.validatePrice("100");
        assertTrue(errors.isEmpty(), "Should have no errors for valid integer price");
    }

    @Test
    @DisplayName("validatePrice - Invalid format")
    void testValidatePrice_InvalidFormat() throws ValidationException {
        List<String> errors = ValidationService.validatePrice("abc");
        assertEquals(1, errors.size(), "Should have one error for invalid price format");
        assertEquals("Price must be a valid number", errors.get(0));
    }

    @Test
    @DisplayName("validatePrice - Leading/trailing spaces")
    void testValidatePrice_LeadingTrailingSpaces() throws ValidationException {
        List<String> errors = ValidationService.validatePrice(" 10.50 ");
        assertTrue(errors.isEmpty(), "Should have no errors for price with leading/trailing spaces");
    }

    @Test
    @DisplayName("validateNumberOfStickers - Valid number")
    void testValidateNumberOfStickers_Valid() throws ValidationException {
        List<String> errors = ValidationService.validateNumberOfStickers("100");
        assertTrue(errors.isEmpty(), "Should have no errors for valid number of stickers");
    }

    @Test
    @DisplayName("validateNumberOfStickers - Null number")
    void testValidateNumberOfStickers_Null() throws ValidationException {
        List<String> errors = ValidationService.validateNumberOfStickers(null);
        assertEquals(1, errors.size(), "Should have one error for null number");
        assertEquals("Number of stickers is required", errors.get(0));
    }

    @Test
    @DisplayName("validateNumberOfStickers - Empty number")
    void testValidateNumberOfStickers_Empty() throws ValidationException {
        List<String> errors = ValidationService.validateNumberOfStickers("");
        assertEquals(1, errors.size(), "Should have one error for empty number");
        assertEquals("Number of stickers is required", errors.get(0));
    }

    @Test
    @DisplayName("validateNumberOfStickers - Whitespace only")
    void testValidateNumberOfStickers_WhitespaceOnly() throws ValidationException {
        List<String> errors = ValidationService.validateNumberOfStickers("   ");
        assertEquals(1, errors.size(), "Should have one error for whitespace only");
        assertEquals("Number of stickers is required", errors.get(0));
    }

    @Test
    @DisplayName("validateNumberOfStickers - Zero")
    void testValidateNumberOfStickers_Zero() throws ValidationException {
        List<String> errors = ValidationService.validateNumberOfStickers("0");
        assertEquals(1, errors.size(), "Should have one error for zero stickers");
        assertEquals("Number of stickers must be between 1 and 1000", errors.get(0));
    }

    @Test
    @DisplayName("validateNumberOfStickers - Negative")
    void testValidateNumberOfStickers_Negative() throws ValidationException {
        List<String> errors = ValidationService.validateNumberOfStickers("-5");
        assertEquals(1, errors.size(), "Should have one error for negative stickers");
        assertEquals("Number of stickers must be between 1 and 1000", errors.get(0));
    }

    @Test
    @DisplayName("validateNumberOfStickers - Too large")
    void testValidateNumberOfStickers_TooLarge() throws ValidationException {
        List<String> errors = ValidationService.validateNumberOfStickers("1001");
        assertEquals(1, errors.size(), "Should have one error for too many stickers");
        assertEquals("Number of stickers must be between 1 and 1000", errors.get(0));
    }

    @Test
    @DisplayName("validateNumberOfStickers - Minimum value")
    void testValidateNumberOfStickers_MinValue() throws ValidationException {
        List<String> errors = ValidationService.validateNumberOfStickers("1");
        assertTrue(errors.isEmpty(), "Should have no errors for minimum stickers");
    }

    @Test
    @DisplayName("validateNumberOfStickers - Maximum value")
    void testValidateNumberOfStickers_MaxValue() throws ValidationException {
        List<String> errors = ValidationService.validateNumberOfStickers("1000");
        assertTrue(errors.isEmpty(), "Should have no errors for maximum stickers");
    }

    @Test
    @DisplayName("validateNumberOfStickers - Invalid format")
    void testValidateNumberOfStickers_InvalidFormat() throws ValidationException {
        List<String> errors = ValidationService.validateNumberOfStickers("abc");
        assertEquals(1, errors.size(), "Should have one error for invalid format");
        assertEquals("Number of stickers must be a valid integer", errors.get(0));
    }

    @Test
    @DisplayName("validateNumberOfStickers - Decimal number")
    void testValidateNumberOfStickers_Decimal() throws ValidationException {
        List<String> errors = ValidationService.validateNumberOfStickers("5.5");
        assertEquals(1, errors.size(), "Should have one error for decimal number");
        assertEquals("Number of stickers must be a valid integer", errors.get(0));
    }

    @Test
    @DisplayName("validateGSTNumber - Valid GST")
    void testValidateGSTNumber_Valid() throws ValidationException {
        List<String> errors = ValidationService.validateGSTNumber("22AAAAA0000A1Z5");
        assertTrue(errors.isEmpty(), "Should have no errors for valid GST number");
    }

    @Test
    @DisplayName("validateGSTNumber - Null GST")
    void testValidateGSTNumber_Null() throws ValidationException {
        List<String> errors = ValidationService.validateGSTNumber(null);
        assertTrue(errors.isEmpty(), "Should have no errors for null GST number");
    }

    @Test
    @DisplayName("validateGSTNumber - Empty GST")
    void testValidateGSTNumber_Empty() throws ValidationException {
        List<String> errors = ValidationService.validateGSTNumber("");
        assertTrue(errors.isEmpty(), "Should have no errors for empty GST number");
    }

    @Test
    @DisplayName("validateGSTNumber - Whitespace only")
    void testValidateGSTNumber_WhitespaceOnly() throws ValidationException {
        List<String> errors = ValidationService.validateGSTNumber("   ");
        assertTrue(errors.isEmpty(), "Should have no errors for whitespace only GST number");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "22AAAAA0000A1Z",    // Too short
        "22AAAAA0000A1Z55",  // Too long
        "22AAAAA0000A1Z5A",  // Invalid character at end
        "22AAAAA0000A1Z5",   // Missing last character
        "22AAAAA0000A1Z55",  // Extra digit
        "2AAAAA0000A1Z5",    // Invalid first two digits
        "22AAAAA0000A1Z5",   // Invalid format
        "22AAAAA0000A1Z5A"   // Invalid last character
    })
    @DisplayName("validateGSTNumber - Invalid formats")
    void testValidateGSTNumber_InvalidFormats(String gst) throws ValidationException {
        List<String> errors = ValidationService.validateGSTNumber(gst);
        assertEquals(1, errors.size(), "Should have one error for invalid GST format: " + gst);
        assertEquals("GST number must be in valid format (e.g., 22AAAAA0000A1Z5)", errors.get(0));
    }

    @Test
    @DisplayName("validatePhone - Valid phone")
    void testValidatePhone_Valid() throws ValidationException {
        List<String> errors = ValidationService.validatePhone("1234567890");
        assertTrue(errors.isEmpty(), "Should have no errors for valid phone number");
    }

    @Test
    @DisplayName("validatePhone - Null phone")
    void testValidatePhone_Null() throws ValidationException {
        List<String> errors = ValidationService.validatePhone(null);
        assertTrue(errors.isEmpty(), "Should have no errors for null phone number");
    }

    @Test
    @DisplayName("validatePhone - Empty phone")
    void testValidatePhone_Empty() throws ValidationException {
        List<String> errors = ValidationService.validatePhone("");
        assertTrue(errors.isEmpty(), "Should have no errors for empty phone number");
    }

    @Test
    @DisplayName("validatePhone - Whitespace only")
    void testValidatePhone_WhitespaceOnly() throws ValidationException {
        List<String> errors = ValidationService.validatePhone("   ");
        assertTrue(errors.isEmpty(), "Should have no errors for whitespace only phone number");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "123456789",   // Too short
        "12345678901", // Too long
        "123456789a",  // Contains letter
        "123 456 7890", // Contains spaces
        "123-456-7890", // Contains dashes
        "(123)456-7890" // Contains parentheses and dash
    })
    @DisplayName("validatePhone - Invalid formats")
    void testValidatePhone_InvalidFormats(String phone) throws ValidationException {
        List<String> errors = ValidationService.validatePhone(phone);
        assertEquals(1, errors.size(), "Should have one error for invalid phone format: " + phone);
        assertEquals("Phone must be 10 digits", errors.get(0));
    }

    @Test
    @DisplayName("validateEmail - Valid email")
    void testValidateEmail_Valid() throws ValidationException {
        List<String> errors = ValidationService.validateEmail("test@example.com");
        assertTrue(errors.isEmpty(), "Should have no errors for valid email");
    }

    @Test
    @DisplayName("validateEmail - Null email")
    void testValidateEmail_Null() throws ValidationException {
        List<String> errors = ValidationService.validateEmail(null);
        assertTrue(errors.isEmpty(), "Should have no errors for null email");
    }

    @Test
    @DisplayName("validateEmail - Empty email")
    void testValidateEmail_Empty() throws ValidationException {
        List<String> errors = ValidationService.validateEmail("");
        assertTrue(errors.isEmpty(), "Should have no errors for empty email");
    }

    @Test
    @DisplayName("validateEmail - Whitespace only")
    void testValidateEmail_WhitespaceOnly() throws ValidationException {
        List<String> errors = ValidationService.validateEmail("   ");
        assertTrue(errors.isEmpty(), "Should have no errors for whitespace only email");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "invalid-email",           // No @ symbol
        "@example.com",            // No local part
        "test@",                   // No domain
        "test@.com",               // Invalid domain
        "test..test@example.com",  // Double dot in local part
        "test@example..com",       // Double dot in domain
        "test @example.com",       // Space in local part
        "test@example.com ",       // Trailing space
        "test@example",            // No TLD
        "test@example.c",          // TLD too short
        "test@123.456.789.000",    // Invalid domain format
        "test@example.com.",       // Trailing dot
        ".test@example.com"        // Leading dot in local part
    })
    @DisplayName("validateEmail - Invalid formats")
    void testValidateEmail_InvalidFormats(String email) throws ValidationException {
        List<String> errors = ValidationService.validateEmail(email);
        assertEquals(1, errors.size(), "Should have one error for invalid email format: " + email);
        assertEquals("Email must be in valid format", errors.get(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "test.email@example.com",
        "test+tag@example.com",
        "test@example.co.uk",
        "123@example.com",
        "test@example-domain.com"
    })
    @DisplayName("validateEmail - Valid formats")
    void testValidateEmail_ValidFormats(String email) throws ValidationException {
        List<String> errors = ValidationService.validateEmail(email);
        assertTrue(errors.isEmpty(), "Should have no errors for valid email: " + email);
    }
}