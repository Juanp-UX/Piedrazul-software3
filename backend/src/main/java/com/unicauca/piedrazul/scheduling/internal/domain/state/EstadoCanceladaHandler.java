package com.unicauca.piedrazul.scheduling.internal.domain.state;


import com.unicauca.piedrazul.scheduling.internal.domain.entity.Cita;
import com.unicauca.piedrazul.scheduling.internal.domain.entity.enums.EstadoCita;
import com.unicauca.piedrazul.scheduling.internal.domain.exceptions.TransicionEstadoInvalidaException;
import org.springframework.stereotype.Component;

/**
 * Handler del estado <b>{@code cancelada}</b>.
 *
 * <p>Estado terminal: una cita cancelada no puede volver a cancelarse
 * ni marcarse como completada. Cualquier intento de transición lanza
 * {@link TransicionEstadoInvalidaException}.
 *
 * <pre>
 *   cancelada ──cancelar()──►  ✗ TransicionEstadoInvalidaException
 *   cancelada ──completar()──► ✗ TransicionEstadoInvalidaException
 * </pre>
 */
@Component
public class EstadoCanceladaHandler implements EstadoCitaHandler {

    @Override
    public EstadoCita getEstado() {
        return EstadoCita.cancelada;
    }

    /**
     * Transición inválida: {@code cancelada → cancelada}.
     *
     * @throws TransicionEstadoInvalidaException siempre.
     */
    @Override
    public void cancelar(Cita cita) {
        throw new TransicionEstadoInvalidaException(
                EstadoCita.cancelada,
                EstadoCita.cancelada
        );
    }

    /**
     * Transición inválida: {@code cancelada → completada}.
     *
     * @throws TransicionEstadoInvalidaException siempre.
     */
    @Override
    public void completar(Cita cita) {
        throw new TransicionEstadoInvalidaException(
                EstadoCita.cancelada,
                EstadoCita.completada
        );
    }
}
