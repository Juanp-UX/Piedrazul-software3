package com.unicauca.piedrazul.scheduling.controller;

import com.unicauca.piedrazul.auth.SecurityConfig;
import com.unicauca.piedrazul.auth.security.JwtAuthenticationFilter;
import com.unicauca.piedrazul.scheduling.AgendamientoFacade;
import com.unicauca.piedrazul.scheduling.dto.CitaDTO;
import com.unicauca.piedrazul.scheduling.internal.domain.entity.enums.EstadoCita;
import com.unicauca.piedrazul.shared.test.JwtAuthTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.ZonedDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cubre el fix #2 en la parte de citas: antes cualquier usuario
 * autenticado podía ver las citas de cualquier paciente o profesional
 * cambiando el id en la URL (IDOR), y cualquiera podía cancelar/completar
 * citas. Ahora:
 *  - listar por paciente/profesional: solo el propio usuario, o un
 *    AGENDADOR/ADMINISTRADOR.
 *  - cancelar/completar/actualizar/contar: solo AGENDADOR/ADMINISTRADOR.
 */
@WebMvcTest(CitaController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class CitaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AgendamientoFacade agendamientoFacade;

    private CitaDTO citaDTO(Long id) {
        return CitaDTO.builder()
                .id(id)
                .pacienteId(5L)
                .profesionalId(10L)
                .fechaHora(ZonedDateTime.now())
                .estado(EstadoCita.programada)
                .build();
    }

    // ── listarPorPaciente: IDOR corregido ───────────────────────────────

    @Test
    void listarPorPaciente_propioUsuario_esPermitido() throws Exception {
        given(agendamientoFacade.listarCitasPorPaciente(5L)).willReturn(List.of(citaDTO(1L)));

        mockMvc.perform(get("/api/scheduling/citas/paciente/5")
                        .with(JwtAuthTestUtils.comoPaciente(5L)))
                .andExpect(status().isOk());
    }

    @Test
    void listarPorPaciente_otroPacienteSinRolOperativo_esRechazado() throws Exception {
        // Antes: cualquier usuario autenticado podía ver las citas de
        // cualquier paciente con solo cambiar el id en la URL.
        mockMvc.perform(get("/api/scheduling/citas/paciente/5")
                        .with(JwtAuthTestUtils.comoPaciente(1L)))
                .andExpect(status().isForbidden());

        verify(agendamientoFacade, never()).listarCitasPorPaciente(any());
    }

    @Test
    void listarPorPaciente_comoAgendador_esPermitido() throws Exception {
        given(agendamientoFacade.listarCitasPorPaciente(5L)).willReturn(List.of(citaDTO(1L)));

        mockMvc.perform(get("/api/scheduling/citas/paciente/5")
                        .with(JwtAuthTestUtils.comoAgendador(2L)))
                .andExpect(status().isOk());
    }

    @Test
    void listarPorPaciente_sinAutenticacion_esRechazado() throws Exception {
        mockMvc.perform(get("/api/scheduling/citas/paciente/5"))
                .andExpect(status().is4xxClientError());

        verify(agendamientoFacade, never()).listarCitasPorPaciente(any());
    }

    // ── listarPorProfesional: IDOR corregido ────────────────────────────

    @Test
    void listarPorProfesional_propioUsuario_esPermitido() throws Exception {
        given(agendamientoFacade.listarCitasPorProfesional(10L)).willReturn(List.of(citaDTO(1L)));

        mockMvc.perform(get("/api/scheduling/citas/profesional/10")
                        .with(JwtAuthTestUtils.comoProfesional(10L)))
                .andExpect(status().isOk());
    }

    @Test
    void listarPorProfesional_otroProfesional_esRechazado() throws Exception {
        mockMvc.perform(get("/api/scheduling/citas/profesional/10")
                        .with(JwtAuthTestUtils.comoProfesional(11L)))
                .andExpect(status().isForbidden());

        verify(agendamientoFacade, never()).listarCitasPorProfesional(any());
    }

    // ── cancelar / completar: solo agendador/administrador ──────────────

    @Test
    void cancelar_comoPaciente_esRechazado() throws Exception {
        mockMvc.perform(patch("/api/scheduling/citas/1/cancelar")
                        .with(JwtAuthTestUtils.comoPaciente(5L)))
                .andExpect(status().isForbidden());

        verify(agendamientoFacade, never()).cancelarCita(any());
    }

    @Test
    void cancelar_comoAgendador_esPermitido() throws Exception {
        given(agendamientoFacade.cancelarCita(1L)).willReturn(citaDTO(1L));

        mockMvc.perform(patch("/api/scheduling/citas/1/cancelar")
                        .with(JwtAuthTestUtils.comoAgendador(2L)))
                .andExpect(status().isOk());

        verify(agendamientoFacade).cancelarCita(eq(1L));
    }

    @Test
    void completar_comoProfesionalSinRolOperativo_esRechazado() throws Exception {
        mockMvc.perform(patch("/api/scheduling/citas/1/completar")
                        .with(JwtAuthTestUtils.comoProfesional(10L)))
                .andExpect(status().isForbidden());

        verify(agendamientoFacade, never()).completarCita(any());
    }

    @Test
    void completar_comoAdministrador_esPermitido() throws Exception {
        given(agendamientoFacade.completarCita(1L)).willReturn(citaDTO(1L));

        mockMvc.perform(patch("/api/scheduling/citas/1/completar")
                        .with(JwtAuthTestUtils.comoAdministrador(9L)))
                .andExpect(status().isOk());
    }

    // ── disponibilidad de horarios: cualquier autenticado puede consultarla ──

    @Test
    void obtenerHorariosDisponibles_cualquierUsuarioAutenticado_esPermitido() throws Exception {
        given(agendamientoFacade.obtenerHorariosDisponibles(eq(10L), any()))
                .willReturn(List.of(ZonedDateTime.now()));

        mockMvc.perform(get("/api/scheduling/citas/profesional/10/disponibilidad")
                        .param("fecha", "2026-10-01")
                        .with(JwtAuthTestUtils.comoPaciente(5L)))
                .andExpect(status().isOk());
    }
}
