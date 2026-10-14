package com.unicauca.piedrazul.users.internal.identity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {

    @Test
    void contrasenaValida_noLanzaExcepcion() {
        assertThatCode(() -> PasswordPolicy.validar("Secreta123")).doesNotThrowAnyException();
    }

    @Test
    void nula_oDemasiadoCorta_esRechazada() {
        assertThatThrownBy(() -> PasswordPolicy.validar(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PasswordPolicy.validar("Ab1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("8 caracteres");
    }

    @Test
    void sinMayusculaOSinNumero_esRechazada() {
        assertThatThrownBy(() -> PasswordPolicy.validar("secreta123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mayúscula");
        assertThatThrownBy(() -> PasswordPolicy.validar("SecretaClave"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("número");
    }
}
