package com.unicauca.piedrazul.scheduling.internal.domain.exceptions;

public class CitaNoEncontradaException extends RuntimeException {
    public CitaNoEncontradaException(String id) {
        super("No se encontró la cita con id: " + id);
    }
}
