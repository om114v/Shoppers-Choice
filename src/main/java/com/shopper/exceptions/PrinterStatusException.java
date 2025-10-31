package com.shopper.exceptions;

public class PrinterStatusException extends RuntimeException {

    public enum PrinterStatus {
        OFFLINE,
        BUSY,
        ERROR,
        UNKNOWN
    }

    private PrinterStatus status;

    public PrinterStatusException(String message) {
        super(message);
    }

    public PrinterStatusException(String message, Throwable cause) {
        super(message, cause);
    }

    public PrinterStatusException(PrinterStatus status, String message) {
        super(message);
        this.status = status;
    }

    public PrinterStatusException(PrinterStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public PrinterStatus getStatus() {
        return status;
    }
}