package com.unicauca.piedrazul.users.internal.application.impl;

import com.unicauca.piedrazul.users.IPacienteService;
import com.unicauca.piedrazul.users.dto.PacienteDTO;
import com.unicauca.piedrazul.users.internal.domain.entity.Paciente;
import com.unicauca.piedrazul.users.internal.domain.entity.Usuario;
import com.unicauca.piedrazul.users.internal.domain.exceptions.PacienteNoEncontradoException;
import com.unicauca.piedrazul.users.internal.domain.repository.PacienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;


@Service
public class PacienteServiceImpl implements IPacienteService {
    private final PacienteRepository pacienteRepository;

    public PacienteServiceImpl(PacienteRepository pacienteRepository){
        this.pacienteRepository=pacienteRepository;
    }

    @Override
    @Transactional
    public void crearPaciente(Usuario usuario, PacienteDTO dto) {
        pacienteRepository.save(Paciente.builder()
                .usuario(usuario)
                .nombreCompleto(dto.getNombreCompleto())
                .cedulaIdentidad(dto.getCedulaIdentidad())
                .fechaNacimiento(dto.getFechaNacimiento())
                .telefono(dto.getTelefono())
                .email(dto.getEmail())
                .direccion(dto.getDireccion())
                .creadoEn(ZonedDateTime.now())
                .build());
    }
    @Override
    @Transactional(readOnly = true)
    public PacienteDTO buscarPorCedula(String cedula) {
        return pacienteRepository.findByCedulaIdentidad(cedula)
                .map(this::toDTO)
                .orElseThrow(() -> new PacienteNoEncontradoException(cedula));
    }

    private PacienteDTO toDTO(Paciente p) {
        return PacienteDTO.builder()
                .id(p.getId())
                .usuarioId(p.getUsuario() != null ? p.getUsuario().getId() : null)
                .nombreCompleto(p.getNombreCompleto())
                .cedulaIdentidad(p.getCedulaIdentidad())
                .fechaNacimiento(p.getFechaNacimiento())
                .telefono(p.getTelefono())
                .email(p.getEmail())
                .direccion(p.getDireccion())
                .build();
    }
}