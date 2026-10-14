package com.unicauca.piedrazul.auth;

import com.unicauca.piedrazul.auth.security.UsuarioJwtConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * La API es un <b>resource server OAuth2</b>: Keycloak emite los JWT y Spring
 * Security los valida (firma contra el JWKS del realm, expiración y emisor).
 * El {@code JwtDecoder} lo crea Spring Boot a partir de
 * {@code spring.security.oauth2.resourceserver.jwt.*} en application.yaml.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final UsuarioJwtConverter usuarioJwtConverter;

    public SecurityConfig(UsuarioJwtConverter usuarioJwtConverter) {
        this.usuarioJwtConverter = usuarioJwtConverter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Único endpoint público: el autorregistro de pacientes.
                        .requestMatchers(HttpMethod.POST, "/api/users/registro/paciente").permitAll()
                        // Crear otros usuarios (administrador, profesional...) es una
                        // operación administrativa; antes estaba abierta a cualquiera.
                        .requestMatchers("/api/users/registro/**").hasRole("ADMINISTRADOR")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(usuarioJwtConverter)));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        // Sin esto, el navegador bloquea el acceso desde JS a headers custom
        // en peticiones cross-origin (ej. X-Total-Count en /citas/.../fecha).
        configuration.setExposedHeaders(List.of("X-Total-Count"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
