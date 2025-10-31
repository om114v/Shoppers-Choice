package com.shopper.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.shopper.utils.ResourceManager;

/**
 * Thread pool manager for managing background operations.
 * Provides centralized thread pool management for the application.
 * Thread-safe enum singleton with resource cleanup.
 */
public enum ThreadPoolManager {
    INSTANCE;

    private ExecutorService backgroundExecutor;
    private ScheduledExecutorService scheduledExecutor;
    private final Logger logger = Logger.getInstance();

    ThreadPoolManager() {
        initializeThreadPools();
        // Register thread pools for cleanup
        ResourceManager.getInstance().registerThreadPool(backgroundExecutor);
        ResourceManager.getInstance().registerThreadPool(scheduledExecutor);
    }

    /**
     * Initializes thread pools with appropriate configurations.
     */
    private void initializeThreadPools() {
        // Background executor for general tasks
        int backgroundThreads = Math.max(1, ConfigManager.getInstance().getIntProperty("threadpool.background.size", 4));
        backgroundExecutor = Executors.newFixedThreadPool(backgroundThreads,
            r -> {
                Thread t = new Thread(r);
                t.setName("Background-Worker");
                t.setDaemon(true);
                return t;
            });

        // Scheduled executor for periodic tasks
        int scheduledThreads = Math.max(1, ConfigManager.getInstance().getIntProperty("threadpool.scheduled.size", 2));
        scheduledExecutor = Executors.newScheduledThreadPool(scheduledThreads,
            r -> {
                Thread t = new Thread(r);
                t.setName("Scheduled-Worker");
                t.setDaemon(true);
                return t;
            });

        logger.info(ThreadPoolManager.class.getSimpleName(), "Thread pools initialized");
    }

    /**
     * Gets the background executor service.
     * @return the background executor
     */
    public ExecutorService getBackgroundExecutor() {
        return backgroundExecutor;
    }

    /**
     * Gets the scheduled executor service.
     * @return the scheduled executor
     */
    public ScheduledExecutorService getScheduledExecutor() {
        return scheduledExecutor;
    }

    /**
     * Submits a task to the background executor.
     * @param task the task to execute
     */
    public void executeBackground(Runnable task) {
        backgroundExecutor.execute(task);
    }

    /**
     * Schedules a task to run after a delay.
     * @param task the task to execute
     * @param delay the delay
     * @param unit the time unit
     */
    public void schedule(Runnable task, long delay, TimeUnit unit) {
        scheduledExecutor.schedule(task, delay, unit);
    }

    /**
     * Schedules a task to run periodically.
     * @param task the task to execute
     * @param initialDelay the initial delay
     * @param period the period
     * @param unit the time unit
     */
    public void scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
        scheduledExecutor.scheduleAtFixedRate(task, initialDelay, period, unit);
    }

    /**
     * Shuts down all thread pools gracefully.
     */
    public void shutdown() {
        try {
            backgroundExecutor.shutdown();
            scheduledExecutor.shutdown();

            // Wait for existing tasks to complete
            if (!backgroundExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                backgroundExecutor.shutdownNow();
            }
            if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }

            logger.info(ThreadPoolManager.class.getSimpleName(), "Thread pools shut down");
        } catch (InterruptedException e) {
            backgroundExecutor.shutdownNow();
            scheduledExecutor.shutdownNow();
            Thread.currentThread().interrupt();
            logger.error(ThreadPoolManager.class.getSimpleName(), "Thread pool shutdown interrupted", e);
        }
    }

    /**
     * Gets the singleton instance.
     * @return the ThreadPoolManager instance
     */
    public static ThreadPoolManager getInstance() {
        return INSTANCE;
    }
}