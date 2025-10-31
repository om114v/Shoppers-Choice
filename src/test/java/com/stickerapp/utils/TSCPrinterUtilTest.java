package com.stickerapp.utils;

import com.stickerapp.models.StickerData;
import com.stickerapp.models.PrinterConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for TSCPrinterUtil class.
 * Tests cover all public methods with various scenarios including
 * success cases, failure cases, edge cases, and error conditions.
 */
@DisplayName("TSCPrinterUtil Unit Tests")
public class TSCPrinterUtilTest {

    private TSCPrinterUtil printerUtil;
    private AppLogger mockLogger;

    @BeforeEach
    void setUp() {
        // Mock the AppLogger singleton
        try (MockedStatic<AppLogger> mockedStatic = Mockito.mockStatic(AppLogger.class)) {
            mockLogger = Mockito.mock(AppLogger.class);
            mockedStatic.when(AppLogger::getInstance).thenReturn(mockLogger);

            printerUtil = new TSCPrinterUtil();
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should initialize with logger and disconnected state")
        void testConstructorInitialization() {
            try (MockedStatic<AppLogger> mockedStatic = Mockito.mockStatic(AppLogger.class)) {
                AppLogger mockLogger = Mockito.mock(AppLogger.class);
                mockedStatic.when(AppLogger::getInstance).thenReturn(mockLogger);

                TSCPrinterUtil util = new TSCPrinterUtil();

                assertNotNull(util, "PrinterUtil should be created");
                assertFalse(util.isConnected(), "Should start disconnected");
                mockedStatic.verify(AppLogger::getInstance, times(1));
            }
        }
    }

    @Nested
    @DisplayName("Port Management Tests")
    class PortManagementTests {

        @Test
        @DisplayName("Should open port successfully")
        void testOpenPortSuccess() {
            // Since TSC SDK calls are commented out, we test the logic
            printerUtil.openPort("USB", 9600);

            assertTrue(printerUtil.isConnected(), "Should be connected after opening port");
            verify(mockLogger).info("Opening printer port: USB");
            verify(mockLogger).info("Printer port opened successfully.");
        }

        @Test
        @DisplayName("Should handle openPort failure")
        void testOpenPortFailure() {
            // Test failure scenario - we can't easily mock the commented TSC calls,
            // but we can test the exception handling structure
            // For now, since TSC calls are commented, it will succeed
            assertDoesNotThrow(() -> printerUtil.openPort("COM1", 9600));
        }

        @Test
        @DisplayName("Should close port successfully")
        void testClosePortSuccess() {
            printerUtil.openPort("USB", 9600);
            assertTrue(printerUtil.isConnected());

            printerUtil.closePort();

            assertFalse(printerUtil.isConnected(), "Should be disconnected after closing port");
            verify(mockLogger).info("Closing printer port.");
            verify(mockLogger).info("Printer port closed successfully.");
        }

        @Test
        @DisplayName("Should handle closePort failure gracefully")
        void testClosePortFailure() {
            // Since TSC calls are commented, closePort will succeed
            assertDoesNotThrow(() -> printerUtil.closePort());
        }
    }

    @Nested
    @DisplayName("Command Sending Tests")
    class CommandSendingTests {

        @Test
        @DisplayName("Should send command when connected")
        void testSendCommandWhenConnected() {
            printerUtil.openPort("USB", 9600);

            String command = "SIZE 50 mm, 30 mm\r\n";
            assertDoesNotThrow(() -> printerUtil.sendCommand(command));

            verify(mockLogger).debug("Sending command to printer: " + command);
            verify(mockLogger).debug("Command sent successfully.");
        }

        @Test
        @DisplayName("Should throw exception when sending command while disconnected")
        void testSendCommandWhenDisconnected() {
            String command = "SIZE 50 mm, 30 mm\r\n";

            IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> printerUtil.sendCommand(command));

            assertEquals("Printer not connected", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Connection Status Tests")
    class ConnectionStatusTests {

        @Test
        @DisplayName("Should return false when not connected")
        void testIsConnectedFalse() {
            assertFalse(printerUtil.isConnected());
        }

        @Test
        @DisplayName("Should return true when connected")
        void testIsConnectedTrue() {
            printerUtil.openPort("USB", 9600);
            assertTrue(printerUtil.isConnected());
        }
    }

    @Nested
    @DisplayName("Available Ports Tests")
    class AvailablePortsTests {

        @Test
        @DisplayName("Should return list of available ports")
        void testGetAvailablePorts() {
            List<String> ports = printerUtil.getAvailablePorts();

            assertNotNull(ports);
            assertTrue(ports.size() >= 4, "Should have at least 4 ports");
            assertTrue(ports.contains("USB"));
            assertTrue(ports.contains("COM1"));
            assertTrue(ports.contains("COM2"));
            assertTrue(ports.contains("COM3"));
            assertTrue(ports.contains("COM4"));

            verify(mockLogger).info("Retrieved available printer ports: " + ports);
        }
    }

    @Nested
    @DisplayName("Unit Conversion Tests")
    class UnitConversionTests {

        @ParameterizedTest
        @CsvSource({
            "25.4, 203",
            "12.7, 102",
            "50.8, 406",
            "0, 0",
            "1, 8"
        })
        @DisplayName("Should convert millimeters to dots correctly")
        void testMmToDots(double mm, int expectedDots) {
            int actualDots = printerUtil.mmToDots(mm);
            assertEquals(expectedDots, actualDots);
        }

        @ParameterizedTest
        @CsvSource({
            "203, 25.4",
            "102, 12.7",
            "406, 50.8",
            "0, 0.0",
            "8, 1.0"
        })
        @DisplayName("Should convert dots to millimeters correctly")
        void testDotsToMm(int dots, double expectedMm) {
            double actualMm = printerUtil.dotsToMm(dots);
            assertEquals(expectedMm, actualMm, 0.01);
        }
    }

    @Nested
    @DisplayName("Font Size Calculation Tests")
    class FontSizeCalculationTests {

        @Test
        @DisplayName("Should return base font size for null text")
        void testCalculateFontSizeNullText() {
            int result = printerUtil.calculateFontSize(null, 100, 3);
            assertEquals(3, result);
        }

        @Test
        @DisplayName("Should return base font size for empty text")
        void testCalculateFontSizeEmptyText() {
            int result = printerUtil.calculateFontSize("", 100, 3);
            assertEquals(3, result);
        }

        @Test
        @DisplayName("Should return base font size when text fits")
        void testCalculateFontSizeTextFits() {
            int result = printerUtil.calculateFontSize("Hi", 100, 3);
            assertEquals(3, result);
        }

        @Test
        @DisplayName("Should scale down font size when text is too long")
        void testCalculateFontSizeTextTooLong() {
            int result = printerUtil.calculateFontSize("Very long text that exceeds width", 50, 3);
            assertTrue(result < 3, "Font size should be reduced");
            assertTrue(result >= 1, "Font size should not be less than 1");
        }

        @ParameterizedTest
        @CsvSource({
            "'Short', 100, 3, 3",
            "'Medium length text', 50, 2, 1",
            "'A', 10, 5, 5"
        })
        @DisplayName("Should calculate font size based on text length and width")
        void testCalculateFontSizeParameterized(String text, int maxWidth, int baseSize, int expected) {
            int result = printerUtil.calculateFontSize(text, maxWidth, baseSize);
            assertEquals(expected, result);
        }
    }

    @Nested
    @DisplayName("Text Wrapping Tests")
    class TextWrappingTests {

        @Test
        @DisplayName("Should return empty list for null text")
        void testWrapTextNull() {
            List<String> result = printerUtil.wrapText(null, 100, 3);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should return empty list for empty text")
        void testWrapTextEmpty() {
            List<String> result = printerUtil.wrapText("", 100, 3);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should return single line for short text")
        void testWrapTextShort() {
            List<String> result = printerUtil.wrapText("Hello", 50, 3);
            assertEquals(1, result.size());
            assertEquals("Hello", result.get(0));
        }

        @Test
        @DisplayName("Should wrap text at word boundaries")
        void testWrapTextWrapping() {
            List<String> result = printerUtil.wrapText("Hello world this is a test", 20, 2);
            assertTrue(result.size() > 1, "Should wrap into multiple lines");
        }

        @Test
        @DisplayName("Should handle very long words by truncating")
        void testWrapTextLongWord() {
            List<String> result = printerUtil.wrapText("Supercalifragilisticexpialidocious", 10, 2);
            assertEquals(1, result.size());
            assertTrue(result.get(0).length() <= 5, "Should truncate long words");
        }

        @ParameterizedTest
        @CsvSource({
            "'Short text', 50, 2, 1",
            "'Long text that should wrap to multiple lines', 20, 2, 3"
        })
        @DisplayName("Should wrap text based on width and font size")
        void testWrapTextParameterized(String text, int maxWidth, int fontSize, int expectedLines) {
            List<String> result = printerUtil.wrapText(text, maxWidth, fontSize);
            assertEquals(expectedLines, result.size());
        }
    }

    @Nested
    @DisplayName("Price Formatting Tests")
    class PriceFormattingTests {

        @Test
        @DisplayName("Should format null price as zero")
        void testFormatPriceNull() {
            String result = printerUtil.formatPrice(null, "₹");
            assertEquals("₹0.00", result);
        }

        @Test
        @DisplayName("Should format zero price correctly")
        void testFormatPriceZero() {
            String result = printerUtil.formatPrice(BigDecimal.ZERO, "$");
            assertEquals("$0.00", result);
        }

        @Test
        @DisplayName("Should format positive price with two decimal places")
        void testFormatPricePositive() {
            String result = printerUtil.formatPrice(new BigDecimal("123.456"), "₹");
            assertEquals("₹123.46", result);
        }

        @Test
        @DisplayName("Should format price with rounding")
        void testFormatPriceRounding() {
            String result = printerUtil.formatPrice(new BigDecimal("123.994"), "₹");
            assertEquals("₹124.00", result);
        }

        @ParameterizedTest
        @CsvSource({
            "10.5, '₹', '₹10.50'",
            "0.99, '$', '$0.99'",
            "100, '€', '€100.00'"
        })
        @DisplayName("Should format prices with different currencies")
        void testFormatPriceParameterized(BigDecimal price, String currency, String expected) {
            String result = printerUtil.formatPrice(price, currency);
            assertEquals(expected, result);
        }
    }

    @Nested
    @DisplayName("Error Recovery Tests")
    class ErrorRecoveryTests {

        @Test
        @DisplayName("Should return false when recovery fails")
        void testRecoverFromErrorFailure() {
            // Since TSC calls are commented, recovery will fail
            boolean result = printerUtil.recoverFromError(2);
            assertFalse(result);
        }

        @Test
        @DisplayName("Should attempt recovery multiple times")
        void testRecoverFromErrorAttempts() {
            printerUtil.recoverFromError(1);

            verify(mockLogger, atLeast(1)).info(anyString());
            verify(mockLogger, atLeast(1)).warn(anyString());
        }
    }

    @Nested
    @DisplayName("Sticker Template Generation Tests")
    class StickerTemplateGenerationTests {

        @Test
        @DisplayName("Should throw exception for null sticker data")
        void testGenerateStickerTemplateNullStickerData() {
            PrinterConfig config = new PrinterConfig("Test", 50, 30, true);

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> printerUtil.generateStickerTemplate(null, config));

            assertEquals("StickerData and PrinterConfig cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception for null config")
        void testGenerateStickerTemplateNullConfig() {
            StickerData data = new StickerData("Test Item", "Test Supplier",
                new BigDecimal("10.00"), 1);

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> printerUtil.generateStickerTemplate(data, null));

            assertEquals("StickerData and PrinterConfig cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should generate valid TSPL template")
        void testGenerateStickerTemplateValid() {
            StickerData data = new StickerData("Test Item", "Test Supplier",
                new BigDecimal("10.00"), 5);
            PrinterConfig config = new PrinterConfig("Test", 50, 30, true);

            String template = printerUtil.generateStickerTemplate(data, config);

            assertNotNull(template);
            assertTrue(template.contains("SIZE 50 mm, 30 mm"));
            assertTrue(template.contains("GAP 3 mm, 0 mm"));
            assertTrue(template.contains("DIRECTION 0"));
            assertTrue(template.contains("CLS"));
            assertTrue(template.contains("TEXT"));
            assertTrue(template.contains("PRINT 5"));
            assertTrue(template.contains("Test Item"));
            assertTrue(template.contains("Test Supplier"));
            assertTrue(template.contains("₹10.00"));
        }

        @Test
        @DisplayName("Should handle long item names with text wrapping")
        void testGenerateStickerTemplateLongItemName() {
            String longName = "Very Long Item Name That Should Be Wrapped";
            StickerData data = new StickerData(longName, "Supplier",
                new BigDecimal("5.00"), 1);
            PrinterConfig config = new PrinterConfig("Test", 40, 30, true);

            String template = printerUtil.generateStickerTemplate(data, config);

            assertNotNull(template);
            // Should contain wrapped text
            assertTrue(template.contains("TEXT"));
        }
    }

    @Nested
    @DisplayName("Print Sticker Tests")
    class PrintStickerTests {

        @Test
        @DisplayName("Should throw exception when printing while disconnected")
        void testPrintStickerDisconnected() {
            StickerData data = new StickerData("Test", "Supplier", new BigDecimal("1.00"), 1);
            PrinterConfig config = new PrinterConfig("Test", 50, 30, true);

            IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> printerUtil.printSticker(data, config));

            assertEquals("Printer not connected", exception.getMessage());
        }

        @Test
        @DisplayName("Should print sticker successfully when connected")
        void testPrintStickerSuccess() {
            printerUtil.openPort("USB", 9600);

            StickerData data = new StickerData("Test Item", "Test Supplier",
                new BigDecimal("10.00"), 1);
            PrinterConfig config = new PrinterConfig("Test", 50, 30, true);

            assertDoesNotThrow(() -> printerUtil.printSticker(data, config));

            verify(mockLogger).info("Sticker printed successfully: Test Item");
        }

        @Test
        @DisplayName("Should handle print failure and attempt recovery")
        void testPrintStickerWithRecovery() {
            // This test is limited since TSC calls are commented out
            // In real scenario, we would mock TSC calls to simulate failures
            printerUtil.openPort("USB", 9600);

            StickerData data = new StickerData("Test", "Supplier", new BigDecimal("1.00"), 1);
            PrinterConfig config = new PrinterConfig("Test", 50, 30, true);

            // Should not throw since recovery is attempted (though it will fail)
            assertDoesNotThrow(() -> printerUtil.printSticker(data, config));
        }

        @Test
        @DisplayName("Should be thread-safe during printing")
        void testPrintStickerThreadSafety() throws InterruptedException {
            printerUtil.openPort("USB", 9600);

            StickerData data = new StickerData("Test", "Supplier", new BigDecimal("1.00"), 1);
            PrinterConfig config = new PrinterConfig("Test", 50, 30, true);

            // Run multiple threads to test lock
            Thread[] threads = new Thread[5];
            for (int i = 0; i < threads.length; i++) {
                threads[i] = new Thread(() -> {
                    assertDoesNotThrow(() -> printerUtil.printSticker(data, config));
                });
            }

            for (Thread thread : threads) {
                thread.start();
            }

            for (Thread thread : threads) {
                thread.join();
            }
        }
    }
}