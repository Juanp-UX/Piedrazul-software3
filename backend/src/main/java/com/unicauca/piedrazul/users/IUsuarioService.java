package com.unicauca.piedrazul.users;


import com.unicauca.piedrazul.shared.RolUsuario;
import com.unicauca.piedrazul.users.dto.UsuarioDTO;
import com.unicauca.piedrazul.users.internal.domain.entity.Usuario;

import java.util.List;
import java.util.Optional;

public interface IUsuarioService {
    Usuario crearUsuarioBase(UsuarioDTO dto);
    List<UsuarioDTO> listarTodos();
    List<UsuarioDTO> listarPorRol(RolUsuario rol);
    UsuarioDTO buscarPorId(Long id);
    /** Usado al autenticar: el login es el claim preferred_username del token de Keycloak. */
    Optional<UsuarioDTO> buscarPorLogin(String login);
    UsuarioDTO actualizarUsuario(Long id, UsuarioDTO dto);
    void          desactivarUsuario(Long id);
    void          activarUsuario(Long id);
    Long buscarPacienteIdPorUsuarioId(Long usuarioId);
    long contarUsuariosActivos();
}