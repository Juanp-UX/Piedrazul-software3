package com.unicauca.piedrazul.users.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


public record ProfesionalCreadoEvent(
        Long profesionalId,
        String nombreCompleto,
        Integer duracionCitaMinutos
) {
}