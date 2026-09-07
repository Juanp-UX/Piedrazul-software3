package com.unicauca.piedrazul.users.dto;

import com.unicauca.piedrazul.shared.RolUsuario;
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
}
