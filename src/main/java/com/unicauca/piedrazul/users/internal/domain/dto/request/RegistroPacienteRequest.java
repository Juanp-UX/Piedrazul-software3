package com.unicauca.piedrazul.users.internal.domain.dto.request;


import com.unicauca.piedrazul.users.dto.PacienteDTO;
import com.unicauca.piedrazul.users.dto.UsuarioDTO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RegistroPacienteRequest {
    private UsuarioDTO usuario;
    private PacienteDTO paciente;
}