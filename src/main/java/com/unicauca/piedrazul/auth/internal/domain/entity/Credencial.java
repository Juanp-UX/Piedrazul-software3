package com.unicauca.piedrazul.auth.internal.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

/**
 * Almacena las credenciales de acceso de un usuario.
 * <p>
 * usuarioId referencia al Usuario del módulo users. Se conserva como
 * referencia lógica (no FK física) para no acoplar el esquema de auth
 * directamente a la tabla de users, igual que en el diseño original.
 */
@Entity
@Table(name = "credenciales", indexes = {
        @Index(name = "idx_credenciales_login", columnList = "login", unique = true),
        @Index(name = "idx_credenciales_usuario_id", columnList = "usuario_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Credencial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false, unique = true)
    private Long usuarioId;

    @Column(nullable = false, unique = true, length = 50)
    private String login;

    /** Hash BCrypt de la contraseña */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private ZonedDateTime creadoEn;

    @Column(name = "actualizado_en")
    private ZonedDateTime actualizadoEn;

    @PrePersist
    void prePersist() {
        creadoEn = ZonedDateTime.now();
        actualizadoEn = ZonedDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        actualizadoEn = ZonedDateTime.now();
    }
}
