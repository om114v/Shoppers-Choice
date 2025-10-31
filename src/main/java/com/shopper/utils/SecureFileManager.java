package com.shopper.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import com.shopper.exceptions.FileIOException;

/**
 * Utility class for secure file system operations with path validation and access controls.
 * Prevents directory traversal attacks and ensures file operations are within allowed directories.
 */
public class SecureFileManager {
    private static final AppLogger logger = AppLogger.getInstance();

    // Allowed directories for file operations
    private static final List<String> ALLOWED_DIRECTORIES = Arrays.asList(
        "data",
        "logs",
        "temp",
        "images"
    );

    // Allowed file extensions
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
        ".png", ".jpg", ".jpeg", ".gif", ".bmp", ".svg", // Images
        ".txt", ".log", ".properties", // Text files
        ".db", ".sqlite", ".sqlite3" // Database files
    );

    // Maximum file size (10MB)
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    /**
     * Validates if a file path is safe and within allowed directories.
     * @param filePath the file path to validate
     * @return true if the path is safe
     * @throws FileIOException if the path is not safe
     */
    public static boolean validateFilePath(String filePath) throws FileIOException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new FileIOException("File path cannot be null or empty");
        }

        try {
            Path path = Paths.get(filePath).normalize();

            // Check for directory traversal attempts
            if (path.isAbsolute()) {
                // For absolute paths, ensure they are within the application directory
                Path appDir = Paths.get(System.getProperty("user.dir")).normalize();
                if (!path.startsWith(appDir)) {
                    throw new FileIOException("Access to files outside application directory is not allowed");
                }
            }

            // Check for path traversal patterns
            String normalizedPath = path.toString();
            if (normalizedPath.contains("..") || normalizedPath.contains("\\") ||
                normalizedPath.contains(":")) {
                throw new FileIOException("Path traversal detected");
            }

            // Check if file is in allowed directory
            String fileName = path.getFileName().toString().toLowerCase();
            boolean inAllowedDir = false;
            for (String allowedDir : ALLOWED_DIRECTORIES) {
                if (normalizedPath.contains("/" + allowedDir + "/") ||
                    normalizedPath.startsWith(allowedDir + "/") ||
                    normalizedPath.contains("\\" + allowedDir + "\\") ||
                    normalizedPath.startsWith(allowedDir + "\\")) {
                    inAllowedDir = true;
                    break;
                }
            }

            // Allow files directly in allowed directories
            Path parent = path.getParent();
            if (parent != null) {
                String parentName = parent.getFileName().toString().toLowerCase();
                if (ALLOWED_DIRECTORIES.contains(parentName)) {
                    inAllowedDir = true;
                }
            }

            if (!inAllowedDir) {
                throw new FileIOException("File access restricted to allowed directories only");
            }

            // Check file extension
            String extension = getFileExtension(fileName);
            if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
                throw new FileIOException("File type not allowed: " + extension);
            }

            return true;

        } catch (Exception e) {
            if (e instanceof FileIOException) {
                throw e;
            }
            throw new FileIOException("Invalid file path: " + e.getMessage());
        }
    }

    /**
     * Safely creates a file with validation.
     * @param filePath the path to create
     * @return true if file was created successfully
     * @throws FileIOException if creation fails or path is invalid
     */
    public static boolean createFile(String filePath) throws FileIOException {
        validateFilePath(filePath);

        try {
            Path path = Paths.get(filePath);
            Files.createDirectories(path.getParent());

            if (Files.exists(path)) {
                throw new FileIOException("File already exists: " + filePath);
            }

            Files.createFile(path);
            logger.info("File created: " + filePath);
            return true;

        } catch (IOException e) {
            logger.error("Failed to create file: " + filePath + " - " + e.getMessage());
            throw new FileIOException("Failed to create file: " + filePath, e);
        }
    }

    /**
     * Safely reads a file with size validation.
     * @param filePath the path to read
     * @return byte array of file contents
     * @throws FileIOException if reading fails or file is too large
     */
    public static byte[] readFile(String filePath) throws FileIOException {
        validateFilePath(filePath);

        try {
            Path path = Paths.get(filePath);
            File file = path.toFile();

            // Check file size
            if (file.length() > MAX_FILE_SIZE) {
                throw new FileIOException("File too large: " + file.length() + " bytes (max: " + MAX_FILE_SIZE + ")");
            }

            // Check if file is readable
            if (!file.canRead()) {
                throw new FileIOException("File not readable: " + filePath);
            }

            byte[] content = Files.readAllBytes(path);
            logger.debug("File read: " + filePath + " (" + content.length + " bytes)");
            return content;

        } catch (IOException e) {
            logger.error("Failed to read file: " + filePath + " - " + e.getMessage());
            throw new FileIOException("Failed to read file: " + filePath, e);
        }
    }

    /**
     * Safely writes to a file with validation.
     * @param filePath the path to write to
     * @param content the content to write
     * @throws FileIOException if writing fails or path is invalid
     */
    public static void writeFile(String filePath, byte[] content) throws FileIOException {
        validateFilePath(filePath);

        if (content == null) {
            throw new FileIOException("Content cannot be null");
        }

        if (content.length > MAX_FILE_SIZE) {
            throw new FileIOException("Content too large: " + content.length + " bytes (max: " + MAX_FILE_SIZE + ")");
        }

        try {
            Path path = Paths.get(filePath);
            Files.createDirectories(path.getParent());
            Files.write(path, content);
            logger.info("File written: " + filePath + " (" + content.length + " bytes)");

        } catch (IOException e) {
            logger.error("Failed to write file: " + filePath + " - " + e.getMessage());
            throw new FileIOException("Failed to write file: " + filePath, e);
        }
    }

    /**
     * Safely deletes a file with validation.
     * @param filePath the path to delete
     * @return true if file was deleted
     * @throws FileIOException if deletion fails or path is invalid
     */
    public static boolean deleteFile(String filePath) throws FileIOException {
        validateFilePath(filePath);

        try {
            Path path = Paths.get(filePath);
            File file = path.toFile();

            // Additional check: don't delete directories
            if (file.isDirectory()) {
                throw new FileIOException("Cannot delete directories: " + filePath);
            }

            boolean deleted = Files.deleteIfExists(path);
            if (deleted) {
                logger.info("File deleted: " + filePath);
            }
            return deleted;

        } catch (IOException e) {
            logger.error("Failed to delete file: " + filePath + " - " + e.getMessage());
            throw new FileIOException("Failed to delete file: " + filePath, e);
        }
    }

    /**
     * Checks if a file exists and is accessible.
     * @param filePath the path to check
     * @return true if file exists and is accessible
     */
    public static boolean fileExists(String filePath) {
        try {
            validateFilePath(filePath);
            return Files.exists(Paths.get(filePath));
        } catch (FileIOException e) {
            return false;
        }
    }

    /**
     * Gets the file extension from a filename.
     * @param filename the filename
     * @return the file extension including the dot, or empty string if none
     */
    private static String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex) : "";
    }

    /**
     * Sanitizes a filename to prevent injection attacks.
     * @param filename the filename to sanitize
     * @return sanitized filename
     */
    public static String sanitizeFilename(String filename) {
        if (filename == null) {
            return null;
        }

        // Remove path separators and control characters
        return filename.replaceAll("[\\\\/:*?\"<>|]", "")
                      .replaceAll("\\x00-\\x1F", "")
                      .trim();
    }
}