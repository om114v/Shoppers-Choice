package com.shopper.utils;

import java.util.concurrent.Callable;
import java.util.function.Predicate;

import com.shopper.exceptions.*;

/**
 * Centralized error handling framework with configurable retry policies.
 * Provides a unified way to handle errors across the application with
 * different strategies for different types of operations.
 */
public class ErrorHandlingFramework {
    private final Logger logger = Logger.getInstance();
    private final ConfigManager config = ConfigManager.getInstance();
    private final ErrorRecoveryManager recoveryManager = new ErrorRecoveryManager();

    public enum RetryPolicy {
        NO_RETRY,           // Don't retry
        FIXED_RETRY,        // Fixed number of retries
        EXPONENTIAL_BACKOFF,// Exponential backoff
        CIRCUIT_BREAKER     // Use circuit breaker
    }

    public enum ErrorSeverity {
        LOW,      // Log and continue
        MEDIUM,   // Log and retry
        HIGH,     // Log, retry, and alert
        CRITICAL  // Log, retry, alert, and fail fast
    }

    /**
     * Configuration for error handling behavior.
     */
    public static class ErrorConfig {
        private RetryPolicy retryPolicy = RetryPolicy.EXPONENTIAL_BACKOFF;
        private int maxRetries = 3;
        private long initialDelayMs = 1000;
        private ErrorSeverity severity = ErrorSeverity.MEDIUM;
        private Predicate<Exception> retryCondition = e -> true; // Retry all by default
        private boolean useCircuitBreaker = false;
        private CircuitBreaker circuitBreaker;

        public ErrorConfig retryPolicy(RetryPolicy policy) {
            this.retryPolicy = policy;
            return this;
        }

        public ErrorConfig maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public ErrorConfig initialDelay(long delayMs) {
            this.initialDelayMs = delayMs;
            return this;
        }

        public ErrorConfig severity(ErrorSeverity severity) {
            this.severity = severity;
            return this;
        }

        public ErrorConfig retryCondition(Predicate<Exception> condition) {
            this.retryCondition = condition;
            return this;
        }

        public ErrorConfig useCircuitBreaker(boolean use) {
            this.useCircuitBreaker = use;
            return this;
        }

        public ErrorConfig circuitBreaker(CircuitBreaker breaker) {
            this.circuitBreaker = breaker;
            return this;
        }
    }

    /**
     * Executes an operation with comprehensive error handling.
     * @param operation the operation to execute
     * @param config error handling configuration
     * @return the result of the operation
     * @throws Exception if operation fails after all retry attempts
     */
    public <T> T executeWithErrorHandling(Callable<T> operation, ErrorConfig config) throws Exception {
        Exception lastException = null;
        int attempts = 0;

        // Check circuit breaker if enabled
        if (config.useCircuitBreaker && config.circuitBreaker != null) {
            try {
                return config.circuitBreaker.execute(() -> operation.call());
            } catch (CircuitBreaker.CircuitBreakerOpenException e) {
                handleCircuitBreakerOpen(config, e);
                throw e;
            }
        }

        while (attempts <= config.maxRetries) {
            try {
                T result = operation.call();
                if (attempts > 0) {
                    logger.info("ErrorHandlingFramework", "Operation succeeded after " + attempts + " retries");
                }
                return result;

            } catch (Exception e) {
                lastException = e;
                attempts++;

                if (!shouldRetry(e, config, attempts)) {
                    break;
                }

                handleError(e, config, attempts);

                if (attempts <= config.maxRetries) {
                    long delay = calculateDelay(config, attempts);
                    if (delay > 0) {
                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Operation interrupted during retry delay", ie);
                        }
                    }
                }
            }
        }

        // All retries exhausted
        handleFinalFailure(lastException, config);
        throw lastException;
    }

    /**
     * Determines if an operation should be retried based on the exception and configuration.
     */
    private boolean shouldRetry(Exception e, ErrorConfig config, int attempts) {
        if (attempts > config.maxRetries) {
            return false;
        }

        return config.retryCondition.test(e);
    }

    /**
     * Calculates the delay before the next retry attempt.
     */
    private long calculateDelay(ErrorConfig config, int attempt) {
        switch (config.retryPolicy) {
            case NO_RETRY:
                return 0;
            case FIXED_RETRY:
                return config.initialDelayMs;
            case EXPONENTIAL_BACKOFF:
                return config.initialDelayMs * (long) Math.pow(2, attempt - 1);
            case CIRCUIT_BREAKER:
                return config.initialDelayMs;
            default:
                return config.initialDelayMs;
        }
    }

    /**
     * Handles an error during retry attempts.
     */
    private void handleError(Exception e, ErrorConfig config, int attempt) {
        String message = String.format("Attempt %d failed: %s", attempt, e.getMessage());

        switch (config.severity) {
            case LOW:
                logger.debug("ErrorHandlingFramework", message);
                break;
            case MEDIUM:
                logger.warn("ErrorHandlingFramework", message);
                break;
            case HIGH:
                logger.error("ErrorHandlingFramework", message, e);
                // Could send notification here
                break;
            case CRITICAL:
                logger.error("ErrorHandlingFramework", "CRITICAL: " + message, e);
                // Could trigger alerts, notifications, etc.
                break;
        }

        // Try recovery strategies
        try {
            recoveryManager.executeWithRecovery(e, () -> {
                // Recovery operation - could be reconnection, cleanup, etc.
                logger.info("ErrorHandlingFramework", "Attempting recovery for error: " + e.getMessage());
                return null;
            });
        } catch (Exception recoveryException) {
            logger.warn("ErrorHandlingFramework", "Recovery failed: " + recoveryException.getMessage());
        }
    }

    /**
     * Handles circuit breaker open exceptions.
     */
    private void handleCircuitBreakerOpen(ErrorConfig config, CircuitBreaker.CircuitBreakerOpenException e) {
        logger.warn("ErrorHandlingFramework", "Circuit breaker is open, failing fast");
        if (config.severity == ErrorSeverity.CRITICAL) {
            // Could trigger additional alerts for critical operations
        }
    }

    /**
     * Handles final failure after all retries are exhausted.
     */
    private void handleFinalFailure(Exception e, ErrorConfig config) {
        String message = "Operation failed after " + config.maxRetries + " retries: " + e.getMessage();

        switch (config.severity) {
            case LOW:
                logger.debug("ErrorHandlingFramework", message);
                break;
            case MEDIUM:
                logger.warn("ErrorHandlingFramework", message);
                break;
            case HIGH:
            case CRITICAL:
                logger.error("ErrorHandlingFramework", "FINAL FAILURE: " + message, e);
                ErrorReporter.getInstance().reportError(e, "Operation failed after retries");
                break;
        }
    }

    /**
     * Creates a default error configuration for database operations.
     */
    public ErrorConfig createDatabaseConfig() {
        return new ErrorConfig()
            .retryPolicy(RetryPolicy.EXPONENTIAL_BACKOFF)
            .maxRetries(3)
            .initialDelay(1000)
            .severity(ErrorSeverity.HIGH)
            .retryCondition(e -> e instanceof DatabaseException &&
                !e.getMessage().toLowerCase().contains("permission") &&
                !e.getMessage().toLowerCase().contains("not found"));
    }

    /**
     * Creates a default error configuration for printer operations.
     */
    public ErrorConfig createPrinterConfig() {
        return new ErrorConfig()
            .retryPolicy(RetryPolicy.EXPONENTIAL_BACKOFF)
            .maxRetries(2)
            .initialDelay(2000)
            .severity(ErrorSeverity.HIGH)
            .useCircuitBreaker(true)
            .retryCondition(e -> !(e instanceof PrinterException &&
                e.getMessage().toLowerCase().contains("not found")));
    }

    /**
     * Creates a default error configuration for validation operations.
     */
    public ErrorConfig createValidationConfig() {
        return new ErrorConfig()
            .retryPolicy(RetryPolicy.NO_RETRY)
            .severity(ErrorSeverity.MEDIUM)
            .retryCondition(e -> false); // Don't retry validation errors
    }
}