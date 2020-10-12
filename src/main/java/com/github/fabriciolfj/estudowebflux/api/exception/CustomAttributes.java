package com.github.fabriciolfj.estudowebflux.api.exception;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Component
public class CustomAttributes extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
        final Map<String, Object> errors = super.getErrorAttributes(request, options);
        final Throwable throwable = getError(request);

        if (throwable instanceof ResponseStatusException) {
            var ex = (ResponseStatusException) throwable;
            errors.put("message", ex.getMessage());
            errors.put("developerMessage", "A ResponseStatusException Happened");
        }

        return errors;
    }
}
