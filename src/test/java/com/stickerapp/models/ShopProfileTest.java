package com.stickerapp.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for ShopProfile class.
 * Tests cover constructors, getters/setters, toString method,
 * and various input scenarios including edge cases.
 */
@DisplayName("ShopProfile Unit Tests")
public class ShopProfileTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create instance with default constructor")
        void testDefaultConstructor() {
            ShopProfile profile = new ShopProfile();

            assertNotNull(profile);
            assertEquals(0, profile.getId());
            assertNull(profile.getShopName());
            assertNull(profile.getGstNumber());
            assertNull(profile.getAddress());
            assertNull(profile.getPhoneNumber());
            assertNull(profile.getEmail());
            assertNull(profile.getLogoPath());
            assertNull(profile.getCreatedAt());
            assertNull(profile.getUpdatedAt());
        }

        @Test
        @DisplayName("Should create instance with parameterized constructor")
        void testParameterizedConstructor() {
            ShopProfile profile = new ShopProfile("Test Shop", "GST123456", "123 Test St", "1234567890", "test@example.com", "/path/to/logo.png");

            assertNotNull(profile);
            assertEquals("Test Shop", profile.getShopName());
            assertEquals("GST123456", profile.getGstNumber());
            assertEquals("123 Test St", profile.getAddress());
            assertEquals("1234567890", profile.getPhoneNumber());
            assertEquals("test@example.com", profile.getEmail());
            assertEquals("/path/to/logo.png", profile.getLogoPath());
            // ID and timestamps should be default
            assertEquals(0, profile.getId());
            assertNull(profile.getCreatedAt());
            assertNull(profile.getUpdatedAt());
        }

        @Test
        @DisplayName("Should handle null values in parameterized constructor")
        void testConstructorWithNullValues() {
            ShopProfile profile = new ShopProfile(null, null, null, null, null, null);

            assertNull(profile.getShopName());
            assertNull(profile.getGstNumber());
            assertNull(profile.getAddress());
            assertNull(profile.getPhoneNumber());
            assertNull(profile.getEmail());
            assertNull(profile.getLogoPath());
        }

        @Test
        @DisplayName("Should handle empty strings in parameterized constructor")
        void testConstructorWithEmptyStrings() {
            ShopProfile profile = new ShopProfile("", "", "", "", "", "");

            assertEquals("", profile.getShopName());
            assertEquals("", profile.getGstNumber());
            assertEquals("", profile.getAddress());
            assertEquals("", profile.getPhoneNumber());
            assertEquals("", profile.getEmail());
            assertEquals("", profile.getLogoPath());
        }
    }

    @Nested
    @DisplayName("Getter and Setter Tests")
    class GetterSetterTests {

        @Test
        @DisplayName("Should set and get id")
        void testIdGetterSetter() {
            ShopProfile profile = new ShopProfile();

            profile.setId(123);
            assertEquals(123, profile.getId());

            profile.setId(0);
            assertEquals(0, profile.getId());

            profile.setId(-1);
            assertEquals(-1, profile.getId());
        }

        @Test
        @DisplayName("Should set and get shop name")
        void testShopNameGetterSetter() {
            ShopProfile profile = new ShopProfile();

            profile.setShopName("New Shop Name");
            assertEquals("New Shop Name", profile.getShopName());

            profile.setShopName(null);
            assertNull(profile.getShopName());

            profile.setShopName("");
            assertEquals("", profile.getShopName());
        }

        @Test
        @DisplayName("Should set and get GST number")
        void testGstNumberGetterSetter() {
            ShopProfile profile = new ShopProfile();

            profile.setGstNumber("GST987654");
            assertEquals("GST987654", profile.getGstNumber());

            profile.setGstNumber(null);
            assertNull(profile.getGstNumber());

            profile.setGstNumber("");
            assertEquals("", profile.getGstNumber());
        }

        @Test
        @DisplayName("Should set and get address")
        void testAddressGetterSetter() {
            ShopProfile profile = new ShopProfile();

            profile.setAddress("456 New Street");
            assertEquals("456 New Street", profile.getAddress());

            profile.setAddress(null);
            assertNull(profile.getAddress());

            profile.setAddress("");
            assertEquals("", profile.getAddress());
        }

        @Test
        @DisplayName("Should set and get phone number")
        void testPhoneNumberGetterSetter() {
            ShopProfile profile = new ShopProfile();

            profile.setPhoneNumber("9876543210");
            assertEquals("9876543210", profile.getPhoneNumber());

            profile.setPhoneNumber(null);
            assertNull(profile.getPhoneNumber());

            profile.setPhoneNumber("");
            assertEquals("", profile.getPhoneNumber());
        }

        @Test
        @DisplayName("Should set and get email")
        void testEmailGetterSetter() {
            ShopProfile profile = new ShopProfile();

            profile.setEmail("new@example.com");
            assertEquals("new@example.com", profile.getEmail());

            profile.setEmail(null);
            assertNull(profile.getEmail());

            profile.setEmail("");
            assertEquals("", profile.getEmail());
        }

        @Test
        @DisplayName("Should set and get logo path")
        void testLogoPathGetterSetter() {
            ShopProfile profile = new ShopProfile();

            profile.setLogoPath("/new/path/to/logo.jpg");
            assertEquals("/new/path/to/logo.jpg", profile.getLogoPath());

            profile.setLogoPath(null);
            assertNull(profile.getLogoPath());

            profile.setLogoPath("");
            assertEquals("", profile.getLogoPath());
        }

        @Test
        @DisplayName("Should set and get created at timestamp")
        void testCreatedAtGetterSetter() {
            ShopProfile profile = new ShopProfile();
            LocalDateTime now = LocalDateTime.now();

            profile.setCreatedAt(now);
            assertEquals(now, profile.getCreatedAt());

            profile.setCreatedAt(null);
            assertNull(profile.getCreatedAt());
        }

        @Test
        @DisplayName("Should set and get updated at timestamp")
        void testUpdatedAtGetterSetter() {
            ShopProfile profile = new ShopProfile();
            LocalDateTime now = LocalDateTime.now();

            profile.setUpdatedAt(now);
            assertEquals(now, profile.getUpdatedAt());

            profile.setUpdatedAt(null);
            assertNull(profile.getUpdatedAt());
        }
    }

    @Nested
    @DisplayName("toString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should return correct string representation")
        void testToString() {
            ShopProfile profile = new ShopProfile("Test Shop", "GST123", "123 St", "1234567890", "test@example.com", "/logo.png");
            profile.setId(1);
            LocalDateTime now = LocalDateTime.of(2023, 10, 30, 12, 0);
            profile.setCreatedAt(now);
            profile.setUpdatedAt(now);

            String toString = profile.toString();
            assertTrue(toString.contains("id=1"));
            assertTrue(toString.contains("shopName='Test Shop'"));
            assertTrue(toString.contains("gstNumber='GST123'"));
            assertTrue(toString.contains("address='123 St'"));
            assertTrue(toString.contains("phoneNumber='1234567890'"));
            assertTrue(toString.contains("email='test@example.com'"));
            assertTrue(toString.contains("logoPath='/logo.png'"));
            assertTrue(toString.contains("createdAt=" + now.toString()));
            assertTrue(toString.contains("updatedAt=" + now.toString()));
        }

        @Test
        @DisplayName("Should handle null values in toString")
        void testToStringWithNullValues() {
            ShopProfile profile = new ShopProfile();

            String toString = profile.toString();
            assertTrue(toString.contains("id=0"));
            assertTrue(toString.contains("shopName='null'"));
            assertTrue(toString.contains("gstNumber='null'"));
            assertTrue(toString.contains("address='null'"));
            assertTrue(toString.contains("phoneNumber='null'"));
            assertTrue(toString.contains("email='null'"));
            assertTrue(toString.contains("logoPath='null'"));
            assertTrue(toString.contains("createdAt=null"));
            assertTrue(toString.contains("updatedAt=null"));
        }

        @Test
        @DisplayName("Should handle empty strings in toString")
        void testToStringWithEmptyStrings() {
            ShopProfile profile = new ShopProfile("", "", "", "", "", "");

            String toString = profile.toString();
            assertTrue(toString.contains("shopName=''"));
            assertTrue(toString.contains("gstNumber=''"));
            assertTrue(toString.contains("address=''"));
            assertTrue(toString.contains("phoneNumber=''"));
            assertTrue(toString.contains("email=''"));
            assertTrue(toString.contains("logoPath=''"));
        }
    }

    @Nested
    @DisplayName("Parameterized Tests")
    class ParameterizedTests {

        @ParameterizedTest
        @CsvSource({
            "'Shop A', 'GST001', 'Addr1', '1111111111', 'a@example.com', '/logo1.png'",
            "'Shop B', 'GST002', 'Addr2', '2222222222', 'b@example.com', '/logo2.png'",
            "'', '', '', '', '', ''",
            "'Special@#$%', 'GST!@#', 'Addr!@#', '3333333333', 'c@example.com', '/logo3.png'"
        })
        @DisplayName("Should handle various constructor parameters")
        void testConstructorParameters(String shopName, String gstNumber, String address, String phoneNumber, String email, String logoPath) {
            ShopProfile profile = new ShopProfile(shopName, gstNumber, address, phoneNumber, email, logoPath);

            assertEquals(shopName, profile.getShopName());
            assertEquals(gstNumber, profile.getGstNumber());
            assertEquals(address, profile.getAddress());
            assertEquals(phoneNumber, profile.getPhoneNumber());
            assertEquals(email, profile.getEmail());
            assertEquals(logoPath, profile.getLogoPath());
        }

        @ParameterizedTest
        @CsvSource({
            "1, 'Shop1', 'GST1', 'Addr1', '1111', 'a@test.com', '/logo1.png'",
            "0, '', '', '', '', '', ''",
            "-1, 'Shop-1', 'GST-1', 'Addr-1', '9999', 'z@test.com', '/logo-1.png'"
        })
        @DisplayName("Should handle various setter values")
        void testSetterParameters(int id, String shopName, String gstNumber, String address, String phoneNumber, String email, String logoPath) {
            ShopProfile profile = new ShopProfile();

            profile.setId(id);
            profile.setShopName(shopName);
            profile.setGstNumber(gstNumber);
            profile.setAddress(address);
            profile.setPhoneNumber(phoneNumber);
            profile.setEmail(email);
            profile.setLogoPath(logoPath);

            assertEquals(id, profile.getId());
            assertEquals(shopName, profile.getShopName());
            assertEquals(gstNumber, profile.getGstNumber());
            assertEquals(address, profile.getAddress());
            assertEquals(phoneNumber, profile.getPhoneNumber());
            assertEquals(email, profile.getEmail());
            assertEquals(logoPath, profile.getLogoPath());
        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle maximum integer id")
        void testMaximumIntegerId() {
            ShopProfile profile = new ShopProfile();
            profile.setId(Integer.MAX_VALUE);

            assertEquals(Integer.MAX_VALUE, profile.getId());
        }

        @Test
        @DisplayName("Should handle minimum integer id")
        void testMinimumIntegerId() {
            ShopProfile profile = new ShopProfile();
            profile.setId(Integer.MIN_VALUE);

            assertEquals(Integer.MIN_VALUE, profile.getId());
        }

        @Test
        @DisplayName("Should handle very long strings")
        void testVeryLongStrings() {
            String longString = "A".repeat(10000);
            ShopProfile profile = new ShopProfile(longString, longString, longString, longString, longString, longString);

            assertEquals(longString, profile.getShopName());
            assertEquals(longString, profile.getGstNumber());
            assertEquals(longString, profile.getAddress());
            assertEquals(longString, profile.getPhoneNumber());
            assertEquals(longString, profile.getEmail());
            assertEquals(longString, profile.getLogoPath());
        }

        @Test
        @DisplayName("Should handle special characters in all string fields")
        void testSpecialCharactersInStrings() {
            String special = "!@#$%^&*()_+-=[]{}|;:,.<>?";
            ShopProfile profile = new ShopProfile(special, special, special, special, special, special);

            assertEquals(special, profile.getShopName());
            assertEquals(special, profile.getGstNumber());
            assertEquals(special, profile.getAddress());
            assertEquals(special, profile.getPhoneNumber());
            assertEquals(special, profile.getEmail());
            assertEquals(special, profile.getLogoPath());
        }

        @Test
        @DisplayName("Should handle LocalDateTime timestamps")
        void testLocalDateTimeTimestamps() {
            ShopProfile profile = new ShopProfile();
            LocalDateTime past = LocalDateTime.of(2000, 1, 1, 0, 0);
            LocalDateTime future = LocalDateTime.of(2100, 12, 31, 23, 59);

            profile.setCreatedAt(past);
            profile.setUpdatedAt(future);

            assertEquals(past, profile.getCreatedAt());
            assertEquals(future, profile.getUpdatedAt());
        }
    }
}