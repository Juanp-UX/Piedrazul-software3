package com.unicauca.piedrazul.auth.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;

/**
 * Autenticación del usuario ya identificado por Keycloak y resuelto contra la
 * tabla {@code usuarios} de Piedrazul.
 * <p>
 * El <b>principal es el id del usuario (Long)</b>, igual que antes con el filtro
 * JWT manual, para que las expresiones {@code @PreAuthorize("#id ==
 * authentication.principal ...")} del proyecto sigan funcionando sin cambios.
 * El JWT original queda disponible como credenciales por si hiciera falta
 * leer otros claims.
 */
public class UsuarioAuthenticationToken extends AbstractAuthenticationToken {

    private final Long usuarioId;
    private final Jwt jwt;

    public UsuarioAuthenticationToken(Long usuarioId,
                                      Jwt jwt,
                                      Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.usuarioId = usuarioId;
        this.jwt = jwt;
        setAuthenticated(true);
    }

    @Override
    public Long getPrincipal() {
        return usuarioId;
    }

    @Override
    public Jwt getCredentials() {
        return jwt;
    }

    @Override
    public String getName() {
        return String.valueOf(usuarioId);
    }
}
