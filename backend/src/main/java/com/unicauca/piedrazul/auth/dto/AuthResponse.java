package com.unicauca.piedrazul.auth.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Respuesta de login y refresh.
 * El cliente (Angular) debe guardar ambos tokens de forma segura.
 */
@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tipo;
    private Long expiresIn;       // milisegundos hasta expiración del access token
    private Long usuarioId;
    private String login;
    private String nombreCompleto;
    private String rol;
}
