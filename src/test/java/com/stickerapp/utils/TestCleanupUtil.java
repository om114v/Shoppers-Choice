package com.stickerapp.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

/**
 * Utility class for cleaning up test data and temporary files.
 * Provides centralized cleanup functionality for integration tests.
 */
public class TestCleanupUtil {

    /**
     * Cleans up test database files and related artifacts.
     * @param dbPath the database file path
     * @throws IOException if cleanup fails
     */
    public static void cleanupDatabaseFiles(String dbPath) throws IOException {
        if (dbPath == null || dbPath.trim().isEmpty()) {
            return;
        }

        // Delete main database file
        deleteFileIfExists(dbPath);

        // Delete WAL file
        deleteFileIfExists(dbPath + "-wal");

        // Delete SHM file
        deleteFileIfExists(dbPath + "-shm");

        // Delete journal file
        deleteFileIfExists(dbPath + "-journal");
    }

    /**
     * Cleans up temporary files in the data directory.
     * @param dataDir the data directory path
     * @throws IOException if cleanup fails
     */
    public static void cleanupTempFiles(String dataDir) throws IOException {
        Path dataPath = Paths.get(dataDir);
        if (!Files.exists(dataPath)) {
            return;
        }

        // Delete temporary files
        Files.walk(dataPath)
            .filter(Files::isRegularFile)
            .filter(path -> {
                String fileName = path.getFileName().toString().toLowerCase();
                return fileName.endsWith(".tmp") ||
                       fileName.endsWith(".temp") ||
                       fileName.startsWith("temp_") ||
                       fileName.contains("_temp_");
            })
            .forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    // Log but don't fail - cleanup should be best effort
                    System.err.println("Failed to delete temp file: " + path + " - " + e.getMessage());
                }
            });
    }

    /**
     * Cleans up log files created during testing.
     * @param logDir the log directory path
     * @throws IOException if cleanup fails
     */
    public static void cleanupLogFiles(String logDir) throws IOException {
        Path logPath = Paths.get(logDir);
        if (!Files.exists(logPath)) {
            return;
        }

        // Delete test log files
        Files.walk(logPath)
            .filter(Files::isRegularFile)
            .filter(path -> {
                String fileName = path.getFileName().toString().toLowerCase();
                return fileName.startsWith("test") && fileName.endsWith(".log");
            })
            .forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    System.err.println("Failed to delete log file: " + path + " - " + e.getMessage());
                }
            });
    }

    /**
     * Performs comprehensive cleanup of all test artifacts.
     * @param dbPath the database file path
     * @param dataDir the data directory path
     * @param logDir the log directory path
     * @throws IOException if cleanup fails
     */
    public static void cleanupAllTestArtifacts(String dbPath, String dataDir, String logDir) throws IOException {
        cleanupDatabaseFiles(dbPath);
        cleanupTempFiles(dataDir);
        cleanupLogFiles(logDir);
    }

    /**
     * Cleans up a directory recursively.
     * @param dirPath the directory path to clean
     * @throws IOException if cleanup fails
     */
    public static void cleanupDirectory(String dirPath) throws IOException {
        Path path = Paths.get(dirPath);
        if (!Files.exists(path)) {
            return;
        }

        // Delete all files and subdirectories
        Files.walk(path)
            .sorted(Comparator.reverseOrder())
            .forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    System.err.println("Failed to delete: " + p + " - " + e.getMessage());
                }
            });
    }

    /**
     * Deletes a file if it exists.
     * @param filePath the file path
     * @throws IOException if deletion fails
     */
    private static void deleteFileIfExists(String filePath) throws IOException {
        File file = new File(filePath);
        if (file.exists()) {
            if (!file.delete()) {
                throw new IOException("Failed to delete file: " + filePath);
            }
        }
    }

    /**
     * Creates a temporary directory for testing.
     * @param prefix the directory prefix
     * @return the path to the created directory
     * @throws IOException if creation fails
     */
    public static Path createTempDirectory(String prefix) throws IOException {
        return Files.createTempDirectory(prefix);
    }

    /**
     * Creates a temporary file for testing.
     * @param prefix the file prefix
     * @param suffix the file suffix
     * @return the path to the created file
     * @throws IOException if creation fails
     */
    public static Path createTempFile(String prefix, String suffix) throws IOException {
        return Files.createTempFile(prefix, suffix);
    }
}