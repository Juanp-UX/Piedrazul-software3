package com.unicauca.piedrazul.scheduling.dto;

import com.unicauca.piedrazul.scheduling.internal.domain.entity.enums.EstadoCita;
import lombok.*;

import java.time.ZonedDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CitaDTO {
    private Long id;
    private Long pacienteId;
    private String pacienteNombre;
    private Long profesionalId;
    private String profesionalNombre;
    private ZonedDateTime fechaHora;
    private EstadoCita estado;
}
