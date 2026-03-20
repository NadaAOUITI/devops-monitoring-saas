package com.n.devopsmonitoringsaas.exception;

/**
 * Thrown when a tenant exceeds its plan's service limit. Map to HTTP 403 in web layer.
 */
public class PlanLimitExceededException extends RuntimeException {

    public PlanLimitExceededException(String message) {
        super(message);
    }
}
