package com.unicauca.piedrazul.scheduling.internal.domain.builder;

import com.unicauca.piedrazul.scheduling.internal.domain.entity.Cita;
import com.unicauca.piedrazul.scheduling.internal.domain.entity.enums.EstadoCita;
import com.unicauca.piedrazul.users.dto.UsuarioDTO;

import java.time.ZonedDateTime;

public abstract class CitaBuilder {

    protected Cita cita;

    public void iniciarNuevaCita() {
        cita = new Cita();
    }

    public Cita getCita() {
        return cita;
    }

    public abstract void buildPaciente(UsuarioDTO paciente);
    public abstract void buildProfesional(UsuarioDTO profesional);
    public abstract void buildFechaHora(ZonedDateTime fechaHora);
    // Added to carry slot duration into the persisted row so overlap queries
    // can compute appointment end-times without re-joining disponibilidad.
    public abstract void buildDuracion(int duracionMinutos);
    public abstract void buildEstado(EstadoCita estado);
    public abstract void buildFechaCreacion();
}
