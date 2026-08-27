package com.unicauca.piedrazul.users;




import com.unicauca.piedrazul.users.dto.UsuarioDTO;
import com.unicauca.piedrazul.users.internal.domain.entity.Usuario;
import com.unicauca.piedrazul.shared.RolUsuario;

import java.util.List;

public interface IUsuarioService {
    Usuario crearUsuarioBase(UsuarioDTO dto);
    List<UsuarioDTO> listarTodos();
    List<UsuarioDTO> listarPorRol(RolUsuario rol);
    UsuarioDTO buscarPorId(Long id);
    UsuarioDTO actualizarUsuario(Long id, UsuarioDTO dto);
    void          desactivarUsuario(Long id);
    void          activarUsuario(Long id);
    Long buscarPacienteIdPorUsuarioId(Long usuarioId);
    long contarUsuariosActivos();
}