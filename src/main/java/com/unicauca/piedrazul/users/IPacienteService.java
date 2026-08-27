package com.unicauca.piedrazul.users;


import com.unicauca.piedrazul.users.dto.PacienteDTO;
import com.unicauca.piedrazul.users.internal.domain.entity.Usuario;

public interface IPacienteService {
    void crearPaciente(Usuario usuario, PacienteDTO dto);
}
