package com.unicauca.piedrazul.users.internal.identity;

import com.unicauca.piedrazul.users.internal.domain.exceptions.LoginDuplicadoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Crea las cuentas de acceso en Keycloak con su Admin REST API.
 * <p>
 * Se autentica con un cliente propio del realm ({@code piedrazul-backend}) usando el
 * flujo client_credentials; ese cliente solo tiene el rol {@code manage-users}.
 * Se usa {@link RestClient} directamente (sin la librería keycloak-admin-client) para
 * no acoplar la versión de la API a la del servidor.
 */
@Component
public class KeycloakIdentityProvider implements IIdentityProvider {

    private static final Logger log = LoggerFactory.getLogger(KeycloakIdentityProvider.class);

    private final RestClient restClient;
    private final String realm;
    private final String clientId;
    private final String clientSecret;

    @Autowired
    public KeycloakIdentityProvider(
            @Value("${keycloak.server-url}") String serverUrl,
            @Value("${keycloak.realm}") String realm,
            @Value("${keycloak.admin.client-id}") String clientId,
            @Value("${keycloak.admin.client-secret}") String clientSecret) {
        this(construirRestClient(serverUrl), realm, clientId, clientSecret);
    }

    /** Visible para pruebas: permite inyectar un RestClient enlazado a un servidor simulado. */
    KeycloakIdentityProvider(RestClient restClient, String realm, String clientId, String clientSecret) {
        this.restClient = restClient;
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    private static RestClient construirRestClient(String serverUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(10));

        String base = serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
        return RestClient.builder().baseUrl(base).requestFactory(factory).build();
    }

    @Override
    public void crearCuenta(CuentaNueva cuenta) {
        String token = obtenerTokenAdministrador();

        ResponseEntity<Void> respuesta;
        try {
            respuesta = restClient.post()
                    .uri("/admin/realms/{realm}/users", realm)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(construirUsuario(cuenta))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.Conflict e) {
            throw new LoginDuplicadoException(cuenta.login());
        } catch (HttpClientErrorException e) {
            log.error("Keycloak rechazó la creación del usuario '{}': {} {}",
                    cuenta.login(), e.getStatusCode(), e.getResponseBodyAsString());
            throw new IdentityProviderException("Keycloak rechazó la creación de la cuenta", e);
        } catch (RestClientException e) {
            throw new IdentityProviderException("No fue posible comunicarse con Keycloak", e);
        }

        String keycloakUserId = extraerId(respuesta.getHeaders().getLocation());
        log.info("Cuenta creada en Keycloak: login={} id={}", cuenta.login(), keycloakUserId);
        eliminarSiSeRevierteLaTransaccion(keycloakUserId);
    }

    // ── Token del cliente administrador ──────────────────────────────────────

    private String obtenerTokenAdministrador() {
        MultiValueMap<String, String> formulario = new LinkedMultiValueMap<>();
        formulario.add("grant_type", "client_credentials");
        formulario.add("client_id", clientId);
        formulario.add("client_secret", clientSecret);

        Map<String, Object> respuesta;
        try {
            respuesta = restClient.post()
                    .uri("/realms/{realm}/protocol/openid-connect/token", realm)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formulario)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {
                    });
        } catch (RestClientException e) {
            throw new IdentityProviderException(
                    "No fue posible obtener el token del cliente administrador de Keycloak", e);
        }

        Object token = respuesta != null ? respuesta.get("access_token") : null;
        if (token == null) {
            throw new IdentityProviderException("Keycloak no devolvió un access_token");
        }
        return token.toString();
    }

    // ── Representación del usuario ───────────────────────────────────────────

    private Map<String, Object> construirUsuario(CuentaNueva cuenta) {
        String[] nombre = separarNombre(cuenta.nombreCompleto());

        Map<String, Object> usuario = new LinkedHashMap<>();
        usuario.put("username", cuenta.login());
        usuario.put("enabled", true);
        usuario.put("emailVerified", true);
        usuario.put("firstName", nombre[0]);
        usuario.put("lastName", nombre[1]);
        if (StringUtils.hasText(cuenta.email())) {
            usuario.put("email", cuenta.email().trim());
        }
        usuario.put("credentials", List.of(Map.of(
                "type", "password",
                "value", cuenta.password(),
                "temporary", false)));
        return usuario;
    }

    /** "Juan Pablo Collazos" → ["Juan", "Pablo Collazos"]; un solo término se repite como apellido. */
    static String[] separarNombre(String nombreCompleto) {
        String limpio = nombreCompleto == null ? "" : nombreCompleto.trim();
        if (limpio.isEmpty()) {
            return new String[]{"-", "-"};
        }
        int espacio = limpio.indexOf(' ');
        if (espacio < 0) {
            return new String[]{limpio, limpio};
        }
        return new String[]{limpio.substring(0, espacio), limpio.substring(espacio + 1).trim()};
    }

    // ── Compensación ─────────────────────────────────────────────────────────

    private static String extraerId(URI location) {
        if (location == null) {
            throw new IdentityProviderException("Keycloak no devolvió la ubicación del usuario creado");
        }
        String path = location.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    /**
     * El registro local y la cuenta de Keycloak no comparten transacción. Si el commit local
     * falla (o algo posterior revierte), se borra la cuenta recién creada para que el login
     * no quede ocupado en Keycloak sin un usuario en la base de datos.
     */
    private void eliminarSiSeRevierteLaTransaccion(String keycloakUserId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    eliminarCuenta(keycloakUserId);
                }
            }
        });
    }

    private void eliminarCuenta(String keycloakUserId) {
        try {
            restClient.delete()
                    .uri("/admin/realms/{realm}/users/{id}", realm, keycloakUserId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + obtenerTokenAdministrador())
                    .retrieve()
                    .toBodilessEntity();
            log.warn("Registro revertido: se eliminó la cuenta de Keycloak {}", keycloakUserId);
        } catch (RuntimeException e) {
            log.error("No se pudo eliminar la cuenta huérfana de Keycloak {}: elimínala manualmente", keycloakUserId, e);
        }
    }
}
