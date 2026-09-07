package com.unicauca.piedrazul.scheduling.events;

import java.time.ZonedDateTime;

public record CitaCanceladaEvent(
        Long citaId,
        Long pacienteId,
        Long profesionalId,
        ZonedDateTime fechaHora
) {}