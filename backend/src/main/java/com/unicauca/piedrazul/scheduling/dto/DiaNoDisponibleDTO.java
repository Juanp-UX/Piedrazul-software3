package com.unicauca.piedrazul.scheduling.dto;

import com.unicauca.piedrazul.scheduling.internal.domain.entity.enums.TipoDiaNoDisponible;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

/**
 * DTO para registrar y consultar días no disponibles (festivos y bloqueos manuales).
 * HU-1.8: el administrador selecciona una fecha y la bloquea globalmente.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiaNoDisponibleDTO {

    private Long id;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    /** Descripción del motivo del bloqueo (opcional). */
    private String motivo;

    @NotNull(message = "El tipo es obligatorio")
    @Builder.Default
    private TipoDiaNoDisponible tipo = TipoDiaNoDisponible.BLOQUEO_MANUAL;
}
