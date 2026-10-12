package com.rentar.customer.exceptions;

/** Credenciales incorrectas o usuario inactivo. Se traduce a UNAUTHENTICATED. */
public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }
}
