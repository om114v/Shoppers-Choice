package com.stickerapp.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for application logging using SLF4J with Logback.
 */
public class AppLogger {
    private static AppLogger instance;
    private Logger logger;

    private AppLogger() {
        this.logger = LoggerFactory.getLogger(AppLogger.class);
    }

    /**
     * Gets the singleton instance of AppLogger.
     * @return the AppLogger instance
     */
    public static synchronized AppLogger getInstance() {
        if (instance == null) {
            instance = new AppLogger();
        }
        return instance;
    }

    /**
     * Logs an info message.
     * @param message the message to log
     */
    public void info(String message) {
        logger.info(message);
    }

    /**
     * Logs a debug message.
     * @param message the message to log
     */
    public void debug(String message) {
        logger.debug(message);
    }

    /**
     * Logs a warning message.
     * @param message the message to log
     */
    public void warn(String message) {
        logger.warn(message);
    }

    /**
     * Logs an error message.
     * @param message the message to log
     */
    public void error(String message) {
        logger.error(message);
    }

    /**
     * Logs an error message with exception.
     * @param message the message to log
     * @param throwable the exception to log
     */
    public void error(String message, Throwable throwable) {
        logger.error(message, throwable);
    }

    /**
     * Gets the underlying SLF4J logger.
     * @return the SLF4J logger
     */
    public Logger getLogger() {
        return logger;
    }
}