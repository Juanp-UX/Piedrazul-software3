package com.unicauca.piedrazul.scheduling.internal.domain.state;

import com.unicauca.piedrazul.scheduling.internal.domain.entity.Cita;
import com.unicauca.piedrazul.scheduling.internal.domain.entity.enums.EstadoCita;
import com.unicauca.piedrazul.scheduling.internal.domain.exceptions.TransicionEstadoInvalidaException;
import org.springframework.stereotype.Component;

/**
 * Handler del estado <b>{@code completada}</b>.
 *
 * <p>Estado terminal: una cita completada no puede cancelarse
 * ni volver a completarse. Cualquier intento de transición lanza
 * {@link TransicionEstadoInvalidaException}.
 *
 * <pre>
 *   completada ──cancelar()──►  ✗ TransicionEstadoInvalidaException
 *   completada ──completar()──► ✗ TransicionEstadoInvalidaException
 * </pre>
 */
@Component
public class EstadoCompletadaHandler implements EstadoCitaHandler {

    @Override
    public EstadoCita getEstado() {
        return EstadoCita.completada;
    }

    /**
     * Transición inválida: {@code completada → cancelada}.
     *
     * @throws TransicionEstadoInvalidaException siempre.
     */
    @Override
    public void cancelar(Cita cita) {
        throw new TransicionEstadoInvalidaException(
                EstadoCita.completada,
                EstadoCita.cancelada
        );
    }

    /**
     * Transición inválida: {@code completada → completada}.
     *
     * @throws TransicionEstadoInvalidaException siempre.
     */
    @Override
    public void completar(Cita cita) {
        throw new TransicionEstadoInvalidaException(
                EstadoCita.completada,
                EstadoCita.completada
        );
    }
}
