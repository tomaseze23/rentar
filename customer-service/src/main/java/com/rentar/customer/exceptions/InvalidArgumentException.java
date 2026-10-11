package com.rentar.customer.exceptions;

/** Datos de entrada faltantes o con formato inválido. Se traduce a INVALID_ARGUMENT. */
public class InvalidArgumentException extends RuntimeException {

    public InvalidArgumentException(String message) {
        super(message);
    }
}
