package com.unicauca.piedrazul.users.internal.application.impl;

import com.unicauca.piedrazul.users.dto.PacienteDTO;
import com.unicauca.piedrazul.users.dto.ProfesionalDTO;
import com.unicauca.piedrazul.users.dto.UsuarioDTO;
import com.unicauca.piedrazul.users.internal.application.interfaces.IRegistroService;
import com.unicauca.piedrazul.users.internal.application.template.RegistroContexto;
import com.unicauca.piedrazul.users.internal.application.template.impl.RegistroAdminService;
import com.unicauca.piedrazul.users.internal.application.template.impl.RegistroPacienteService;
import com.unicauca.piedrazul.users.internal.application.template.impl.RegistroProfesionalService;
import com.unicauca.piedrazul.users.internal.identity.IIdentityProvider;
import com.unicauca.piedrazul.users.internal.identity.IIdentityProvider.CuentaNueva;
import com.unicauca.piedrazul.users.internal.identity.PasswordPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fachada que implementa {@link IRegistroService} y delega cada tipo de
 * registro al subclase concreta del patrón <b>Template Method</b> que
 * le corresponde.
 *
 * <p>Esta clase actúa como punto de entrada único para el controlador,
 * preservando el contrato existente de {@link IRegistroService} sin exponer
 * la jerarquía interna de templates. Internamente, construye un
 * {@link RegistroContexto} con los datos de la solicitud y se lo entrega
 * al template method de la subclase adecuada.
 *
 * <pre>
 * RegistroController
 *       │
 *       ▼  IRegistroService
 * RegistroServiceImpl  (fachada)
 *   ├──► RegistroAdminService.registrar(ctx)
 *   ├──► RegistroPacienteService.registrar(ctx)
 *   └──► RegistroProfesionalService.registrar(ctx)
 *              │
 *              ▼  RegistroTemplate.registrar()  [template method]
 *         1. crearUsuarioBase()
 *         2. vincularPerfil()       ← hook sobreescrito en cada subclase
 *         3. toDTO()
 * </pre>
 *
 * <p>Después de crear el perfil local, la fachada crea la cuenta de acceso (usuario y
 * contraseña) en Keycloak. Todo ocurre en la misma transacción: si Keycloak falla se
 * revierte el registro local, y si el commit local falla se elimina la cuenta de Keycloak.
 */
@Service
public class RegistroServiceImpl implements IRegistroService {

    private final RegistroAdminService adminService;
    private final RegistroPacienteService pacienteService;
    private final RegistroProfesionalService profesionalService;
    private final IIdentityProvider identityProvider;

    public RegistroServiceImpl(RegistroAdminService adminService,
                               RegistroPacienteService pacienteService,
                               RegistroProfesionalService profesionalService,
                               IIdentityProvider identityProvider) {
        this.adminService       = adminService;
        this.pacienteService    = pacienteService;
        this.profesionalService = profesionalService;
        this.identityProvider   = identityProvider;
    }

    /**
     * Registro de usuario administrador (sin perfil clínico).
     * Delega a {@link RegistroAdminService}, cuyo hook es un no-op.
     */
    @Override
    @Transactional
    public UsuarioDTO registrarUsuario(UsuarioDTO usuarioDTO) {
        PasswordPolicy.validar(usuarioDTO.getPassword());
        UsuarioDTO creado = adminService.registrar(
                new RegistroContexto(usuarioDTO, null, null)
        );
        crearCuentaDeAcceso(creado, null, usuarioDTO.getPassword());
        return creado;
    }

    /**
     * Registro de usuario + perfil de paciente.
     * Delega a {@link RegistroPacienteService}, que en su hook crea el Paciente.
     */
    @Override
    @Transactional
    public UsuarioDTO registrarPaciente(UsuarioDTO usuarioDTO, PacienteDTO pacienteDTO) {
        PasswordPolicy.validar(usuarioDTO.getPassword());
        UsuarioDTO creado = pacienteService.registrar(
                new RegistroContexto(usuarioDTO, pacienteDTO, null)
        );
        crearCuentaDeAcceso(creado, pacienteDTO.getEmail(), usuarioDTO.getPassword());
        return creado;
    }

    /**
     * Registro de usuario + perfil de profesional.
     * Delega a {@link RegistroProfesionalService}, que en su hook crea el Profesional
     * y publica el evento de dominio {@code profesional.creado}.
     */
    @Override
    @Transactional
    public UsuarioDTO registrarProfesional(UsuarioDTO usuarioDTO, ProfesionalDTO profesionalDTO) {
        PasswordPolicy.validar(usuarioDTO.getPassword());
        UsuarioDTO creado = profesionalService.registrar(
                new RegistroContexto(usuarioDTO, null, profesionalDTO)
        );
        crearCuentaDeAcceso(creado, null, usuarioDTO.getPassword());
        return creado;
    }

    private void crearCuentaDeAcceso(UsuarioDTO usuario, String email, String password) {
        identityProvider.crearCuenta(new CuentaNueva(
                usuario.getLogin(), usuario.getNombreCompleto(), email, password));
    }
}
