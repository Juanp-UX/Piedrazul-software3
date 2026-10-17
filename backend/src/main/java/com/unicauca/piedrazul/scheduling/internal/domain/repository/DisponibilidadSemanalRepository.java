package com.unicauca.piedrazul.scheduling.internal.domain.repository;

import com.unicauca.piedrazul.scheduling.internal.domain.entity.DisponibilidadSemanal;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DisponibilidadSemanalRepository extends JpaRepository<DisponibilidadSemanal, Long> {
    List<DisponibilidadSemanal> findByProfesionalId(Long profesionalId);
    List<DisponibilidadSemanal> findByProfesionalIdAndDiaSemana(Long profesionalId, Integer diaSemana);

    /**
     * Bloqueo pesimista (SELECT ... FOR UPDATE) sobre las franjas del profesional.
     * Se usa como "mutex" por profesional para serializar el agendamiento y la
     * reprogramación: la transacción que lo obtiene primero termina de validar e
     * insertar antes de que otra pueda ejecutar su chequeo de solapamiento.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d.id from DisponibilidadSemanal d where d.profesionalId = :profesionalId")
    List<Long> bloquearPorProfesionalId(@Param("profesionalId") Long profesionalId);
}
