package com.unicauca.piedrazul.auth.security;

import com.unicauca.piedrazul.users.IUsuarioService;
import com.unicauca.piedrazul.users.dto.UsuarioDTO;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Convierte un JWT ya validado por Spring (firma, expiración y emisor) en la
 * {@link UsuarioAuthenticationToken} que usa el resto de la aplicación.
 * <p>
 * Reparto de responsabilidades:
 * <ul>
 *   <li><b>Keycloak</b> autentica (contraseñas, sesiones, tokens).</li>
 *   <li><b>Piedrazul</b> autoriza: el id y el rol salen de la tabla
 *       {@code usuarios}, que es la fuente de verdad (el administrador cambia
 *       el rol desde la HU 1.3 y el cambio rige de inmediato, sin sincronizar
 *       roles con Keycloak). El vínculo es {@code preferred_username} del token
 *       = {@code usuarios.login}.</li>
 * </ul>
 * Si el usuario no existe localmente o está desactivado, se rechaza el token
 * (401), de modo que "desactivar usuario" también bloquea el acceso a la API.
 */
@Component
public class UsuarioJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String CLAIM_LOGIN = "preferred_username";

    private final IUsuarioService usuarioService;

    public UsuarioJwtConverter(IUsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String login = jwt.getClaimAsString(CLAIM_LOGIN);
        if (!StringUtils.hasText(login)) {
            throw new InvalidBearerTokenException("El token no incluye el claim " + CLAIM_LOGIN);
        }

        UsuarioDTO usuario = usuarioService.buscarPorLogin(login)
                .orElseThrow(() -> new BadCredentialsException(
                        "El usuario '" + login + "' no está registrado en Piedrazul"));

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new DisabledException("El usuario '" + login + "' está desactivado");
        }

        var authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name().toUpperCase()));

        return new UsuarioAuthenticationToken(usuario.getId(), jwt, authorities);
    }
}
