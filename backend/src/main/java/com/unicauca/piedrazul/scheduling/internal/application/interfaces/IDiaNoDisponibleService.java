package com.unicauca.piedrazul.scheduling.internal.application.interfaces;


import com.unicauca.piedrazul.scheduling.dto.DiaNoDisponibleDTO;

import java.time.LocalDate;
import java.util.List;

/**
 * Contrato del servicio de días no disponibles (festivos y bloqueos manuales).
 */
public interface IDiaNoDisponibleService {

    /**
     * Registra un día no disponible.
     */
    DiaNoDisponibleDTO registrar(DiaNoDisponibleDTO dto);

    /**
     * Lista todos los días no disponibles registrados.
     */
    List<DiaNoDisponibleDTO> listarTodos();

    /**
     * Lista días no disponibles dentro de un rango (para calendario UI).
     */
    List<DiaNoDisponibleDTO> listarEnRango(LocalDate desde, LocalDate hasta);

    /**
     * Elimina la restricción de una fecha, habilitándola nuevamente.
     */
    void eliminar(Long id);

    /**
     * Verifica si una fecha está bloqueada a nivel global.
     */
    boolean esFechaNoDisponible(LocalDate fecha);
}
