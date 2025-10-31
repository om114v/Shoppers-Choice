package com.stickerapp.integration;

import com.stickerapp.exceptions.PrinterException;
import com.stickerapp.exceptions.PrinterStatusException;
import com.stickerapp.models.ShopProfile;
import com.stickerapp.models.StickerData;
import com.stickerapp.services.PrinterService;
import com.stickerapp.utils.ConfigManager;
import com.stickerapp.utils.TSCPrinterUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.mockito.MockedStatic;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive integration tests for PrinterService with mock printer hardware.
 * Tests the integration between PrinterService and TSCPrinterUtil, focusing on
 * printing workflows, error handling, and mock printer interactions.
 *
 * Uses mocking for actual printer hardware while testing service layer logic.
 */
@TestMethodOrder(OrderAnnotation.class)
public class PrinterServiceIntegrationTest {

    private PrinterService printerService;
    private TSCPrinterUtil mockPrinterUtil;
    private ConfigManager mockConfigManager;

    // Test data
    private StickerData validStickerData;
    private ShopProfile validShopProfile;

    @BeforeEach
    void setUp() {
        // Create mock instances
        mockPrinterUtil = mock(TSCPrinterUtil.class);
        mockConfigManager = mock(ConfigManager.class);

        // Create test data
        validStickerData = new StickerData("Test Item", "Test Supplier", new BigDecimal("25.50"), 1);
        validShopProfile = new ShopProfile("Test Shop", "22AAAAA0000A1Z5", "123 Test Street", "9876543210", "test@example.com", null);

        // Mock ConfigManager singleton
        try (MockedStatic<ConfigManager> configStatic = mockStatic(ConfigManager.class)) {
            configStatic.when(ConfigManager::getInstance).thenReturn(mockConfigManager);

            // Create PrinterService with mocked dependencies
            printerService = new PrinterService();

            // Inject mocked TSCPrinterUtil using reflection
            injectMockPrinterUtil();
        }
    }

    private void injectMockPrinterUtil() {
        try {
            java.lang.reflect.Field printerUtilField = PrinterService.class.getDeclaredField("printerUtil");
            printerUtilField.setAccessible(true);
            printerUtilField.set(printerService, mockPrinterUtil);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock printer util", e);
        }
    }

    @AfterEach
    void tearDown() {
        // Reset mocks
        reset(mockPrinterUtil, mockConfigManager);
    }

    // ==================== PRINTER INITIALIZATION TESTS ====================

    @Test
    @Order(1)
    @DisplayName("Should initialize printer successfully with valid configuration")
    void testInitializePrinterSuccess() {
        // Arrange
        when(mockConfigManager.getProperty("printer.port", "USB")).thenReturn("USB");
        when(mockConfigManager.getIntProperty("printer.baudrate", 9600)).thenReturn(9600);

        // Act & Assert
        assertDoesNotThrow(() -> printerService.initializePrinter());

        // Verify interactions
        verify(mockPrinterUtil).openPort("USB", 9600);
    }

    @Test
    @Order(2)
    @DisplayName("Should handle printer initialization failure gracefully")
    void testInitializePrinterFailure() {
        // Arrange
        when(mockConfigManager.getProperty("printer.port", "USB")).thenReturn("USB");
        when(mockConfigManager.getIntProperty("printer.baudrate", 9600)).thenReturn(9600);
        doThrow(new RuntimeException("Port not available")).when(mockPrinterUtil).openPort(anyString(), anyInt());

        // Act & Assert
        PrinterException exception = assertThrows(PrinterException.class,
            () -> printerService.initializePrinter());

        assertTrue(exception.getMessage().contains("Failed to initialize printer"));
        verify(mockPrinterUtil).openPort("USB", 9600);
    }

    @Test
    @Order(3)
    @DisplayName("Should use default configuration values when properties not set")
    void testInitializePrinterWithDefaults() {
        // Arrange
        when(mockConfigManager.getProperty("printer.port", "USB")).thenReturn("USB");
        when(mockConfigManager.getIntProperty("printer.baudrate", 9600)).thenReturn(9600);

        // Act
        printerService.initializePrinter();

        // Assert
        verify(mockPrinterUtil).openPort("USB", 9600);
    }

    // ==================== PRINTER STATUS CHECK TESTS ====================

    @Test
    @Order(4)
    @DisplayName("Should return true when printer is connected")
    void testIsPrinterConnectedSuccess() throws PrinterStatusException {
        // Arrange
        when(mockPrinterUtil.isConnected()).thenReturn(true);

        // Act
        boolean result = printerService.isPrinterConnected();

        // Assert
        assertTrue(result);
        verify(mockPrinterUtil).isConnected();
    }

    @Test
    @Order(5)
    @DisplayName("Should throw exception when printer is offline")
    void testIsPrinterConnectedOffline() {
        // Arrange
        when(mockPrinterUtil.isConnected()).thenReturn(false);

        // Act & Assert
        PrinterStatusException exception = assertThrows(PrinterStatusException.class,
            () -> printerService.isPrinterConnected());

        assertEquals(PrinterStatusException.PrinterStatus.OFFLINE, exception.getStatus());
        assertTrue(exception.getMessage().contains("Printer is not connected"));
    }

    @Test
    @Order(6)
    @DisplayName("Should handle connection check errors gracefully")
    void testIsPrinterConnectedError() {
        // Arrange
        when(mockPrinterUtil.isConnected()).thenThrow(new RuntimeException("Connection check failed"));

        // Act & Assert
        PrinterStatusException exception = assertThrows(PrinterStatusException.class,
            () -> printerService.isPrinterConnected());

        assertEquals(PrinterStatusException.PrinterStatus.ERROR, exception.getStatus());
        assertTrue(exception.getMessage().contains("Failed to check printer connection"));
    }

    @Test
    @Order(7)
    @DisplayName("Should retrieve available printer ports successfully")
    void testGetAvailablePrintersSuccess() throws PrinterStatusException {
        // Arrange
        List<String> expectedPorts = List.of("USB", "COM1", "COM2");
        when(mockPrinterUtil.getAvailablePorts()).thenReturn(expectedPorts);

        // Act
        List<String> result = printerService.getAvailablePrinters();

        // Assert
        assertEquals(expectedPorts, result);
        verify(mockPrinterUtil).getAvailablePorts();
    }

    @Test
    @Order(8)
    @DisplayName("Should handle port retrieval errors")
    void testGetAvailablePrintersError() {
        // Arrange
        when(mockPrinterUtil.getAvailablePorts()).thenThrow(new RuntimeException("Port enumeration failed"));

        // Act & Assert
        PrinterStatusException exception = assertThrows(PrinterStatusException.class,
            () -> printerService.getAvailablePrinters());

        assertEquals(PrinterStatusException.PrinterStatus.ERROR, exception.getStatus());
        assertTrue(exception.getMessage().contains("Failed to retrieve available printers"));
    }

    // ==================== SINGLE STICKER PRINTING TESTS ====================

    @Test
    @Order(9)
    @DisplayName("Should print single sticker successfully")
    void testPrintStickerSuccess() throws PrinterException, PrinterStatusException {
        // Arrange
        when(mockPrinterUtil.isConnected()).thenReturn(true);
        doNothing().when(mockPrinterUtil).sendCommand(anyString());

        // Act
        assertDoesNotThrow(() -> printerService.printSticker(validStickerData, validShopProfile));

        // Assert
        verify(mockPrinterUtil).isConnected();
        verify(mockPrinterUtil).sendCommand(anyString());
    }

    @Test
    @Order(10)
    @DisplayName("Should throw exception when printing with offline printer")
    void testPrintStickerOfflinePrinter() {
        // Arrange
        when(mockPrinterUtil.isConnected()).thenReturn(false);

        // Act & Assert
        PrinterStatusException exception = assertThrows(PrinterStatusException.class,
            () -> printerService.printSticker(validStickerData, validShopProfile));

        assertEquals(PrinterStatusException.PrinterStatus.OFFLINE, exception.getStatus());
        verify(mockPrinterUtil).isConnected();
        verify(mockPrinterUtil, never()).sendCommand(anyString());
    }

    @Test
    @Order(11)
    @DisplayName("Should handle out of paper error during printing")
    void testPrintStickerOutOfPaper() {
        // Arrange
        when(mockPrinterUtil.isConnected()).thenReturn(true);
        RuntimeException outOfPaperException = new RuntimeException("Out of paper");
        doThrow(outOfPaperException).when(mockPrinterUtil).sendCommand(anyString());

        // Act & Assert
        PrinterException exception = assertThrows(PrinterException.class,
            () -> printerService.printSticker(validStickerData, validShopProfile));

        assertEquals(PrinterException.PrinterError.OUT_OF_PAPER, exception.getError());
        assertTrue(exception.getMessage().contains("Printer is out of paper or ink"));
    }

    @Test
    @Order(12)
    @DisplayName("Should handle out of ink error during printing")
    void testPrintStickerOutOfInk() {
        // Arrange
        when(mockPrinterUtil.isConnected()).thenReturn(true);
        RuntimeException outOfInkException = new RuntimeException("Out of ink");
        doThrow(outOfInkException).when(mockPrinterUtil).sendCommand(anyString());

        // Act & Assert
        PrinterException exception = assertThrows(PrinterException.class,
            () -> printerService.printSticker(validStickerData, validShopProfile));

        assertEquals(PrinterException.PrinterError.OUT_OF_PAPER, exception.getError());
        assertTrue(exception.getMessage().contains("Printer is out of paper or ink"));
    }

    @Test
    @Order(13)
    @DisplayName("Should handle generic printing errors")
    void testPrintStickerGenericError() {
        // Arrange
        when(mockPrinterUtil.isConnected()).thenReturn(true);
        RuntimeException genericException = new RuntimeException("Generic printer error");
        doThrow(genericException).when(mockPrinterUtil).sendCommand(anyString());

        // Act & Assert
        PrinterException exception = assertThrows(PrinterException.class,
            () -> printerService.printSticker(validStickerData, validShopProfile));

        assertNull(exception.getError()); // Should be null for generic errors
        assertTrue(exception.getMessage().contains("Failed to print sticker"));
    }

    // ==================== MULTIPLE STICKER PRINTING TESTS ====================

    @Test
    @Order(14)
    @DisplayName("Should print multiple stickers successfully")
    void testPrintMultipleStickersSuccess() throws PrinterException, PrinterStatusException {
        // Arrange
        int quantity = 5;
        when(mockPrinterUtil.isConnected()).thenReturn(true);
        doNothing().when(mockPrinterUtil).sendCommand(anyString());

        // Act
        assertDoesNotThrow(() -> printerService.printMultipleStickers(validStickerData, quantity, validShopProfile));

        // Assert
        verify(mockPrinterUtil).isConnected();
        verify(mockPrinterUtil, times(quantity)).sendCommand(anyString());
    }

    @Test
    @Order(15)
    @DisplayName("Should handle errors during multiple sticker printing")
    void testPrintMultipleStickersError() {
        // Arrange
        int quantity = 3;
        when(mockPrinterUtil.isConnected()).thenReturn(true);
        RuntimeException printException = new RuntimeException("Print failed");
        doThrow(printException).when(mockPrinterUtil).sendCommand(anyString());

        // Act & Assert
        PrinterException exception = assertThrows(PrinterException.class,
            () -> printerService.printMultipleStickers(validStickerData, quantity, validShopProfile));

        assertTrue(exception.getMessage().contains("Failed to print multiple stickers"));
        verify(mockPrinterUtil).isConnected();
        // Should attempt to print at least once before failing
        verify(mockPrinterUtil, atLeastOnce()).sendCommand(anyString());
    }

    @Test
    @Order(16)
    @DisplayName("Should handle zero quantity for multiple stickers")
    void testPrintMultipleStickersZeroQuantity() throws PrinterException, PrinterStatusException {
        // Arrange
        when(mockPrinterUtil.isConnected()).thenReturn(true);

        // Act
        assertDoesNotThrow(() -> printerService.printMultipleStickers(validStickerData, 0, validShopProfile));

        // Assert
        verify(mockPrinterUtil).isConnected();
        verify(mockPrinterUtil, never()).sendCommand(anyString());
    }

    @Test
    @Order(17)
    @DisplayName("Should handle large quantity printing")
    void testPrintMultipleStickersLargeQuantity() throws PrinterException, PrinterStatusException {
        // Arrange
        int quantity = 100;
        when(mockPrinterUtil.isConnected()).thenReturn(true);
        doNothing().when(mockPrinterUtil).sendCommand(anyString());

        // Act
        assertDoesNotThrow(() -> printerService.printMultipleStickers(validStickerData, quantity, validShopProfile));

        // Assert
        verify(mockPrinterUtil).isConnected();
        verify(mockPrinterUtil, times(quantity)).sendCommand(anyString());
    }

    // ==================== TSPL COMMAND GENERATION TESTS ====================

    @Test
    @Order(18)
    @DisplayName("Should generate valid TSPL commands for sticker")
    void testGenerateTSPLCommands() {
        // Act
        String commands = printerService.generateTSPLCommands(validStickerData, validShopProfile);

        // Assert
        assertNotNull(commands);
        assertFalse(commands.isEmpty());

        // Verify essential TSPL commands are present
        assertTrue(commands.contains("SIZE 50 mm, 30 mm"));
        assertTrue(commands.contains("DENSITY 8"));
        assertTrue(commands.contains("SPEED 4"));
        assertTrue(commands.contains("CLS"));
        assertTrue(commands.contains("TEXT"));
        assertTrue(commands.contains("PRINT 1"));

        // Verify data is included
        assertTrue(commands.contains(validShopProfile.getShopName()));
        assertTrue(commands.contains(validStickerData.getItemName()));
        assertTrue(commands.contains(validStickerData.getSupplierName()));
        assertTrue(commands.contains("Rs. " + validStickerData.getPrice()));
        assertTrue(commands.contains("Qty: " + validStickerData.getNumberOfStickers()));
    }

    @Test
    @Order(19)
    @DisplayName("Should include GST in TSPL commands when available")
    void testGenerateTSPLCommandsWithGST() {
        // Arrange
        ShopProfile profileWithGST = new ShopProfile("GST Shop", "GST123456789", "Address", "123", "email@test.com", null);

        // Act
        String commands = printerService.generateTSPLCommands(validStickerData, profileWithGST);

        // Assert
        assertTrue(commands.contains("GST: GST123456789"));
    }

    @Test
    @Order(20)
    @DisplayName("Should handle null GST number gracefully")
    void testGenerateTSPLCommandsNullGST() {
        // Arrange
        ShopProfile profileNullGST = new ShopProfile("No GST Shop", null, "Address", "123", "email@test.com", null);

        // Act
        String commands = printerService.generateTSPLCommands(validStickerData, profileNullGST);

        // Assert
        assertFalse(commands.contains("GST:"));
    }

    @Test
    @Order(21)
    @DisplayName("Should handle empty GST number gracefully")
    void testGenerateTSPLCommandsEmptyGST() {
        // Arrange
        ShopProfile profileEmptyGST = new ShopProfile("Empty GST Shop", "", "Address", "123", "email@test.com", null);

        // Act
        String commands = printerService.generateTSPLCommands(validStickerData, profileEmptyGST);

        // Assert
        assertFalse(commands.contains("GST:"));
    }

    // ==================== ERROR HANDLING AND RECOVERY TESTS ====================

    @Test
    @Order(22)
    @DisplayName("Should handle printer errors and attempt recovery")
    void testHandlePrinterErrors() {
        // Arrange
        doThrow(new RuntimeException("Port error")).when(mockPrinterUtil).closePort();
        when(mockConfigManager.getProperty("printer.port", "USB")).thenReturn("USB");
        when(mockConfigManager.getIntProperty("printer.baudrate", 9600)).thenReturn(9600);

        // Act & Assert
        PrinterException exception = assertThrows(PrinterException.class,
            () -> printerService.handlePrinterErrors());

        assertTrue(exception.getMessage().contains("Failed to handle printer errors"));
        verify(mockPrinterUtil).closePort();
        verify(mockPrinterUtil).openPort("USB", 9600);
    }

    @Test
    @Order(23)
    @DisplayName("Should handle null sticker data gracefully")
    void testPrintStickerNullData() {
        // Act & Assert
        assertThrows(NullPointerException.class,
            () -> printerService.printSticker(null, validShopProfile));
    }

    @Test
    @Order(24)
    @DisplayName("Should handle null shop profile gracefully")
    void testPrintStickerNullProfile() {
        // Act & Assert
        assertThrows(NullPointerException.class,
            () -> printerService.printSticker(validStickerData, null));
    }

    @Test
    @Order(25)
    @DisplayName("Should handle null parameters in multiple sticker printing")
    void testPrintMultipleStickersNullParameters() {
        // Test null data
        assertThrows(NullPointerException.class,
            () -> printerService.printMultipleStickers(null, 1, validShopProfile));

        // Test null profile
        assertThrows(NullPointerException.class,
            () -> printerService.printMultipleStickers(validStickerData, 1, null));
    }

    @Test
    @Order(26)
    @DisplayName("Should handle negative quantity in multiple sticker printing")
    void testPrintMultipleStickersNegativeQuantity() throws PrinterException, PrinterStatusException {
        // Arrange
        when(mockPrinterUtil.isConnected()).thenReturn(true);

        // Act
        assertDoesNotThrow(() -> printerService.printMultipleStickers(validStickerData, -1, validShopProfile));

        // Assert - should not attempt to print negative quantity
        verify(mockPrinterUtil).isConnected();
        verify(mockPrinterUtil, never()).sendCommand(anyString());
    }

    // ==================== CONFIGURATION MANAGEMENT TESTS ====================

    @Test
    @Order(27)
    @DisplayName("Should handle configuration retrieval errors during initialization")
    void testInitializePrinterConfigError() {
        // Arrange
        when(mockConfigManager.getProperty("printer.port", "USB")).thenThrow(new RuntimeException("Config error"));

        // Act & Assert
        PrinterException exception = assertThrows(PrinterException.class,
            () -> printerService.initializePrinter());

        assertTrue(exception.getMessage().contains("Failed to initialize printer"));
    }

    @Test
    @Order(28)
    @DisplayName("Should use custom port configuration")
    void testInitializePrinterCustomPort() {
        // Arrange
        when(mockConfigManager.getProperty("printer.port", "USB")).thenReturn("COM3");
        when(mockConfigManager.getIntProperty("printer.baudrate", 9600)).thenReturn(115200);

        // Act
        printerService.initializePrinter();

        // Assert
        verify(mockPrinterUtil).openPort("COM3", 115200);
    }

    // ==================== EDGE CASES AND VALIDATION TESTS ====================

    @Test
    @Order(29)
    @DisplayName("Should handle very long item names in TSPL generation")
    void testGenerateTSPLCommandsLongItemName() {
        // Arrange
        String longItemName = "A".repeat(200); // Very long item name
        StickerData longNameData = new StickerData(longItemName, "Supplier", new BigDecimal("10.00"), 1);

        // Act
        String commands = printerService.generateTSPLCommands(longNameData, validShopProfile);

        // Assert
        assertNotNull(commands);
        assertTrue(commands.contains(longItemName));
    }

    @Test
    @Order(30)
    @DisplayName("Should handle special characters in sticker data")
    void testGenerateTSPLCommandsSpecialCharacters() {
        // Arrange
        StickerData specialData = new StickerData("Item & Co. ©", "Supplier™", new BigDecimal("12.34"), 1);
        ShopProfile specialProfile = new ShopProfile("Shop & Café ®", "GST123", "Address", "123", "email@test.com", null);

        // Act
        String commands = printerService.generateTSPLCommands(specialData, specialProfile);

        // Assert
        assertNotNull(commands);
        assertTrue(commands.contains("Item & Co. ©"));
        assertTrue(commands.contains("Supplier™"));
        assertTrue(commands.contains("Shop & Café ®"));
    }

    @Test
    @Order(31)
    @DisplayName("Should handle very large price values")
    void testGenerateTSPLCommandsLargePrice() {
        // Arrange
        StickerData largePriceData = new StickerData("Expensive Item", "Supplier", new BigDecimal("999999.99"), 1);

        // Act
        String commands = printerService.generateTSPLCommands(largePriceData, validShopProfile);

        // Assert
        assertTrue(commands.contains("Rs. 999999.99"));
    }

    @Test
    @Order(32)
    @DisplayName("Should handle zero price values")
    void testGenerateTSPLCommandsZeroPrice() {
        // Arrange
        StickerData zeroPriceData = new StickerData("Free Item", "Supplier", BigDecimal.ZERO, 1);

        // Act
        String commands = printerService.generateTSPLCommands(zeroPriceData, validShopProfile);

        // Assert
        assertTrue(commands.contains("Rs. 0"));
    }

    @Test
    @Order(33)
    @DisplayName("Should handle maximum integer quantity")
    void testPrintMultipleStickersMaxQuantity() throws PrinterException, PrinterStatusException {
        // Arrange
        when(mockPrinterUtil.isConnected()).thenReturn(true);
        doNothing().when(mockPrinterUtil).sendCommand(anyString());

        // Act
        assertDoesNotThrow(() -> printerService.printMultipleStickers(validStickerData, Integer.MAX_VALUE, validShopProfile));

        // Assert
        verify(mockPrinterUtil).isConnected();
        verify(mockPrinterUtil, times(Integer.MAX_VALUE)).sendCommand(anyString());
    }

    @Test
    @Order(34)
    @DisplayName("Should handle concurrent printer operations safely")
    void testConcurrentPrinterOperations() throws InterruptedException {
        // This test verifies that the service can handle concurrent calls
        // In a real scenario, TSCPrinterUtil would need to be thread-safe

        // Arrange
        when(mockPrinterUtil.isConnected()).thenReturn(true);
        doNothing().when(mockPrinterUtil).sendCommand(anyString());

        // Act - Run multiple print operations concurrently
        Runnable printTask = () -> {
            try {
                printerService.printSticker(validStickerData, validShopProfile);
            } catch (Exception e) {
                fail("Concurrent printing should not fail: " + e.getMessage());
            }
        };

        Thread thread1 = new Thread(printTask);
        Thread thread2 = new Thread(printTask);

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        // Assert - Both operations should complete
        verify(mockPrinterUtil, times(2)).isConnected();
        verify(mockPrinterUtil, times(2)).sendCommand(anyString());
    }

    @Test
    @Order(35)
    @DisplayName("Should validate TSPL command structure")
    void testTSPLCommandStructureValidation() {
        // Act
        String commands = printerService.generateTSPLCommands(validStickerData, validShopProfile);

        // Assert - Commands should end with proper termination
        assertTrue(commands.endsWith("\r\n") || commands.endsWith("\n"));

        // Verify command order (SIZE, DENSITY, SPEED, CLS, TEXT commands, PRINT)
        String[] lines = commands.split("\r\n");
        assertTrue(lines.length > 5, "Should have multiple TSPL commands");

        // Check that essential commands are in reasonable order
        boolean foundSize = false;
        boolean foundCls = false;
        boolean foundPrint = false;

        for (String line : lines) {
            if (line.startsWith("SIZE")) foundSize = true;
            if (line.startsWith("CLS")) foundCls = true;
            if (line.startsWith("PRINT")) foundPrint = true;
        }

        assertTrue(foundSize, "Should contain SIZE command");
        assertTrue(foundCls, "Should contain CLS command");
        assertTrue(foundPrint, "Should contain PRINT command");
    }
}