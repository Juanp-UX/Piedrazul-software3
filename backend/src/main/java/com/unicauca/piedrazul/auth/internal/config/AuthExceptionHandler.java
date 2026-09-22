package com.unicauca.piedrazul.auth.internal.config;

import com.unicauca.piedrazul.auth.internal.domain.exceptions.CredencialDuplicadaException;
import com.unicauca.piedrazul.auth.internal.domain.exceptions.CredencialesInvalidasException;
import com.unicauca.piedrazul.auth.internal.domain.exceptions.PasswordInvalidaException;
import com.unicauca.piedrazul.auth.internal.domain.exceptions.RefreshTokenInvalidoException;
import com.unicauca.piedrazul.shared.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Handler dedicado para las excepciones de negocio del módulo auth.
 * <p>
 * Antes de este fix, estas excepciones ya tenían {@code @ResponseStatus}
 * en su propia clase, pero el {@code @ExceptionHandler(Exception.class)}
 * catch-all de {@code GlobalExceptionHandler} tiene prioridad de
 * resolución sobre {@code @ResponseStatus} en Spring MVC, así que todas
 * terminaban devolviendo 500 en vez de 401/409/400. Por ejemplo, un
 * login con credenciales incorrectas o un refresh token expirado se
 * reportaban como "error interno del servidor".
 */
@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ApiError> handleCredencialesInvalidas(CredencialesInvalidasException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError(HttpStatus.UNAUTHORIZED.value(),
                        "Credenciales inválidas", ex.getMessage()));
    }

    @ExceptionHandler(RefreshTokenInvalidoException.class)
    public ResponseEntity<ApiError> handleRefreshTokenInvalido(RefreshTokenInvalidoException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError(HttpStatus.UNAUTHORIZED.value(),
                        "Sesión inválida", ex.getMessage()));
    }

    @ExceptionHandler(CredencialDuplicadaException.class)
    public ResponseEntity<ApiError> handleCredencialDuplicada(CredencialDuplicadaException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError(HttpStatus.CONFLICT.value(),
                        "Credencial ya registrada", ex.getMessage()));
    }

    @ExceptionHandler(PasswordInvalidaException.class)
    public ResponseEntity<ApiError> handlePasswordInvalida(PasswordInvalidaException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(HttpStatus.BAD_REQUEST.value(),
                        "Contraseña inválida", ex.getMessage()));
    }
}
