package com.unicauca.piedrazul.scheduling.internal.application.interfaces;


import com.unicauca.piedrazul.scheduling.dto.DisponibilidadSemanalDTO;

import java.util.List;

/**
 * Contrato del servicio de disponibilidad semanal de profesionales.
 */
public interface IDisponibilidadService {

    /**
     * Crea una nueva configuración de disponibilidad.
     */
    DisponibilidadSemanalDTO crear(DisponibilidadSemanalDTO dto);

    /**
     * Actualiza una configuración de disponibilidad existente.
     */
    DisponibilidadSemanalDTO actualizar(Long id, DisponibilidadSemanalDTO dto);

    /**
     * Lista todas las disponibilidades de un profesional.
     */
    List<DisponibilidadSemanalDTO> listarPorProfesional(Long profesionalId);

    /**
     * Elimina una configuración de disponibilidad.
     */
    void eliminar(Long id);
}
