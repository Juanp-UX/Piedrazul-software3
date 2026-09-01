package com.unicauca.piedrazul.scheduling.events;

import java.time.ZonedDateTime;

public record CitaCompletadaEvent(
        Long citaId,
        Long pacienteId,
        Long profesionalId,
        ZonedDateTime fechaHora
) {}