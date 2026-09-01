package com.unicauca.piedrazul.scheduling.internal.domain.builder;



import com.unicauca.piedrazul.scheduling.internal.domain.entity.Cita;
import com.unicauca.piedrazul.scheduling.internal.domain.entity.enums.EstadoCita;
import com.unicauca.piedrazul.users.dto.UsuarioDTO;

import java.time.ZonedDateTime;

public class DirectorCita {

    private CitaBuilder citaBuilder;

    public void setCitaBuilder(CitaBuilder citaBuilder) {
        this.citaBuilder = citaBuilder;
    }

    public Cita getCita() {
        return citaBuilder.getCita();
    }

    public void construirCita(UsuarioDTO paciente, UsuarioDTO profesional,
                              ZonedDateTime fechaHora, int duracionMinutos) {
        citaBuilder.iniciarNuevaCita();
        citaBuilder.buildPaciente(paciente);
        citaBuilder.buildProfesional(profesional);
        citaBuilder.buildFechaHora(fechaHora);
        citaBuilder.buildDuracion(duracionMinutos);
        citaBuilder.buildEstado(EstadoCita.programada);
        citaBuilder.buildFechaCreacion();
    }
}
