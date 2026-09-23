package com.unicauca.piedrazul.users.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicauca.piedrazul.auth.SecurityConfig;
import com.unicauca.piedrazul.auth.internal.security.JwtUtil;
import com.unicauca.piedrazul.auth.security.JwtAuthenticationFilter;
import com.unicauca.piedrazul.shared.RolUsuario;
import com.unicauca.piedrazul.shared.test.JwtAuthTestUtils;
import com.unicauca.piedrazul.users.IUsuarioService;
import com.unicauca.piedrazul.users.dto.UsuarioDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cubre el fix #2: antes ningún endpoint de UsuarioController validaba el
 * rol de quien llamaba. Un paciente podía listar todos los usuarios,
 * activar/desactivar cuentas ajenas, o editar el rol de cualquiera
 * (incluido auto-asignarse ADMINISTRADOR). Ahora esas operaciones exigen
 * ADMINISTRADOR; consultar el propio perfil sigue permitido para
 * cualquier usuario autenticado.
 */
@WebMvcTest(UsuarioController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IUsuarioService usuarioService;

    // Ver AuthControllerTest: JwtAuthenticationFilter necesita un JwtUtil
    // real; sin mockearlo aquí, el ApplicationContext no arranca.
    @MockitoBean
    private JwtUtil jwtUtil;

    private UsuarioDTO usuarioDTO(Long id, RolUsuario rol) {
        return UsuarioDTO.builder()
                .id(id)
                .nombreCompleto("Usuario " + id)
                .login("usuario" + id + "@correo.com")
                .rol(rol)
                .activo(true)
                .build();
    }

    // ── listar: solo ADMINISTRADOR ──────────────────────────────────────

    @Test
    void listar_comoPaciente_esRechazado() throws Exception {
        mockMvc.perform(get("/api/users/usuarios")
                        .with(JwtAuthTestUtils.comoPaciente(1L)))
                .andExpect(status().isForbidden());

        verify(usuarioService, never()).listarTodos();
    }

    @Test
    void listar_comoAdministrador_esPermitido() throws Exception {
        given(usuarioService.listarTodos())
                .willReturn(List.of(usuarioDTO(1L, RolUsuario.paciente)));

        mockMvc.perform(get("/api/users/usuarios")
                        .with(JwtAuthTestUtils.comoAdministrador(9L)))
                .andExpect(status().isOk());

        verify(usuarioService).listarTodos();
    }

    @Test
    void listar_sinAutenticacion_esRechazado() throws Exception {
        mockMvc.perform(get("/api/users/usuarios"))
                .andExpect(status().is4xxClientError());

        verify(usuarioService, never()).listarTodos();
    }

    // ── buscarPorId: propio usuario o administrador ─────────────────────

    @Test
    void buscarPorId_propioUsuario_esPermitido() throws Exception {
        given(usuarioService.buscarPorId(5L)).willReturn(usuarioDTO(5L, RolUsuario.paciente));

        mockMvc.perform(get("/api/users/usuarios/5")
                        .with(JwtAuthTestUtils.comoPaciente(5L)))
                .andExpect(status().isOk());
    }

    @Test
    void buscarPorId_otroUsuarioSinSerAdmin_esRechazado() throws Exception {
        mockMvc.perform(get("/api/users/usuarios/5")
                        .with(JwtAuthTestUtils.comoPaciente(1L)))
                .andExpect(status().isForbidden());

        verify(usuarioService, never()).buscarPorId(any());
    }

    @Test
    void buscarPorId_administrador_puedeVerCualquierUsuario() throws Exception {
        given(usuarioService.buscarPorId(5L)).willReturn(usuarioDTO(5L, RolUsuario.paciente));

        mockMvc.perform(get("/api/users/usuarios/5")
                        .with(JwtAuthTestUtils.comoAdministrador(1L)))
                .andExpect(status().isOk());
    }

    // ── actualizar (incluye cambio de rol): solo ADMINISTRADOR ──────────

    @Test
    void actualizar_comoPaciente_esRechazado_inclusoSuPropioId() throws Exception {
        // Antes de este fix, un paciente podía llamar a este endpoint sobre
        // su propio id y cambiarse el rol a ADMINISTRADOR. Por eso esta
        // operación NO se permite ni siquiera sobre el propio usuario.
        UsuarioDTO dto = usuarioDTO(1L, RolUsuario.administrador);

        mockMvc.perform(put("/api/users/usuarios/1")
                        .with(JwtAuthTestUtils.comoPaciente(1L))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());

        verify(usuarioService, never()).actualizarUsuario(any(), any());
    }

    @Test
    void actualizar_comoAdministrador_esPermitido() throws Exception {
        UsuarioDTO dto = usuarioDTO(3L, RolUsuario.agendador);
        given(usuarioService.actualizarUsuario(eq(3L), any())).willReturn(dto);

        mockMvc.perform(put("/api/users/usuarios/3")
                        .with(JwtAuthTestUtils.comoAdministrador(9L))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(usuarioService).actualizarUsuario(eq(3L), any());
    }

    // ── activar / desactivar: solo ADMINISTRADOR ────────────────────────

    @Test
    void desactivar_comoAgendador_esRechazado() throws Exception {
        mockMvc.perform(patch("/api/users/usuarios/4/desactivar")
                        .with(JwtAuthTestUtils.comoAgendador(2L)))
                .andExpect(status().isForbidden());

        verify(usuarioService, never()).desactivarUsuario(any());
    }

    @Test
    void desactivar_comoAdministrador_esPermitido() throws Exception {
        mockMvc.perform(patch("/api/users/usuarios/4/desactivar")
                        .with(JwtAuthTestUtils.comoAdministrador(9L)))
                .andExpect(status().isNoContent());

        verify(usuarioService).desactivarUsuario(4L);
    }

    @Test
    void activar_comoPaciente_esRechazado() throws Exception {
        mockMvc.perform(patch("/api/users/usuarios/4/activar")
                        .with(JwtAuthTestUtils.comoPaciente(4L)))
                .andExpect(status().isForbidden());

        verify(usuarioService, never()).activarUsuario(any());
    }
}
