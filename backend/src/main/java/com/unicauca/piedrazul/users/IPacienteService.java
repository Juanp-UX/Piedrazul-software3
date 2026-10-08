package com.unicauca.piedrazul.users;


import com.unicauca.piedrazul.users.dto.PacienteDTO;
import com.unicauca.piedrazul.users.internal.domain.entity.Usuario;

public interface IPacienteService {
    void crearPaciente(Usuario usuario, PacienteDTO dto);

    /**
     * HU-4.1: identifica a un paciente a partir de su cédula.
     * @throws com.unicauca.piedrazul.users.internal.domain.exceptions.PacienteNoEncontradoException
     *         si no existe un paciente con esa cédula (se traduce a HTTP 404).
     */
    PacienteDTO buscarPorCedula(String cedula);
}