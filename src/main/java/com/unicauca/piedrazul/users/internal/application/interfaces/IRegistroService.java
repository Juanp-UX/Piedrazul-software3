package com.unicauca.piedrazul.users.internal.application.interfaces;


import com.unicauca.piedrazul.users.dto.PacienteDTO;
import com.unicauca.piedrazul.users.dto.ProfesionalDTO;
import com.unicauca.piedrazul.users.dto.UsuarioDTO;

public interface IRegistroService {
    UsuarioDTO registrarUsuario(UsuarioDTO usuarioDTO);
    UsuarioDTO registrarPaciente(UsuarioDTO usuarioDTO, PacienteDTO pacienteDTO);
    UsuarioDTO registrarProfesional(UsuarioDTO usuarioDTO, ProfesionalDTO profesionalDTO);
}