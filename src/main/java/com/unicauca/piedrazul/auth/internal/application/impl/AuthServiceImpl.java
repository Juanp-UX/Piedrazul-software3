package com.unicauca.piedrazul.auth.internal.application.impl;

import com.unicauca.piedrazul.auth.AuthService;
import com.unicauca.piedrazul.auth.dto.*;
import com.unicauca.piedrazul.auth.internal.application.IPasswordService;
import com.unicauca.piedrazul.auth.internal.domain.entity.Credencial;
import com.unicauca.piedrazul.auth.internal.domain.entity.RefreshToken;
import com.unicauca.piedrazul.auth.internal.domain.exceptions.CredencialDuplicadaException;
import com.unicauca.piedrazul.auth.internal.domain.exceptions.CredencialesInvalidasException;
import com.unicauca.piedrazul.auth.internal.domain.exceptions.RefreshTokenInvalidoException;
import com.unicauca.piedrazul.auth.internal.domain.repository.CredencialRepository;
import com.unicauca.piedrazul.auth.internal.domain.repository.RefreshTokenRepository;
import com.unicauca.piedrazul.auth.internal.security.JwtUtil;
import com.unicauca.piedrazul.users.IUsuarioService;
import com.unicauca.piedrazul.users.dto.UsuarioDTO;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final CredencialRepository credencialRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final IPasswordService passwordService;
    private final IUsuarioService usuarioService;
    private final JwtUtil jwtUtil;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        Credencial credencial = credencialRepository.findByLogin(request.getLogin())
                .orElseThrow(CredencialesInvalidasException::new);

        if (!credencial.getActivo()) {
            throw new CredencialesInvalidasException();
        }

        if (!passwordService.verificar(request.getPassword(), credencial.getPasswordHash())) {
            throw new CredencialesInvalidasException();
        }

        UsuarioDTO usuario = usuarioService.buscarPorId(credencial.getUsuarioId());

        String refreshTokenValor = persistirRefreshToken(credencial.getUsuarioId());
        String accessToken = jwtUtil.generarAccessToken(usuario.getId(), usuario.getLogin(), usuario.getRol().name());

        return construirAuthResponse(accessToken, refreshTokenValor, usuario);
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(RefreshTokenInvalidoException::new);

        if (!refreshToken.esValido()) {
            throw new RefreshTokenInvalidoException();
        }

        // Rotación: el token usado se marca y no puede reutilizarse
        refreshToken.setUsado(true);
        refreshTokenRepository.save(refreshToken);

        Credencial credencial = credencialRepository.findByUsuarioId(refreshToken.getUsuarioId())
                .orElseThrow(CredencialesInvalidasException::new);

        if (!credencial.getActivo()) {
            throw new CredencialesInvalidasException();
        }

        UsuarioDTO usuario = usuarioService.buscarPorId(credencial.getUsuarioId());

        String nuevoRefreshToken = persistirRefreshToken(credencial.getUsuarioId());
        String accessToken = jwtUtil.generarAccessToken(usuario.getId(), usuario.getLogin(), usuario.getRol().name());

        return construirAuthResponse(accessToken, nuevoRefreshToken, usuario);
    }

    @Override
    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokenRepository.findByToken(request.getRefreshToken())
                .ifPresent(rt -> {
                    rt.setRevocado(true);
                    refreshTokenRepository.save(rt);
                });
    }

    @Override
    @Transactional
    public void logoutAll(Long usuarioId) {
        refreshTokenRepository.deleteByUsuarioId(usuarioId);
    }

    @Override
    @Transactional
    public void registrarCredencial(RegistroCredencialRequest request) {
        if (credencialRepository.existsByLogin(request.getLogin())) {
            throw new CredencialDuplicadaException("El login ya está en uso");
        }
        if (credencialRepository.existsByUsuarioId(request.getUsuarioId())) {
            throw new CredencialDuplicadaException("El usuario ya tiene credenciales registradas");
        }

        passwordService.validarFormato(request.getPassword());

        Credencial credencial = Credencial.builder()
                .usuarioId(request.getUsuarioId())
                .login(request.getLogin())
                .passwordHash(passwordService.encriptar(request.getPassword()))
                .activo(true)
                .build();

        credencialRepository.save(credencial);
    }

    @Override
    @Transactional
    public void cambiarPassword(Long usuarioId, CambioPasswordRequest request) {
        Credencial credencial = credencialRepository.findByUsuarioId(usuarioId)
                .orElseThrow(CredencialesInvalidasException::new);

        if (!passwordService.verificar(request.getPasswordActual(), credencial.getPasswordHash())) {
            throw new CredencialesInvalidasException();
        }

        passwordService.validarFormato(request.getPasswordNuevo());

        credencial.setPasswordHash(passwordService.encriptar(request.getPasswordNuevo()));
        credencialRepository.save(credencial);
    }

    @Override
    public TokenValidationResponse validarToken(String token) {
        try {
            Claims claims = jwtUtil.extraerClaims(token);
            return TokenValidationResponse.builder()
                    .valido(true)
                    .usuarioId(Long.valueOf(claims.getSubject()))
                    .login(claims.get("login", String.class))
                    .rol(claims.get("rol", String.class))
                    .build();
        } catch (Exception e) {
            return TokenValidationResponse.builder().valido(false).build();
        }
    }

    private String persistirRefreshToken(Long usuarioId) {
        String valor = UUID.randomUUID().toString();
        RefreshToken rt = RefreshToken.builder()
                .usuarioId(usuarioId)
                .token(valor)
                .expiraEn(ZonedDateTime.now().plusSeconds(refreshExpiration / 1000))
                .build();
        refreshTokenRepository.save(rt);
        return valor;
    }

    private AuthResponse construirAuthResponse(String accessToken, String refreshToken, UsuarioDTO usuario) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tipo("Bearer")
                .expiresIn(jwtUtil.getAccessExpiration())
                .usuarioId(usuario.getId())
                .login(usuario.getLogin())
                .nombreCompleto(usuario.getNombreCompleto())
                .rol(usuario.getRol().name())
                .build();
    }
}
