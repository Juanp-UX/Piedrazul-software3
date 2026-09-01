package com.unicauca.piedrazul.scheduling.internal.application.listener;

import com.unicauca.piedrazul.scheduling.internal.domain.entity.DisponibilidadSemanal;
import com.unicauca.piedrazul.scheduling.internal.domain.repository.DisponibilidadSemanalRepository;
import com.unicauca.piedrazul.users.events.ProfesionalCreadoEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class ProfesionalCreadoListener {

    private static final int[] DIAS_LABORALES = {1, 2, 3, 4, 5};
    private static final LocalTime HORA_INICIO_DEFAULT = LocalTime.of(7, 0);
    private static final LocalTime HORA_FIN_DEFAULT    = LocalTime.of(14, 0);

    private final DisponibilidadSemanalRepository disponibilidadRepository;

    public ProfesionalCreadoListener(DisponibilidadSemanalRepository disponibilidadRepository) {
        this.disponibilidadRepository = disponibilidadRepository;
    }

    @ApplicationModuleListener
    void on(ProfesionalCreadoEvent event) {
        log.info("Evento recibido: profesional.creado profesionalId={} duracion={}min",
                event.profesionalId(), event.duracionCitaMinutos());

        int duracion = (event.duracionCitaMinutos() != null && event.duracionCitaMinutos() > 0)
                ? event.duracionCitaMinutos()
                : 30;

        List<DisponibilidadSemanal> slots = new ArrayList<>();
        for (int dia : DIAS_LABORALES) {
            slots.add(DisponibilidadSemanal.builder()
                    .profesionalId(event.profesionalId())
                    .diaSemana(dia)
                    .horaInicio(HORA_INICIO_DEFAULT)
                    .horaFin(HORA_FIN_DEFAULT)
                    .duracionCitaMinutos(duracion)
                    .build());
        }

        disponibilidadRepository.saveAll(slots);
        log.info("Disponibilidad semanal creada para profesionalId={} ({} días)",
                event.profesionalId(), slots.size());
    }
}