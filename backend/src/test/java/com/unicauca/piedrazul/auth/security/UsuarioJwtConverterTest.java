package com.unicauca.piedrazul.auth.security;

import com.unicauca.piedrazul.shared.RolUsuario;
import com.unicauca.piedrazul.users.IUsuarioService;
import com.unicauca.piedrazul.users.dto.UsuarioDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * El token de Keycloak solo aporta la identidad (preferred_username); el id y el rol
 * salen de la tabla usuarios. Estas pruebas fijan ese contrato.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioJwtConverterTest {

    @Mock
    private IUsuarioService usuarioService;

    private UsuarioJwtConverter converter;

    @BeforeEach
    void setUp() {
        converter = new UsuarioJwtConverter(usuarioService);
    }

    private Jwt jwtDe(String login) {
        Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "RS256").subject("keycloak-id");
        if (login != null) {
            builder.claim("preferred_username", login);
        }
        return builder.build();
    }

    private UsuarioDTO usuario(Long id, RolUsuario rol, boolean activo) {
        return UsuarioDTO.builder()
                .id(id)
                .login("ana")
                .nombreCompleto("Ana Pérez")
                .rol(rol)
                .activo(activo)
                .build();
    }

    @Test
    void convert_usuarioActivo_usaElIdComoPrincipalYElRolLocalComoAutoridad() {
        given(usuarioService.buscarPorLogin("ana"))
                .willReturn(Optional.of(usuario(5L, RolUsuario.agendador, true)));

        var autenticacion = converter.convert(jwtDe("ana"));

        assertThat(autenticacion.getPrincipal()).isEqualTo(5L);
        assertThat(autenticacion.isAuthenticated()).isTrue();
        assertThat(autenticacion.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_AGENDADOR");
    }

    @Test
    void convert_usuarioInexistenteEnPiedrazul_esRechazado() {
        given(usuarioService.buscarPorLogin("ana")).willReturn(Optional.empty());

        assertThatThrownBy(() -> converter.convert(jwtDe("ana")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void convert_usuarioDesactivado_esRechazado() {
        given(usuarioService.buscarPorLogin("ana"))
                .willReturn(Optional.of(usuario(5L, RolUsuario.paciente, false)));

        assertThatThrownBy(() -> converter.convert(jwtDe("ana")))
                .isInstanceOf(DisabledException.class);
    }

    @Test
    void convert_tokenSinPreferredUsername_esRechazado() {
        assertThatThrownBy(() -> converter.convert(jwtDe(null)))
                .isInstanceOf(InvalidBearerTokenException.class);
    }
}
