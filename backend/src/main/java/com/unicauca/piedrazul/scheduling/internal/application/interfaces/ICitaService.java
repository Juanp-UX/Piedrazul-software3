package com.unicauca.piedrazul.scheduling.internal.application.interfaces;


import com.unicauca.piedrazul.scheduling.dto.CitaDTO;
import com.unicauca.piedrazul.scheduling.dto.HistorialCitasPacienteDTO;
import com.unicauca.piedrazul.scheduling.internal.domain.entity.enums.EstadoCita;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

public interface ICitaService {
    CitaDTO agendarCita(CitaDTO dto);
    CitaDTO buscarPorId(Long id);
    List<CitaDTO> listarPorPaciente(Long pacienteId);
    List<CitaDTO> listarPorProfesional(Long profesionalId);

    // HU-6.1: búsqueda de citas de un profesional en una fecha específica
    List<CitaDTO> listarPorProfesionalYFecha(Long profesionalId, LocalDate fecha);

    // HU-1.2: exporta (CSV) las citas de un profesional en una fecha específica
    byte[] exportarCitasCsv(Long profesionalId, LocalDate fecha);

    // HU-4.1: historial de citas de un paciente a partir de su cédula
    HistorialCitasPacienteDTO consultarHistorialPorCedula(String cedula);

    List<ZonedDateTime> obtenerHorariosDisponibles(Long profesionalId, LocalDate fecha);
    CitaDTO cancelarCita(Long id);
    CitaDTO completarCita(Long id);

    // HU-6.3: reprogramar una cita existente
    CitaDTO actualizarCita(Long id, CitaDTO dto);

    long contarCitasPorEstado(EstadoCita estado);
}