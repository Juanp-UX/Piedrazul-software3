package com.unicauca.piedrazul.scheduling.internal.application.impl;


import com.unicauca.piedrazul.scheduling.dto.CitaDTO;
import com.unicauca.piedrazul.scheduling.events.CitaAgendadaEvent;
import com.unicauca.piedrazul.scheduling.events.CitaCanceladaEvent;
import com.unicauca.piedrazul.scheduling.events.CitaCompletadaEvent;
import com.unicauca.piedrazul.scheduling.internal.application.interfaces.ICitaService;
import com.unicauca.piedrazul.scheduling.internal.application.interfaces.IConfiguracionAgendamientoService;
import com.unicauca.piedrazul.scheduling.internal.application.interfaces.IDiaNoDisponibleService;
import com.unicauca.piedrazul.scheduling.internal.domain.builder.CitaProgramadaBuilder;
import com.unicauca.piedrazul.scheduling.internal.domain.builder.DirectorCita;
import com.unicauca.piedrazul.scheduling.internal.domain.entity.Cita;
import com.unicauca.piedrazul.scheduling.internal.domain.entity.DisponibilidadSemanal;
import com.unicauca.piedrazul.scheduling.internal.domain.entity.enums.EstadoCita;
import com.unicauca.piedrazul.scheduling.internal.domain.exceptions.*;
import com.unicauca.piedrazul.scheduling.internal.domain.repository.BloqueoDisponibilidadRepository;
import com.unicauca.piedrazul.scheduling.internal.domain.repository.CitaRepository;
import com.unicauca.piedrazul.scheduling.internal.domain.repository.DisponibilidadSemanalRepository;
import com.unicauca.piedrazul.scheduling.internal.domain.state.CitaEstadoResolver;
import com.unicauca.piedrazul.users.IUsuarioService;
import com.unicauca.piedrazul.users.dto.UsuarioDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CitaServiceImpl implements ICitaService {

    // Maximum slot duration used to widen the candidate window when querying
    // for overlap. 480 minutes (8 hours) is safely larger than any real slot.
    private static final int MAX_DURACION_MINUTOS = 480;

    private final CitaRepository citaRepository;
    private final DisponibilidadSemanalRepository disponibilidadRepository;
    private final BloqueoDisponibilidadRepository bloqueoRepository;
    private final CitaEstadoResolver estadoResolver;
    private final IConfiguracionAgendamientoService configuracionService;
    private final IDiaNoDisponibleService diaNoDisponibleService;
    private final IUsuarioService usuarioService;
    private final ApplicationEventPublisher events;

    public CitaServiceImpl(CitaRepository citaRepository,
                           DisponibilidadSemanalRepository disponibilidadRepository,
                           BloqueoDisponibilidadRepository bloqueoRepository,
                           CitaEstadoResolver estadoResolver,
                           IConfiguracionAgendamientoService configuracionService,
                           IDiaNoDisponibleService diaNoDisponibleService,
                           IUsuarioService usuarioService,
                           ApplicationEventPublisher events) {
        this.citaRepository           = citaRepository;
        this.disponibilidadRepository = disponibilidadRepository;
        this.bloqueoRepository        = bloqueoRepository;
        this.estadoResolver           = estadoResolver;
        this.configuracionService     = configuracionService;
        this.diaNoDisponibleService   = diaNoDisponibleService;
        this.usuarioService           = usuarioService;
        this.events                   = events;
    }

    // ── Agendar ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public CitaDTO agendarCita(CitaDTO dto) {
        ZonedDateTime fechaHora = dto.getFechaHora();

        validarVentanaAgendamiento(fechaHora.toLocalDate());
        validarDiaNoDisponible(fechaHora.toLocalDate());

        // Resolve the slot duration from the professional's weekly availability
        // so it can be stored on the Cita and used in future overlap queries.
        int duracion = resolverDuracion(dto.getProfesionalId(), fechaHora);

        if (!isProfesionalDisponible(dto.getProfesionalId(), fechaHora, duracion)) {
            throw new HorarioOcupadoException();
        }

        UsuarioDTO paciente = usuarioService.buscarPorId(dto.getPacienteId());
        UsuarioDTO profesional = usuarioService.buscarPorId(dto.getProfesionalId());

        DirectorCita director = new DirectorCita();
        director.setCitaBuilder(new CitaProgramadaBuilder());
        director.construirCita(paciente, profesional, fechaHora, duracion);
        Cita cita = director.getCita();

        CitaDTO guardada = toDTO(citaRepository.save(cita));

        final CitaDTO citaGuardada = guardada;
        events.publishEvent(new CitaAgendadaEvent(
                guardada.getId(), paciente.getId(), paciente.getNombreCompleto(),
                profesional.getId(), profesional.getNombreCompleto(), fechaHora
        ));

        log.info("Cita agendada: paciente={} profesional={} fecha={}",
                paciente.getId(), profesional.getId(), fechaHora);
        return guardada;
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    @Override
    public CitaDTO buscarPorId(Long id) {
        return citaRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new CitaNoEncontradaException(id.toString()));
    }

    @Override
    public List<CitaDTO> listarPorPaciente(Long pacienteId) {
        return citaRepository.findByPacienteId(pacienteId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<CitaDTO> listarPorProfesional(Long profesionalId) {
        return citaRepository.findByProfesionalId(profesionalId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<CitaDTO> listarPorProfesionalYFecha(Long profesionalId, LocalDate fecha) {
        ZoneId zona = ZoneId.systemDefault();
        ZonedDateTime inicio = fecha.atStartOfDay(zona);
        ZonedDateTime fin    = fecha.atTime(LocalTime.MAX).atZone(zona);
        return citaRepository.findByProfesionalIdAndFechaHoraBetween(profesionalId, inicio, fin)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<ZonedDateTime> obtenerHorariosDisponibles(Long profesionalId, LocalDate fecha) {
        if (fecha.isAfter(configuracionService.obtenerFechaMaximaAgendamiento())) {
            return List.of();
        }
        if (diaNoDisponibleService.esFechaNoDisponible(fecha)) {
            return List.of();
        }

        int diaSemana = fecha.getDayOfWeek().getValue() % 7;

        return disponibilidadRepository
                .findByProfesionalIdAndDiaSemana(profesionalId, diaSemana)
                .stream()
                .flatMap(d -> {
                    List<ZonedDateTime> slots = new ArrayList<>();
                    LocalTime cursor = d.getHoraInicio();
                    while (!cursor.isAfter(d.getHoraFin().minusMinutes(d.getDuracionCitaMinutos()))) {
                        ZonedDateTime slot = ZonedDateTime.of(fecha, cursor, ZoneId.systemDefault());
                        slots.add(slot);
                        cursor = cursor.plusMinutes(d.getDuracionCitaMinutos());
                    }
                    return slots.stream();
                })
                .filter(slot ->
                        slot.isAfter(ZonedDateTime.now()) &&
                        // Bug-fix: use the duration-aware overlap check so that a slot
                        // mid-way through an active appointment is correctly hidden.
                        // Bug-fix: cancelled/completed rows are now excluded because
                        // isProfesionalDisponible only queries PROGRAMADA rows.
                        isProfesionalDisponible(profesionalId, slot,
                                resolverDuracion(profesionalId, slot)) &&
                        !bloqueoRepository.existeBloqueoEnFecha(profesionalId, slot)
                )
                .collect(Collectors.toList());
    }

    @Override
    public long contarCitasPorEstado(EstadoCita estado) {
        // FIX CRÍTICO: delegamos el COUNT al motor SQL.
        // El patrón anterior (findAll + stream + filter) descargaba la tabla
        // completa en heap, provocando OOM o latencias inaceptables en producción.
        // Spring Data genera: SELECT COUNT(*) FROM cita WHERE estado = ?
        return citaRepository.countByEstado(estado);
    }

    // ── Transiciones de estado ────────────────────────────────────────────────

    @Override
    @Transactional
    public CitaDTO cancelarCita(Long id) {
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new CitaNoEncontradaException(id.toString()));
        estadoResolver.resolve(cita.getEstado()).cancelar(cita);
        CitaDTO cancelada = toDTO(citaRepository.save(cita));
        final CitaDTO citaCancelada = cancelada;
        events.publishEvent(new CitaCanceladaEvent(
                cancelada.getId(),
                cancelada.getPacienteId(),
                cancelada.getProfesionalId(),
                cancelada.getFechaHora()
        ));
        log.info("Cita cancelada: id={}", id);
        return cancelada;
    }

    @Override
    @Transactional
    public CitaDTO completarCita(Long id) {
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new CitaNoEncontradaException(id.toString()));
        estadoResolver.resolve(cita.getEstado()).completar(cita);
        CitaDTO completada = toDTO(citaRepository.save(cita));
        final CitaDTO citaCompletada = completada;
        events.publishEvent(new CitaCompletadaEvent(
                completada.getId(),
                completada.getPacienteId(),
                completada.getProfesionalId(),
                completada.getFechaHora()
        ));
        log.info("Cita completada: id={}", id);
        return completada;
    }

    @Override
    @Transactional
    public CitaDTO actualizarCita(Long id, CitaDTO dto) {
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new CitaNoEncontradaException(id.toString()));

        if (cita.getEstado() != EstadoCita.programada) {
            throw new TransicionEstadoInvalidaException(
                    cita.getEstado(),
                    EstadoCita.programada
            );
        }

        ZonedDateTime nuevaFechaHora = dto.getFechaHora();

        if (!nuevaFechaHora.equals(cita.getFechaHora())) {
            validarVentanaAgendamiento(nuevaFechaHora.toLocalDate());
            validarDiaNoDisponible(nuevaFechaHora.toLocalDate());

            int duracion = resolverDuracion(cita.getProfesionalId(), nuevaFechaHora);

            // Exclude the appointment being rescheduled from the overlap check
            // by temporarily treating it as cancelled; we re-check against all
            // OTHER programada rows.
            if (!isProfesionalDisponibleExcluyendo(
                    cita.getProfesionalId(), nuevaFechaHora, duracion, id)) {
                throw new HorarioOcupadoException();
            }
            cita.setFechaHora(nuevaFechaHora);
            cita.setDuracionMinutos(duracion);
        }

        CitaDTO actualizada = toDTO(citaRepository.save(cita));
        log.info("Cita reprogramada: id={} nuevaFecha={}", id, nuevaFechaHora);
        return actualizada;
    }

    // ── Validaciones de política de agendamiento ─────────────────────────────

    private void validarVentanaAgendamiento(LocalDate fecha) {
        LocalDate fechaMaxima = configuracionService.obtenerFechaMaximaAgendamiento();
        if (fecha.isAfter(fechaMaxima)) {
            int semanas = configuracionService.obtener().getSemanasHabilitadas();
            throw new FueraDeVentanaAgendamientoException(semanas);
        }
        if (fecha.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("No se pueden agendar citas en fechas pasadas.");
        }
    }

    private void validarDiaNoDisponible(LocalDate fecha) {
        if (diaNoDisponibleService.esFechaNoDisponible(fecha)) {
            throw new FechaNoDisponibleException(fecha.toString());
        }
    }

    // ── Disponibilidad del profesional ────────────────────────────────────────

    /**
     * Returns true when the professional is free for the entire slot
     * [fechaHora, fechaHora + duracionMinutos).
     *
     * Bug-fix (cancelled slots): candidates are fetched with estado = PROGRAMADA
     * only, so cancelled/completed rows never block a slot.
     *
     * Bug-fix (duration blindness): overlap is tested with the standard
     * interval condition — two appointments overlap when startA < endB AND
     * startB < endA — so a 09:30 request is correctly blocked by a 09:00/60-min
     * appointment.
     */
    private boolean isProfesionalDisponible(Long profesionalId,
                                             ZonedDateTime fechaHora,
                                             int duracionMinutos) {
        if (fechaHora.isBefore(ZonedDateTime.now())) return false;
        if (bloqueoRepository.existeBloqueoEnFecha(profesionalId, fechaHora)) return false;
        if (!estaEnVentanaDisponibilidad(profesionalId, fechaHora)) return false;

        return !hayConflictoConProgramadas(profesionalId, fechaHora, duracionMinutos, null);
    }

    /**
     * Same as isProfesionalDisponible but ignores the appointment identified by
     * {@code excludeId}. Used when rescheduling so the appointment being moved
     * does not block its own target slot.
     */
    private boolean isProfesionalDisponibleExcluyendo(Long profesionalId,
                                                       ZonedDateTime fechaHora,
                                                       int duracionMinutos,
                                                       Long excludeId) {
        if (fechaHora.isBefore(ZonedDateTime.now())) return false;
        if (bloqueoRepository.existeBloqueoEnFecha(profesionalId, fechaHora)) return false;
        if (!estaEnVentanaDisponibilidad(profesionalId, fechaHora)) return false;

        return !hayConflictoConProgramadas(profesionalId, fechaHora, duracionMinutos, excludeId);
    }

    /**
     * Loads all PROGRAMADA appointments for the professional in a window wide
     * enough to catch any appointment that might overlap with [inicio, fin), then
     * checks the interval overlap condition in Java.
     *
     * Window: [inicio - MAX_DURACION, fin]
     *   – subtracting MAX_DURACION_MINUTOS ensures an appointment that started
     *     before "inicio" but extends into it is not missed.
     */
    private boolean hayConflictoConProgramadas(Long profesionalId,
                                                ZonedDateTime inicio,
                                                int duracionMinutos,
                                                Long excludeId) {
        ZonedDateTime fin           = inicio.plusMinutes(duracionMinutos);
        ZonedDateTime ventanaInicio = inicio.minusMinutes(MAX_DURACION_MINUTOS);

        List<Cita> candidatas = citaRepository
                .findByProfesionalIdAndEstadoAndFechaHoraBetween(
                        profesionalId, EstadoCita.programada, ventanaInicio, fin);

        return candidatas.stream()
                .filter(c -> excludeId == null || !excludeId.equals(c.getId()))
                .anyMatch(c -> {
                    ZonedDateTime existingStart = c.getFechaHora();
                    ZonedDateTime existingEnd   = existingStart.plusMinutes(c.getDuracionMinutos());
                    // Standard interval-overlap test: [A,B) ∩ [C,D) ≠ ∅  ⟺  A < D && C < B
                    return existingStart.isBefore(fin) && inicio.isBefore(existingEnd);
                });
    }

    /**
     * Verifies that fechaHora falls within the professional's configured weekly
     * availability window (not in the past, and within horaInicio..horaFin).
     */
    private boolean estaEnVentanaDisponibilidad(Long profesionalId, ZonedDateTime fechaHora) {
        int diaSemana = fechaHora.getDayOfWeek().getValue() % 7;
        LocalTime hora = fechaHora.toLocalTime();

        return disponibilidadRepository
                .findByProfesionalIdAndDiaSemana(profesionalId, diaSemana)
                .stream()
                .anyMatch(d ->
                        !hora.isBefore(d.getHoraInicio()) &&
                        !hora.isAfter(d.getHoraFin().minusMinutes(d.getDuracionCitaMinutos()))
                );
    }

    /**
     * Looks up the slot duration (in minutes) for the professional on the day
     * of fechaHora.  Falls back to DisponibilidadSemanal.duracionCitaMinutos
     * default (30) when no schedule is found.
     */
    private int resolverDuracion(Long profesionalId, ZonedDateTime fechaHora) {
        int diaSemana = fechaHora.getDayOfWeek().getValue() % 7;
        return disponibilidadRepository
                .findByProfesionalIdAndDiaSemana(profesionalId, diaSemana)
                .stream()
                .findFirst()
                .map(DisponibilidadSemanal::getDuracionCitaMinutos)
                .orElse(30);
    }

    private CitaDTO toDTO(Cita c) {
        return CitaDTO.builder()
                .id(c.getId())
                .pacienteId(c.getPacienteId())
                .pacienteNombre(c.getPacienteNombre())
                .profesionalId(c.getProfesionalId())
                .profesionalNombre(c.getProfesionalNombre())
                .fechaHora(c.getFechaHora())
                .estado(c.getEstado())
                .build();
    }
}
