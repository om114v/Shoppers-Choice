package com.stickerapp.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for PrinterConfig class.
 * Tests cover constructors, getters/setters, toString method,
 * and various input scenarios including edge cases.
 */
@DisplayName("PrinterConfig Unit Tests")
public class PrinterConfigTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create instance with default constructor")
        void testDefaultConstructor() {
            PrinterConfig config = new PrinterConfig();

            assertNotNull(config);
            assertNull(config.getPrinterName());
            assertEquals(0, config.getPaperWidth());
            assertEquals(0, config.getPaperHeight());
            assertFalse(config.isDefault());
        }

        @Test
        @DisplayName("Should create instance with parameterized constructor")
        void testParameterizedConstructor() {
            PrinterConfig config = new PrinterConfig("Test Printer", 50, 30, true);

            assertNotNull(config);
            assertEquals("Test Printer", config.getPrinterName());
            assertEquals(50, config.getPaperWidth());
            assertEquals(30, config.getPaperHeight());
            assertTrue(config.isDefault());
        }

        @Test
        @DisplayName("Should handle null printer name")
        void testConstructorWithNullPrinterName() {
            PrinterConfig config = new PrinterConfig(null, 40, 25, false);

            assertNull(config.getPrinterName());
            assertEquals(40, config.getPaperWidth());
            assertEquals(25, config.getPaperHeight());
            assertFalse(config.isDefault());
        }

        @Test
        @DisplayName("Should handle zero dimensions")
        void testConstructorWithZeroDimensions() {
            PrinterConfig config = new PrinterConfig("Zero Size", 0, 0, true);

            assertEquals("Zero Size", config.getPrinterName());
            assertEquals(0, config.getPaperWidth());
            assertEquals(0, config.getPaperHeight());
            assertTrue(config.isDefault());
        }

        @Test
        @DisplayName("Should handle negative dimensions")
        void testConstructorWithNegativeDimensions() {
            PrinterConfig config = new PrinterConfig("Negative Size", -10, -20, false);

            assertEquals("Negative Size", config.getPrinterName());
            assertEquals(-10, config.getPaperWidth());
            assertEquals(-20, config.getPaperHeight());
            assertFalse(config.isDefault());
        }
    }

    @Nested
    @DisplayName("Getter and Setter Tests")
    class GetterSetterTests {

        @Test
        @DisplayName("Should set and get printer name")
        void testPrinterNameGetterSetter() {
            PrinterConfig config = new PrinterConfig();

            config.setPrinterName("New Printer");
            assertEquals("New Printer", config.getPrinterName());

            config.setPrinterName(null);
            assertNull(config.getPrinterName());

            config.setPrinterName("");
            assertEquals("", config.getPrinterName());
        }

        @Test
        @DisplayName("Should set and get paper width")
        void testPaperWidthGetterSetter() {
            PrinterConfig config = new PrinterConfig();

            config.setPaperWidth(100);
            assertEquals(100, config.getPaperWidth());

            config.setPaperWidth(0);
            assertEquals(0, config.getPaperWidth());

            config.setPaperWidth(-50);
            assertEquals(-50, config.getPaperWidth());
        }

        @Test
        @DisplayName("Should set and get paper height")
        void testPaperHeightGetterSetter() {
            PrinterConfig config = new PrinterConfig();

            config.setPaperHeight(75);
            assertEquals(75, config.getPaperHeight());

            config.setPaperHeight(0);
            assertEquals(0, config.getPaperHeight());

            config.setPaperHeight(-25);
            assertEquals(-25, config.getPaperHeight());
        }

        @Test
        @DisplayName("Should set and get default flag")
        void testDefaultGetterSetter() {
            PrinterConfig config = new PrinterConfig();

            config.setDefault(true);
            assertTrue(config.isDefault());

            config.setDefault(false);
            assertFalse(config.isDefault());
        }
    }

    @Nested
    @DisplayName("toString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should return correct string representation")
        void testToString() {
            PrinterConfig config = new PrinterConfig("Test Printer", 50, 30, true);
            String expected = "PrinterConfig{printerName='Test Printer', paperWidth=50, paperHeight=30, isDefault=true}";

            assertEquals(expected, config.toString());
        }

        @Test
        @DisplayName("Should handle null printer name in toString")
        void testToStringWithNullPrinterName() {
            PrinterConfig config = new PrinterConfig(null, 40, 25, false);
            String expected = "PrinterConfig{printerName='null', paperWidth=40, paperHeight=25, isDefault=false}";

            assertEquals(expected, config.toString());
        }

        @Test
        @DisplayName("Should handle zero values in toString")
        void testToStringWithZeroValues() {
            PrinterConfig config = new PrinterConfig("", 0, 0, false);
            String expected = "PrinterConfig{printerName='', paperWidth=0, paperHeight=0, isDefault=false}";

            assertEquals(expected, config.toString());
        }

        @Test
        @DisplayName("Should handle negative values in toString")
        void testToStringWithNegativeValues() {
            PrinterConfig config = new PrinterConfig("Negative", -10, -20, true);
            String expected = "PrinterConfig{printerName='Negative', paperWidth=-10, paperHeight=-20, isDefault=true}";

            assertEquals(expected, config.toString());
        }
    }

    @Nested
    @DisplayName("Parameterized Tests")
    class ParameterizedTests {

        @ParameterizedTest
        @CsvSource({
            "'Printer A', 50, 30, true",
            "'Printer B', 40, 25, false",
            "'', 100, 75, true",
            "'Special Printer', 0, 0, false",
            "'Negative Printer', -20, -30, true"
        })
        @DisplayName("Should handle various constructor parameters")
        void testConstructorParameters(String printerName, int paperWidth, int paperHeight, boolean isDefault) {
            PrinterConfig config = new PrinterConfig(printerName, paperWidth, paperHeight, isDefault);

            assertEquals(printerName, config.getPrinterName());
            assertEquals(paperWidth, config.getPaperWidth());
            assertEquals(paperHeight, config.getPaperHeight());
            assertEquals(isDefault, config.isDefault());
        }

        @ParameterizedTest
        @CsvSource({
            "50, 30, true, 'PrinterConfig{printerName=''null'', paperWidth=50, paperHeight=30, isDefault=true}'",
            "0, 0, false, 'PrinterConfig{printerName=''null'', paperWidth=0, paperHeight=0, isDefault=false}'",
            "-10, -20, true, 'PrinterConfig{printerName=''null'', paperWidth=-10, paperHeight=-20, isDefault=true}'"
        })
        @DisplayName("Should generate correct toString for null printer names")
        void testToStringWithNullNameParameterized(int paperWidth, int paperHeight, boolean isDefault, String expected) {
            PrinterConfig config = new PrinterConfig(null, paperWidth, paperHeight, isDefault);

            assertEquals(expected, config.toString());
        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle maximum integer values")
        void testMaximumIntegerValues() {
            PrinterConfig config = new PrinterConfig("Max Printer", Integer.MAX_VALUE, Integer.MAX_VALUE, true);

            assertEquals(Integer.MAX_VALUE, config.getPaperWidth());
            assertEquals(Integer.MAX_VALUE, config.getPaperHeight());
            assertTrue(config.isDefault());
        }

        @Test
        @DisplayName("Should handle minimum integer values")
        void testMinimumIntegerValues() {
            PrinterConfig config = new PrinterConfig("Min Printer", Integer.MIN_VALUE, Integer.MIN_VALUE, false);

            assertEquals(Integer.MIN_VALUE, config.getPaperWidth());
            assertEquals(Integer.MIN_VALUE, config.getPaperHeight());
            assertFalse(config.isDefault());
        }

        @Test
        @DisplayName("Should handle very long printer names")
        void testVeryLongPrinterName() {
            String longName = "A".repeat(1000);
            PrinterConfig config = new PrinterConfig(longName, 50, 30, true);

            assertEquals(longName, config.getPrinterName());
            assertTrue(config.toString().contains(longName));
        }

        @Test
        @DisplayName("Should handle special characters in printer name")
        void testSpecialCharactersInPrinterName() {
            String specialName = "Printer@#$%^&*()_+{}|:<>?[]\\;',./";
            PrinterConfig config = new PrinterConfig(specialName, 50, 30, true);

            assertEquals(specialName, config.getPrinterName());
            assertTrue(config.toString().contains(specialName));
        }
    }
}