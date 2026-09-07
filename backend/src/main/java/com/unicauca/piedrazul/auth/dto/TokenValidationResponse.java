package com.unicauca.piedrazul.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenValidationResponse {
    private boolean valido;
    private Long usuarioId;
    private String login;
    private String rol;
}
