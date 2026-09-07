package com.unicauca.piedrazul.users.internal.domain.exceptions;

public class LoginDuplicadoException extends RuntimeException {
    public LoginDuplicadoException(String login) {
        super("El login '" + login + "' ya está en uso");
    }
}