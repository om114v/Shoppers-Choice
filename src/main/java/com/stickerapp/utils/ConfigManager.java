package com.stickerapp.utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Properties;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import com.stickerapp.exceptions.FileIOException;

/**
 * Utility class for managing application configuration with encryption support.
 * Loads properties from environment-specific application.properties file and user.properties for overrides.
 * Supports environment variable fallbacks for production configuration.
 * Sensitive data is encrypted using AES encryption.
 * Thread-safe enum singleton implementation with resource cleanup.
 */
public enum ConfigManager {
    INSTANCE;

    private Properties defaultProperties;
    private Properties userProperties;
    private Properties properties;
    private static final String USER_CONFIG_FILE = "data/user.properties";
    private static final String ENV_PROFILE = System.getProperty("app.profile", System.getenv("APP_PROFILE"));
    private static final String DEFAULT_PROFILE = "dev"; // Default to dev if no profile specified

    // Encryption settings
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final String ENCRYPTION_KEY_ENV = "CONFIG_ENCRYPTION_KEY";
    private static final String ENCRYPTION_PREFIX = "{encrypted}";

    // Sensitive property keys that should be encrypted
    private static final String[] SENSITIVE_KEYS = {
        "db.password", "api.key", "secret.key", "encryption.key"
    };

    ConfigManager() {
        loadProperties();
        // Register for cleanup - ConfigManager doesn't need explicit cleanup but registers for consistency
        ResourceManager.getInstance().registerResource(() -> {
            AppLogger.getInstance().info("ConfigManager cleanup completed");
        });
    }

    /**
     * Gets the singleton instance of ConfigManager.
     * @return the ConfigManager instance
     */
    public static ConfigManager getInstance() {
        return INSTANCE;
    }

    /**
     * Loads properties from the environment-specific application.properties file and user.properties.
     * Supports environment variable fallbacks for production configuration.
     * @throws FileIOException if file operations fail
     */
    private void loadProperties() throws FileIOException {
        defaultProperties = new Properties();

        // Determine which properties file to load based on environment
        String profile = ENV_PROFILE != null ? ENV_PROFILE.toLowerCase() : DEFAULT_PROFILE;
        String configFile = "config/application-" + profile + ".properties";

        // Fallback to default if profile-specific file doesn't exist
        if (getClass().getClassLoader().getResourceAsStream(configFile) == null) {
            configFile = "config/application.properties";
        }

        try (InputStream input = getClass().getClassLoader().getResourceAsStream(configFile)) {
            if (input == null) {
                AppLogger.getInstance().error("Unable to find " + configFile);
                throw new FileIOException("Unable to find " + configFile);
            }
            defaultProperties.load(input);
        } catch (IOException e) {
            AppLogger.getInstance().error("Error loading " + configFile, e);
            throw new FileIOException("Error loading " + configFile, e);
        }

        // Resolve environment variables in properties
        resolveEnvironmentVariables(defaultProperties);

        userProperties = new Properties(defaultProperties);
        try (FileInputStream fis = new FileInputStream(USER_CONFIG_FILE)) {
            userProperties.load(fis);
        } catch (IOException e) {
            // User config not found, use defaults
        }

        properties = userProperties;
    }

    /**
     * Saves user properties to the user.properties file with encryption for sensitive data.
     * @throws FileIOException if file write operation fails
     */
    private void saveProperties() throws FileIOException {
        try (FileOutputStream fos = new FileOutputStream(USER_CONFIG_FILE)) {
            // Create a copy for saving with encryption
            Properties encryptedProperties = new Properties();

            for (String key : userProperties.stringPropertyNames()) {
                String value = userProperties.getProperty(key);
                if (isSensitiveKey(key) && value != null && !value.startsWith(ENCRYPTION_PREFIX)) {
                    // Encrypt sensitive values
                    try {
                        value = ENCRYPTION_PREFIX + encrypt(value);
                    } catch (Exception e) {
                        AppLogger.getInstance().warn("Failed to encrypt property: " + key + " - " + e.getMessage());
                        // Continue without encryption if encryption fails
                    }
                }
                encryptedProperties.setProperty(key, value);
            }

            encryptedProperties.store(fos, "User configuration (sensitive data encrypted)");
        } catch (IOException e) {
            AppLogger.getInstance().error("Error saving user properties", e);
            throw new FileIOException("Error saving user properties", e);
        }
    }

    /**
     * Gets a property value by key, with environment variable fallback and decryption.
     * @param key the property key
     * @return the property value or environment variable value if property is null
     */
    public String getProperty(String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            // Try environment variable fallback for production
            String envKey = key.toUpperCase().replace(".", "_");
            value = System.getenv(envKey);
        } else if (value.startsWith(ENCRYPTION_PREFIX)) {
            // Decrypt encrypted values
            try {
                value = decrypt(value.substring(ENCRYPTION_PREFIX.length()));
            } catch (Exception e) {
                AppLogger.getInstance().error("Failed to decrypt property: " + key, e);
                value = null; // Return null if decryption fails
            }
        }
        return value;
    }

    /**
     * Gets a property value by key with a default value.
     * @param key the property key
     * @param defaultValue the default value
     * @return the property value or default value
     */
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * Sets a property value and saves it.
     * @param key the property key
     * @param value the property value
     * @throws FileIOException if saving fails
     */
    public synchronized void setProperty(String key, String value) throws FileIOException {
        userProperties.setProperty(key, value);
        saveProperties();
    }

    /**
     * Gets an integer property value by key.
     * @param key the property key
     * @param defaultValue the default value
     * @return the integer property value or default value
     */
    public int getIntProperty(String key, int defaultValue) {
        String value = getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                AppLogger.getInstance().warn("Invalid integer value for key: " + key + ", using default: " + defaultValue);
            }
        }
        return defaultValue;
    }

    /**
     * Gets the database path.
     * @return the database path
     */
    public String getDatabasePath() {
        return getProperty("db.url");
    }

    /**
     * Sets the database path.
     * @param path the database path
     */
    public void setDatabasePath(String path) {
        setProperty("db.url", path);
    }

    /**
     * Gets the printer name.
     * @return the printer name
     */
    public String getPrinterName() {
        return getProperty("printer.name", "");
    }

    /**
     * Sets the printer name.
     * @param name the printer name
     */
    public void setPrinterName(String name) {
        setProperty("printer.name", name);
    }

    /**
     * Gets the UI theme.
     * @return the UI theme
     */
    public String getTheme() {
        return getProperty("ui.theme", "light");
    }

    /**
     * Sets the UI theme.
     * @param theme the UI theme
     */
    public void setTheme(String theme) {
        setProperty("ui.theme", theme);
    }

    /**
     * Gets the UI font size.
     * @return the UI font size
     */
    public int getFontSize() {
        return getIntProperty("ui.font.size", 12);
    }

    /**
     * Sets the UI font size.
     * @param size the UI font size
     */
    public void setFontSize(int size) {
        setProperty("ui.font.size", String.valueOf(size));
    }

    /**
     * Gets the last used shop ID.
     * @return the last used shop ID
     */
    public String getLastUsedShopId() {
        return getProperty("last.used.shop.id", "");
    }

    /**
     * Sets the last used shop ID.
     * @param id the last used shop ID
     */
    public void setLastUsedShopId(String id) {
        setProperty("last.used.shop.id", id);
    }

    /**
     * Gets the last used printer config ID.
     * @return the last used printer config ID
     */
    public String getLastUsedPrinterConfigId() {
        return getProperty("last.used.printer.config.id", "");
    }

    /**
     * Sets the last used printer config ID.
     * @param id the last used printer config ID
     */
    public void setLastUsedPrinterConfigId(String id) {
        setProperty("last.used.printer.config.id", id);
    }

    /**
     * Resolves environment variables in property values.
     * Supports ${VAR_NAME:default_value} syntax.
     * @param props the properties to resolve
     */
    private void resolveEnvironmentVariables(Properties props) {
        for (String key : props.stringPropertyNames()) {
            String value = props.getProperty(key);
            if (value != null && value.contains("${")) {
                String resolvedValue = resolvePlaceholders(value);
                props.setProperty(key, resolvedValue);
            }
        }
    }

    /**
     * Resolves placeholders in the format ${VAR_NAME:default_value}.
     * @param value the value containing placeholders
     * @return the resolved value
     */
    private String resolvePlaceholders(String value) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < value.length()) {
            if (value.startsWith("${", i)) {
                int endIndex = value.indexOf("}", i);
                if (endIndex != -1) {
                    String placeholder = value.substring(i + 2, endIndex);
                    String resolved = resolvePlaceholder(placeholder);
                    result.append(resolved);
                    i = endIndex + 1;
                } else {
                    result.append(value.charAt(i));
                    i++;
                }
            } else {
                result.append(value.charAt(i));
                i++;
            }
        }
        return result.toString();
    }

    /**
     * Resolves a single placeholder.
     * @param placeholder the placeholder content (VAR_NAME or VAR_NAME:default)
     * @return the resolved value
     */
    private String resolvePlaceholder(String placeholder) {
        String[] parts = placeholder.split(":", 2);
        String varName = parts[0];
        String defaultValue = parts.length > 1 ? parts[1] : "";

        String envValue = System.getenv(varName);
        return envValue != null ? envValue : defaultValue;
    }

    /**
     * Checks if a property key contains sensitive data that should be encrypted.
     * @param key the property key
     * @return true if the key is sensitive
     */
    private boolean isSensitiveKey(String key) {
        if (key == null) return false;
        for (String sensitiveKey : SENSITIVE_KEYS) {
            if (key.toLowerCase().contains(sensitiveKey.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Encrypts a string using AES encryption.
     * @param value the value to encrypt
     * @return the encrypted value as Base64 string
     * @throws Exception if encryption fails
     */
    private String encrypt(String value) throws Exception {
        if (value == null || value.isEmpty()) {
            return value;
        }

        SecretKeySpec keySpec = new SecretKeySpec(getEncryptionKey().getBytes(StandardCharsets.UTF_8), ALGORITHM);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);

        byte[] encryptedBytes = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    /**
     * Decrypts a Base64 encoded string using AES decryption.
     * @param encryptedValue the encrypted value as Base64 string
     * @return the decrypted value
     * @throws Exception if decryption fails
     */
    private String decrypt(String encryptedValue) throws Exception {
        if (encryptedValue == null || encryptedValue.isEmpty()) {
            return encryptedValue;
        }

        SecretKeySpec keySpec = new SecretKeySpec(getEncryptionKey().getBytes(StandardCharsets.UTF_8), ALGORITHM);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, keySpec);

        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedValue));
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    /**
     * Gets the encryption key from environment variable or generates a default one.
     * @return the encryption key
     */
    private String getEncryptionKey() {
        String key = System.getenv(ENCRYPTION_KEY_ENV);
        if (key != null && !key.trim().isEmpty()) {
            // Ensure key is exactly 16, 24, or 32 bytes for AES
            key = key.trim();
            if (key.length() < 16) {
                key = String.format("%-16s", key).substring(0, 16);
            } else if (key.length() > 32) {
                key = key.substring(0, 32);
            } else if (key.length() != 16 && key.length() != 24 && key.length() != 32) {
                // Pad or truncate to 16 bytes
                key = String.format("%-16s", key).substring(0, 16);
            }
            return key;
        }

        // Generate a default key based on application path (not secure for production!)
        try {
            String appPath = System.getProperty("user.dir", "StickerPrinterPro");
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(appPath.getBytes(StandardCharsets.UTF_8));
            // Take first 16 bytes for AES-128
            byte[] keyBytes = new byte[16];
            System.arraycopy(hash, 0, keyBytes, 0, 16);
            return Base64.getEncoder().encodeToString(keyBytes);
        } catch (NoSuchAlgorithmException e) {
            // Fallback to a hardcoded key (not recommended for production)
            AppLogger.getInstance().warn("Using fallback encryption key - NOT SECURE FOR PRODUCTION");
            return "DefaultKey123456"; // 16 bytes
        }
    }

    /**
     * Reloads the properties from the files.
     */
    public synchronized void reload() {
        loadProperties();
    }
}