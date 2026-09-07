package com.unicauca.piedrazul.scheduling.internal.domain.state;


import com.unicauca.piedrazul.scheduling.internal.domain.entity.Cita;
import com.unicauca.piedrazul.scheduling.internal.domain.entity.enums.EstadoCita;
import org.springframework.stereotype.Component;

/**
 * Handler del estado <b>{@code programada}</b>.
 *
 * <p>Es el único estado no terminal: acepta las dos transiciones posibles.
 *
 * <pre>
 *   programada ──cancelar()──►  cancelada
 *   programada ──completar()──► completada
 * </pre>
 *
 * No lanza excepciones: cualquier acción sobre una cita programada es válida.
 */
@Component
public class EstadoProgramadaHandler implements EstadoCitaHandler {

    @Override
    public EstadoCita getEstado() {
        return EstadoCita.programada;
    }

    /**
     * Transición válida: {@code programada → cancelada}.
     */
    @Override
    public void cancelar(Cita cita) {
        cita.setEstado(EstadoCita.cancelada);
    }

    /**
     * Transición válida: {@code programada → completada}.
     */
    @Override
    public void completar(Cita cita) {
        cita.setEstado(EstadoCita.completada);
    }
}
