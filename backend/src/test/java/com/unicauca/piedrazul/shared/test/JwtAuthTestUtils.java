package com.unicauca.piedrazul.shared.test;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

/**
 * Reproduce en los tests @WebMvcTest la misma forma de Authentication que
 * JwtAuthenticationFilter coloca en el SecurityContext en producción:
 * principal = usuarioId (Long), authority = "ROLE_" + rol en mayúsculas.
 * <p>
 * Las anotaciones @WithMockUser de spring-security-test no sirven aquí
 * porque por defecto usan un String como principal (el username), no un
 * Long, y las expresiones @PreAuthorize del proyecto comparan
 * "#id == authentication.principal" asumiendo que el principal es Long.
 */
public final class JwtAuthTestUtils {

    private JwtAuthTestUtils() {
    }

    /** Simula un usuario autenticado con el usuarioId y rol dados. */
    public static RequestPostProcessor comoUsuario(Long usuarioId, String rol) {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + rol.toUpperCase()));
        var authentication = new UsernamePasswordAuthenticationToken(usuarioId, null, authorities);
        return SecurityMockMvcRequestPostProcessors.authentication(authentication);
    }

    public static RequestPostProcessor comoPaciente(Long usuarioId) {
        return comoUsuario(usuarioId, "paciente");
    }

    public static RequestPostProcessor comoProfesional(Long usuarioId) {
        return comoUsuario(usuarioId, "profesional");
    }

    public static RequestPostProcessor comoAgendador(Long usuarioId) {
        return comoUsuario(usuarioId, "agendador");
    }

    public static RequestPostProcessor comoAdministrador(Long usuarioId) {
        return comoUsuario(usuarioId, "administrador");
    }
}
