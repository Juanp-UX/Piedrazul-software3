package com.unicauca.piedrazul.scheduling.internal.domain.state;


import com.unicauca.piedrazul.scheduling.internal.domain.entity.Cita;
import com.unicauca.piedrazul.scheduling.internal.domain.entity.enums.EstadoCita;

/**
 * Interfaz del patrón <b>State</b> para el ciclo de vida de una {@link Cita}.
 *
 * <p>Cada implementación representa un estado concreto de la cita y encapsula
 * las reglas de transición que ese estado permite o prohíbe. De esta forma,
 * la lógica de qué operaciones son válidas en cada momento queda distribuida
 * en clases cohesivas en lugar de acumularse como condicionales en el servicio.
 *
 * <p>Las implementaciones concretas son:
 * <ul>
 *   <li>{@code EstadoProgramadaHandler} — permite {@code cancelar} y {@code completar}.</li>
 *   <li>{@code EstadoCanceladaHandler}  — rechaza cualquier transición (estado terminal).</li>
 *   <li>{@code EstadoCompletadaHandler} — rechaza cualquier transición (estado terminal).</li>
 * </ul>
 *
 * <p><b>Contrato de los métodos de transición:</b> si la transición es válida,
 * el método muta el estado de la {@link Cita} recibida. Si es inválida, lanza
 * {@link com.unicauca.piedrazul.scheduling.internal.domain.exceptions.TransicionEstadoInvalidaException}.
 * El llamador (el servicio de aplicación) es responsable de persistir y publicar
 * el evento correspondiente.
 */
public interface EstadoCitaHandler {

    /**
     * Retorna el valor de {@link EstadoCita} que identifica a este handler.
     * Usado por {@link CitaEstadoResolver} para construir el mapa de despacho.
     */
    EstadoCita getEstado();

    /**
     * Intenta cancelar la cita.
     *
     * @param cita entidad cuyo estado será mutado si la transición es válida.
     *         si el estado actual no permite la cancelación.
     */
    void cancelar(Cita cita);

    /**
     * Intenta marcar la cita como completada.
     *
     * @param cita entidad cuyo estado será mutado si la transición es válida.
     *         si el estado actual no permite la compleción.
     */
    void completar(Cita cita);
}
