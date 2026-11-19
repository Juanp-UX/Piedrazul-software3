package com.unicauca.piedrazul.scheduling.internal.domain.exceptions;

/** HU-2.3 SC-3: la cita ya no cumple la anticipación mínima para cancelarse. */
public class CancelacionFueraDePlazoException extends RuntimeException {
    public CancelacionFueraDePlazoException(int diasMinimos) {
        super("La cita ya no puede cancelarse: debe cancelarse con al menos "
                + diasMinimos + " días de anticipación.");
    }
}
