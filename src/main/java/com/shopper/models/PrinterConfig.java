package com.shopper.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.shopper.exceptions.ValidationException;

/**
 * Represents the printer configuration entity with comprehensive input validation.
 */
public class PrinterConfig {
    private static final Pattern PRINTER_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s_-]{2,50}$");
    private static final Pattern PRINTER_MODEL_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s_-]{0,50}$");
    private static final Pattern PORT_NAME_PATTERN = Pattern.compile("^(COM[0-9]+|USB|LPT[0-9]+|/dev/tty[A-Z0-9]+)$");

    private int id;
    private String printerName;
    private String printerModel;
    private int paperWidth;
    private int paperHeight;
    private int dpi;
    private int darkness;
    private int speed;
    private String portName;
    private int baudRate;
    private boolean isDefault;
    private LocalDateTime createdAt;

    // Constructors
    public PrinterConfig() {}

    public PrinterConfig(String printerName, int paperWidth, int paperHeight, boolean isDefault) throws ValidationException {
        setPrinterName(printerName);
        setPaperWidth(paperWidth);
        setPaperHeight(paperHeight);
        setDefault(isDefault);
        this.dpi = 203; // Default TSC DPI
        this.darkness = 8;
        this.speed = 4;
        this.baudRate = 9600;
    }

    public PrinterConfig(String printerName, String printerModel, int paperWidth, int paperHeight,
                        int dpi, int darkness, int speed, String portName, int baudRate, boolean isDefault) throws ValidationException {
        setPrinterName(printerName);
        setPrinterModel(printerModel);
        setPaperWidth(paperWidth);
        setPaperHeight(paperHeight);
        setDpi(dpi);
        setDarkness(darkness);
        setSpeed(speed);
        setPortName(portName);
        setBaudRate(baudRate);
        setDefault(isDefault);
    }

    // Getters and Setters with validation
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getPrinterName() { return printerName; }
    public void setPrinterName(String printerName) throws ValidationException {
        if (printerName == null || printerName.trim().isEmpty()) {
            throw new ValidationException("Printer name cannot be null or empty");
        }
        printerName = printerName.trim();
        if (!PRINTER_NAME_PATTERN.matcher(printerName).matches()) {
            throw new ValidationException("Printer name contains invalid characters or length");
        }
        this.printerName = printerName;
    }

    public String getPrinterModel() { return printerModel; }
    public void setPrinterModel(String printerModel) throws ValidationException {
        if (printerModel != null && !printerModel.trim().isEmpty()) {
            printerModel = printerModel.trim();
            if (!PRINTER_MODEL_PATTERN.matcher(printerModel).matches()) {
                throw new ValidationException("Printer model contains invalid characters");
            }
        }
        this.printerModel = printerModel;
    }

    public int getPaperWidth() { return paperWidth; }
    public void setPaperWidth(int paperWidth) throws ValidationException {
        if (paperWidth <= 0) {
            throw new ValidationException("Paper width must be positive");
        }
        if (paperWidth > 1000) {
            throw new ValidationException("Paper width cannot exceed 1000mm");
        }
        this.paperWidth = paperWidth;
    }

    public int getPaperHeight() { return paperHeight; }
    public void setPaperHeight(int paperHeight) throws ValidationException {
        if (paperHeight <= 0) {
            throw new ValidationException("Paper height must be positive");
        }
        if (paperHeight > 1000) {
            throw new ValidationException("Paper height cannot exceed 1000mm");
        }
        this.paperHeight = paperHeight;
    }

    public int getDpi() { return dpi; }
    public void setDpi(int dpi) throws ValidationException {
        if (dpi <= 0) {
            throw new ValidationException("DPI must be positive");
        }
        if (dpi < 100 || dpi > 600) {
            throw new ValidationException("DPI must be between 100 and 600");
        }
        this.dpi = dpi;
    }

    public int getDarkness() { return darkness; }
    public void setDarkness(int darkness) throws ValidationException {
        if (darkness < 0 || darkness > 15) {
            throw new ValidationException("Darkness must be between 0 and 15");
        }
        this.darkness = darkness;
    }

    public int getSpeed() { return speed; }
    public void setSpeed(int speed) throws ValidationException {
        if (speed < 1 || speed > 6) {
            throw new ValidationException("Speed must be between 1 and 6");
        }
        this.speed = speed;
    }

    public String getPortName() { return portName; }
    public void setPortName(String portName) throws ValidationException {
        if (portName != null && !portName.trim().isEmpty()) {
            portName = portName.trim().toUpperCase();
            if (!PORT_NAME_PATTERN.matcher(portName).matches()) {
                throw new ValidationException("Invalid port name format");
            }
        }
        this.portName = portName;
    }

    public int getBaudRate() { return baudRate; }
    public void setBaudRate(int baudRate) throws ValidationException {
        if (baudRate <= 0) {
            throw new ValidationException("Baud rate must be positive");
        }
        // Common baud rates for serial communication
        int[] validBaudRates = {9600, 19200, 38400, 57600, 115200};
        boolean valid = false;
        for (int rate : validBaudRates) {
            if (baudRate == rate) {
                valid = true;
                break;
            }
        }
        if (!valid) {
            throw new ValidationException("Invalid baud rate");
        }
        this.baudRate = baudRate;
    }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /**
     * Validates all fields of the printer configuration.
     * @return list of validation errors, empty if valid
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();

        try {
            // Validate printer name
            if (printerName == null || printerName.trim().isEmpty()) {
                errors.add("Printer name is required");
            } else if (!PRINTER_NAME_PATTERN.matcher(printerName).matches()) {
                errors.add("Printer name contains invalid characters or length");
            }

            // Validate printer model if provided
            if (printerModel != null && !printerModel.trim().isEmpty() &&
                !PRINTER_MODEL_PATTERN.matcher(printerModel).matches()) {
                errors.add("Printer model contains invalid characters");
            }

            // Validate paper dimensions
            if (paperWidth <= 0) {
                errors.add("Paper width must be positive");
            } else if (paperWidth > 1000) {
                errors.add("Paper width cannot exceed 1000mm");
            }

            if (paperHeight <= 0) {
                errors.add("Paper height must be positive");
            } else if (paperHeight > 1000) {
                errors.add("Paper height cannot exceed 1000mm");
            }

            // Validate DPI
            if (dpi <= 0) {
                errors.add("DPI must be positive");
            } else if (dpi < 100 || dpi > 600) {
                errors.add("DPI must be between 100 and 600");
            }

            // Validate darkness
            if (darkness < 0 || darkness > 15) {
                errors.add("Darkness must be between 0 and 15");
            }

            // Validate speed
            if (speed < 1 || speed > 6) {
                errors.add("Speed must be between 1 and 6");
            }

            // Validate port name if provided
            if (portName != null && !portName.trim().isEmpty() &&
                !PORT_NAME_PATTERN.matcher(portName).matches()) {
                errors.add("Invalid port name format");
            }

            // Validate baud rate
            if (baudRate <= 0) {
                errors.add("Baud rate must be positive");
            } else {
                int[] validBaudRates = {9600, 19200, 38400, 57600, 115200};
                boolean valid = false;
                for (int rate : validBaudRates) {
                    if (baudRate == rate) {
                        valid = true;
                        break;
                    }
                }
                if (!valid) {
                    errors.add("Invalid baud rate");
                }
            }

        } catch (Exception e) {
            errors.add("Validation error: " + e.getMessage());
        }

        return errors;
    }

    @Override
    public String toString() {
        return "PrinterConfig{" +
                "printerName='" + printerName + '\'' +
                ", printerModel='" + printerModel + '\'' +
                ", paperWidth=" + paperWidth +
                ", paperHeight=" + paperHeight +
                ", dpi=" + dpi +
                ", darkness=" + darkness +
                ", speed=" + speed +
                ", portName='" + portName + '\'' +
                ", baudRate=" + baudRate +
                ", isDefault=" + isDefault +
                '}';
    }
}