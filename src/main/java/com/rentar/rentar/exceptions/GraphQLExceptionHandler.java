package com.rentar.rentar.exceptions;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.stream.Collectors;

@Component
public class GraphQLExceptionHandler extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        if (ex instanceof ResourceNotFoundException) {
            return build(ex.getMessage(), ErrorType.NOT_FOUND, env);
        }
        if (ex instanceof DuplicateResourceException) {
            return build(ex.getMessage(), ErrorType.BAD_REQUEST, env);
        }
        if (ex instanceof MethodArgumentNotValidException manv) {
            String mensaje = manv.getBindingResult().getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.joining("; "));
            return build(mensaje.isBlank() ? "Datos inválidos" : mensaje, ErrorType.BAD_REQUEST, env);
        }
        if (ex instanceof jakarta.validation.ConstraintViolationException cve) {
            return build(cve.getMessage(), ErrorType.BAD_REQUEST, env);
        }
        return null; // deja que otros resolvers / el default se encarguen
    }

    private GraphQLError build(String message, ErrorType type, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError(env)
                .message(message)
                .errorType(type)
                .build();
    }
}
