package com.unicauca.piedrazul.auth.internal.domain.repository;

import com.unicauca.piedrazul.auth.internal.domain.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.ZonedDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    void deleteByExpiraEnBeforeOrUsadoTrueOrRevocadoTrue(ZonedDateTime fecha);
    void deleteByUsuarioId(Long usuarioId);
}
