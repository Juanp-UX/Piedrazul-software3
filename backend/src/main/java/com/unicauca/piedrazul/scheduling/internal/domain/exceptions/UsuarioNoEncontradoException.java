package com.unicauca.piedrazul.scheduling.internal.domain.exceptions;

public class UsuarioNoEncontradoException extends RuntimeException {
    public UsuarioNoEncontradoException(String id) {
        super("Usuario no encontrado en caché local con id: " + id);
    }
}
