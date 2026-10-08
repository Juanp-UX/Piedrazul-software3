package com.unicauca.piedrazul.scheduling.dto;

import lombok.*;

import java.util.List;

/**
 * HU-4.1: historial de citas de un paciente identificado por su cédula.
 * Incluye los datos mínimos del paciente para poder mostrar el encabezado
 * aun cuando el paciente todavía no tenga citas.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistorialCitasPacienteDTO {
    private Long pacienteId;
    private String pacienteNombre;
    private String cedulaIdentidad;
    private List<CitaDTO> citas;
}