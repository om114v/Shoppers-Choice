package com.shopper.utils;

import java.util.concurrent.TimeUnit;

import com.shopper.exceptions.DatabaseException;
import com.shopper.exceptions.PrinterException;
import com.shopper.exceptions.PrinterStatusException;

/**
 * Error recovery manager that implements different recovery strategies
 * for transient vs permanent failures.
 */
public class ErrorRecoveryManager {
    private final Logger logger = Logger.getInstance();
    private final ConfigManager config = ConfigManager.getInstance();

    public enum FailureType {
        TRANSIENT,    // Temporary issues that may resolve themselves
        PERMANENT,    // Issues requiring manual intervention
        UNKNOWN       // Cannot determine failure type
    }

    public enum RecoveryStrategy {
        RETRY_IMMEDIATE,     // Retry immediately
        RETRY_WITH_BACKOFF,  // Retry with exponential backoff
        RECONNECT,          // Reconnect and retry
        FAIL_FAST,          // Don't retry, fail immediately
        MANUAL_INTERVENTION  // Requires manual action
    }

    /**
     * Analyzes an exception to determine the failure type.
     * @param exception the exception to analyze
     * @return the failure type
     */
    public FailureType analyzeFailure(Exception exception) {
        if (exception instanceof PrinterStatusException) {
            PrinterStatusException pse = (PrinterStatusException) exception;
            switch (pse.getStatus()) {
                case OFFLINE:
                case BUSY:
                    return FailureType.TRANSIENT;
                case ERROR:
                    return FailureType.UNKNOWN; // Could be transient or permanent
                default:
                    return FailureType.UNKNOWN;
            }
        } else if (exception instanceof PrinterException) {
            String message = exception.getMessage().toLowerCase();
            if (message.contains("timeout") || message.contains("busy") ||
                message.contains("temporarily") || message.contains("connection")) {
                return FailureType.TRANSIENT;
            } else if (message.contains("not found") || message.contains("invalid") ||
                       message.contains("unsupported")) {
                return FailureType.PERMANENT;
            }
        } else if (exception instanceof DatabaseException) {
            String message = exception.getMessage().toLowerCase();
            if (message.contains("timeout") || message.contains("lock") ||
                message.contains("busy") || message.contains("connection")) {
                return FailureType.TRANSIENT;
            } else if (message.contains("not found") || message.contains("permission") ||
                       message.contains("disk")) {
                return FailureType.PERMANENT;
            }
        }

        return FailureType.UNKNOWN;
    }

    /**
     * Determines the appropriate recovery strategy for a failure type.
     * @param failureType the failure type
     * @return the recovery strategy
     */
    public RecoveryStrategy getRecoveryStrategy(FailureType failureType) {
        switch (failureType) {
            case TRANSIENT:
                return RecoveryStrategy.RETRY_WITH_BACKOFF;
            case PERMANENT:
                return RecoveryStrategy.MANUAL_INTERVENTION;
            case UNKNOWN:
            default:
                return RecoveryStrategy.RETRY_IMMEDIATE;
        }
    }

    /**
     * Executes recovery for a given exception.
     * @param exception the original exception
     * @param operation the operation to retry
     * @return the result of the operation if successful
     * @throws Exception if recovery fails
     */
    public <T> T executeWithRecovery(Exception exception, RecoveryOperation<T> operation) throws Exception {
        FailureType failureType = analyzeFailure(exception);
        RecoveryStrategy strategy = getRecoveryStrategy(failureType);

        logger.info("ErrorRecoveryManager", "Analyzed failure as " + failureType + ", using strategy " + strategy);

        switch (strategy) {
            case RETRY_IMMEDIATE:
                return retryImmediate(operation, 3);
            case RETRY_WITH_BACKOFF:
                return retryWithBackoff(operation, 3, 1000);
            case RECONNECT:
                return retryWithReconnect(operation, 2);
            case FAIL_FAST:
                throw exception;
            case MANUAL_INTERVENTION:
                logger.error("ErrorRecoveryManager", "Manual intervention required for error: " + exception.getMessage());
                throw new RuntimeException("Manual intervention required: " + exception.getMessage(), exception);
            default:
                throw exception;
        }
    }

    /**
     * Retries an operation immediately without delay.
     */
    private <T> T retryImmediate(RecoveryOperation<T> operation, int maxRetries) throws Exception {
        Exception lastException = null;
        for (int i = 0; i < maxRetries; i++) {
            try {
                return operation.execute();
            } catch (Exception e) {
                lastException = e;
                logger.warn("ErrorRecoveryManager", "Retry attempt " + (i + 1) + " failed: " + e.getMessage());
                if (i < maxRetries - 1) {
                    try {
                        Thread.sleep(100); // Small delay between immediate retries
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Recovery interrupted", ie);
                    }
                }
            }
        }
        throw lastException;
    }

    /**
     * Retries an operation with exponential backoff.
     */
    private <T> T retryWithBackoff(RecoveryOperation<T> operation, int maxRetries, long initialDelayMs) throws Exception {
        Exception lastException = null;
        long delay = initialDelayMs;

        for (int i = 0; i < maxRetries; i++) {
            try {
                return operation.execute();
            } catch (Exception e) {
                lastException = e;
                logger.warn("ErrorRecoveryManager", "Retry attempt " + (i + 1) + " failed: " + e.getMessage());

                if (i < maxRetries - 1) {
                    try {
                        Thread.sleep(delay);
                        delay = Math.min(delay * 2, 30000); // Exponential backoff, max 30 seconds
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Recovery interrupted", ie);
                    }
                }
            }
        }
        throw lastException;
    }

    /**
     * Retries an operation after attempting to reconnect.
     */
    private <T> T retryWithReconnect(RecoveryOperation<T> operation, int maxRetries) throws Exception {
        Exception lastException = null;

        for (int i = 0; i < maxRetries; i++) {
            try {
                // Attempt reconnection logic would go here
                // For now, just retry
                return operation.execute();
            } catch (Exception e) {
                lastException = e;
                logger.warn("ErrorRecoveryManager", "Reconnect retry attempt " + (i + 1) + " failed: " + e.getMessage());

                if (i < maxRetries - 1) {
                    try {
                        Thread.sleep(2000); // 2 second delay for reconnection attempts
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Recovery interrupted", ie);
                    }
                }
            }
        }
        throw lastException;
    }

    /**
     * Functional interface for operations that can be retried.
     */
    @FunctionalInterface
    public interface RecoveryOperation<T> {
        T execute() throws Exception;
    }
}