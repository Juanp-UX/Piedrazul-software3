package com.unicauca.piedrazul.users.internal.config;

import com.unicauca.piedrazul.users.internal.domain.exceptions.LoginDuplicadoException;
import com.unicauca.piedrazul.users.internal.domain.exceptions.UsuarioNoEncontradoException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fix #5: LoginDuplicadoException y UsuarioNoEncontradoException (users)
 * no tenían @ResponseStatus ni handler dedicado, así que caían en el
 * catch-all de GlobalExceptionHandler y siempre devolvían 500. En
 * particular, registrarse con un correo/login ya existente debía
 * responder 409 (registro.ts en el frontend ya esperaba ese código para
 * mostrar "Este correo ya está registrado", pero nunca lo recibía).
 */
class UsersExceptionHandlerTest {

    private final UsersExceptionHandler handler = new UsersExceptionHandler();

    @Test
    void loginDuplicado_devuelve409() {
        var response = handler.handleLoginDuplicado(new LoginDuplicadoException("paciente@correo.com"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getMensaje()).contains("paciente@correo.com");
    }

    @Test
    void usuarioNoEncontrado_devuelve404() {
        var response = handler.handleUsuarioNoEncontrado(new UsuarioNoEncontradoException("42"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
    }
}
