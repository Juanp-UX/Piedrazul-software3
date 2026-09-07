package com.unicauca.piedrazul.users.internal.domain.exceptions;

public class UsuarioNoEncontradoException extends RuntimeException {
    public UsuarioNoEncontradoException(String login) {
        super("No se encontró el usuario: " + login);
    }
}