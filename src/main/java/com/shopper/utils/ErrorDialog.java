package com.shopper.utils;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import com.shopper.exceptions.DatabaseException;
import com.shopper.exceptions.FileIOException;
import com.shopper.exceptions.PrinterException;
import com.shopper.exceptions.ValidationException;
import com.shopper.utils.ErrorReporter;

/**
 * User-friendly error dialog for the StickerPrinterPro application.
 * Extends JavaFX Alert to provide customized error handling with icons, titles, and suggestions.
 */
public class ErrorDialog extends Alert {

    public enum ErrorType {
        PRINTER,
        DATABASE,
        VALIDATION,
        FILE_IO,
        GENERAL
    }

    public ErrorDialog(ErrorType errorType, String message, String details) {
        super(AlertType.ERROR);

        setTitle(getTitleForErrorType(errorType));
        setHeaderText(message);
        setContentText(getContentTextForErrorType(errorType, details));

        // Set icon based on error type
        Stage stage = (Stage) getDialogPane().getScene().getWindow();
        stage.getIcons().add(getIconForErrorType(errorType));

        // Make dialog resizable for better readability
        getDialogPane().setPrefWidth(400);

        // Add Report Error button
        getButtonTypes().add(ButtonType.OK);
        getButtonTypes().add(new ButtonType("Report Error"));
    }

    /**
     * Static method to show the error dialog.
     * @param errorType the type of error
     * @param message the error message
     * @param details optional additional details
     */
    public static void show(ErrorType errorType, String message, String details) {
        ErrorDialog dialog = new ErrorDialog(errorType, message, details);
        dialog.showAndWait();
    }

    /**
     * Static method to show the error dialog with reporting option.
     * @param errorType the type of error
     * @param message the error message
     * @param details optional additional details
     * @param exception the exception to report
     */
    public static void showWithReporting(ErrorType errorType, String message, String details, Exception exception) {
        ErrorDialog dialog = new ErrorDialog(errorType, message, details);
        dialog.showAndWait().ifPresent(buttonType -> {
            if (buttonType.getText().equals("Report Error") && exception != null) {
                ErrorReporter.getInstance().reportError(exception, details);
            }
        });
    }

    /**
     * Static method to show the error dialog based on an exception.
     * Determines error type from exception class.
     * @param exception the exception that occurred
     */
    public static void show(Exception exception) {
        ErrorType errorType = determineErrorType(exception);
        String message = exception.getMessage() != null ? exception.getMessage() : "An unexpected error occurred.";
        String details = exception.getCause() != null ? exception.getCause().getMessage() : null;
        show(errorType, message, details);
    }

    /**
     * Static method to show the error dialog based on an exception with reporting option.
     * Determines error type from exception class.
     * @param exception the exception that occurred
     */
    public static void showWithReporting(Exception exception) {
        ErrorType errorType = determineErrorType(exception);
        String message = exception.getMessage() != null ? exception.getMessage() : "An unexpected error occurred.";
        String details = exception.getCause() != null ? exception.getCause().getMessage() : null;
        showWithReporting(errorType, message, details, exception);
    }

    private static ErrorType determineErrorType(Exception exception) {
        if (exception instanceof PrinterException) {
            return ErrorType.PRINTER;
        } else if (exception instanceof DatabaseException) {
            return ErrorType.DATABASE;
        } else if (exception instanceof ValidationException) {
            return ErrorType.VALIDATION;
        } else if (exception instanceof FileIOException) {
            return ErrorType.FILE_IO;
        } else {
            return ErrorType.GENERAL;
        }
    }

    private String getTitleForErrorType(ErrorType errorType) {
        switch (errorType) {
            case PRINTER:
                return "Printer Error";
            case DATABASE:
                return "Database Error";
            case VALIDATION:
                return "Validation Error";
            case FILE_IO:
                return "File I/O Error";
            case GENERAL:
            default:
                return "Error";
        }
    }

    private String getContentTextForErrorType(ErrorType errorType, String details) {
        StringBuilder content = new StringBuilder();
        if (details != null && !details.isEmpty()) {
            content.append(details).append("\n\n");
        }

        switch (errorType) {
            case PRINTER:
                content.append("Suggestions:\n")
                       .append("• Check if the printer is connected and turned on.\n")
                       .append("• Verify printer settings in the application.\n")
                       .append("• Ensure there is sufficient paper and ink.\n")
                       .append("• Try restarting the printer.");
                break;
            case DATABASE:
                content.append("Suggestions:\n")
                       .append("• Check database connection settings.\n")
                       .append("• Ensure the database server is running.\n")
                       .append("• Verify database credentials.\n")
                       .append("• Contact system administrator if issue persists.");
                break;
            case VALIDATION:
                content.append("Suggestions:\n")
                       .append("• Review the input fields for errors.\n")
                       .append("• Ensure all required fields are filled.\n")
                       .append("• Check data formats (e.g., price should be a valid number).");
                break;
            case FILE_IO:
                content.append("Suggestions:\n")
                       .append("• Check file permissions.\n")
                       .append("• Ensure the file path is correct.\n")
                       .append("• Verify disk space availability.\n")
                       .append("• Close any applications using the file.");
                break;
            case GENERAL:
            default:
                content.append("An unexpected error occurred. Please try again or contact support.");
                break;
        }
        return content.toString();
    }

    private Image getIconForErrorType(ErrorType errorType) {
        // For simplicity, using default error icon. In a real app, you might load custom icons.
        // Since JavaFX Alert uses default icons, we'll keep it simple.
        return null; // Alert handles default error icon
    }
}