package com.unicauca.piedrazul.users.internal.identity;

import com.unicauca.piedrazul.users.internal.domain.exceptions.LoginDuplicadoException;
import com.unicauca.piedrazul.users.internal.identity.IIdentityProvider.CuentaNueva;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withCreatedEntity;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

class KeycloakIdentityProviderTest {

    private static final String BASE = "http://keycloak.test";

    private MockRestServiceServer servidor;
    private KeycloakIdentityProvider proveedor;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        servidor = MockRestServiceServer.bindTo(builder).build();
        proveedor = new KeycloakIdentityProvider(builder.build(), "piedrazul", "piedrazul-backend", "secreto");
    }

    private void esperarTokenAdministrador() {
        servidor.expect(requestTo(BASE + "/realms/piedrazul/protocol/openid-connect/token"))
                .andExpect(method(POST))
                .andRespond(withSuccess("{\"access_token\":\"token-admin\"}", MediaType.APPLICATION_JSON));
    }

    @Test
    void crearCuenta_pideTokenYCreaElUsuarioConContrasenaDefinitiva() {
        esperarTokenAdministrador();
        servidor.expect(requestTo(BASE + "/admin/realms/piedrazul/users"))
                .andExpect(method(POST))
                .andExpect(header("Authorization", "Bearer token-admin"))
                .andExpect(jsonPath("$.username").value("ana"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.firstName").value("Ana"))
                .andExpect(jsonPath("$.lastName").value("Pérez Gómez"))
                .andExpect(jsonPath("$.email").value("ana@correo.com"))
                .andExpect(jsonPath("$.credentials[0].type").value("password"))
                .andExpect(jsonPath("$.credentials[0].temporary").value(false))
                .andRespond(withCreatedEntity(URI.create(BASE + "/admin/realms/piedrazul/users/abc-123")));

        proveedor.crearCuenta(new CuentaNueva("ana", "Ana Pérez Gómez", "ana@correo.com", "Secreta123"));

        servidor.verify();
    }

    @Test
    void crearCuenta_conLoginYaExistenteEnKeycloak_lanzaLoginDuplicado() {
        esperarTokenAdministrador();
        servidor.expect(requestTo(BASE + "/admin/realms/piedrazul/users"))
                .andRespond(withStatus(HttpStatus.CONFLICT));

        assertThatThrownBy(() ->
                proveedor.crearCuenta(new CuentaNueva("ana", "Ana Pérez", null, "Secreta123")))
                .isInstanceOf(LoginDuplicadoException.class);
    }

    @Test
    void crearCuenta_siKeycloakFalla_lanzaIdentityProviderException() {
        esperarTokenAdministrador();
        servidor.expect(requestTo(BASE + "/admin/realms/piedrazul/users"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() ->
                proveedor.crearCuenta(new CuentaNueva("ana", "Ana Pérez", null, "Secreta123")))
                .isInstanceOf(IdentityProviderException.class);
    }

    @Test
    void crearCuenta_sinPoderObtenerTokenAdministrador_lanzaIdentityProviderException() {
        servidor.expect(requestTo(BASE + "/realms/piedrazul/protocol/openid-connect/token"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() ->
                proveedor.crearCuenta(new CuentaNueva("ana", "Ana Pérez", null, "Secreta123")))
                .isInstanceOf(IdentityProviderException.class);
    }

    @Test
    void separarNombre_dividePrimerNombreYApellidos() {
        assertThat(KeycloakIdentityProvider.separarNombre("Juan Pablo Collazos"))
                .containsExactly("Juan", "Pablo Collazos");
        assertThat(KeycloakIdentityProvider.separarNombre("Madonna")).containsExactly("Madonna", "Madonna");
        assertThat(KeycloakIdentityProvider.separarNombre("  ")).containsExactly("-", "-");
    }
}
