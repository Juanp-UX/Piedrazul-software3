package com.unicauca.piedrazul.users.controller;


import com.unicauca.piedrazul.shared.RolUsuario;
import com.unicauca.piedrazul.users.IUsuarioService;
import com.unicauca.piedrazul.users.dto.UsuarioDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Antes de este fix, ningún endpoint validaba el rol de quien llamaba:
 * cualquier usuario autenticado (incluido un paciente) podía listar todos
 * los usuarios, activar/desactivar cuentas ajenas o cambiar el rol de
 * cualquiera (escalada de privilegios). Se restringe a ADMINISTRADOR,
 * salvo la consulta/edición del propio perfil.
 */
@RestController
@RequestMapping("/api/users/usuarios")
public class UsuarioController {

    private final IUsuarioService usuarioService;

    public UsuarioController(IUsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<UsuarioDTO>> listar(@RequestParam(required = false) RolUsuario rol) {
        if (rol != null) {
            return ResponseEntity.ok(usuarioService.listarPorRol(rol));
        }
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    /** Un usuario puede ver su propio perfil; un administrador puede ver cualquiera. */
    @GetMapping("/{id}")
    @PreAuthorize("#id == authentication.principal or hasRole('ADMINISTRADOR')")
    public ResponseEntity<UsuarioDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }

    @GetMapping("/activos/count")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Map<String, Long>> contarActivos() {
        return ResponseEntity.ok(Map.of("total", usuarioService.contarUsuariosActivos()));
    }

    @GetMapping("/{usuarioId}/paciente-id")
    @PreAuthorize("#usuarioId == authentication.principal or hasRole('ADMINISTRADOR')")
    public ResponseEntity<Map<String, Long>> getPacienteId(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(Map.of("pacienteId", usuarioService.buscarPacienteIdPorUsuarioId(usuarioId)));
    }

    /**
     * HU 1.3 - edición de usuario, incluye el rol: exclusivo de ADMINISTRADOR.
     * Antes cualquier usuario autenticado podía llamar este endpoint sobre
     * cualquier {id} y auto-asignarse el rol ADMINISTRADOR.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UsuarioDTO> actualizar(@PathVariable Long id, @RequestBody UsuarioDTO dto) {
        return ResponseEntity.ok(usuarioService.actualizarUsuario(id, dto));
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        usuarioService.desactivarUsuario(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> activar(@PathVariable Long id) {
        usuarioService.activarUsuario(id);
        return ResponseEntity.noContent().build();
    }
}