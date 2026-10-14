package com.unicauca.piedrazul.users.internal.identity;

/**
 * Regla de contraseña de Piedrazul, que se valida antes de llamar a Keycloak
 * para devolver un 400 con un mensaje claro. El realm (keycloak/piedrazul-realm.json)
 * define la misma política: length(8) and upperCase(1) and digits(1).
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;

    private PasswordPolicy() {
    }

    /** @throws IllegalArgumentException (HTTP 400) si la contraseña no cumple la política. */
    public static void validar(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener al menos " + MIN_LENGTH + " caracteres");
        }
        if (!password.matches("^(?=.*[A-Z])(?=.*\\d).+$")) {
            throw new IllegalArgumentException(
                    "La contraseña debe contener al menos una mayúscula y un número");
        }
    }
}
