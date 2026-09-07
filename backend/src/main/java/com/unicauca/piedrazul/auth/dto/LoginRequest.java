package com.unicauca.piedrazul.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "El login es obligatorio")
    @Size(max = 50)
    private String login;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
}
