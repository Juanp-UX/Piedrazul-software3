package com.unicauca.piedrazul.users.internal.application.impl;

import com.unicauca.piedrazul.users.dto.ProfesionalDTO;
import com.unicauca.piedrazul.users.internal.domain.entity.Especialidad;
import com.unicauca.piedrazul.users.internal.domain.entity.Profesional;
import com.unicauca.piedrazul.users.internal.domain.entity.Usuario;
import com.unicauca.piedrazul.users.internal.domain.entity.enums.TipoProfesional;
import com.unicauca.piedrazul.users.internal.domain.exceptions.UsuarioNoEncontradoException;
import com.unicauca.piedrazul.users.internal.domain.repository.EspecialidadRepository;
import com.unicauca.piedrazul.users.internal.domain.repository.ProfesionalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Fix #7: ProfesionalServiceImpl.buscarPorId(id) buscaba por el id propio
 * de la fila "profesionales" (profesionalRepository.findById), pero
 * listarActivos()/toDTO() siempre exponen el Usuario.id como "id" del
 * profesional — igual que Cita.profesionalId y el resto del sistema de
 * scheduling. Ahora buscarPorId busca de forma consistente por
 * Usuario.id (findByUsuarioId).
 */
@ExtendWith(MockitoExtension.class)
class ProfesionalServiceImplTest {

    @Mock
    private ProfesionalRepository profesionalRepository;

    @Mock
    private EspecialidadRepository especialidadRepository;

    @Mock
    private ApplicationEventPublisher events;

    @InjectMocks
    private ProfesionalServiceImpl profesionalService;

    private Profesional profesionalConUsuario(Long profesionalRowId, Long usuarioId) {
        Usuario usuario = Usuario.builder()
                .id(usuarioId)
                .nombreCompleto("Dra. Ana Pérez")
                .login("ana.perez@correo.com")
                .build();

        Especialidad especialidad = Especialidad.builder()
                .id(1)
                .nombre("Odontología")
                .build();

        return Profesional.builder()
                .id(profesionalRowId)
                .usuario(usuario)
                .tipo(TipoProfesional.medico)
                .especialidad(especialidad)
                .licenciaProfesional("LIC-001")
                .activo(true)
                .duracionCitaMinutos(30)
                .build();
    }

    @Test
    void buscarPorId_buscaPorUsuarioId_noPorElIdPropioDeLaFila() {
        // El id de la fila "profesionales" (99) es distinto del Usuario.id (7).
        Profesional profesional = profesionalConUsuario(99L, 7L);
        given(profesionalRepository.findByUsuarioId(7L)).willReturn(Optional.of(profesional));

        ProfesionalDTO resultado = profesionalService.buscarPorId(7L);

        assertThat(resultado.getId()).isEqualTo(7L); // Usuario.id, consistente con toDTO()
        assertThat(resultado.getNombreCompleto()).isEqualTo("Dra. Ana Pérez");
        verify(profesionalRepository).findByUsuarioId(7L);
        verify(profesionalRepository, never()).findById(any());
    }

    @Test
    void buscarPorId_noExiste_lanzaUsuarioNoEncontrado() {
        given(profesionalRepository.findByUsuarioId(123L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> profesionalService.buscarPorId(123L))
                .isInstanceOf(UsuarioNoEncontradoException.class);
    }

    @Test
    void listarActivos_exponeUsuarioIdComoId_consistenteConBuscarPorId() {
        Profesional profesional = profesionalConUsuario(99L, 7L);
        given(profesionalRepository.findByActivoTrue()).willReturn(java.util.List.of(profesional));

        var resultado = profesionalService.listarActivos();

        // El id que devuelve listarActivos() debe ser el mismo que acepta
        // buscarPorId(): ambos deben referirse siempre al Usuario.id.
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getId()).isEqualTo(7L);
    }
}
