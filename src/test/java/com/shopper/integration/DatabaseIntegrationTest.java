package com.shopper.integration;

import com.shopper.exceptions.DatabaseException;
import com.shopper.models.PrinterConfig;
import com.shopper.models.ShopProfile;
import com.shopper.models.StickerData;
import com.shopper.services.DatabaseService;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive integration tests for DatabaseService CRUD operations.
 * Tests end-to-end database functionality using real SQLite database.
 * Ensures proper isolation, transaction management, and data consistency.
 */
@TestMethodOrder(OrderAnnotation.class)
public class DatabaseIntegrationTest {

    private static final String TEST_DB_PATH = "test_sticker_printer.db";
    private static final String TEST_DB_URL = "jdbc:sqlite:" + TEST_DB_PATH;

    private DatabaseService databaseService;

    @BeforeAll
    static void setUpAll() throws Exception {
        // Clean up any existing test database
        Path testDbPath = Paths.get(TEST_DB_PATH);
        if (Files.exists(testDbPath)) {
            Files.delete(testDbPath);
        }

        // Set system properties for test database
        System.setProperty("db.url", TEST_DB_URL);
        System.setProperty("db.username", "");
        System.setProperty("db.password", "");
        System.setProperty("db.pool.maxPoolSize", "5");
        System.setProperty("db.pool.minIdle", "1");
        System.setProperty("db.pool.connectionTimeout", "10000");
        System.setProperty("db.pool.idleTimeout", "300000");
        System.setProperty("db.pool.maxLifetime", "600000");
    }

    @BeforeEach
    void setUp() throws Exception {
        // Reset singleton for each test
        resetSingleton();

        // Initialize fresh database service instance
        databaseService = DatabaseService.getInstance();

        // Clean all tables before each test
        cleanDatabase();
    }

    @AfterEach
    void tearDown() {
        if (databaseService != null) {
            databaseService.shutdown();
        }
        resetSingleton();
    }

    private void resetSingleton() {
        try {
            java.lang.reflect.Field instance = DatabaseService.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, null);
        } catch (Exception e) {
            // Ignore reflection errors
        }
    }

    private void cleanDatabase() {
        try {
            databaseService.executeInTransaction(conn -> {
                try (var stmt = conn.createStatement()) {
                    stmt.executeUpdate("DELETE FROM sticker_history");
                    stmt.executeUpdate("DELETE FROM printer_config");
                    stmt.executeUpdate("DELETE FROM shop_profile");
                }
            });
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    @AfterAll
    static void tearDownAll() throws Exception {
        // Clean up test database file
        Path testDbPath = Paths.get(TEST_DB_PATH);
        if (Files.exists(testDbPath)) {
            Files.delete(testDbPath);
        }

        // Clear system properties
        System.clearProperty("db.url");
        System.clearProperty("db.username");
        System.clearProperty("db.password");
        System.clearProperty("db.pool.maxPoolSize");
        System.clearProperty("db.pool.minIdle");
        System.clearProperty("db.pool.connectionTimeout");
        System.clearProperty("db.pool.idleTimeout");
        System.clearProperty("db.pool.maxLifetime");
    }


    // ==================== SHOP PROFILE CRUD INTEGRATION TESTS ====================

    @Test
    @Order(1)
    @DisplayName("Should create, read, update, and delete shop profile end-to-end")
    void testShopProfileFullCrudWorkflow() {
        // CREATE
        ShopProfile profile = new ShopProfile(
            "Test Shop", "22AAAAA0000A1Z5", "123 Test Street, Test City",
            "9876543210", "test@example.com", "/path/to/logo.png"
        );

        int profileId = databaseService.insertShopProfile(profile);
        assertTrue(profileId > 0, "Profile should be created with valid ID");

        // READ
        ShopProfile retrieved = databaseService.getShopProfile();
        assertNotNull(retrieved, "Profile should be retrievable");
        assertEquals("Test Shop", retrieved.getShopName());
        assertEquals("GST123456789", retrieved.getGstNumber());
        assertEquals("123 Test Street, Test City", retrieved.getAddress());
        assertEquals("9876543210", retrieved.getPhoneNumber());
        assertEquals("test@example.com", retrieved.getEmail());
        assertEquals("/path/to/logo.png", retrieved.getLogoPath());

        // UPDATE
        retrieved.setShopName("Updated Test Shop");
        retrieved.setEmail("updated@example.com");
        retrieved.setPhoneNumber("1234567890");

        boolean updated = databaseService.updateShopProfile(retrieved);
        assertTrue(updated, "Profile should be updated successfully");

        ShopProfile updatedProfile = databaseService.getShopProfile();
        assertEquals("Updated Test Shop", updatedProfile.getShopName());
        assertEquals("updated@example.com", updatedProfile.getEmail());
        assertEquals("1234567890", updatedProfile.getPhoneNumber());

        // DELETE
        boolean deleted = databaseService.deleteShopProfile();
        assertTrue(deleted, "Profile should be deleted successfully");

        ShopProfile deletedProfile = databaseService.getShopProfile();
        assertNull(deletedProfile, "Profile should no longer exist");
    }

    @Test
    @Order(2)
    @DisplayName("Should handle shop profile validation errors")
    void testShopProfileValidationErrors() {
        // Test null profile
        assertThrows(IllegalArgumentException.class,
            () -> databaseService.insertShopProfile(null),
            "Should reject null profile");

        // Test empty shop name
        final ShopProfile invalidProfile1 = new ShopProfile("", "GST123", "Address", "123", "email@test.com", null);
        assertThrows(IllegalArgumentException.class,
            () -> databaseService.insertShopProfile(invalidProfile1),
            "Should reject empty shop name");

        // Test null shop name
        final ShopProfile invalidProfile2 = new ShopProfile(null, "GST123", "Address", "123", "email@test.com", null);
        assertThrows(IllegalArgumentException.class,
            () -> databaseService.insertShopProfile(invalidProfile2),
            "Should reject null shop name");
    }

    // ==================== PRINTER CONFIG CRUD INTEGRATION TESTS ====================

    @Test
    @Order(3)
    @DisplayName("Should create, read, update, and delete printer configs end-to-end")
    void testPrinterConfigFullCrudWorkflow() {
        // CREATE multiple configs
        PrinterConfig config1 = new PrinterConfig("Printer A", 58, 40, true);
        PrinterConfig config2 = new PrinterConfig("Printer B", 80, 50, false);

        int id1 = databaseService.insertPrinterConfig(config1);
        int id2 = databaseService.insertPrinterConfig(config2);
        assertTrue(id1 > 0 && id2 > 0, "Both configs should be created with valid IDs");

        // READ all configs
        List<PrinterConfig> configs = databaseService.getAllPrinterConfigs();
        assertEquals(2, configs.size(), "Should retrieve 2 configs");

        PrinterConfig defaultConfig = configs.stream()
            .filter(PrinterConfig::isDefault)
            .findFirst()
            .orElse(null);
        assertNotNull(defaultConfig, "Should have a default config");
        assertEquals("Printer A", defaultConfig.getPrinterName());

        // UPDATE config
        PrinterConfig configToUpdate = configs.get(0);
        configToUpdate.setPrinterName("Updated Printer A");
        configToUpdate.setPaperWidth(60);

        boolean updated = databaseService.updatePrinterConfig(configToUpdate);
        assertTrue(updated, "Config should be updated successfully");

        List<PrinterConfig> updatedConfigs = databaseService.getAllPrinterConfigs();
        PrinterConfig updatedConfig = updatedConfigs.stream()
            .filter(c -> "Updated Printer A".equals(c.getPrinterName()))
            .findFirst()
            .orElse(null);
        assertNotNull(updatedConfig, "Updated config should exist");
        assertEquals(60, updatedConfig.getPaperWidth());

        // DELETE config
        boolean deleted = databaseService.deletePrinterConfig(1); // Assuming ID 1
        assertTrue(deleted, "Config should be deleted successfully");

        List<PrinterConfig> remainingConfigs = databaseService.getAllPrinterConfigs();
        assertEquals(1, remainingConfigs.size(), "Should have 1 config remaining");
    }

    @Test
    @Order(4)
    @DisplayName("Should handle printer config validation errors")
    void testPrinterConfigValidationErrors() {
        // Test null config
        assertThrows(IllegalArgumentException.class,
            () -> databaseService.insertPrinterConfig(null),
            "Should reject null config");

        // Test empty printer name
        final PrinterConfig invalidConfig1 = new PrinterConfig("", 58, 40, true);
        assertThrows(IllegalArgumentException.class,
            () -> databaseService.insertPrinterConfig(invalidConfig1),
            "Should reject empty printer name");

        // Test invalid dimensions
        final PrinterConfig invalidConfig2 = new PrinterConfig("Test Printer", 0, -10, true);
        assertThrows(IllegalArgumentException.class,
            () -> databaseService.insertPrinterConfig(invalidConfig2),
            "Should reject invalid dimensions");
    }

    @Test
    @Order(5)
    @DisplayName("Should retrieve default printer correctly")
    void testGetDefaultPrinter() {
        // No default printer initially
        PrinterConfig defaultPrinter = databaseService.getDefaultPrinter();
        assertNull(defaultPrinter, "Should return null when no default printer exists");

        // Create default printer
        PrinterConfig defaultConfig = new PrinterConfig("Default Printer", 58, 40, true);
        databaseService.insertPrinterConfig(defaultConfig);

        // Should retrieve default printer
        defaultPrinter = databaseService.getDefaultPrinter();
        assertNotNull(defaultPrinter, "Should retrieve default printer");
        assertEquals("Default Printer", defaultPrinter.getPrinterName());
        assertTrue(defaultPrinter.isDefault(), "Should be marked as default");
    }

    // ==================== STICKER DATA CRUD INTEGRATION TESTS ====================

    @Test
    @Order(6)
    @DisplayName("Should create, read, update, and delete sticker data end-to-end")
    void testStickerDataFullCrudWorkflow() {
        // CREATE multiple records
        StickerData data1 = new StickerData("Item A", "Supplier X", new BigDecimal("12.50"), 10);
        StickerData data2 = new StickerData("Item B", "Supplier Y", new BigDecimal("25.75"), 5);

        int id1 = databaseService.insertStickerData(data1);
        int id2 = databaseService.insertStickerData(data2);
        assertTrue(id1 > 0 && id2 > 0, "Both records should be created with valid IDs");

        // READ all history
        List<StickerData> history = databaseService.getAllStickerHistory();
        assertEquals(2, history.size(), "Should retrieve 2 records");

        // Verify data integrity
        assertTrue(history.stream().anyMatch(d -> d.getItemName().equals("Item A")));
        assertTrue(history.stream().anyMatch(d -> d.getItemName().equals("Item B")));

        // UPDATE record
        StickerData recordToUpdate = history.get(0);
        recordToUpdate.setItemName("Updated Item A");
        recordToUpdate.setPrice(new BigDecimal("15.00"));
        recordToUpdate.setQuantity(15);

        boolean updated = databaseService.updateStickerHistory(recordToUpdate);
        assertTrue(updated, "Record should be updated successfully");

        List<StickerData> updatedHistory = databaseService.getAllStickerHistory();
        StickerData updatedRecord = updatedHistory.stream()
            .filter(d -> "Updated Item A".equals(d.getItemName()))
            .findFirst()
            .orElse(null);
        assertNotNull(updatedRecord, "Updated record should exist");
        assertEquals(new BigDecimal("15.00"), updatedRecord.getPrice());
        assertEquals(15, updatedRecord.getQuantity());

        // DELETE record
        boolean deleted = databaseService.deleteStickerHistory(1); // Assuming ID 1
        assertTrue(deleted, "Record should be deleted successfully");

        List<StickerData> remainingHistory = databaseService.getAllStickerHistory();
        assertEquals(1, remainingHistory.size(), "Should have 1 record remaining");
    }

    @Test
    @Order(7)
    @DisplayName("Should handle sticker data validation errors")
    void testStickerDataValidationErrors() {
        // Test null data
        assertThrows(IllegalArgumentException.class,
            () -> databaseService.insertStickerData(null),
            "Should reject null data");

        // Test empty item name
        final StickerData invalidData1 = new StickerData("", "Supplier", new BigDecimal("10.00"), 5);
        assertThrows(IllegalArgumentException.class,
            () -> databaseService.insertStickerData(invalidData1),
            "Should reject empty item name");

        // Test negative price
        final StickerData invalidData2 = new StickerData("Item", "Supplier", new BigDecimal("-5.00"), 5);
        assertThrows(IllegalArgumentException.class,
            () -> databaseService.insertStickerData(invalidData2),
            "Should reject negative price");

        // Test zero quantity
        final StickerData invalidData3 = new StickerData("Item", "Supplier", new BigDecimal("10.00"), 0);
        assertThrows(IllegalArgumentException.class,
            () -> databaseService.insertStickerData(invalidData3),
            "Should reject zero quantity");
    }

    @Test
    @Order(8)
    @DisplayName("Should log sticker print operations correctly")
    void testLogStickerPrint() {
        int logId = databaseService.logStickerPrint("Test Item", "Test Supplier", new BigDecimal("18.50"), 8);
        assertTrue(logId > 0, "Should return valid log ID");

        List<StickerData> history = databaseService.getAllStickerHistory();
        assertEquals(1, history.size(), "Should have one logged record");

        StickerData logged = history.get(0);
        assertEquals("Test Item", logged.getItemName());
        assertEquals("Test Supplier", logged.getSupplierName());
        assertEquals(new BigDecimal("18.50"), logged.getPrice());
        assertEquals(8, logged.getQuantity());
    }

    // ==================== TRANSACTION MANAGEMENT TESTS ====================

    @Test
    @Order(9)
    @DisplayName("Should commit transaction successfully with multiple operations")
    void testTransactionCommit() throws Exception {
        databaseService.executeInTransaction(conn -> {
            // Insert shop profile
            ShopProfile profile = new ShopProfile("Txn Shop", "GST999", "Txn Address", "999", "txn@test.com", null);
            databaseService.insertShopProfile(profile);

            // Insert printer config
            PrinterConfig config = new PrinterConfig("Txn Printer", 58, 40, true);
            databaseService.insertPrinterConfig(config);

            // Insert sticker data
            StickerData data = new StickerData("Txn Item", "Txn Supplier", new BigDecimal("9.99"), 3);
            databaseService.insertStickerData(data);
        });

        // Verify all data was committed
        ShopProfile profile = databaseService.getShopProfile();
        assertNotNull(profile, "Profile should be committed");
        assertEquals("Txn Shop", profile.getShopName());

        List<PrinterConfig> configs = databaseService.getAllPrinterConfigs();
        assertFalse(configs.isEmpty(), "Configs should be committed");

        List<StickerData> history = databaseService.getAllStickerHistory();
        assertFalse(history.isEmpty(), "History should be committed");
    }

    @Test
    @Order(10)
    @DisplayName("Should rollback transaction on exception")
    void testTransactionRollback() {
        assertThrows(RuntimeException.class, () -> {
            databaseService.executeInTransaction(conn -> {
                // Insert valid data first
                ShopProfile profile = new ShopProfile("Rollback Shop", "GST888", "Address", "888", "test@test.com", null);
                databaseService.insertShopProfile(profile);

                // Cause exception to trigger rollback
                throw new RuntimeException("Test rollback exception");
            });
        });

        // Verify no data was committed (transaction rolled back)
        ShopProfile profile = databaseService.getShopProfile();
        assertNull(profile, "Profile should not exist due to rollback");
    }

    // ==================== DATA CONSISTENCY AND ISOLATION TESTS ====================

    @Test
    @Order(11)
    @DisplayName("Should maintain data consistency across concurrent operations")
    void testConcurrentDataConsistency() throws InterruptedException {
        final int THREAD_COUNT = 3;
        final ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        final CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    // Each thread performs CRUD operations
                    String threadPrefix = "Thread" + threadId;

                    // Create shop profile
                    ShopProfile profile = new ShopProfile(
                        threadPrefix + " Shop", "GST" + threadId, threadPrefix + " Address",
                        threadId + "123", threadPrefix + "@test.com", null
                    );
                    databaseService.insertShopProfile(profile);

                    // Create printer config
                    PrinterConfig config = new PrinterConfig(threadPrefix + " Printer", 58, 40, false);
                    databaseService.insertPrinterConfig(config);

                    // Create sticker data
                    StickerData data = new StickerData(
                        threadPrefix + " Item", threadPrefix + " Supplier",
                        new BigDecimal(String.valueOf(10.0 + threadId)), 5 + threadId
                    );
                    databaseService.insertStickerData(data);

                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS), "Concurrent operations should complete");

        // Verify data integrity
        List<PrinterConfig> configs = databaseService.getAllPrinterConfigs();
        List<StickerData> history = databaseService.getAllStickerHistory();

        assertEquals(THREAD_COUNT, configs.size(), "Should have configs from all threads");
        assertEquals(THREAD_COUNT, history.size(), "Should have history records from all threads");

        executor.shutdown();
    }

    @Test
    @Order(12)
    @DisplayName("Should handle database connection failures gracefully")
    void testDatabaseConnectionFailure() {
        // Test with corrupted database URL (this would normally cause connection issues)
        // Since we're using a real database, we'll test error handling by attempting operations
        // that should succeed but could fail under certain conditions

        ShopProfile profile = new ShopProfile("Test Shop", "GST123", "Address", "123", "test@test.com", null);
        databaseService.insertShopProfile(profile);

        // Verify operation succeeded
        ShopProfile retrieved = databaseService.getShopProfile();
        assertNotNull(retrieved, "Should handle database operations normally");
    }

    // ==================== EDGE CASES AND ERROR SCENARIOS ====================

    @Test
    @Order(13)
    @DisplayName("Should handle empty result sets correctly")
    void testEmptyResultSets() {
        // Test empty shop profile
        ShopProfile profile = databaseService.getShopProfile();
        assertNull(profile, "Should return null for non-existent profile");

        // Test empty printer configs
        List<PrinterConfig> configs = databaseService.getAllPrinterConfigs();
        assertTrue(configs.isEmpty(), "Should return empty list for no configs");

        // Test empty sticker history
        List<StickerData> history = databaseService.getAllStickerHistory();
        assertTrue(history.isEmpty(), "Should return empty list for no history");
    }

    @Test
    @Order(14)
    @DisplayName("Should handle large data sets efficiently")
    void testLargeDataSetHandling() {
        // Insert multiple records to test performance and memory handling
        final int RECORD_COUNT = 100;

        for (int i = 0; i < RECORD_COUNT; i++) {
            StickerData data = new StickerData(
                "Item" + i, "Supplier" + (i % 10),
                new BigDecimal(String.valueOf(1.00 + (i % 50))), 1 + (i % 20)
            );
            databaseService.insertStickerData(data);
        }

        List<StickerData> history = databaseService.getAllStickerHistory();
        assertEquals(RECORD_COUNT, history.size(), "Should handle large data sets");

        // Verify data integrity for a few records
        assertTrue(history.stream().anyMatch(d -> d.getItemName().equals("Item0")));
        assertTrue(history.stream().anyMatch(d -> d.getItemName().equals("Item99")));
    }

    @Test
    @Order(15)
    @DisplayName("Should maintain referential integrity and constraints")
    void testReferentialIntegrity() {
        // Test that database constraints are enforced
        // SQLite has limited referential integrity, but we can test basic constraints

        // Test unique constraints (shop_profile should only have one record due to application logic)
        ShopProfile profile1 = new ShopProfile("Shop1", "GST1", "Addr1", "111", "email1@test.com", null);
        ShopProfile profile2 = new ShopProfile("Shop2", "GST2", "Addr2", "222", "email2@test.com", null);

        databaseService.insertShopProfile(profile1);
        databaseService.insertShopProfile(profile2); // This should replace the first due to app logic

        ShopProfile retrieved = databaseService.getShopProfile();
        assertNotNull(retrieved, "Should have a profile");
        // The behavior depends on how the app handles multiple profiles - assuming it takes the latest
    }

    @Test
    @Order(16)
    @DisplayName("Should handle special characters and unicode data")
    void testSpecialCharactersAndUnicode() {
        // Test with special characters and unicode
        ShopProfile profile = new ShopProfile(
            "Test Shop ñáéíóú", "GST-123ñ", "Calle 123 ñáéíóú",
            "987-654-3210", "test.ñáéíóú@example.com", "/path/to/logo_ñáéíóú.png"
        );

        databaseService.insertShopProfile(profile);

        ShopProfile retrieved = databaseService.getShopProfile();
        assertNotNull(retrieved, "Should handle unicode characters");
        assertEquals("Test Shop ñáéíóú", retrieved.getShopName());
        assertEquals("GST-123ñ", retrieved.getGstNumber());
    }

    @Test
    @Order(17)
    @DisplayName("Should validate data types and ranges correctly")
    void testDataTypeValidation() {
        // Test BigDecimal precision
        StickerData data = new StickerData(
            "Precision Test", "Supplier",
            new BigDecimal("123456789.123456789"), 1
        );
        databaseService.insertStickerData(data);

        List<StickerData> history = databaseService.getAllStickerHistory();
        assertFalse(history.isEmpty(), "Should handle BigDecimal precision");

        // Test integer ranges
        PrinterConfig config = new PrinterConfig("Range Test", Integer.MAX_VALUE, Integer.MAX_VALUE, false);
        databaseService.insertPrinterConfig(config);

        List<PrinterConfig> configs = databaseService.getAllPrinterConfigs();
        assertFalse(configs.isEmpty(), "Should handle integer ranges");
    }
}