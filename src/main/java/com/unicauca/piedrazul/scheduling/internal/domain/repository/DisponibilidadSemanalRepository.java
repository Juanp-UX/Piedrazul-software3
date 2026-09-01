package com.unicauca.piedrazul.scheduling.internal.domain.repository;

import com.unicauca.piedrazul.scheduling.internal.domain.entity.DisponibilidadSemanal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DisponibilidadSemanalRepository extends JpaRepository<DisponibilidadSemanal, Long> {
    List<DisponibilidadSemanal> findByProfesionalId(Long profesionalId);
    List<DisponibilidadSemanal> findByProfesionalIdAndDiaSemana(Long profesionalId, Integer diaSemana);
}
