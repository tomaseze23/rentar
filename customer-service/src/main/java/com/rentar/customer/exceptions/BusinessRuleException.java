package com.rentar.customer.exceptions;

/**
 * La operación no aplica al estado actual del cliente (ej: reactivar uno ya activo).
 * Se traduce a FAILED_PRECONDITION.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
