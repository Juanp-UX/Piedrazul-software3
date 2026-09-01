package com.unicauca.piedrazul.scheduling.internal.domain.exceptions;

public class HorarioOcupadoException extends RuntimeException {
    public HorarioOcupadoException() {
        super("El profesional no está disponible en ese horario");
    }
}
