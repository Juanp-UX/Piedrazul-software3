package com.unicauca.piedrazul.users.dto;

import com.unicauca.piedrazul.shared.RolUsuario;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioDTO {
    private Long id;
    private String nombreCompleto;
    private String login;
    private RolUsuario rol;
    private Boolean activo;

    /**
     * Solo de entrada (registro): se envía a Keycloak para crear la cuenta de acceso
     * y nunca se guarda en la base de datos ni se devuelve en las respuestas.
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
}
