package com.shopper.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Centralized resource manager for tracking and cleaning up application resources.
 * Manages file handles, threads, temporary files, and provides JVM shutdown hooks.
 * Thread-safe singleton implementation.
 */
public enum ResourceManager {
    INSTANCE;

    private final List<AutoCloseable> trackedResources = new CopyOnWriteArrayList<>();
    private final List<Path> tempFiles = new CopyOnWriteArrayList<>();
    private final List<ExecutorService> threadPools = new CopyOnWriteArrayList<>();
    private final Logger logger = Logger.getInstance();
    private volatile boolean shutdownInitiated = false;

    ResourceManager() {
        registerShutdownHook();
    }

    /**
     * Gets the singleton instance of ResourceManager.
     * @return the ResourceManager instance
     */
    public static ResourceManager getInstance() {
        return INSTANCE;
    }

    /**
     * Registers a resource for automatic cleanup.
     * @param resource the resource to track
     */
    public void registerResource(AutoCloseable resource) {
        if (resource != null && !shutdownInitiated) {
            trackedResources.add(resource);
            logger.debug(ResourceManager.class.getSimpleName(), "Registered resource: " + resource.getClass().getSimpleName());
        }
    }

    /**
     * Unregisters a resource from tracking.
     * @param resource the resource to remove
     */
    public void unregisterResource(AutoCloseable resource) {
        if (resource != null) {
            trackedResources.remove(resource);
            logger.debug(ResourceManager.class.getSimpleName(), "Unregistered resource: " + resource.getClass().getSimpleName());
        }
    }

    /**
     * Registers a temporary file for cleanup.
     * @param filePath the path to the temporary file
     */
    public void registerTempFile(String filePath) {
        if (filePath != null && !shutdownInitiated) {
            Path path = Paths.get(filePath);
            tempFiles.add(path);
            logger.debug(ResourceManager.class.getSimpleName(), "Registered temp file: " + filePath);
        }
    }

    /**
     * Registers a temporary file for cleanup.
     * @param file the temporary file
     */
    public void registerTempFile(File file) {
        if (file != null) {
            registerTempFile(file.getAbsolutePath());
        }
    }

    /**
     * Registers a thread pool for shutdown management.
     * @param executor the executor service to track
     */
    public void registerThreadPool(ExecutorService executor) {
        if (executor != null && !shutdownInitiated) {
            threadPools.add(executor);
            logger.debug(ResourceManager.class.getSimpleName(), "Registered thread pool: " + executor.getClass().getSimpleName());
        }
    }

    /**
     * Unregisters a thread pool from tracking.
     * @param executor the executor service to remove
     */
    public void unregisterThreadPool(ExecutorService executor) {
        if (executor != null) {
            threadPools.remove(executor);
            logger.debug(ResourceManager.class.getSimpleName(), "Unregistered thread pool: " + executor.getClass().getSimpleName());
        }
    }

    /**
     * Creates a temporary file and registers it for cleanup.
     * @param prefix the prefix for the temp file name
     * @param suffix the suffix for the temp file name
     * @return the created temporary file
     * @throws IOException if file creation fails
     */
    public File createTempFile(String prefix, String suffix) throws IOException {
        File tempFile = File.createTempFile(prefix, suffix);
        registerTempFile(tempFile);
        return tempFile;
    }

    /**
     * Creates a temporary file in a specific directory and registers it for cleanup.
     * @param prefix the prefix for the temp file name
     * @param suffix the suffix for the temp file name
     * @param directory the directory to create the file in
     * @return the created temporary file
     * @throws IOException if file creation fails
     */
    public File createTempFile(String prefix, String suffix, File directory) throws IOException {
        File tempFile = File.createTempFile(prefix, suffix, directory);
        registerTempFile(tempFile);
        return tempFile;
    }

    /**
     * Registers a JVM shutdown hook for graceful resource cleanup.
     */
    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "ResourceManager-Shutdown"));
        logger.info(ResourceManager.class.getSimpleName(), "Shutdown hook registered");
    }

    /**
     * Performs graceful shutdown of all tracked resources.
     */
    public void shutdown() {
        if (shutdownInitiated) {
            return;
        }

        shutdownInitiated = true;
        logger.info(ResourceManager.class.getSimpleName(), "Initiating resource cleanup...");

        // Close tracked resources
        cleanupResources();

        // Shutdown thread pools
        shutdownThreadPools();

        // Clean up temporary files
        cleanupTempFiles();

        logger.info(ResourceManager.class.getSimpleName(), "Resource cleanup completed");
    }

    /**
     * Closes all tracked AutoCloseable resources.
     */
    private void cleanupResources() {
        List<AutoCloseable> resourcesToClose = new ArrayList<>(trackedResources);
        trackedResources.clear();

        for (AutoCloseable resource : resourcesToClose) {
            try {
                resource.close();
                logger.debug(ResourceManager.class.getSimpleName(), "Closed resource: " + resource.getClass().getSimpleName());
            } catch (Exception e) {
                logger.error(ResourceManager.class.getSimpleName(), "Error closing resource: " + resource.getClass().getSimpleName(), e);
            }
        }
    }

    /**
     * Shuts down all tracked thread pools.
     */
    private void shutdownThreadPools() {
        List<ExecutorService> poolsToShutdown = new ArrayList<>(threadPools);
        threadPools.clear();

        for (ExecutorService executor : poolsToShutdown) {
            try {
                executor.shutdown();
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                    if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                        logger.warn(ResourceManager.class.getSimpleName(), "Thread pool did not terminate cleanly");
                    }
                }
                logger.debug(ResourceManager.class.getSimpleName(), "Shut down thread pool: " + executor.getClass().getSimpleName());
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
                logger.error(ResourceManager.class.getSimpleName(), "Thread pool shutdown interrupted", e);
            } catch (Exception e) {
                logger.error(ResourceManager.class.getSimpleName(), "Error shutting down thread pool", e);
            }
        }
    }

    /**
     * Cleans up all registered temporary files.
     */
    private void cleanupTempFiles() {
        List<Path> filesToDelete = new ArrayList<>(tempFiles);
        tempFiles.clear();

        for (Path tempFile : filesToDelete) {
            try {
                if (Files.exists(tempFile)) {
                    Files.delete(tempFile);
                    logger.debug(ResourceManager.class.getSimpleName(), "Deleted temp file: " + tempFile);
                }
            } catch (Exception e) {
                logger.error(ResourceManager.class.getSimpleName(), "Error deleting temp file: " + tempFile, e);
            }
        }
    }

    /**
     * Gets the count of currently tracked resources.
     * @return number of tracked resources
     */
    public int getTrackedResourceCount() {
        return trackedResources.size();
    }

    /**
     * Gets the count of registered temporary files.
     * @return number of temp files
     */
    public int getTempFileCount() {
        return tempFiles.size();
    }

    /**
     * Gets the count of tracked thread pools.
     * @return number of thread pools
     */
    public int getThreadPoolCount() {
        return threadPools.size();
    }

    /**
     * Checks if shutdown has been initiated.
     * @return true if shutdown has started
     */
    public boolean isShutdownInitiated() {
        return shutdownInitiated;
    }
}