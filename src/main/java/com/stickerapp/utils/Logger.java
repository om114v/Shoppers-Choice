package com.stickerapp.utils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

/**
 * Singleton Logger class using java.util.logging (JUL) for logging to console and file.
 * Supports log levels: DEBUG, INFO, WARN, ERROR.
 * Logs are rotated based on file size (1MB per file, max 5 files).
 * Format: [timestamp] [LEVEL] [class] message
 * Thread-safe enum singleton implementation.
 */
public enum Logger {
    INSTANCE;

    private java.util.logging.Logger julLogger;
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    Logger() {
        julLogger = java.util.logging.Logger.getLogger("StickerPrinterPro");
        julLogger.setLevel(Level.ALL);

        // Remove default handlers
        for (Handler h : julLogger.getHandlers()) {
            julLogger.removeHandler(h);
        }

        // Console handler
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.ALL);
        consoleHandler.setFormatter(new CustomFormatter());
        julLogger.addHandler(consoleHandler);

        // File handler with rotation
        try {
            FileHandler fileHandler = new FileHandler("logs/app.log", 1024 * 1024, 5, true);
            fileHandler.setLevel(Level.ALL);
            fileHandler.setFormatter(new CustomFormatter());
            julLogger.addHandler(fileHandler);
        } catch (IOException e) {
            julLogger.log(Level.SEVERE, "Failed to create file handler", e);
        }
    }

    public static Logger getInstance() {
        return INSTANCE;
    }

    private synchronized void log(Level level, String clazz, String message) {
        julLogger.logp(level, clazz, "", message);
    }

    public synchronized void debug(String clazz, String message) {
        log(Level.FINE, clazz, message);
    }

    public synchronized void info(String clazz, String message) {
        log(Level.INFO, clazz, message);
    }

    public synchronized void warn(String clazz, String message) {
        log(Level.WARNING, clazz, message);
    }

    public synchronized void error(String clazz, String message) {
        log(Level.SEVERE, clazz, message);
    }

    public synchronized void error(String clazz, String message, Exception exception) {
        julLogger.logp(Level.SEVERE, clazz, "", message, exception);
    }

    private static class CustomFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            return String.format("[%s] [%s] [%s] %s%n",
                sdf.format(new Date(record.getMillis())),
                record.getLevel(),
                record.getSourceClassName(),
                record.getMessage());
        }
    }
}