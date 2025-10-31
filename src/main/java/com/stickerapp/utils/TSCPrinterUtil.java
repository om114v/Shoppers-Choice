package com.stickerapp.utils;

import com.stickerapp.utils.AppLogger;
import com.stickerapp.models.StickerData;
import com.stickerapp.models.PrinterConfig;
import com.stickerapp.exceptions.PrinterException;
import com.stickerapp.exceptions.PrinterStatusException;

import com.fazecast.jSerialComm.SerialPort;

import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Utility class for TSC printer operations using TSC SDK.
 * Provides low-level interface to TSC thermal printers with helper methods
 * for coordinate conversion, font calculations, text wrapping, price formatting,
 * error recovery, and sticker template generation.
 */
public class TSCPrinterUtil {
    private final AppLogger logger;
    private SerialPort serialPort;
    private boolean connected;
    private final ReentrantLock lock = new ReentrantLock();
    private CompletableFuture<Void> currentPrintJob;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public TSCPrinterUtil() {
        this.logger = AppLogger.getInstance();
        this.connected = false;
    }

    /**
     * Opens the printer port.
     * @param portName the port name (e.g., "USB", "COM1")
     * @param baudRate the baud rate for serial connection
     */
    public void openPort(String portName, int baudRate) {
        SerialPort tempPort = null;
        try {
            logger.info("Opening printer port: " + portName + " with baudrate: " + baudRate);

            if ("USB".equalsIgnoreCase(portName)) {
                // For USB printers, try to find the actual port
                SerialPort[] ports = SerialPort.getCommPorts();
                for (SerialPort port : ports) {
                    if (port.getDescriptivePortName().toLowerCase().contains("usb") ||
                        port.getPortDescription().toLowerCase().contains("printer")) {
                        tempPort = port;
                        break;
                    }
                }
                if (tempPort == null) {
                    throw new RuntimeException("No USB printer port found");
                }
            } else {
                // For serial ports
                tempPort = SerialPort.getCommPort(portName);
            }

            tempPort.setBaudRate(baudRate);
            tempPort.setNumDataBits(8);
            tempPort.setNumStopBits(SerialPort.ONE_STOP_BIT);
            tempPort.setParity(SerialPort.NO_PARITY);

            if (!tempPort.openPort()) {
                throw new RuntimeException("Failed to open serial port: " + portName);
            }

            // Set timeout for read operations
            tempPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 1000, 0);

            // Only assign to instance variable after successful setup
            this.serialPort = tempPort;
            this.connected = true;
            logger.info("Printer port opened successfully: " + portName);
        } catch (Exception e) {
            logger.error("Failed to open printer port: " + portName, e);
            // Close port if it was opened but failed during setup
            if (tempPort != null && tempPort.isOpen()) {
                try {
                    tempPort.closePort();
                } catch (Exception closeEx) {
                    logger.warn("Failed to close port after failed open: " + closeEx.getMessage());
                }
            }
            this.connected = false;
            throw new RuntimeException("Failed to open printer port", e);
        }
    }

    /**
     * Closes the printer port.
     */
    public void closePort() {
        try {
            logger.info("Closing printer port.");
            if (serialPort != null && serialPort.isOpen()) {
                serialPort.closePort();
            }
            this.connected = false;
            logger.info("Printer port closed successfully.");
        } catch (Exception e) {
            logger.error("Failed to close printer port", e);
            throw new RuntimeException("Failed to close printer port", e);
        }
    }

    /**
     * Sends a command to the printer.
     * @param command the TSPL command string
     */
    public void sendCommand(String command) throws PrinterException {
        if (!connected || serialPort == null || !serialPort.isOpen()) {
            throw new IllegalStateException("Printer not connected");
        }

        if (cancelled.get()) {
            throw new CancellationException("Print operation was cancelled");
        }

        logger.debug("Sending command to printer: " + command.replace("\r\n", "\\r\\n"));

        byte[] commandBytes = null;
        try {
            commandBytes = command.getBytes("ASCII");
            int bytesWritten = serialPort.writeBytes(commandBytes, commandBytes.length);

            if (bytesWritten != commandBytes.length) {
                throw new PrinterException("Failed to write complete command to printer");
            }

            // Small delay to ensure command is processed
            Thread.sleep(50);

            logger.debug("Command sent successfully (" + bytesWritten + " bytes).");
        } catch (CancellationException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to send command to printer", e);
            throw new PrinterException("Failed to send command to printer", e);
        }
    }

    /**
     * Checks if the printer is connected.
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        try {
            if (!this.connected || serialPort == null) {
                return false;
            }
            return serialPort.isOpen();
        } catch (Exception e) {
            logger.error("Error checking printer connection", e);
            return false;
        }
    }

    /**
     * Gets the current printer status including paper, ink, and hardware status.
     * @return PrinterStatus enum value
     */
    public PrinterStatus getPrinterStatus() throws PrinterStatusException {
        try {
            if (!isConnected()) {
                return PrinterStatus.OFFLINE;
            }

            // Send status query command
            String statusCommand = "~HS\r\n"; // TSC status command
            sendCommand(statusCommand);

            // Read response (this is simplified - real implementation would parse response)
            byte[] buffer = new byte[32];
            int bytesRead = serialPort.readBytes(buffer, buffer.length);

            if (bytesRead > 0) {
                String response = new String(buffer, 0, bytesRead, "ASCII");
                logger.debug("Printer status response: " + response);

                // Parse status response (simplified)
                if (response.contains("00")) {
                    return PrinterStatus.READY;
                } else if (response.contains("01")) {
                    return PrinterStatus.BUSY;
                } else if (response.contains("02")) {
                    return PrinterStatus.OUT_OF_PAPER;
                } else if (response.contains("03")) {
                    return PrinterStatus.OUT_OF_INK;
                }
            }

            return PrinterStatus.UNKNOWN;
        } catch (Exception e) {
            logger.error("Error getting printer status", e);
            throw new PrinterStatusException(PrinterStatusException.PrinterStatus.ERROR, "Failed to get printer status", e);
        }
    }

    /**
     * Printer status enumeration.
     */
    public enum PrinterStatus {
        READY,
        BUSY,
        OFFLINE,
        OUT_OF_PAPER,
        OUT_OF_INK,
        PAPER_JAM,
        ERROR,
        UNKNOWN
    }

    /**
     * Gets the list of available printer ports.
     * @return list of available port names
     */
    public List<String> getAvailablePorts() {
        List<String> ports = new ArrayList<>();
        try {
            SerialPort[] availablePorts = SerialPort.getCommPorts();

            for (SerialPort port : availablePorts) {
                String portName = port.getSystemPortName();
                String description = port.getDescriptivePortName().toLowerCase();

                // Include ports that might be printers
                if (description.contains("usb") || description.contains("serial") ||
                    description.contains("printer") || portName.startsWith("COM") ||
                    portName.startsWith("tty")) {
                    ports.add(portName);
                }
            }

            // Always include USB as a fallback
            if (!ports.contains("USB")) {
                ports.add(0, "USB");
            }

            logger.info("Retrieved available printer ports: " + ports);
        } catch (Exception e) {
            logger.error("Error retrieving available ports", e);
            // Fallback to common ports
            ports.add("USB");
            ports.add("COM1");
            ports.add("COM2");
            ports.add("COM3");
            ports.add("COM4");
        }
        return ports;
    }

    /**
     * Converts millimeters to dots (TSC printer units).
     * TSC printers typically use 203 DPI (dots per inch).
     * @param mm millimeters
     * @return dots
     */
    public int mmToDots(double mm) {
        // 1 inch = 25.4 mm, 203 DPI
        return (int) Math.round((mm / 25.4) * 203);
    }

    /**
     * Converts dots to millimeters.
     * @param dots printer dots
     * @return millimeters
     */
    public double dotsToMm(int dots) {
        return (dots / 203.0) * 25.4;
    }

    /**
     * Calculates optimal font size based on text length and available width.
     * @param text the text to display
     * @param maxWidthDots maximum width in dots
     * @param baseFontSize base font size
     * @return optimal font size
     */
    public int calculateFontSize(String text, int maxWidthDots, int baseFontSize) {
        if (text == null || text.isEmpty()) {
            return baseFontSize;
        }

        // Estimate character width (rough approximation)
        int estimatedWidth = text.length() * baseFontSize;
        if (estimatedWidth <= maxWidthDots) {
            return baseFontSize;
        }

        // Scale down font size proportionally
        int scaledSize = (int) Math.max(1, (maxWidthDots * baseFontSize) / estimatedWidth);
        logger.debug("Calculated font size for '" + text + "': " + scaledSize);
        return scaledSize;
    }

    /**
     * Wraps text to fit within specified width, breaking at word boundaries.
     * @param text the text to wrap
     * @param maxWidthDots maximum width in dots
     * @param fontSize font size for width calculation
     * @return list of wrapped text lines
     */
    public List<String> wrapText(String text, int maxWidthDots, int fontSize) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return lines;
        }

        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            int estimatedWidth = testLine.length() * fontSize;

            if (estimatedWidth <= maxWidthDots) {
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    // Word is too long, truncate if necessary
                    lines.add(word.substring(0, Math.min(word.length(), maxWidthDots / fontSize)));
                }
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        logger.debug("Wrapped text '" + text + "' into " + lines.size() + " lines");
        return lines;
    }

    /**
     * Formats price with currency symbol and proper decimal places.
     * @param price the price as BigDecimal
     * @param currencySymbol currency symbol (e.g., "₹", "$")
     * @return formatted price string
     */
    public String formatPrice(BigDecimal price, String currencySymbol) {
        if (price == null) {
            return currencySymbol + "0.00";
        }
        return currencySymbol + price.setScale(2, BigDecimal.ROUND_HALF_UP).toString();
    }

    /**
     * Attempts to recover from printer errors by resetting connection and retrying.
     * @param maxRetries maximum number of retry attempts
     * @return true if recovery successful, false otherwise
     */
    public boolean recoverFromError(int maxRetries) {
        lock.lock();
        try {
            for (int i = 0; i < maxRetries; i++) {
                try {
                    logger.info("Attempting error recovery, attempt " + (i + 1) + "/" + maxRetries);

                    // Close and reopen port
                    if (connected) {
                        closePort();
                    }

                    // Wait before retry
                    Thread.sleep(1000 * (i + 1));

                    // Try to reopen (assuming port name is stored or can be retrieved)
                    // For now, assume USB port
                    openPort("USB", 9600);

                    if (isConnected()) {
                        logger.info("Error recovery successful");
                        return true;
                    }
                } catch (Exception e) {
                    logger.warn("Error recovery attempt " + (i + 1) + " failed: " + e.getMessage());
                }
            }
            logger.error("Error recovery failed after " + maxRetries + " attempts");
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Generates TSPL commands for sticker template based on sticker data and printer config.
     * @param stickerData the sticker data
     * @param config the printer configuration
     * @return TSPL command string
     */
    public String generateStickerTemplate(StickerData stickerData, PrinterConfig config) {
        if (stickerData == null || config == null) {
            throw new IllegalArgumentException("StickerData and PrinterConfig cannot be null");
        }

        StringBuilder commands = new StringBuilder();

        // SIZE command (width, height in mm)
        int widthMm = config.getPaperWidth();
        int heightMm = config.getPaperHeight();
        commands.append("SIZE ").append(widthMm).append(" mm, ").append(heightMm).append(" mm\r\n");

        // GAP command (gap between labels)
        commands.append("GAP 3 mm, 0 mm\r\n");

        // DIRECTION command (print direction)
        commands.append("DIRECTION 0\r\n");

        // CLS command (clear image buffer)
        commands.append("CLS\r\n");

        // Calculate positions (convert mm to dots)
        int centerX = mmToDots(widthMm / 2.0);
        int startY = mmToDots(5); // 5mm from top
        int lineHeight = mmToDots(8); // 8mm line height

        // Item Name
        List<String> itemLines = wrapText(stickerData.getItemName(), mmToDots(widthMm - 10), 3);
        for (int i = 0; i < itemLines.size(); i++) {
            commands.append("TEXT ").append(centerX).append(",").append(startY + i * lineHeight)
                    .append(",\"TSS24.BF2\",0,1,1,\"").append(itemLines.get(i)).append("\"\r\n");
        }

        // Supplier Name
        int supplierY = startY + itemLines.size() * lineHeight + mmToDots(3);
        commands.append("TEXT ").append(centerX).append(",").append(supplierY)
                .append(",\"TSS16.BF2\",0,1,1,\"").append(stickerData.getSupplierName()).append("\"\r\n");

        // Price
        int priceY = supplierY + lineHeight;
        String formattedPrice = formatPrice(stickerData.getPrice(), "₹");
        commands.append("TEXT ").append(centerX).append(",").append(priceY)
                .append(",\"TSS32.BF2\",0,2,2,\"").append(formattedPrice).append("\"\r\n");

        // PRINT command
        commands.append("PRINT ").append(stickerData.getNumberOfStickers()).append("\r\n");

        logger.debug("Generated TSPL template for sticker: " + stickerData.getItemName());
        return commands.toString();
    }

    /**
     * Prints sticker using generated template with thread safety and cancellation support.
     * @param stickerData the sticker data
     * @param config the printer configuration
     * @return CompletableFuture for async operation
     */
    public CompletableFuture<Void> printStickerAsync(StickerData stickerData, PrinterConfig config) {
        if (currentPrintJob != null && !currentPrintJob.isDone()) {
            throw new IllegalStateException("Another print job is already in progress");
        }

        cancelled.set(false);
        currentPrintJob = CompletableFuture.runAsync(() -> {
            lock.lock();
            try {
                printStickerWithRetry(stickerData, config, 3);
            } finally {
                lock.unlock();
            }
        });

        return currentPrintJob;
    }

    /**
     * Cancels the current print operation.
     */
    public void cancelPrint() {
        cancelled.set(true);
        if (currentPrintJob != null) {
            currentPrintJob.cancel(true);
        }
        logger.info("Print operation cancelled");
    }

    /**
     * Prints sticker with retry logic and exponential backoff.
     */
    private void printStickerWithRetry(StickerData stickerData, PrinterConfig config, int maxRetries) throws PrinterException {
        int attempt = 0;
        long backoffMs = 1000; // Start with 1 second

        while (attempt <= maxRetries) {
            try {
                if (cancelled.get()) {
                    throw new CancellationException("Print operation was cancelled");
                }

                if (!isConnected()) {
                    throw new IllegalStateException("Printer not connected");
                }

                // Check printer status before printing
                PrinterStatus status = getPrinterStatus();
                if (status != PrinterStatus.READY) {
                    throw new PrinterStatusException(PrinterStatusException.PrinterStatus.ERROR,
                        "Printer not ready: " + status);
                }

                String template = generateStickerTemplate(stickerData, config);
                sendCommand(template);
                logger.info("Sticker printed successfully: " + stickerData.getItemName());
                return;

            } catch (CancellationException e) {
                throw e;
            } catch (Exception e) {
                logger.warn("Print attempt " + (attempt + 1) + " failed: " + e.getMessage());

                if (attempt == maxRetries) {
                    logger.error("Failed to print sticker after " + (maxRetries + 1) + " attempts", e);
                    throw new PrinterException("Failed to print sticker after retries", e);
                }

                // Attempt recovery
                if (recoverFromError(2)) {
                    logger.info("Recovered from error, retrying print...");
                } else {
                    logger.warn("Could not recover from error, waiting before retry...");
                    try {
                        Thread.sleep(backoffMs);
                        backoffMs = Math.min(backoffMs * 2, 30000); // Exponential backoff, max 30s
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new PrinterException("Print operation interrupted", ie);
                    }
                }

                attempt++;
            }
        }
    }

    /**
     * Synchronous print method for backward compatibility.
     */
    public void printSticker(StickerData stickerData, PrinterConfig config) throws PrinterException {
        try {
            printStickerAsync(stickerData, config).get();
        } catch (Exception e) {
            if (e.getCause() instanceof PrinterException) {
                throw (PrinterException) e.getCause();
            }
            throw new PrinterException("Print operation failed", e);
        }
    }
}