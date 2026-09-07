package com.unicauca.piedrazul.auth.controller;

import com.unicauca.piedrazul.auth.AuthService;
import com.unicauca.piedrazul.auth.dto.CambioPasswordRequest;
import com.unicauca.piedrazul.auth.dto.LoginRequest;
import com.unicauca.piedrazul.auth.dto.RefreshTokenRequest;
import com.unicauca.piedrazul.auth.dto.RegistroCredencialRequest;
import com.unicauca.piedrazul.auth.dto.AuthResponse;
import com.unicauca.piedrazul.auth.dto.TokenValidationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** Login — recibe credenciales, devuelve access + refresh token */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /** Renueva el access token con un refresh token válido */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    /** Logout de la sesión actual — revoca el refresh token */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    /** Logout de todas las sesiones del usuario */
    @PostMapping("/logout-all/{usuarioId}")
    public ResponseEntity<Void> logoutAll(@PathVariable Long usuarioId) {
        authService.logoutAll(usuarioId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Registro de credenciales — llamado internamente por el módulo users
     * al crear un nuevo usuario. No expuesto al cliente final (Angular).
     */
    @PostMapping("/registro")
    public ResponseEntity<Void> registrar(@Valid @RequestBody RegistroCredencialRequest request) {
        authService.registrarCredencial(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /** Cambio de contraseña */
    @PutMapping("/password/{usuarioId}")
    public ResponseEntity<Void> cambiarPassword(
            @PathVariable Long usuarioId,
            @Valid @RequestBody CambioPasswordRequest request) {
        authService.cambiarPassword(usuarioId, request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Validación de token — utilidad expuesta para depuración/pruebas.
     * En el flujo normal, la validación la hace JwtAuthenticationFilter internamente.
     */
    @GetMapping("/validate")
    public ResponseEntity<TokenValidationResponse> validar(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.startsWith("Bearer ")
                ? authHeader.substring(7)
                : authHeader;
        return ResponseEntity.ok(authService.validarToken(token));
    }
}
