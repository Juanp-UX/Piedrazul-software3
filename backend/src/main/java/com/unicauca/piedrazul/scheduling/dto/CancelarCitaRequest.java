package com.unicauca.piedrazul.scheduling.dto;

import lombok.*;

/** HU-2.3: cuerpo de la solicitud de cancelación hecha por el paciente. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CancelarCitaRequest {
    private String motivo;
}
