package com.unicauca.piedrazul.users.controller;

import com.unicauca.piedrazul.auth.SecurityConfig;
import com.unicauca.piedrazul.auth.security.UsuarioJwtConverter;
import com.unicauca.piedrazul.shared.RolUsuario;
import com.unicauca.piedrazul.shared.test.JwtAuthTestUtils;
import com.unicauca.piedrazul.users.dto.UsuarioDTO;
import com.unicauca.piedrazul.users.internal.application.interfaces.IRegistroService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Con Keycloak el único endpoint público es el autorregistro de pacientes;
 * crear administradores o profesionales exige ser ADMINISTRADOR.
 */
@WebMvcTest(RegistroController.class)
@Import(SecurityConfig.class)
class RegistroControllerTest {

    private static final String BODY_PACIENTE = """
            {"usuario":{"nombreCompleto":"Ana Pérez","login":"ana","rol":"administrador","password":"Secreta123"},
             "paciente":{"nombreCompleto":"Ana Pérez","cedulaIdentidad":"123","telefono":"300","email":"ana@correo.com"}}""";

    private static final String BODY_PROFESIONAL = """
            {"usuario":{"nombreCompleto":"Dr. Gómez","login":"gomez","rol":"profesional","password":"Secreta123"},
             "profesional":{"nombreCompleto":"Dr. Gómez"}}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IRegistroService registroService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private UsuarioJwtConverter usuarioJwtConverter;

    @Test
    void registrarPaciente_esPublico_ignoraElRolDelClienteYNoDevuelveLaContrasena() throws Exception {
        given(registroService.registrarPaciente(any(), any())).willReturn(
                UsuarioDTO.builder().id(1L).login("ana").nombreCompleto("Ana Pérez")
                        .rol(RolUsuario.paciente).activo(true).password("Secreta123").build());

        mockMvc.perform(post("/api/users/registro/paciente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_PACIENTE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist());

        ArgumentCaptor<UsuarioDTO> usuario = ArgumentCaptor.forClass(UsuarioDTO.class);
        verify(registroService).registrarPaciente(usuario.capture(), any());
        // Aunque el cliente envió "administrador", el endpoint público solo crea pacientes.
        assertThat(usuario.getValue().getRol()).isEqualTo(RolUsuario.paciente);
        assertThat(usuario.getValue().getPassword()).isEqualTo("Secreta123");
    }

    @Test
    void registrarProfesional_sinAutenticar_esRechazadoConUnauthorized() throws Exception {
        mockMvc.perform(post("/api/users/registro/profesional")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_PROFESIONAL))
                .andExpect(status().isUnauthorized());

        verify(registroService, never()).registrarProfesional(any(), any());
    }

    @Test
    void registrarProfesional_comoPaciente_esRechazadoConForbidden() throws Exception {
        mockMvc.perform(post("/api/users/registro/profesional")
                        .with(JwtAuthTestUtils.comoPaciente(7L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_PROFESIONAL))
                .andExpect(status().isForbidden());

        verify(registroService, never()).registrarProfesional(any(), any());
    }

    @Test
    void registrarProfesional_comoAdministrador_esPermitido() throws Exception {
        given(registroService.registrarProfesional(any(), any())).willReturn(
                UsuarioDTO.builder().id(2L).login("gomez").rol(RolUsuario.profesional).activo(true).build());

        mockMvc.perform(post("/api/users/registro/profesional")
                        .with(JwtAuthTestUtils.comoAdministrador(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_PROFESIONAL))
                .andExpect(status().isCreated());
    }
}
