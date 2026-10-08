package com.unicauca.piedrazul.users.internal.domain.exceptions;

public class PacienteNoEncontradoException extends RuntimeException {
    public PacienteNoEncontradoException(String cedula) {
        super("No se encontró un paciente con la cédula: " + cedula);
    }
}