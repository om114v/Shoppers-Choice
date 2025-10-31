package com.shopper.services;

import com.shopper.exceptions.PrinterException;
import com.shopper.exceptions.PrinterStatusException;
import com.shopper.models.PrinterConfig;
import com.shopper.models.ShopProfile;
import com.shopper.models.StickerData;
import com.shopper.utils.CircuitBreaker;
import com.shopper.utils.ConfigManager;
import com.shopper.utils.ErrorRecoveryManager;
import com.shopper.utils.ErrorReporter;
import com.shopper.utils.Logger;
import com.shopper.utils.TSCPrinterUtil;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

/**
 * Service class for handling printer operations using TSC SDK.
 * Integrates with TSCPrinterUtil for low-level printer interactions.
 */
public class PrinterService {
    private final TSCPrinterUtil printerUtil;
    private final ConfigManager configManager;
    private final Logger logger;
    private final CircuitBreaker circuitBreaker;
    private final ErrorRecoveryManager errorRecoveryManager;
    private PrinterConfig currentConfig;

    public PrinterService() {
        this.printerUtil = new TSCPrinterUtil();
        this.configManager = ConfigManager.getInstance();
        this.logger = Logger.getInstance();
        // Initialize circuit breaker with configurable parameters
        int failureThreshold = configManager.getIntProperty("circuitbreaker.failure.threshold", 5);
        int successThreshold = configManager.getIntProperty("circuitbreaker.success.threshold", 3);
        long timeoutMs = 60000L; // 1 minute default - ConfigManager doesn't have getLongProperty
        this.circuitBreaker = new CircuitBreaker(failureThreshold, successThreshold, timeoutMs);
        this.errorRecoveryManager = new ErrorRecoveryManager();
        this.currentConfig = loadPrinterConfig();
    }

    /**
     * Loads printer configuration from config manager.
     */
    private PrinterConfig loadPrinterConfig() {
        String portName = configManager.getProperty("printer.port", "USB");
        int baudRate = configManager.getIntProperty("printer.baudrate", 9600);
        int paperWidth = configManager.getIntProperty("printer.paper.width", 50);
        int paperHeight = configManager.getIntProperty("printer.paper.height", 30);
        int dpi = configManager.getIntProperty("printer.dpi", 203);
        int darkness = configManager.getIntProperty("printer.darkness", 8);
        int speed = configManager.getIntProperty("printer.speed", 4);
        String model = configManager.getProperty("printer.model", "TSC");

        return new PrinterConfig("Default Printer", model, paperWidth, paperHeight,
                                dpi, darkness, speed, portName, baudRate, true);
    }

    /**
     * Initializes the printer by opening the configured port.
     */
    public void initializePrinter() {
        try {
            logger.info(PrinterService.class.getSimpleName(), "Initializing printer with config: " + currentConfig);
            printerUtil.openPort(currentConfig.getPortName(), currentConfig.getBaudRate());
            logger.info(PrinterService.class.getSimpleName(), "Printer initialized successfully.");
        } catch (Exception e) {
            logger.error(PrinterService.class.getSimpleName(), "Failed to initialize printer", e);
            ErrorReporter.getInstance().reportError(e, "Printer initialization failed");
            throw new PrinterException("Failed to initialize printer", e);
        }
    }

    /**
     * Gets the current printer status.
     * @return printer status
     */
    public TSCPrinterUtil.PrinterStatus getPrinterStatus() throws PrinterStatusException {
        try {
            return printerUtil.getPrinterStatus();
        } catch (Exception e) {
            logger.error(PrinterService.class.getSimpleName(), "Error getting printer status", e);
            throw new PrinterStatusException(PrinterStatusException.PrinterStatus.ERROR, "Failed to get printer status", e);
        }
    }

    /**
     * Sets the printer configuration.
     * @param config the new printer configuration
     */
    public void setPrinterConfig(PrinterConfig config) {
        this.currentConfig = config;
        logger.info(PrinterService.class.getSimpleName(), "Printer configuration updated: " + config);
    }

    /**
     * Gets the current printer configuration.
     * @return current printer config
     */
    public PrinterConfig getPrinterConfig() {
        return currentConfig;
    }

    /**
     * Checks if the printer is connected.
     * @return true if connected, false otherwise
     * @throws PrinterStatusException if printer is offline or has status issues
     */
    public boolean isPrinterConnected() throws PrinterStatusException {
        try {
            if (!printerUtil.isConnected()) {
                throw new PrinterStatusException(PrinterStatusException.PrinterStatus.OFFLINE, "Printer is not connected");
            }
            return true;
        } catch (PrinterStatusException e) {
            throw e;
        } catch (Exception e) {
            logger.error(PrinterService.class.getSimpleName(), "Error checking printer connection", e);
            throw new PrinterStatusException(PrinterStatusException.PrinterStatus.ERROR, "Failed to check printer connection", e);
        }
    }

    /**
     * Gets the list of available printers (ports).
     * @return list of available printer names
     * @throws PrinterStatusException if unable to retrieve printer list
     */
    public List<String> getAvailablePrinters() throws PrinterStatusException {
        try {
            return printerUtil.getAvailablePorts();
        } catch (Exception e) {
            logger.error(PrinterService.class.getSimpleName(), "Error retrieving available printers", e);
            throw new PrinterStatusException(PrinterStatusException.PrinterStatus.ERROR, "Failed to retrieve available printers", e);
        }
    }

    /**
     * Prints a single sticker with the given data and profile.
     * Uses circuit breaker to prevent cascading failures.
     * @param data the sticker data
     * @param profile the shop profile
     * @throws PrinterException if printing fails
     * @throws PrinterStatusException if printer is not connected
     */
    public void printSticker(StickerData data, ShopProfile profile) throws PrinterException, PrinterStatusException {
        try {
            circuitBreaker.execute(() -> {
                isPrinterConnected(); // This will throw if not connected
                printerUtil.printSticker(data, currentConfig);
                logger.info(PrinterService.class.getSimpleName(), "Sticker printed successfully for item: " + data.getItemName());
                return null;
            });
        } catch (CircuitBreaker.CircuitBreakerOpenException e) {
            logger.error(PrinterService.class.getSimpleName(), "Circuit breaker is OPEN - skipping print operation");
            throw new PrinterException("Printer service temporarily unavailable due to repeated failures", e);
        } catch (PrinterStatusException e) {
            // Try recovery for printer status issues
            try {
                errorRecoveryManager.executeWithRecovery(e, () -> {
                    isPrinterConnected(); // This will throw if not connected
                    printerUtil.printSticker(data, currentConfig);
                    logger.info(PrinterService.class.getSimpleName(), "Sticker printed successfully after recovery for item: " + data.getItemName());
                    return null;
                });
            } catch (Exception recoveryException) {
                logger.error(PrinterService.class.getSimpleName(), "Recovery failed for printer status exception", recoveryException);
                throw e; // Re-throw original exception
            }
        } catch (Exception e) {
            // Try recovery for other exceptions
            try {
                errorRecoveryManager.executeWithRecovery(e, () -> {
                    isPrinterConnected(); // This will throw if not connected
                    printerUtil.printSticker(data, currentConfig);
                    logger.info(PrinterService.class.getSimpleName(), "Sticker printed successfully after recovery for item: " + data.getItemName());
                    return null;
                });
            } catch (Exception recoveryException) {
                logger.error(PrinterService.class.getSimpleName(), "Recovery failed, reporting error", recoveryException);
                ErrorReporter.getInstance().reportError(e, "Sticker printing failed for item: " + data.getItemName());
                throw new PrinterException("Failed to print sticker", e);
            }
        }
    }

    /**
     * Prints a single sticker asynchronously with cancellation support.
     * @param data the sticker data
     * @param profile the shop profile
     * @return CompletableFuture for the print operation
     */
    public CompletableFuture<Void> printStickerAsync(StickerData data, ShopProfile profile) {
        return CompletableFuture.runAsync(() -> {
            try {
                printSticker(data, profile);
            } catch (Exception e) {
                logger.error(PrinterService.class.getSimpleName(), "Async print failed", e);
                throw new RuntimeException("Async print failed", e);
            }
        });
    }

    /**
     * Cancels any ongoing print operations.
     */
    public void cancelPrintOperations() {
        printerUtil.cancelPrint();
        logger.info(PrinterService.class.getSimpleName(), "Print operations cancelled");
    }

    /**
     * Prints multiple stickers with the given data and profile.
     * Uses circuit breaker to prevent cascading failures.
     * @param data the sticker data
     * @param quantity the number of stickers to print
     * @param profile the shop profile
     * @throws PrinterException if printing fails
     * @throws PrinterStatusException if printer is not connected
     */
    public void printMultipleStickers(StickerData data, int quantity, ShopProfile profile) throws PrinterException, PrinterStatusException {
        try {
            circuitBreaker.execute(() -> {
                isPrinterConnected(); // This will throw if not connected

                // Update quantity in sticker data for batch printing
                StickerData batchData = new StickerData(
                    data.getItemName(),
                    data.getSupplierName(),
                    data.getPrice(),
                    quantity  // Set quantity for batch printing
                );

                printerUtil.printSticker(batchData, currentConfig);
                logger.info(PrinterService.class.getSimpleName(), "Printed " + quantity + " stickers for item: " + data.getItemName());
                return null;
            });
        } catch (CircuitBreaker.CircuitBreakerOpenException e) {
            logger.error(PrinterService.class.getSimpleName(), "Circuit breaker is OPEN - skipping batch print operation");
            throw new PrinterException("Printer service temporarily unavailable due to repeated failures", e);
        } catch (PrinterStatusException e) {
            throw e;
        } catch (Exception e) {
            logger.error(PrinterService.class.getSimpleName(), "Failed to print multiple stickers", e);
            ErrorReporter.getInstance().reportError(e, "Multiple sticker printing failed for item: " + data.getItemName() + ", quantity: " + quantity);
            throw new PrinterException("Failed to print multiple stickers", e);
        }
    }

    /**
     * Generates TSPL commands for printing a sticker (legacy method for compatibility).
     * @param data the sticker data
     * @param profile the shop profile
     * @return the TSPL command string
     * @deprecated Use TSCPrinterUtil.generateStickerTemplate instead
     */
    @Deprecated
    public String generateTSPLCommands(StickerData data, ShopProfile profile) {
        return printerUtil.generateStickerTemplate(data, currentConfig);
    }

    /**
     * Handles printer errors by logging and attempting recovery.
     */
    public void handlePrinterErrors() {
        try {
            logger.warn(PrinterService.class.getSimpleName(), "Handling printer errors. Attempting to reset connection.");
            printerUtil.closePort();
            initializePrinter();
        } catch (Exception e) {
            logger.error(PrinterService.class.getSimpleName(), "Failed to handle printer errors", e);
            ErrorReporter.getInstance().reportError(e, "Printer error handling failed");
            throw new PrinterException("Failed to handle printer errors", e);
        }
    }

    /**
     * Gets the current circuit breaker state.
     * @return the circuit breaker state
     */
    public CircuitBreaker.State getCircuitBreakerState() {
        return circuitBreaker.getState();
    }

    /**
     * Resets the circuit breaker to allow operations again.
     */
    public void resetCircuitBreaker() {
        circuitBreaker.reset();
        logger.info(PrinterService.class.getSimpleName(), "Circuit breaker reset manually");
    }

    /**
     * Handles printer errors with specific exception.
     * @param e the exception that occurred
     */
    private void handlePrinterErrors(Exception e) {
        logger.error(PrinterService.class.getSimpleName(), "Printer error occurred: " + e.getMessage(), e);
        // Additional error handling logic can be added here, e.g., notify user, retry, etc.
    }
}