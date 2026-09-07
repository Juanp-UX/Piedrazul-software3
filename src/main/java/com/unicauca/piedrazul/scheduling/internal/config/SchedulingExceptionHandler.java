package com.unicauca.piedrazul.scheduling.internal.config;

import com.unicauca.piedrazul.scheduling.internal.domain.exceptions.CitaNoEncontradaException;
import com.unicauca.piedrazul.scheduling.internal.domain.exceptions.ConfiguracionInvalidaException;
import com.unicauca.piedrazul.scheduling.internal.domain.exceptions.FechaNoDisponibleException;
import com.unicauca.piedrazul.scheduling.internal.domain.exceptions.FueraDeVentanaAgendamientoException;
import com.unicauca.piedrazul.scheduling.internal.domain.exceptions.HorarioOcupadoException;
import com.unicauca.piedrazul.scheduling.internal.domain.exceptions.TransicionEstadoInvalidaException;
import com.unicauca.piedrazul.scheduling.internal.domain.exceptions.UsuarioNoEncontradoException;
import com.unicauca.piedrazul.shared.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class SchedulingExceptionHandler {

    // ── Excepciones de citas ─────────────────────────────────────────────────

    @ExceptionHandler(CitaNoEncontradaException.class)
    public ResponseEntity<ApiError> handleCitaNoEncontrada(CitaNoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError(HttpStatus.NOT_FOUND.value(), "Recurso no encontrado", ex.getMessage()));
    }

    @ExceptionHandler(HorarioOcupadoException.class)
    public ResponseEntity<ApiError> handleHorarioOcupado(HorarioOcupadoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError(HttpStatus.CONFLICT.value(), "Horario no disponible", ex.getMessage()));
    }

    @ExceptionHandler(TransicionEstadoInvalidaException.class)
    public ResponseEntity<ApiError> handleTransicionInvalida(TransicionEstadoInvalidaException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ApiError(HttpStatus.UNPROCESSABLE_ENTITY.value(),
                        "Transición de estado inválida", ex.getMessage()));
    }

    @ExceptionHandler(UsuarioNoEncontradoException.class)
    public ResponseEntity<ApiError> handleUsuarioNoEncontrado(UsuarioNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ApiError(HttpStatus.UNPROCESSABLE_ENTITY.value(),
                        "Usuario no sincronizado", ex.getMessage()));
    }

    @ExceptionHandler(ConfiguracionInvalidaException.class)
    public ResponseEntity<ApiError> handleConfiguracionInvalida(ConfiguracionInvalidaException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ApiError(HttpStatus.UNPROCESSABLE_ENTITY.value(),
                        "Configuración de disponibilidad inválida", ex.getMessage()));
    }

    @ExceptionHandler(FueraDeVentanaAgendamientoException.class)
    public ResponseEntity<ApiError> handleFueraDeVentana(FueraDeVentanaAgendamientoException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ApiError(HttpStatus.UNPROCESSABLE_ENTITY.value(),
                        "Fecha fuera del rango de agendamiento", ex.getMessage()));
    }

    @ExceptionHandler(FechaNoDisponibleException.class)
    public ResponseEntity<ApiError> handleFechaNoDisponible(FechaNoDisponibleException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ApiError(HttpStatus.UNPROCESSABLE_ENTITY.value(),
                        "Fecha no disponible", ex.getMessage()));
    }
}