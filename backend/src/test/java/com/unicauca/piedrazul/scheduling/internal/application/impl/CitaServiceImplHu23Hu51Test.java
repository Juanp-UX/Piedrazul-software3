package com.unicauca.piedrazul.scheduling.internal.application.impl;

import com.unicauca.piedrazul.scheduling.dto.CitaDTO;
import com.unicauca.piedrazul.scheduling.dto.ReporteCitasMensualDTO;
import com.unicauca.piedrazul.scheduling.events.CitaCanceladaEvent;
import com.unicauca.piedrazul.scheduling.internal.domain.entity.Cita;
import com.unicauca.piedrazul.scheduling.internal.domain.entity.enums.EstadoCita;
import com.unicauca.piedrazul.scheduling.internal.domain.exceptions.CancelacionFueraDePlazoException;
import com.unicauca.piedrazul.scheduling.internal.domain.exceptions.TransicionEstadoInvalidaException;
import com.unicauca.piedrazul.scheduling.internal.domain.repository.BloqueoDisponibilidadRepository;
import com.unicauca.piedrazul.scheduling.internal.domain.repository.CitaRepository;
import com.unicauca.piedrazul.scheduling.internal.domain.repository.DisponibilidadSemanalRepository;
import com.unicauca.piedrazul.scheduling.internal.domain.state.CitaEstadoResolver;
import com.unicauca.piedrazul.scheduling.internal.domain.state.EstadoCanceladaHandler;
import com.unicauca.piedrazul.scheduling.internal.domain.state.EstadoCompletadaHandler;
import com.unicauca.piedrazul.scheduling.internal.domain.state.EstadoProgramadaHandler;
import com.unicauca.piedrazul.scheduling.internal.application.interfaces.IConfiguracionAgendamientoService;
import com.unicauca.piedrazul.scheduling.internal.application.interfaces.IDiaNoDisponibleService;
import com.unicauca.piedrazul.users.IPacienteService;
import com.unicauca.piedrazul.users.IUsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** HU-2.3 (cancelación por el paciente) y HU-5.1 (reporte por mes y médico). */
class CitaServiceImplHu23Hu51Test {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    private CitaRepository citaRepository;
    private ApplicationEventPublisher events;
    private CitaServiceImpl service;

    @BeforeEach
    void setUp() {
        citaRepository = mock(CitaRepository.class);
        events = mock(ApplicationEventPublisher.class);
        CitaEstadoResolver resolver = new CitaEstadoResolver(List.of(
                new EstadoProgramadaHandler(), new EstadoCanceladaHandler(), new EstadoCompletadaHandler()));
        service = new CitaServiceImpl(citaRepository,
                mock(DisponibilidadSemanalRepository.class), mock(BloqueoDisponibilidadRepository.class),
                resolver, mock(IConfiguracionAgendamientoService.class), mock(IDiaNoDisponibleService.class),
                mock(IUsuarioService.class), mock(IPacienteService.class), events);
        when(citaRepository.save(any(Cita.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Cita cita(long id, long pacienteId, ZonedDateTime fecha, EstadoCita estado) {
        return Cita.builder().id(id).pacienteId(pacienteId).pacienteNombre("Ana")
                .profesionalId(10L).profesionalNombre("Dr. Perez")
                .fechaHora(fecha).duracionMinutos(30).estado(estado).build();
    }

    // ── HU-2.3 ──────────────────────────────────────────────────────────────

    @Test
    void cancelar_citaConAnticipacion_cambiaEstadoGuardaMotivoYPublicaEvento() {
        Cita c = cita(1, 5, ZonedDateTime.now(BOGOTA).plusDays(5), EstadoCita.programada);
        when(citaRepository.findById(1L)).thenReturn(Optional.of(c));

        CitaDTO dto = service.cancelarCitaPorPaciente(1L, 5L, "  Viaje imprevisto  ");

        assertEquals(EstadoCita.cancelada, dto.getEstado());
        assertEquals("Viaje imprevisto", dto.getMotivoCancelacion());
        assertNotNull(c.getCanceladaEn());
        verify(events).publishEvent(any(CitaCanceladaEvent.class));
    }

    @Test
    void cancelar_conMenosDeDosDias_esRechazada() {
        Cita c = cita(1, 5, ZonedDateTime.now(BOGOTA).plusDays(1), EstadoCita.programada);
        when(citaRepository.findById(1L)).thenReturn(Optional.of(c));

        assertThrows(CancelacionFueraDePlazoException.class,
                () -> service.cancelarCitaPorPaciente(1L, 5L, "motivo"));
        assertEquals(EstadoCita.programada, c.getEstado());
        verify(citaRepository, never()).save(any());
    }

    @Test
    void cancelar_justificacionVacia_esRechazada() {
        Cita c = cita(1, 5, ZonedDateTime.now(BOGOTA).plusDays(5), EstadoCita.programada);
        when(citaRepository.findById(1L)).thenReturn(Optional.of(c));

        assertThrows(IllegalArgumentException.class, () -> service.cancelarCitaPorPaciente(1L, 5L, "   "));
        assertThrows(IllegalArgumentException.class, () -> service.cancelarCitaPorPaciente(1L, 5L, null));
        verify(citaRepository, never()).save(any());
    }

    @Test
    void cancelar_citaDeOtroPaciente_esDenegada() {
        Cita c = cita(1, 5, ZonedDateTime.now(BOGOTA).plusDays(5), EstadoCita.programada);
        when(citaRepository.findById(1L)).thenReturn(Optional.of(c));

        assertThrows(AccessDeniedException.class, () -> service.cancelarCitaPorPaciente(1L, 99L, "motivo"));
        verify(citaRepository, never()).save(any());
    }

    @Test
    void cancelar_citaYaCancelada_lanzaTransicionInvalida() {
        Cita c = cita(1, 5, ZonedDateTime.now(BOGOTA).plusDays(5), EstadoCita.cancelada);
        when(citaRepository.findById(1L)).thenReturn(Optional.of(c));

        assertThrows(TransicionEstadoInvalidaException.class,
                () -> service.cancelarCitaPorPaciente(1L, 5L, "motivo"));
    }

    // ── HU-5.1 ──────────────────────────────────────────────────────────────

    @Test
    void reporte_agrupaPorMesYMedico_conDoceMeses() {
        Cita enero1 = cita(1, 5, ZonedDateTime.of(2026, 1, 10, 9, 0, 0, 0, BOGOTA), EstadoCita.programada);
        Cita enero2 = cita(2, 6, ZonedDateTime.of(2026, 1, 11, 9, 0, 0, 0, BOGOTA), EstadoCita.completada);
        Cita marzoOtro = cita(3, 5, ZonedDateTime.of(2026, 3, 2, 9, 0, 0, 0, BOGOTA), EstadoCita.programada);
        marzoOtro.setProfesionalId(11L);
        marzoOtro.setProfesionalNombre("Dra. Gomez");
        // 31 dic 23:30 hora Bogotá (= 1 ene UTC) debe contarse en diciembre
        Cita finDeAnio = cita(4, 5, ZonedDateTime.of(2026, 12, 31, 23, 30, 0, 0, BOGOTA), EstadoCita.programada);
        when(citaRepository.findByFechaHoraGreaterThanEqualAndFechaHoraLessThan(any(), any()))
                .thenReturn(List.of(enero1, enero2, marzoOtro, finDeAnio));

        ReporteCitasMensualDTO r = service.reporteCitasPorMesYMedico(2026);

        assertEquals(2026, r.getAnio());
        assertEquals(4, r.getTotalCitas());
        assertEquals(2, r.getSeries().size());
        // orden alfabético: Dra. Gomez, Dr. Perez
        var gomez = r.getSeries().get(0);
        var perez = r.getSeries().get(1);
        assertEquals("Dra. Gomez", gomez.getProfesionalNombre());
        assertEquals(12, gomez.getCitasPorMes().size());
        assertEquals(1L, gomez.getCitasPorMes().get(2));
        assertEquals(2L, perez.getCitasPorMes().get(0));
        assertEquals(1L, perez.getCitasPorMes().get(11));
        assertEquals(3L, perez.getTotal());
    }

    @Test
    void reporte_anioSinCitas_devuelveSeriesVacias() {
        when(citaRepository.findByFechaHoraGreaterThanEqualAndFechaHoraLessThan(any(), any()))
                .thenReturn(List.of());
        ReporteCitasMensualDTO r = service.reporteCitasPorMesYMedico(2025);
        assertEquals(0, r.getTotalCitas());
        assertTrue(r.getSeries().isEmpty());
    }

    @Test
    void reporte_anioInvalido_esRechazado() {
        assertThrows(IllegalArgumentException.class, () -> service.reporteCitasPorMesYMedico(1800));
        assertThrows(IllegalArgumentException.class, () -> service.reporteCitasPorMesYMedico(3000));
    }
}
