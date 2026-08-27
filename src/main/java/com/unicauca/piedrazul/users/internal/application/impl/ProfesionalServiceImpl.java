package com.unicauca.piedrazul.users.internal.application.impl;

import com.unicauca.piedrazul.users.IProfesionalService;
import com.unicauca.piedrazul.users.dto.ProfesionalDTO;
import com.unicauca.piedrazul.users.events.ProfesionalCreadoEvent;
import com.unicauca.piedrazul.users.internal.domain.entity.Profesional;
import com.unicauca.piedrazul.users.internal.domain.entity.Usuario;
import com.unicauca.piedrazul.users.internal.domain.exceptions.UsuarioNoEncontradoException;
import com.unicauca.piedrazul.users.internal.domain.repository.EspecialidadRepository;
import com.unicauca.piedrazul.users.internal.domain.repository.ProfesionalRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProfesionalServiceImpl implements IProfesionalService {

    private final ProfesionalRepository profesionalRepository;
    private final EspecialidadRepository especialidadRepository;
    private final ApplicationEventPublisher events;

    public ProfesionalServiceImpl(ProfesionalRepository profesionalRepository,
                                  EspecialidadRepository especialidadRepository,
                                  ApplicationEventPublisher events) {
        this.profesionalRepository  = profesionalRepository;
        this.especialidadRepository = especialidadRepository;
        this.events = events;
    }

    @Override
    @Transactional
    public Profesional crearProfesional(Usuario usuario, ProfesionalDTO dto) {

        var especialidad = especialidadRepository
                .findByNombre(dto.getEspecialidadNombre())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Especialidad no encontrada: " + dto.getEspecialidadNombre()));

        Profesional profesional = profesionalRepository.save(
                Profesional.builder()
                        .usuario(usuario)
                        .tipo(dto.getTipo())
                        .especialidad(especialidad)
                        .licenciaProfesional(dto.getLicenciaProfesional())
                        .activo(true)
                        .duracionCitaMinutos(dto.getDuracionCitaMinutos())
                        .build()
        );

        events.publishEvent(new ProfesionalCreadoEvent(
                usuario.getId(),
                usuario.getNombreCompleto(),
                profesional.getDuracionCitaMinutos()
        ));

        return profesional;
    }

    @Override
    public List<ProfesionalDTO> listarActivos() {
        return profesionalRepository.findByActivoTrue()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ProfesionalDTO buscarPorId(Long id) {

        return profesionalRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new UsuarioNoEncontradoException(
                        "Profesional no encontrado: " + id));
    }

    @Override
    public List<ProfesionalDTO> listarActivosPorEspecialidad(String especialidadNombre) {
        return profesionalRepository.findByEspecialidadNombreAndActivoTrue(especialidadNombre)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }


    private ProfesionalDTO toDTO(Profesional p) {
        return ProfesionalDTO.builder()
                .id(p.getUsuario().getId())
                .nombreCompleto(p.getUsuario().getNombreCompleto())
                .tipo(p.getTipo())
                .especialidadNombre(p.getEspecialidad() != null ? p.getEspecialidad().getNombre() : "")
                .licenciaProfesional(p.getLicenciaProfesional())
                .activo(p.getActivo())
                .duracionCitaMinutos(p.getDuracionCitaMinutos())
                .build();
    }
}