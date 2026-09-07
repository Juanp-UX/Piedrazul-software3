package com.unicauca.piedrazul.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CambioPasswordRequest {

    @NotBlank
    private String passwordActual;

    @NotBlank
    private String passwordNuevo;
}
