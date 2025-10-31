package com.shopper.exceptions;

public class PrinterException extends RuntimeException {

    public enum PrinterError {
        OUT_OF_PAPER,
        OUT_OF_INK,
        PAPER_JAM,
        OTHER
    }

    private PrinterError error;

    public PrinterException(String message) {
        super(message);
    }

    public PrinterException(String message, Throwable cause) {
        super(message, cause);
    }

    public PrinterException(PrinterError error, String message) {
        super(message);
        this.error = error;
    }

    public PrinterException(PrinterError error, String message, Throwable cause) {
        super(message, cause);
        this.error = error;
    }

    public PrinterError getError() {
        return error;
    }
}