package com.unicauca.piedrazul.auth.internal.config;

import com.unicauca.piedrazul.auth.internal.domain.exceptions.CredencialDuplicadaException;
import com.unicauca.piedrazul.auth.internal.domain.exceptions.CredencialesInvalidasException;
import com.unicauca.piedrazul.auth.internal.domain.exceptions.PasswordInvalidaException;
import com.unicauca.piedrazul.auth.internal.domain.exceptions.RefreshTokenInvalidoException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fix #5: antes de crear este handler, estas excepciones (aunque tenían
 * su propio @ResponseStatus) eran interceptadas por el
 * @ExceptionHandler(Exception.class) catch-all de GlobalExceptionHandler,
 * que tiene prioridad de resolución sobre @ResponseStatus en Spring MVC,
 * así que login incorrecto, refresh inválido, etc. devolvían 500 en vez
 * de 401/409/400. Se prueba directamente como POJO: AuthExceptionHandler
 * no tiene dependencias inyectadas, así que no hace falta levantar
 * contexto Spring para verificar el mapeo excepción -> status.
 */
class AuthExceptionHandlerTest {

    private final AuthExceptionHandler handler = new AuthExceptionHandler();

    @Test
    void credencialesInvalidas_devuelve401() {
        var response = handler.handleCredencialesInvalidas(new CredencialesInvalidasException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(401);
        assertThat(response.getBody().getMensaje()).isEqualTo("Credenciales inválidas");
    }

    @Test
    void refreshTokenInvalido_devuelve401() {
        var response = handler.handleRefreshTokenInvalido(new RefreshTokenInvalidoException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMensaje()).isEqualTo("Refresh token inválido o expirado");
    }

    @Test
    void credencialDuplicada_devuelve409() {
        var response = handler.handleCredencialDuplicada(
                new CredencialDuplicadaException("El login 'x@correo.com' ya tiene credenciales"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
    }

    @Test
    void passwordInvalida_devuelve400() {
        var response = handler.handlePasswordInvalida(
                new PasswordInvalidaException("La contraseña debe tener al menos 6 caracteres"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
    }
}
