package com.unicauca.piedrazul.scheduling.events;

import java.time.ZonedDateTime;

public record CitaAgendadaEvent(
        Long citaId,
        Long pacienteId,
        String pacienteNombre,
        Long profesionalId,
        String profesionalNombre,
        ZonedDateTime fechaHora
) {}