package com.unicauca.piedrazul.users.internal.identity;

/**
 * Puerto hacia el proveedor de identidad (Keycloak): las contraseñas y las
 * sesiones viven allá; Piedrazul solo conserva el perfil del usuario.
 */
public interface IIdentityProvider {

    /**
     * Crea la cuenta de acceso (usuario + contraseña definitiva).
     * <p>
     * Si hay una transacción en curso y se revierte después de crear la cuenta,
     * la implementación la elimina del proveedor para no dejar cuentas huérfanas.
     *
     * @throws com.unicauca.piedrazul.users.internal.domain.exceptions.LoginDuplicadoException
     *         si el proveedor ya tiene un usuario con ese login.
     * @throws IdentityProviderException si el proveedor no responde o rechaza la creación.
     */
    void crearCuenta(CuentaNueva cuenta);

    /**
     * @param login          nombre de usuario con el que inicia sesión (= usuarios.login)
     * @param nombreCompleto se separa en nombre y apellido para el perfil del proveedor
     * @param email          opcional (solo los pacientes lo registran)
     * @param password       contraseña en claro; no se guarda ni se registra en logs
     */
    record CuentaNueva(String login, String nombreCompleto, String email, String password) {
    }
}
