package com.unicauca.piedrazul.users.internal.identity;

/** Fallo al comunicarse con el proveedor de identidad (Keycloak). */
public class IdentityProviderException extends RuntimeException {

    public IdentityProviderException(String mensaje) {
        super(mensaje);
    }

    public IdentityProviderException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
