package com.unicauca.piedrazul.scheduling.internal.domain.builder;



import com.unicauca.piedrazul.scheduling.internal.domain.entity.enums.EstadoCita;
import com.unicauca.piedrazul.users.dto.UsuarioDTO;

import java.time.ZonedDateTime;

public class CitaProgramadaBuilder extends CitaBuilder {

    @Override
    public void buildPaciente(UsuarioDTO paciente) {
        cita.setPacienteId(paciente.getId());
        cita.setPacienteNombre(paciente.getNombreCompleto());
    }

    @Override
    public void buildProfesional(UsuarioDTO profesional) {
        cita.setProfesionalId(profesional.getId());
        cita.setProfesionalNombre(profesional.getNombreCompleto());
    }

    @Override
    public void buildFechaHora(ZonedDateTime fechaHora) {
        cita.setFechaHora(fechaHora);
    }

    @Override
    public void buildDuracion(int duracionMinutos) {
        cita.setDuracionMinutos(duracionMinutos);
    }

    @Override
    public void buildEstado(EstadoCita estado) {
        cita.setEstado(EstadoCita.programada);
    }

    @Override
    public void buildFechaCreacion() {
        cita.setCreadoEn(ZonedDateTime.now());
    }
}
