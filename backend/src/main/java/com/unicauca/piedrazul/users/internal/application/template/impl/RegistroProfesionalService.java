package com.unicauca.piedrazul.users.internal.application.template.impl;

import com.unicauca.piedrazul.users.IProfesionalService;
import com.unicauca.piedrazul.users.IUsuarioService;
import com.unicauca.piedrazul.users.internal.application.template.RegistroContexto;
import com.unicauca.piedrazul.users.internal.application.template.RegistroTemplate;
import com.unicauca.piedrazul.users.internal.domain.entity.Usuario;
import org.springframework.stereotype.Service;

/**
 * Subclase concreta del patrón Template Method para el registro de usuarios
 * con rol <b>PROFESIONAL</b>.
 *
 * <p>Sobrescribe el hook {@link #vincularPerfil} para crear el perfil
 * profesional (especialidad, tipo, licencia, duración de cita, etc.),
 * asociarlo al usuario base y publicar el evento {@code profesional.creado}
 * en RabbitMQ — toda esa lógica ya encapsulada en {@link IProfesionalService}.
 *
 * <p>El orden de ejecución garantizado por {@link RegistroTemplate#registrar}:
 * <ol>
 *   <li>Crear usuario base (RegistroTemplate).</li>
 *   <li><b>Crear y asociar Profesional + publicar evento (este método).</b></li>
 *   <li>Retornar DTO (RegistroTemplate).</li>
 * </ol>
 */
@Service
public class RegistroProfesionalService extends RegistroTemplate {

    private final IProfesionalService profesionalService;

    public RegistroProfesionalService(IUsuarioService usuarioService,
                                      IProfesionalService profesionalService) {
        super(usuarioService);
        this.profesionalService = profesionalService;
    }

    /**
     * Hook — crea el perfil de Profesional, lo vincula al usuario base y
     * dispara el evento de dominio {@code profesional.creado}.
     *
     * @param usuario  usuario base ya persistido por el template method.
     * @param contexto contexto que contiene el {@code ProfesionalDTO} con los
     *                 datos del perfil a crear (especialidad, licencia, etc.).
     */
    @Override
    protected void vincularPerfil(Usuario usuario, RegistroContexto contexto) {
        profesionalService.crearProfesional(usuario, contexto.profesionalDTO());
    }
}
