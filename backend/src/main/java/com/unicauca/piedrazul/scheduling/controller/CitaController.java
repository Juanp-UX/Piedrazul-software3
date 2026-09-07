package com.unicauca.piedrazul.scheduling.controller;

import com.unicauca.piedrazul.scheduling.AgendamientoFacade;
import com.unicauca.piedrazul.scheduling.dto.CitaDTO;
import com.unicauca.piedrazul.scheduling.internal.domain.entity.enums.EstadoCita;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Patrón Facade — «Client»
 *
 * CAMBIOS respecto al original:
 *   - Se elimina la dependencia directa de ICitaService
 *   - Se inyecta IAgendamientoFacade en su lugar
 *   - Todos los endpoints delegan a la Facade con el mismo resultado
 *   - Las URLs y respuestas HTTP son exactamente iguales al original
 * El controller ahora tiene UNA sola dependencia en lugar de cuatro.
 */
@RestController
@RequestMapping("/api/scheduling/citas")
public class CitaController {

    private final AgendamientoFacade agendamientoFacade;

    public CitaController(AgendamientoFacade agendamientoFacade) {
        this.agendamientoFacade = agendamientoFacade;
    }

    // ── Agendar ──────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<CitaDTO> agendar(@RequestBody CitaDTO dto) {
        return ResponseEntity.ok(agendamientoFacade.agendarCita(dto));
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<CitaDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(agendamientoFacade.buscarCitaPorId(id));
    }

    @GetMapping("/paciente/{pacienteId}")
    public ResponseEntity<List<CitaDTO>> listarPorPaciente(@PathVariable Long pacienteId) {
        return ResponseEntity.ok(agendamientoFacade.listarCitasPorPaciente(pacienteId));
    }

    @GetMapping("/profesional/{profesionalId}")
    public ResponseEntity<List<CitaDTO>> listarPorProfesional(@PathVariable Long profesionalId) {
        return ResponseEntity.ok(agendamientoFacade.listarCitasPorProfesional(profesionalId));
    }

    @GetMapping("/profesional/{profesionalId}/fecha")
    public ResponseEntity<List<CitaDTO>> listarPorProfesionalYFecha(
            @PathVariable Long profesionalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

        List<CitaDTO> citas = agendamientoFacade.listarCitasPorProfesionalYFecha(profesionalId, fecha);
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(citas.size()))
                .body(citas);
    }

    @GetMapping("/profesional/{profesionalId}/disponibilidad")
    public ResponseEntity<List<ZonedDateTime>> obtenerHorariosDisponibles(
            @PathVariable Long profesionalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(agendamientoFacade.obtenerHorariosDisponibles(profesionalId, fecha));
    }

    // ── Transiciones de estado ────────────────────────────────────────────────

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<CitaDTO> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(agendamientoFacade.cancelarCita(id));
    }

    @PatchMapping("/{id}/completar")
    public ResponseEntity<CitaDTO> completar(@PathVariable Long id) {
        return ResponseEntity.ok(agendamientoFacade.completarCita(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CitaDTO> actualizar(
            @PathVariable Long id,
            @RequestBody CitaDTO dto) {
        return ResponseEntity.ok(agendamientoFacade.actualizarCita(id, dto));
    }

    // ── Conteo ────────────────────────────────────────────────────────────────

    @GetMapping("/contar")
    public ResponseEntity<Long> contarPorEstado(@RequestParam EstadoCita estado) {
        return ResponseEntity.ok(agendamientoFacade.contarCitasPorEstado(estado));
    }
}
