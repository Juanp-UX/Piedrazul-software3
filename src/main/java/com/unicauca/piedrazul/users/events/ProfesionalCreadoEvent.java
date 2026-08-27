package com.unicauca.piedrazul.users.events;


public record ProfesionalCreadoEvent(
        Long profesionalId,
        String nombreCompleto,
        Integer duracionCitaMinutos
) {
}