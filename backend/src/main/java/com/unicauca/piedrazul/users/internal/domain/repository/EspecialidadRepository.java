package com.unicauca.piedrazul.users.internal.domain.repository;

import com.unicauca.piedrazul.users.internal.domain.entity.Especialidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EspecialidadRepository extends JpaRepository<Especialidad, Integer> {
    Optional<Especialidad> findByNombre(String nombre);
}