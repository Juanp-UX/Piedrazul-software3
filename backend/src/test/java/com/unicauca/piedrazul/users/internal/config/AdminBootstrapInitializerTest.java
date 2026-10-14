package com.unicauca.piedrazul.users.internal.config;

import com.unicauca.piedrazul.shared.RolUsuario;
import com.unicauca.piedrazul.users.internal.domain.entity.Usuario;
import com.unicauca.piedrazul.users.internal.domain.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapInitializerTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    private AdminBootstrapInitializer inicializador(boolean habilitado) {
        return new AdminBootstrapInitializer(usuarioRepository, habilitado, "admin", "Administrador Piedra Azul");
    }

    @Test
    void sinAdministradores_creaElPerfilInicial() {
        given(usuarioRepository.findByRol(RolUsuario.administrador)).willReturn(List.of());
        given(usuarioRepository.existsByLogin("admin")).willReturn(false);

        inicializador(true).run(null);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals("admin", captor.getValue().getLogin());
        assertEquals(RolUsuario.administrador, captor.getValue().getRol());
        assertTrue(captor.getValue().getActivo());
    }

    @Test
    void siYaHayUnAdministrador_noCreaNada() {
        given(usuarioRepository.findByRol(RolUsuario.administrador))
                .willReturn(List.of(Usuario.builder().login("otro").rol(RolUsuario.administrador).build()));

        inicializador(true).run(null);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void siElLoginYaExisteConOtroRol_noCreaNada() {
        given(usuarioRepository.findByRol(RolUsuario.administrador)).willReturn(List.of());
        given(usuarioRepository.existsByLogin("admin")).willReturn(true);

        inicializador(true).run(null);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deshabilitado_noConsultaNiCrea() {
        inicializador(false).run(null);

        verify(usuarioRepository, never()).save(any());
        verify(usuarioRepository, never()).findByRol(any());
    }
}
