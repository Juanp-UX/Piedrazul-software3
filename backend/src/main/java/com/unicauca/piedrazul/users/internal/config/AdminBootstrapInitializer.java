package com.unicauca.piedrazul.users.internal.config;

import com.unicauca.piedrazul.shared.RolUsuario;
import com.unicauca.piedrazul.users.internal.domain.entity.Usuario;
import com.unicauca.piedrazul.users.internal.domain.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Crea el primer administrador en la tabla {@code usuarios} si todavía no existe ninguno.
 * <p>
 * Registrar usuarios que no son pacientes exige ser ADMINISTRADOR, así que sin un primer
 * administrador nadie podría crear al resto. Este runner solo crea el <b>perfil</b>; la
 * contraseña vive en Keycloak: el realm de desarrollo (keycloak/piedrazul-realm.json) ya
 * trae un usuario con el mismo login ({@code admin}). En otros entornos, crea ese usuario
 * en la consola de Keycloak con el login configurado en
 * {@code piedrazul.bootstrap-admin.login}.
 */
@Component
public class AdminBootstrapInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapInitializer.class);

    private final UsuarioRepository usuarioRepository;
    private final boolean habilitado;
    private final String login;
    private final String nombreCompleto;

    public AdminBootstrapInitializer(
            UsuarioRepository usuarioRepository,
            @Value("${piedrazul.bootstrap-admin.enabled:true}") boolean habilitado,
            @Value("${piedrazul.bootstrap-admin.login:admin}") String login,
            @Value("${piedrazul.bootstrap-admin.nombre-completo:Administrador Piedra Azul}") String nombreCompleto) {
        this.usuarioRepository = usuarioRepository;
        this.habilitado = habilitado;
        this.login = login;
        this.nombreCompleto = nombreCompleto;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!habilitado || !usuarioRepository.findByRol(RolUsuario.administrador).isEmpty()) {
            return;
        }
        if (usuarioRepository.existsByLogin(login)) {
            log.warn("No se creó el administrador inicial: el login '{}' ya existe con otro rol.", login);
            return;
        }

        usuarioRepository.save(Usuario.builder()
                .login(login)
                .nombreCompleto(nombreCompleto)
                .rol(RolUsuario.administrador)
                .activo(true)
                .build());
        log.info("Administrador inicial creado: login='{}'. Su contraseña se gestiona en Keycloak.", login);
    }
}
