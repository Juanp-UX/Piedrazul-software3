package com.unicauca.piedrazul.users.internal.config;

import com.unicauca.piedrazul.shared.ApiError;
import com.unicauca.piedrazul.users.internal.domain.exceptions.LoginDuplicadoException;
import com.unicauca.piedrazul.users.internal.domain.exceptions.UsuarioNoEncontradoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Handler dedicado para las excepciones de negocio del módulo users.
 * <p>
 * Antes de este fix, estas excepciones no tenían ni {@code @ResponseStatus}
 * ni un handler propio, así que caían en el
 * {@code @ExceptionHandler(Exception.class)} catch-all de
 * {@code GlobalExceptionHandler} y siempre devolvían 500. En particular,
 * registrarse con un correo/login ya existente (LoginDuplicadoException)
 * debía responder 409 — el frontend (registro.ts) ya esperaba ese código
 * para mostrar "Este correo ya está registrado", pero nunca lo recibía.
 */
@RestControllerAdvice
public class UsersExceptionHandler {

    @ExceptionHandler(LoginDuplicadoException.class)
    public ResponseEntity<ApiError> handleLoginDuplicado(LoginDuplicadoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError(HttpStatus.CONFLICT.value(),
                        "Login ya registrado", ex.getMessage()));
    }

    @ExceptionHandler(UsuarioNoEncontradoException.class)
    public ResponseEntity<ApiError> handleUsuarioNoEncontrado(UsuarioNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError(HttpStatus.NOT_FOUND.value(),
                        "Usuario no encontrado", ex.getMessage()));
    }
}
