package com.unicauca.piedrazul.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicauca.piedrazul.auth.AuthService;
import com.unicauca.piedrazul.auth.SecurityConfig;
import com.unicauca.piedrazul.auth.dto.AuthResponse;
import com.unicauca.piedrazul.auth.dto.CambioPasswordRequest;
import com.unicauca.piedrazul.auth.dto.LoginRequest;
import com.unicauca.piedrazul.auth.dto.RefreshTokenRequest;
import com.unicauca.piedrazul.auth.security.JwtAuthenticationFilter;
import com.unicauca.piedrazul.shared.test.JwtAuthTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cubre principalmente los fixes de seguridad #1 y #3: antes,
 * /api/auth/logout-all/{usuarioId} era público (permitAll) y
 * /api/auth/password/{usuarioId} no validaba que el usuarioId del path
 * coincidiera con el usuario autenticado. Ahora ambos exigen que sea el
 * propio usuario, o un ADMINISTRADOR.
 */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    // ── login: sigue siendo público ────────────────────────────────────────

    @Test
    void login_sinAutenticacion_esPermitido() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setLogin("paciente@correo.com");
        request.setPassword("secreta123");

        given(authService.login(any())).willReturn(AuthResponse.builder()
                .accessToken("access")
                .refreshToken("refresh")
                .tipo("Bearer")
                .expiresIn(900000L)
                .usuarioId(1L)
                .login("paciente@correo.com")
                .nombreCompleto("Paciente Uno")
                .rol("paciente")
                .build());

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    // ── logout-all: fix #1 ───────────────────────────────────────────────

    @Test
    void logoutAll_sinAutenticacion_esRechazado() throws Exception {
        // Antes de los fixes este endpoint era permitAll(): cualquiera,
        // sin token, podía cerrar la sesión de cualquier usuario. Ahora
        // debe rechazarse (401 sin AuthenticationEntryPoint custom, o 403
        // según el manejo por defecto de Spring Security); lo importante
        // es que ya NO es un 2xx y el servicio nunca se invoca.
        mockMvc.perform(post("/api/auth/logout-all/1"))
                .andExpect(status().is4xxClientError());

        verify(authService, never()).logoutAll(any());
    }

    @Test
    void logoutAll_propioUsuario_esPermitido() throws Exception {
        mockMvc.perform(post("/api/auth/logout-all/42")
                        .with(JwtAuthTestUtils.comoPaciente(42L)))
                .andExpect(status().isNoContent());

        verify(authService).logoutAll(42L);
    }

    @Test
    void logoutAll_otroUsuarioSinSerAdmin_esRechazado() throws Exception {
        // Un usuario autenticado (id=1) intenta cerrar la sesión de otro (id=99).
        mockMvc.perform(post("/api/auth/logout-all/99")
                        .with(JwtAuthTestUtils.comoPaciente(1L)))
                .andExpect(status().isForbidden());

        verify(authService, never()).logoutAll(any());
    }

    @Test
    void logoutAll_administrador_puedeCerrarSesionDeOtroUsuario() throws Exception {
        mockMvc.perform(post("/api/auth/logout-all/99")
                        .with(JwtAuthTestUtils.comoAdministrador(1L)))
                .andExpect(status().isNoContent());

        verify(authService).logoutAll(99L);
    }

    // ── cambiarPassword: fix #3 ─────────────────────────────────────────

    @Test
    void cambiarPassword_otroUsuarioSinSerAdmin_esRechazado() throws Exception {
        CambioPasswordRequest request = new CambioPasswordRequest();
        request.setPasswordActual("actual123");
        request.setPasswordNuevo("nueva12345");

        mockMvc.perform(put("/api/auth/password/99")
                        .with(JwtAuthTestUtils.comoPaciente(1L))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(authService, never()).cambiarPassword(any(), any());
    }

    @Test
    void cambiarPassword_propioUsuario_esPermitido() throws Exception {
        CambioPasswordRequest request = new CambioPasswordRequest();
        request.setPasswordActual("actual123");
        request.setPasswordNuevo("nueva12345");

        mockMvc.perform(put("/api/auth/password/7")
                        .with(JwtAuthTestUtils.comoPaciente(7L))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(authService).cambiarPassword(eq(7L), any());
    }

    @Test
    void cambiarPassword_sinAutenticacion_esRechazado() throws Exception {
        CambioPasswordRequest request = new CambioPasswordRequest();
        request.setPasswordActual("actual123");
        request.setPasswordNuevo("nueva12345");

        mockMvc.perform(put("/api/auth/password/7")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());

        verify(authService, never()).cambiarPassword(any(), any());
    }

    // ── refresh y logout: siguen siendo públicos ────────────────────────

    @Test
    void refresh_sinAutenticacion_esPermitido() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("un-refresh-token");

        given(authService.refresh(any())).willReturn(AuthResponse.builder()
                .accessToken("nuevo-access")
                .refreshToken("nuevo-refresh")
                .tipo("Bearer")
                .expiresIn(900000L)
                .usuarioId(1L)
                .login("paciente@correo.com")
                .nombreCompleto("Paciente Uno")
                .rol("paciente")
                .build());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
