package com.unicauca.piedrazul.users.internal.application.template.impl;


import com.unicauca.piedrazul.users.IPacienteService;
import com.unicauca.piedrazul.users.IUsuarioService;
import com.unicauca.piedrazul.users.internal.application.template.RegistroContexto;
import com.unicauca.piedrazul.users.internal.application.template.RegistroTemplate;
import com.unicauca.piedrazul.users.internal.domain.entity.Usuario;
import org.springframework.stereotype.Service;

/**
 * Subclase concreta del patrón Template Method para el registro de usuarios
 * con rol <b>PACIENTE</b>.
 *
 * <p>Sobrescribe el hook {@link #vincularPerfil} para crear el perfil clínico
 * del paciente (datos personales, contacto, etc.) y asociarlo al usuario base
 * recién creado en el paso anterior del template method.
 *
 * <p>El orden de ejecución garantizado por {@link RegistroTemplate#registrar}:
 * <ol>
 *   <li>Crear usuario base (RegistroTemplate).</li>
 *   <li><b>Crear y asociar Paciente (este método).</b></li>
 *   <li>Retornar DTO (RegistroTemplate).</li>
 * </ol>
 */
@Service
public class RegistroPacienteService extends RegistroTemplate {

    private final IPacienteService pacienteService;

    public RegistroPacienteService(IUsuarioService usuarioService,
                                   IPacienteService pacienteService) {
        super(usuarioService);
        this.pacienteService = pacienteService;
    }

    /**
     * Hook — crea el perfil de Paciente y lo vincula al usuario base.
     *
     * @param usuario  usuario base ya persistido por el template method.
     * @param contexto contexto que contiene el {@code PacienteDTO} con los
     *                 datos del perfil a crear.
     */
    @Override
    protected void vincularPerfil(Usuario usuario, RegistroContexto contexto) {
        pacienteService.crearPaciente(usuario, contexto.pacienteDTO());
    }
}
