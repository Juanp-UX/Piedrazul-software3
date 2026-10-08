package com.unicauca.piedrazul.scheduling.internal.application.impl;


import com.unicauca.piedrazul.scheduling.dto.CitaDTO;
import com.unicauca.piedrazul.scheduling.dto.HistorialCitasPacienteDTO;
import com.unicauca.piedrazul.scheduling.events.CitaAgendadaEvent;
import com.unicauca.piedrazul.scheduling.events.CitaCanceladaEvent;
import com.unicauca.piedrazul.scheduling.events.CitaCompletadaEvent;
import com.unicauca.piedrazul.scheduling.internal.application.interfaces.ICitaService;
import com.unicauca.piedrazul.scheduling.internal.application.interfaces.IConfiguracionAgendamientoService;
import com.unicauca.piedrazul.scheduling.internal.application.interfaces.IDiaNoDisponibleService;
import com.unicauca.piedrazul.scheduling.internal.domain.builder.CitaProgramadaBuilder;
import com.unicauca.piedrazul.scheduling.internal.domain.builder.DirectorCita;
import com.unicauca.piedrazul.scheduling.internal.domain.entity.Cita;
import com.unicauca.piedrazul.scheduling.internal.domain.entity.enums.EstadoCita;
import com.unicauca.piedrazul.scheduling.internal.domain.exceptions.*;
import com.unicauca.piedrazul.scheduling.internal.domain.repository.BloqueoDisponibilidadRepository;
import com.unicauca.piedrazul.scheduling.internal.domain.repository.CitaRepository;
import com.unicauca.piedrazul.scheduling.internal.domain.repository.DisponibilidadSemanalRepository;
import com.unicauca.piedrazul.scheduling.internal.domain.state.CitaEstadoResolver;
import com.unicauca.piedrazul.users.IPacienteService;
import com.unicauca.piedrazul.users.IUsuarioService;
import com.unicauca.piedrazul.users.dto.PacienteDTO;
import com.unicauca.piedrazul.users.dto.UsuarioDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


@Service
@Slf4j
public class CitaServiceImpl implements ICitaService {

    private static final int MAX_DURACION_MINUTOS = 480;

    private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Bogota");

    // HU-1.2: formato del archivo exportado.
    // Separador ';' y BOM UTF-8 para que Excel (configuración regional es-CO) lo abra
    // en columnas y con tildes correctas; Google Sheets y LibreOffice también lo detectan.
    private static final char CSV_SEPARADOR = ';';
    private static final String CSV_BOM = "\uFEFF";
    private static final String CSV_SALTO = "\r\n";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_HORA  = DateTimeFormatter.ofPattern("HH:mm");
    private static final int MAX_LONGITUD_CEDULA = 20;

    private final CitaRepository citaRepository;
    private final DisponibilidadSemanalRepository disponibilidadRepository;
    private final BloqueoDisponibilidadRepository bloqueoRepository;
    private final CitaEstadoResolver estadoResolver;
    private final IConfiguracionAgendamientoService configuracionService;
    private final IDiaNoDisponibleService diaNoDisponibleService;
    private final IUsuarioService usuarioService;
    private final IPacienteService pacienteService;
    private final ApplicationEventPublisher events;

    public CitaServiceImpl(CitaRepository citaRepository,
                           DisponibilidadSemanalRepository disponibilidadRepository,
                           BloqueoDisponibilidadRepository bloqueoRepository,
                           CitaEstadoResolver estadoResolver,
                           IConfiguracionAgendamientoService configuracionService,
                           IDiaNoDisponibleService diaNoDisponibleService,
                           IUsuarioService usuarioService,
                           IPacienteService pacienteService,
                           ApplicationEventPublisher events) {
        this.citaRepository           = citaRepository;
        this.disponibilidadRepository = disponibilidadRepository;
        this.bloqueoRepository        = bloqueoRepository;
        this.estadoResolver           = estadoResolver;
        this.configuracionService     = configuracionService;
        this.diaNoDisponibleService   = diaNoDisponibleService;
        this.usuarioService           = usuarioService;
        this.pacienteService          = pacienteService;
        this.events                   = events;
    }

    // ── Agendar ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public CitaDTO agendarCita(CitaDTO dto) {
        ZonedDateTime fechaHora = normalizarFechaHora(dto.getFechaHora());

        validarVentanaAgendamiento(fechaHora.toLocalDate());
        validarDiaNoDisponible(fechaHora.toLocalDate());

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
        ZonedDateTime inicio = fecha.atStartOfDay(ZONA_NEGOCIO);
        ZonedDateTime fin    = fecha.atTime(LocalTime.MAX).atZone(ZONA_NEGOCIO);
        return citaRepository.findByProfesionalIdAndFechaHoraBetween(profesionalId, inicio, fin)
                .stream()
                .sorted(Comparator.comparing(Cita::getFechaHora))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ── HU-1.2: exportar citas del día de un profesional (CSV) ───────────────

    @Override
    public byte[] exportarCitasCsv(Long profesionalId, LocalDate fecha) {
        List<CitaDTO> citas = listarPorProfesionalYFecha(profesionalId, fecha);

        StringBuilder csv = new StringBuilder(CSV_BOM);
        csv.append(filaCsv("Fecha", "Hora", "Paciente", "Médico/Terapista", "Estado"));
        for (CitaDTO c : citas) {
            ZonedDateTime fechaHora = c.getFechaHora().withZoneSameInstant(ZONA_NEGOCIO);
            csv.append(filaCsv(
                    fechaHora.format(FORMATO_FECHA),
                    fechaHora.format(FORMATO_HORA),
                    c.getPacienteNombre(),
                    c.getProfesionalNombre(),
                    c.getEstado() != null ? c.getEstado().name() : ""));
        }

        log.info("Exportación CSV: profesional={} fecha={} filas={}", profesionalId, fecha, citas.size());
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String filaCsv(String... campos) {
        StringBuilder fila = new StringBuilder();
        for (int i = 0; i < campos.length; i++) {
            if (i > 0) fila.append(CSV_SEPARADOR);
            fila.append(escaparCampoCsv(campos[i]));
        }
        return fila.append(CSV_SALTO).toString();
    }

    /**
     * Escapa un campo CSV: lo encierra en comillas si contiene separador,
     * comillas o saltos de línea, y neutraliza la "inyección de fórmulas"
     * (valores que empiezan por = + - @ se interpretarían como fórmula al
     * abrirse en una hoja de cálculo; el nombre del paciente es texto libre).
     */
    private String escaparCampoCsv(String valor) {
        if (valor == null || valor.isEmpty()) return "";
        String v = valor;
        char primero = v.charAt(0);
        if (primero == '=' || primero == '+' || primero == '-' || primero == '@'
                || primero == '\t' || primero == '\r') {
            v = "'" + v;
        }
        boolean requiereComillas = v.indexOf(CSV_SEPARADOR) >= 0 || v.indexOf('"') >= 0
                || v.indexOf('\n') >= 0 || v.indexOf('\r') >= 0;
        if (requiereComillas) {
            v = "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }

    // ── HU-4.1: historial de citas de un paciente por cédula ─────────────────

    @Override
    public HistorialCitasPacienteDTO consultarHistorialPorCedula(String cedula) {
        if (cedula == null || cedula.trim().isEmpty()) {
            throw new IllegalArgumentException("La cédula del paciente es obligatoria.");
        }
        String cedulaNormalizada = cedula.trim();
        if (cedulaNormalizada.length() > MAX_LONGITUD_CEDULA) {
            throw new IllegalArgumentException(
                    "La cédula no puede superar los " + MAX_LONGITUD_CEDULA + " caracteres.");
        }

        // Buscar paciente (lanza PacienteNoEncontradoException -> 404 si no existe)
        PacienteDTO paciente = pacienteService.buscarPorCedula(cedulaNormalizada);

        // Cita.pacienteId referencia al Usuario.id del paciente
        List<CitaDTO> citas = citaRepository.findByPacienteId(paciente.getUsuarioId())
                .stream()
                .sorted(Comparator.comparing(Cita::getFechaHora).reversed())
                .map(this::toDTO)
                .collect(Collectors.toList());

        return HistorialCitasPacienteDTO.builder()
                .pacienteId(paciente.getUsuarioId())
                .pacienteNombre(paciente.getNombreCompleto())
                .cedulaIdentidad(paciente.getCedulaIdentidad())
                .citas(citas)
                .build();
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
                        ZonedDateTime slot = ZonedDateTime.of(fecha, cursor, ZONA_NEGOCIO);
                        slots.add(slot);
                        cursor = cursor.plusMinutes(d.getDuracionCitaMinutos());
                    }
                    return slots.stream();
                })
                .filter(slot ->
                        slot.isAfter(ZonedDateTime.now(ZONA_NEGOCIO)) &&
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

        ZonedDateTime nuevaFechaHora = normalizarFechaHora(dto.getFechaHora());

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
    private ZonedDateTime normalizarFechaHora(ZonedDateTime fechaHora) {
        if (fechaHora == null) {
            throw new IllegalArgumentException("La fecha y hora de la cita son obligatorias.");
        }

        return fechaHora.withZoneSameInstant(ZONA_NEGOCIO);
    }

    private void validarVentanaAgendamiento(LocalDate fecha) {
        LocalDate fechaMaxima = configuracionService.obtenerFechaMaximaAgendamiento();
        if (fecha.isAfter(fechaMaxima)) {
            int semanas = configuracionService.obtener().getSemanasHabilitadas();
            throw new FueraDeVentanaAgendamientoException(semanas);
        }
        if (fecha.isBefore(LocalDate.now(ZONA_NEGOCIO))) {
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
        if (fechaHora.isBefore(ZonedDateTime.now(ZONA_NEGOCIO))) return false;
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
        if (fechaHora.isBefore(ZonedDateTime.now(ZONA_NEGOCIO))) return false;
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
    private boolean estaEnVentanaDisponibilidad(
            Long profesionalId,
            ZonedDateTime fechaHora) {

        ZonedDateTime fechaHoraBogota =
                fechaHora.withZoneSameInstant(ZONA_NEGOCIO);

        int diaSemana =
                fechaHoraBogota.getDayOfWeek().getValue() % 7;

        LocalTime hora =
                fechaHoraBogota.toLocalTime();

        return disponibilidadRepository
                .findByProfesionalIdAndDiaSemana(profesionalId, diaSemana)
                .stream()
                .anyMatch(d -> {
                    int duracion = d.getDuracionCitaMinutos() != null
                            ? d.getDuracionCitaMinutos()
                            : 30;

                    LocalTime ultimaHoraInicioPermitida =
                            d.getHoraFin().minusMinutes(duracion);

                    return !hora.isBefore(d.getHoraInicio())
                            && !hora.isAfter(ultimaHoraInicioPermitida);
                });
    }

    /**
     * Looks up the slot duration (in minutes) for the professional on the day
     * of fechaHora.  Falls back to DisponibilidadSemanal.duracionCitaMinutos
     * default (30) when no schedule is found.
     */
    private int resolverDuracion(
            Long profesionalId,
            ZonedDateTime fechaHora) {

        ZonedDateTime fechaHoraBogota =
                fechaHora.withZoneSameInstant(ZONA_NEGOCIO);

        int diaSemana =
                fechaHoraBogota.getDayOfWeek().getValue() % 7;

        LocalTime hora =
                fechaHoraBogota.toLocalTime();
        return disponibilidadRepository
                .findByProfesionalIdAndDiaSemana(profesionalId, diaSemana)
                .stream()
                .filter(d -> {
                    int duracion = d.getDuracionCitaMinutos() != null
                            ? d.getDuracionCitaMinutos()
                            : 30;

                    LocalTime ultimaHoraInicioPermitida =
                            d.getHoraFin().minusMinutes(duracion);

                    return !hora.isBefore(d.getHoraInicio())
                            && !hora.isAfter(ultimaHoraInicioPermitida);
                })
                .findFirst()
                .map(d -> d.getDuracionCitaMinutos() != null
                        ? d.getDuracionCitaMinutos()
                        : 30)
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