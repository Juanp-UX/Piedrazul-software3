package com.unicauca.piedrazul.users.internal.domain.dto.request;


import com.unicauca.piedrazul.users.dto.ProfesionalDTO;
import com.unicauca.piedrazul.users.dto.UsuarioDTO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RegistroProfesionalRequest {
    private UsuarioDTO usuario;
    private ProfesionalDTO profesional;
}