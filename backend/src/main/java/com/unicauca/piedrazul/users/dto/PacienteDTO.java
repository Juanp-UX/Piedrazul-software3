package com.unicauca.piedrazul.users.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PacienteDTO {
    private Long id;
    /** Id del Usuario asociado (es el que referencian las citas como pacienteId). */
    private Long usuarioId;
    private String nombreCompleto;
    private String cedulaIdentidad;
    private LocalDate fechaNacimiento;
    private String telefono;
    private String email;
    private String direccion;
}