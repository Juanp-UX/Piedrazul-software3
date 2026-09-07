package com.unicauca.piedrazul.auth;

import com.unicauca.piedrazul.auth.dto.CambioPasswordRequest;
import com.unicauca.piedrazul.auth.dto.LoginRequest;
import com.unicauca.piedrazul.auth.dto.RefreshTokenRequest;
import com.unicauca.piedrazul.auth.dto.RegistroCredencialRequest;
import com.unicauca.piedrazul.auth.dto.AuthResponse;
import com.unicauca.piedrazul.auth.dto.TokenValidationResponse;

public interface AuthService {
    /** Autentica al usuario y devuelve access + refresh token */
    AuthResponse login(LoginRequest request);
    /** Renueva el access token usando un refresh token válido */
    AuthResponse refresh(RefreshTokenRequest request);
    /** Revoca el refresh token del usuario (logout de sesión actual) */
    void logout(RefreshTokenRequest request);
    /** Revoca todos los refresh tokens del usuario (logout de todas las sesiones) */
    void logoutAll(Long usuarioId);
    /** Registra credenciales para un usuario recién creado */
    void registrarCredencial(RegistroCredencialRequest request);
    /** Cambia la contraseña verificando la actual */
    void cambiarPassword(Long usuarioId, CambioPasswordRequest request);
    /** Valida un JWT — usado por el filtro de seguridad interno */
    TokenValidationResponse validarToken(String token);
}
