package com.rentar.customer.grpc;

import com.rentar.customer.exceptions.BusinessRuleException;
import com.rentar.customer.exceptions.DuplicateResourceException;
import com.rentar.customer.exceptions.InvalidArgumentException;
import com.rentar.customer.exceptions.ResourceNotFoundException;
import io.grpc.Status;
import io.grpc.StatusException;
import org.springframework.grpc.server.exception.GrpcExceptionHandler;
import org.springframework.stereotype.Component;

/**
 * Traduce las excepciones del dominio a códigos de estado gRPC
 * (equivalente al GlobalExceptionHandler REST del Hito 1).
 * Devolver null deja que Spring gRPC responda INTERNAL / UNKNOWN.
 */
@Component
public class GrpcErrorHandler implements GrpcExceptionHandler {

    @Override
    public StatusException handleException(Throwable exception) {
        Status status = switch (exception) {
            case InvalidArgumentException e -> Status.INVALID_ARGUMENT;
            case ResourceNotFoundException e -> Status.NOT_FOUND;
            case DuplicateResourceException e -> Status.ALREADY_EXISTS;
            case BusinessRuleException e -> Status.FAILED_PRECONDITION;
            default -> null;
        };
        return status == null ? null : status.withDescription(exception.getMessage()).asException();
    }
}
