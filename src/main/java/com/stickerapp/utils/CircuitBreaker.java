package com.stickerapp.utils;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Circuit Breaker implementation for fault tolerance.
 * Prevents cascading failures by temporarily stopping operations when failure rate exceeds threshold.
 */
public class CircuitBreaker {
    public enum State {
        CLOSED,    // Normal operation
        OPEN,      // Circuit is open, failing fast
        HALF_OPEN  // Testing if service recovered
    }

    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);

    private final int failureThreshold;
    private final int successThreshold;
    private final long timeoutMs;
    private final Logger logger;

    public CircuitBreaker(int failureThreshold, int successThreshold, long timeoutMs) {
        this.failureThreshold = failureThreshold;
        this.successThreshold = successThreshold;
        this.timeoutMs = timeoutMs;
        this.logger = Logger.getInstance();
    }

    /**
     * Executes the given operation with circuit breaker protection.
     * @param operation the operation to execute
     * @return the result of the operation
     * @throws Exception if operation fails or circuit is open
     */
    public <T> T execute(Operation<T> operation) throws Exception {
        State currentState = state.get();

        if (currentState == State.OPEN) {
            if (System.currentTimeMillis() - lastFailureTime.get() > timeoutMs) {
                // Try to transition to half-open
                if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                    logger.info("CircuitBreaker", "Transitioning to HALF_OPEN state");
                    currentState = State.HALF_OPEN;
                } else {
                    throw new CircuitBreakerOpenException("Circuit breaker is OPEN");
                }
            } else {
                throw new CircuitBreakerOpenException("Circuit breaker is OPEN");
            }
        }

        try {
            T result = operation.call();
            onSuccess();
            return result;
        } catch (Exception e) {
            onFailure();
            throw e;
        }
    }

    /**
     * Records a successful operation.
     */
    private void onSuccess() {
        failureCount.set(0);
        State currentState = state.get();

        if (currentState == State.HALF_OPEN) {
            int successes = successCount.incrementAndGet();
            if (successes >= successThreshold) {
                if (state.compareAndSet(State.HALF_OPEN, State.CLOSED)) {
                    successCount.set(0);
                    logger.info("CircuitBreaker", "Transitioning to CLOSED state");
                }
            }
        }
    }

    /**
     * Records a failed operation.
     */
    private void onFailure() {
        lastFailureTime.set(System.currentTimeMillis());
        int failures = failureCount.incrementAndGet();

        if (failures >= failureThreshold) {
            if (state.compareAndSet(State.CLOSED, State.OPEN)) {
                logger.warn("CircuitBreaker", "Transitioning to OPEN state after " + failures + " failures");
            }
        }
    }

    /**
     * Gets the current state of the circuit breaker.
     * @return current state
     */
    public State getState() {
        return state.get();
    }

    /**
     * Gets the current failure count.
     * @return failure count
     */
    public int getFailureCount() {
        return failureCount.get();
    }

    /**
     * Resets the circuit breaker to CLOSED state.
     */
    public void reset() {
        state.set(State.CLOSED);
        failureCount.set(0);
        successCount.set(0);
        logger.info("CircuitBreaker", "Circuit breaker reset to CLOSED state");
    }

    /**
     * Functional interface for operations.
     */
    @FunctionalInterface
    public interface Operation<T> {
        T call() throws Exception;
    }

    /**
     * Exception thrown when circuit breaker is open.
     */
    public static class CircuitBreakerOpenException extends Exception {
        public CircuitBreakerOpenException(String message) {
            super(message);
        }
    }
}