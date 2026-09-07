package com.unicauca.piedrazul.auth.internal.domain.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PasswordInvalidaException extends RuntimeException {
    public PasswordInvalidaException(String mensaje) {
        super(mensaje);
    }
}
