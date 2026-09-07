package com.unicauca.piedrazul.auth.internal.application;

public interface IPasswordService {
    /** Hashea la contraseña en texto plano con BCrypt */
    String encriptar(String passwordPlano);
    /** Verifica que el texto plano coincide con el hash almacenado */
    boolean verificar(String passwordPlano, String hash);
    /**
     * Valida que la contraseña cumpla las reglas de formato:
     * mínimo 8 caracteres, al menos una mayúscula y un número.
     */
    void validarFormato(String password);
}
