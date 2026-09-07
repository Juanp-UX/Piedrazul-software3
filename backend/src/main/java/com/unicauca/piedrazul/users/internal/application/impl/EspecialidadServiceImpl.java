package com.unicauca.piedrazul.users.internal.application.impl;



import com.unicauca.piedrazul.users.IEspecialidadService;
import com.unicauca.piedrazul.users.internal.domain.repository.EspecialidadRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EspecialidadServiceImpl implements IEspecialidadService {

    private final EspecialidadRepository especialidadRepository;

    public EspecialidadServiceImpl(EspecialidadRepository especialidadRepository) {
        this.especialidadRepository = especialidadRepository;
    }

    @Override
    public List<String> listarNombres() {
        return especialidadRepository.findAll()
                .stream()
                .map(e -> e.getNombre())
                .sorted()
                .collect(Collectors.toList());
    }
}