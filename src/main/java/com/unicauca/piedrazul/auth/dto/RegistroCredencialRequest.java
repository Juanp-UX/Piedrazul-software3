package com.unicauca.piedrazul.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Recibido internamente desde el módulo users al crear un usuario.
 * Nunca proviene directamente del cliente final.
 */
@Data
public class RegistroCredencialRequest {

    @NotNull(message = "El usuarioId es obligatorio")
    private Long usuarioId;

    @NotBlank(message = "El login es obligatorio")
    @Size(max = 50)
    private String login;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
}
