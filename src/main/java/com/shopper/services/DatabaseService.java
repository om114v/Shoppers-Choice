package com.shopper.services;

import com.shopper.exceptions.DatabaseException;
import com.shopper.models.PrinterConfig;
import com.shopper.models.ShopProfile;
import com.shopper.models.StickerData;
import com.shopper.utils.ConfigManager;
import com.shopper.utils.ErrorReporter;
import com.shopper.utils.Logger;
import com.shopper.utils.ResourceManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Singleton service class for database operations.
 * Manages SQLite database connections using HikariCP connection pooling.
 * Provides CRUD operations for all entities and transaction support.
 * Thread-safe enum singleton implementation.
 */
public enum DatabaseService {
    INSTANCE;

    private HikariDataSource dataSource;
    private final Logger logger = Logger.getInstance();
    private final ConfigManager config = ConfigManager.getInstance();

    DatabaseService() {
        initializeDataSource();
//        initializeDatabase();
        // Register data source for cleanup
        ResourceManager.getInstance().registerResource(dataSource);
    }

    /**
     * Gets the singleton instance of DatabaseService.
     * @return the DatabaseService instance
     */
    public static DatabaseService getInstance() {
        return INSTANCE;
    }

    /**
     * Initializes the HikariCP data source.
     * @throws DatabaseException if connection pool initialization fails
     */
    private void initializeDataSource() throws DatabaseException {
        try {
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(config.getProperty("db.url"));
            hikariConfig.setUsername(config.getProperty("db.username", ""));
            hikariConfig.setPassword(config.getProperty("db.password", ""));
            hikariConfig.setMaximumPoolSize(config.getIntProperty("db.pool.maxPoolSize", 5)); // Reduced for SQLite
            hikariConfig.setMinimumIdle(config.getIntProperty("db.pool.minIdle", 1));
            hikariConfig.setConnectionTimeout(config.getIntProperty("db.pool.connectionTimeout", 30000));
            hikariConfig.setIdleTimeout(config.getIntProperty("db.pool.idleTimeout", 600000));
            hikariConfig.setMaxLifetime(config.getIntProperty("db.pool.maxLifetime", 1800000));
            hikariConfig.setConnectionTestQuery("SELECT 1"); // For SQLite connection validation
            hikariConfig.setLeakDetectionThreshold(60000); // Detect connection leaks
            hikariConfig.setValidationTimeout(5000); // Timeout for validation

            this.dataSource = new HikariDataSource(hikariConfig);
            logger.info(DatabaseService.class.getSimpleName(), "Database connection pool initialized");
        } catch (Exception e) {
            logger.error(DatabaseService.class.getSimpleName(), "Failed to initialize database connection pool", e);
            ErrorReporter.getInstance().reportError(e, "Database connection pool initialization failed");
            throw new DatabaseException("Database connection failed - check database URL, credentials, and server status", e);
        }
    }

    /**
     * Initializes the database by creating tables if they don't exist.
     */
    private void initializeDatabase() {
        Connection conn = null;
        Statement stmt = null;
        java.io.InputStream schemaStream = null;

        try {
            conn = getConnection();
            stmt = conn.createStatement();

            // Enable WAL mode for better concurrent access in SQLite
            stmt.execute("PRAGMA journal_mode=WAL;");
            stmt.execute("PRAGMA synchronous=NORMAL;");
            stmt.execute("PRAGMA cache_size=1000;");
            stmt.execute("PRAGMA temp_store=MEMORY;");
            stmt.execute("PRAGMA busy_timeout=30000;"); // 30 second timeout for locks

            // Read schema from resource file
            schemaStream = getClass().getClassLoader().getResourceAsStream("schema.sql");
            if (schemaStream != null) {
                String schema = new String(schemaStream.readAllBytes());
                stmt.executeUpdate(schema);
                logger.info(DatabaseService.class.getSimpleName(), "Database schema initialized");
            } else {
                logger.error(DatabaseService.class.getSimpleName(), "Schema file not found");
                throw new DatabaseException("Schema file not found");
            }

        } catch (Exception e) {
            logger.error(DatabaseService.class.getSimpleName(), "Failed to initialize database", e);
            ErrorReporter.getInstance().reportError(e, "Database schema initialization failed");
            throw new DatabaseException("Database initialization failed", e);
        } finally {
            // Proper resource cleanup
            if (schemaStream != null) {
                try {
                    schemaStream.close();
                } catch (Exception e) {
                    logger.warn(DatabaseService.class.getSimpleName(), "Failed to close schema stream: " + e.getMessage());
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (Exception e) {
                    logger.warn(DatabaseService.class.getSimpleName(), "Failed to close statement: " + e.getMessage());
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                    logger.warn(DatabaseService.class.getSimpleName(), "Failed to close connection: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Gets a database connection from the pool.
     * Uses ThreadLocal to ensure each thread gets its own connection for thread safety.
     * @return a database connection
     * @throws SQLException if connection fails
     * @throws DatabaseException if database is not accessible
     */
    private final ThreadLocal<Connection> threadLocalConnection = new ThreadLocal<>();

    private Connection getConnection() throws SQLException, DatabaseException {
        Connection conn = threadLocalConnection.get();
        if (conn == null || conn.isClosed() || !isConnectionValid(conn)) {
            try {
                conn = dataSource.getConnection();
                // Validate connection before returning
                if (!isConnectionValid(conn)) {
                    conn.close();
                    throw new DatabaseException("Database connection validation failed");
                }
                threadLocalConnection.set(conn);
            } catch (SQLException e) {
                logger.error(DatabaseService.class.getSimpleName(), "Database connection failed", e);
                throw new DatabaseException("Database connection failed - check database server and network connectivity", e);
            }
        }
        return conn;
    }

    /**
     * Closes the thread-local connection.
     */
    private void closeThreadLocalConnection() {
        Connection conn = threadLocalConnection.get();
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                logger.error(DatabaseService.class.getSimpleName(), "Error closing thread-local connection", e);
            } finally {
                threadLocalConnection.remove();
            }
        }
    }

    /**
     * Validates if a database connection is healthy.
     * @param connection the connection to validate
     * @return true if connection is valid
     */
    public boolean isConnectionValid(Connection connection) {
        if (connection == null) {
            return false;
        }
        try {
            // For SQLite, check if connection is closed and try a simple query
            if (connection.isClosed()) {
                return false;
            }
            try (PreparedStatement stmt = connection.prepareStatement("SELECT 1")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            logger.error(DatabaseService.class.getSimpleName(), "Connection validation failed", e);
            return false;
        }
    }

    /**
     * Tests database connectivity by attempting to get and validate a connection.
     * @return true if database is accessible
     */
    public boolean testConnectivity() {
        try (Connection conn = getConnection()) {
            return isConnectionValid(conn);
        } catch (Exception e) {
            logger.error(DatabaseService.class.getSimpleName(), "Database connectivity test failed", e);
            return false;
        }
    }

    /**
     * Closes the data source and releases all connections.
     * Also cleans up thread-local connections.
     */
    public void shutdown() {
        try {
            // Close thread-local connections first
            closeThreadLocalConnection();

            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
                logger.info(DatabaseService.class.getSimpleName(), "Database connection pool shut down");
            }
        } catch (Exception e) {
            logger.error(DatabaseService.class.getSimpleName(), "Error shutting down database connection pool", e);
            ErrorReporter.getInstance().reportError(e, "Database connection pool shutdown failed");
            throw new DatabaseException("Failed to shutdown database connection pool", e);
        }
    }

    // ==================== SHOP PROFILE CRUD OPERATIONS ====================

    /**
     * Inserts a new shop profile.
     * @param profile the shop profile to insert
     * @return the generated ID
     */
    public int insertShopProfile(ShopProfile profile) {
        try {
            validateShopProfile(profile);

            String sql = "INSERT INTO shop_profile (shop_name, gst_number, address, phone_number, email, logo_path) VALUES (?, ?, ?, ?, ?, ?)";

            try (Connection conn = getConnection();
                  PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                // Sanitize and validate all string inputs
                pstmt.setString(1, sanitizeString(profile.getShopName(), 100));
                pstmt.setString(2, sanitizeString(profile.getGstNumber(), 15));
                pstmt.setString(3, sanitizeString(profile.getAddress(), 500));
                pstmt.setString(4, sanitizeString(profile.getPhoneNumber(), 15));
                pstmt.setString(5, sanitizeString(profile.getEmail(), 100));
                pstmt.setString(6, sanitizeString(profile.getLogoPath(), 255));

                int affectedRows = pstmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Inserting shop profile failed, no rows affected.");
                }

                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int id = generatedKeys.getInt(1);
                        profile.setId(id);
                        logger.info(DatabaseService.class.getSimpleName(), "Shop profile inserted with ID: " + id);
                        return id;
                    } else {
                        throw new SQLException("Inserting shop profile failed, no ID obtained.");
                    }
                }

            }
        } catch (SQLException e) {
            logger.error(DatabaseService.class.getSimpleName(), "Error inserting shop profile", e);
            throw new DatabaseException("Failed to insert shop profile", e);
        }
    }

    /**
     * Retrieves the shop profile.
     * @return the shop profile or null if not found
     */
    public ShopProfile getShopProfile() {
        try {
            String sql = "SELECT * FROM shop_profile LIMIT 1";

            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {

                if (rs.next()) {
                    ShopProfile profile = new ShopProfile();
                    profile.setId(rs.getInt("id"));
                    profile.setShopName(rs.getString("shop_name"));
                    profile.setGstNumber(rs.getString("gst_number"));
                    profile.setAddress(rs.getString("address"));
                    profile.setPhoneNumber(rs.getString("phone_number"));
                    profile.setEmail(rs.getString("email"));
                    profile.setLogoPath(rs.getString("logo_path"));

                    Timestamp createdAt = rs.getTimestamp("created_at");
                    if (createdAt != null) {
                        profile.setCreatedAt(createdAt.toLocalDateTime());
                    }

                    Timestamp updatedAt = rs.getTimestamp("updated_at");
                    if (updatedAt != null) {
                        profile.setUpdatedAt(updatedAt.toLocalDateTime());
                    }

                    return profile;
                }

            }
        } catch (SQLException e) {
            logger.error(DatabaseService.class.getSimpleName(), "Error retrieving shop profile", e);
            throw new DatabaseException("Failed to retrieve shop profile", e);
        }

        return null;
    }

    /**
     * Updates the shop profile.
     * @param profile the shop profile to update
     * @return true if updated successfully
     */
    public boolean updateShopProfile(ShopProfile profile) {
        try {
            validateShopProfile(profile);
            if (profile.getId() <= 0) {
                throw new IllegalArgumentException("Shop profile ID must be set for update");
            }

            String sql = "UPDATE shop_profile SET shop_name = ?, gst_number = ?, address = ?, phone_number = ?, email = ?, logo_path = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";

            try (Connection conn = getConnection();
                  PreparedStatement pstmt = conn.prepareStatement(sql)) {

                // Sanitize and validate all string inputs
                pstmt.setString(1, sanitizeString(profile.getShopName(), 100));
                pstmt.setString(2, sanitizeString(profile.getGstNumber(), 15));
                pstmt.setString(3, sanitizeString(profile.getAddress(), 500));
                pstmt.setString(4, sanitizeString(profile.getPhoneNumber(), 15));
                pstmt.setString(5, sanitizeString(profile.getEmail(), 100));
                pstmt.setString(6, sanitizeString(profile.getLogoPath(), 255));
                pstmt.setInt(7, validateInteger(profile.getId(), 1, Integer.MAX_VALUE));

                int affectedRows = pstmt.executeUpdate();
                boolean success = affectedRows > 0;
                if (success) {
                    logger.info(DatabaseService.class.getSimpleName(), "Shop profile updated with ID: " + profile.getId());
                }
                return success;

            }
        } catch (SQLException e) {
            logger.error(DatabaseService.class.getSimpleName(), "Error updating shop profile", e);
            throw new DatabaseException("Failed to update shop profile", e);
        }
    }

    /**
     * Deletes the shop profile.
     * @return true if deleted successfully
     */
    public boolean deleteShopProfile() {
        try {
            String sql = "DELETE FROM shop_profile";

            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                int affectedRows = pstmt.executeUpdate();
                boolean success = affectedRows > 0;
                if (success) {
                    logger.info(DatabaseService.class.getSimpleName(), "Shop profile deleted");
                }
                return success;

            }
        } catch (SQLException e) {
            logger.error(DatabaseService.class.getSimpleName(), "Error deleting shop profile", e);
            throw new DatabaseException("Failed to delete shop profile", e);
        }
    }

    // ==================== PRINTER CONFIG CRUD OPERATIONS ====================

    /**
     * Inserts a new printer configuration.
     * @param config the printer config to insert
     * @return the generated ID
     */
    public int insertPrinterConfig(PrinterConfig config) {
        try {
            validatePrinterConfig(config);

            String sql = "INSERT INTO printer_config (printer_name, paper_width, paper_height, is_default) VALUES (?, ?, ?, ?)";

            try (Connection conn = getConnection();
                  PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                // Sanitize and validate all inputs
                pstmt.setString(1, sanitizeString(config.getPrinterName(), 50));
                pstmt.setInt(2, validateInteger(config.getPaperWidth(), 1, 1000));
                pstmt.setInt(3, validateInteger(config.getPaperHeight(), 1, 1000));
                pstmt.setBoolean(4, config.isDefault());

                int affectedRows = pstmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Inserting printer config failed, no rows affected.");
                }

                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int id = generatedKeys.getInt(1);
                        config.setId(id);
                        logger.info(DatabaseService.class.getSimpleName(), "Printer config inserted with ID: " + id);
                        return id;
                    } else {
                        throw new SQLException("Inserting printer config failed, no ID obtained.");
                    }
                }

            }
        } catch (SQLException e) {
            logger.error(DatabaseService.class.getSimpleName(), "Error inserting printer config", e);
            throw new DatabaseException("Failed to insert printer config", e);
        }
    }

    /**
     * Retrieves all printer configurations.
     * @return list of printer configurations
     */
    public List<PrinterConfig> getAllPrinterConfigs() {
        try {
            List<PrinterConfig> configs = new ArrayList<>();
            String sql = "SELECT * FROM printer_config ORDER BY is_default DESC, printer_name";

            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {
                    PrinterConfig config = new PrinterConfig();
                    config.setId(rs.getInt("id"));
                    config.setPrinterName(rs.getString("printer_name"));
                    config.setPaperWidth(rs.getInt("paper_width"));
                    config.setPaperHeight(rs.getInt("paper_height"));
                    config.setDefault(rs.getBoolean("is_default"));

                    Timestamp createdAt = rs.getTimestamp("created_at");
                    if (createdAt != null) {
                        config.setCreatedAt(createdAt.toLocalDateTime());
                    }

                    configs.add(config);
                }

            }
            return configs;
        } catch (SQLException e) {
            logger.error(DatabaseService.class.getSimpleName(), "Error retrieving printer configs", e);
            throw new DatabaseException("Failed to retrieve printer configs", e);
        }
    }

    /**
     * Updates a printer configuration.
     * @param config the printer config to update
     * @return true if updated successfully
     */
    public boolean updatePrinterConfig(PrinterConfig config) {
        try {
            validatePrinterConfig(config);
            if (config.getId() <= 0) {
                throw new IllegalArgumentException("Printer config ID must be set for update");
            }

            String sql = "UPDATE printer_config SET printer_name = ?, paper_width = ?, paper_height = ?, is_default = ? WHERE id = ?";

            try (Connection conn = getConnection();
                  PreparedStatement pstmt = conn.prepareStatement(sql)) {

                // Sanitize and validate all inputs
                pstmt.setString(1, sanitizeString(config.getPrinterName(), 50));
                pstmt.setInt(2, validateInteger(config.getPaperWidth(), 1, 1000));
                pstmt.setInt(3, validateInteger(config.getPaperHeight(), 1, 1000));
                pstmt.setBoolean(4, config.isDefault());
                pstmt.setInt(5, validateInteger(config.getId(), 1, Integer.MAX_VALUE));

                int affectedRows = pstmt.executeUpdate();
                boolean success = affectedRows > 0;
                if (success) {
                    logger.info(DatabaseService.class.getSimpleName(), "Printer config updated with ID: " + config.getId());
                }
                return success;

            }
        } catch (SQLException e) {
            logger.error(DatabaseService.class.getSimpleName(), "Error updating printer config", e);
            throw new DatabaseException("Failed to update printer config", e);
        }
    }

    /**
     * Deletes a printer configuration by ID.
     * @param id the printer config ID to delete
     * @return true if deleted successfully
     */
    public boolean deletePrinterConfig(int id) {
        try {
            String sql = "DELETE FROM printer_config WHERE id = ?";

            try (Connection conn = getConnection();
                  PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setInt(1, validateInteger(id, 1, Integer.MAX_VALUE));
                int affectedRows = pstmt.executeUpdate();
                boolean success = affectedRows > 0;
                if (success) {
                    logger.info(DatabaseService.class.getSimpleName(), "Printer config deleted with ID: " + id);
                }
                return success;

            }
        } catch (SQLException e) {
            logger.error(DatabaseService.class.getSimpleName(), "Error deleting printer config", e);
            throw new DatabaseException("Failed to delete printer config", e);
        }
    }

    // ==================== STICKER HISTORY CRUD OPERATIONS ====================

    /**
     * Inserts a new sticker data record.
     * @param stickerData the sticker data to insert
     * @return the generated ID
     */
    public int insertStickerData(StickerData stickerData) {
        try {
            validateStickerData(stickerData);

            String sql = "INSERT INTO sticker_history (item_name, supplier_name, price, quantity) VALUES (?, ?, ?, ?)";

            try (Connection conn = getConnection();
                  PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                // Sanitize and validate all inputs
                pstmt.setString(1, sanitizeString(stickerData.getItemName(), 100));
                pstmt.setString(2, sanitizeString(stickerData.getSupplierName(), 100));
                pstmt.setBigDecimal(3, validateBigDecimal(stickerData.getPrice(), BigDecimal.ZERO, new BigDecimal("999999.99")));
                pstmt.setInt(4, validateInteger(stickerData.getQuantity(), 1, 1000));

                int affectedRows = pstmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Inserting sticker data failed, no rows affected.");
                }

                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int id = generatedKeys.getInt(1);
                        stickerData.setId(id);
                        logger.info(DatabaseService.class.getSimpleName(), "Sticker data inserted with ID: " + id);
                        return id;
                    } else {
                        throw new SQLException("Inserting sticker data failed, no ID obtained.");
                    }
                }

            }
        } catch (SQLException e) {
            logger.error(DatabaseService.class.getSimpleName(), "Error inserting sticker data", e);
            throw new DatabaseException("Failed to insert sticker data", e);
        }
    }

    /**
     * Retrieves all sticker history records.
     * @return list of sticker data records
     */
    public List<StickerData> getAllStickerHistory() {
        try {
            List<StickerData> history = new ArrayList<>();
            String sql = "SELECT * FROM sticker_history ORDER BY printed_at DESC";

            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {
                    StickerData data = new StickerData();
                    data.setId(rs.getInt("id"));
                    data.setItemName(rs.getString("item_name"));
                    data.setSupplierName(rs.getString("supplier_name"));
                    data.setPrice(rs.getBigDecimal("price"));
                    data.setQuantity(rs.getInt("quantity"));

                    Timestamp printedAt = rs.getTimestamp("printed_at");
                    if (printedAt != null) {
                        data.setPrintedAt(printedAt.toLocalDateTime());
                    }

                    history.add(data);
                }

            }
            return history;
        } catch (SQLException e) {
            logger.error(DatabaseService.class.getSimpleName(), "Error retrieving sticker history", e);
            throw new DatabaseException("Failed to retrieve sticker history", e);
        }
    }

    /**
     * Updates a sticker data record.
     * @param stickerData the sticker data to update
     * @return true if updated successfully
     */
    public boolean updateStickerHistory(StickerData stickerData) {
        try {
            validateStickerData(stickerData);
            if (stickerData.getId() <= 0) {
                throw new IllegalArgumentException("Sticker data ID must be set for update");
            }

            String sql = "UPDATE sticker_history SET item_name = ?, supplier_name = ?, price = ?, quantity = ? WHERE id = ?";

            try (Connection conn = getConnection();
                  PreparedStatement pstmt = conn.prepareStatement(sql)) {

                // Sanitize and validate all inputs
                pstmt.setString(1, sanitizeString(stickerData.getItemName(), 100));
                pstmt.setString(2, sanitizeString(stickerData.getSupplierName(), 100));
                pstmt.setBigDecimal(3, validateBigDecimal(stickerData.getPrice(), BigDecimal.ZERO, new BigDecimal("999999.99")));
                pstmt.setInt(4, validateInteger(stickerData.getQuantity(), 1, 1000));
                pstmt.setInt(5, validateInteger(stickerData.getId(), 1, Integer.MAX_VALUE));

                int affectedRows = pstmt.executeUpdate();
                boolean success = affectedRows > 0;
                if (success) {
                    logger.info(DatabaseService.class.getSimpleName(), "Sticker history updated with ID: " + stickerData.getId());
                }
                return success;

            }
        } catch (SQLException e) {
            logger.error(DatabaseService.class.getSimpleName(), "Error updating sticker history", e);
            throw new DatabaseException("Failed to update sticker history", e);
        }
    }

    /**
     * Deletes a sticker data record by ID.
     * @param id the sticker data ID to delete
     * @return true if deleted successfully
     */
    public boolean deleteStickerHistory(int id) {
        try {
            String sql = "DELETE FROM sticker_history WHERE id = ?";

            try (Connection conn = getConnection();
                  PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setInt(1, validateInteger(id, 1, Integer.MAX_VALUE));
                int affectedRows = pstmt.executeUpdate();
                boolean success = affectedRows > 0;
                if (success) {
                    logger.info(DatabaseService.class.getSimpleName(), "Sticker history deleted with ID: " + id);
                }
                return success;

            }
        } catch (SQLException e) {
            logger.error(DatabaseService.class.getSimpleName(), "Error deleting sticker history", e);
            throw new DatabaseException("Failed to delete sticker history", e);
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Gets the default printer configuration.
     * @return the default printer config or null if not found
     */
    public PrinterConfig getDefaultPrinter() {
        try {
            String sql = "SELECT * FROM printer_config WHERE is_default = 1 LIMIT 1";

            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {

                if (rs.next()) {
                    PrinterConfig config = new PrinterConfig();
                    config.setId(rs.getInt("id"));
                    config.setPrinterName(rs.getString("printer_name"));
                    config.setPaperWidth(rs.getInt("paper_width"));
                    config.setPaperHeight(rs.getInt("paper_height"));
                    config.setDefault(rs.getBoolean("is_default"));

                    Timestamp createdAt = rs.getTimestamp("created_at");
                    if (createdAt != null) {
                        config.setCreatedAt(createdAt.toLocalDateTime());
                    }

                    return config;
                }

            }
        } catch (SQLException e) {
            logger.error(DatabaseService.class.getSimpleName(), "Error retrieving default printer", e);
            throw new DatabaseException("Failed to retrieve default printer", e);
        }

        return null;
    }

    /**
     * Logs a sticker print operation.
     * @param itemName the item name
     * @param supplierName the supplier name
     * @param price the price
     * @param quantity the quantity
     * @return the generated ID
     */
    public int logStickerPrint(String itemName, String supplierName, BigDecimal price, int quantity) {
        // Validate inputs before creating StickerData object
        if (itemName == null || itemName.trim().isEmpty()) {
            throw new IllegalArgumentException("Item name cannot be null or empty");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price must be non-negative");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        // Sanitize inputs
        itemName = sanitizeString(itemName, 100);
        supplierName = sanitizeString(supplierName, 100);
        price = validateBigDecimal(price, BigDecimal.ZERO, new BigDecimal("999999.99"));
        quantity = validateInteger(quantity, 1, 1000);

        StickerData stickerData = new StickerData(itemName, supplierName, price, quantity);
        return insertStickerData(stickerData);
    }

    // ==================== TRANSACTION SUPPORT ====================

    /**
     * Executes operations within a transaction.
     * @param operations the operations to execute
     * @throws Exception if any operation fails
     */
    public void executeInTransaction(TransactionOperations operations) throws Exception {
        Connection conn = null;
        boolean originalAutoCommit = true;
        try {
            conn = getConnection();
            originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            operations.execute(conn);

            conn.commit();
            logger.info(DatabaseService.class.getSimpleName(), "Transaction committed successfully");

        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    logger.warn(DatabaseService.class.getSimpleName(), "Transaction rolled back due to error: " + e.getMessage());
                } catch (SQLException rollbackEx) {
                    logger.error(DatabaseService.class.getSimpleName(), "Error during transaction rollback", rollbackEx);
                    // Re-throw rollback exception
                    throw new DatabaseException("Transaction rollback failed - potential data corruption", rollbackEx);
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(originalAutoCommit);
                    conn.close();
                } catch (SQLException e) {
                    logger.error(DatabaseService.class.getSimpleName(), "Error closing connection after transaction", e);
                }
            }
        }
    }

    /**
     * Functional interface for transaction operations.
     */
    @FunctionalInterface
    public interface TransactionOperations {
        void execute(Connection connection) throws Exception;
    }

    // ==================== VALIDATION METHODS ====================

    private void validateShopProfile(ShopProfile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("Shop profile cannot be null");
        }
        // Use the model's built-in validation
        List<String> errors = profile.validate();
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Shop profile validation failed: " + String.join(", ", errors));
        }
    }

    private void validatePrinterConfig(PrinterConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Printer config cannot be null");
        }
        // Use the model's built-in validation
        List<String> errors = config.validate();
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Printer config validation failed: " + String.join(", ", errors));
        }
    }

    private void validateStickerData(StickerData data) {
        if (data == null) {
            throw new IllegalArgumentException("Sticker data cannot be null");
        }
        // Use the model's built-in validation
        List<String> errors = data.validate();
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Sticker data validation failed: " + String.join(", ", errors));
        }
    }

    /**
     * Sanitizes string input for database operations.
     * Removes potentially dangerous characters and limits length.
     * @param input the input string to sanitize
     * @param maxLength maximum allowed length
     * @return sanitized string or null if input is null
     */
    private String sanitizeString(String input, int maxLength) {
        if (input == null) {
            return null;
        }
        // Remove null bytes and other control characters
        String sanitized = input.replaceAll("\\x00", "").trim();
        // Limit length to prevent buffer overflow attacks
        if (sanitized.length() > maxLength) {
            sanitized = sanitized.substring(0, maxLength);
        }
        return sanitized;
    }

    /**
     * Validates and sanitizes BigDecimal values.
     * @param value the BigDecimal value
     * @param minValue minimum allowed value
     * @param maxValue maximum allowed value
     * @return validated BigDecimal
     */
    private BigDecimal validateBigDecimal(BigDecimal value, BigDecimal minValue, BigDecimal maxValue) {
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        if (value.compareTo(minValue) < 0) {
            throw new IllegalArgumentException("Value cannot be less than " + minValue);
        }
        if (value.compareTo(maxValue) > 0) {
            throw new IllegalArgumentException("Value cannot be greater than " + maxValue);
        }
        return value;
    }

    /**
     * Validates integer values within bounds.
     * @param value the integer value
     * @param minValue minimum allowed value
     * @param maxValue maximum allowed value
     * @return validated integer
     */
    private int validateInteger(int value, int minValue, int maxValue) {
        if (value < minValue) {
            throw new IllegalArgumentException("Value cannot be less than " + minValue);
        }
        if (value > maxValue) {
            throw new IllegalArgumentException("Value cannot be greater than " + maxValue);
        }
        return value;
    }
}