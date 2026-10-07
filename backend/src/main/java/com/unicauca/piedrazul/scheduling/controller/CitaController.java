package com.unicauca.piedrazul.scheduling.controller;

import com.unicauca.piedrazul.scheduling.AgendamientoFacade;
import com.unicauca.piedrazul.scheduling.dto.CitaDTO;
import com.unicauca.piedrazul.scheduling.dto.HistorialCitasPacienteDTO;
import com.unicauca.piedrazul.scheduling.internal.domain.entity.enums.EstadoCita;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
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

    /**
     * pacienteId es el Usuario.id del paciente. Antes cualquier usuario
     * autenticado podía ver las citas de cualquier paciente cambiando el
     * id en la URL; ahora solo el propio paciente, un agendador o un
     * administrador pueden hacerlo.
     */
    @GetMapping("/paciente/{pacienteId}")
    @PreAuthorize("#pacienteId == authentication.principal or hasAnyRole('AGENDADOR','ADMINISTRADOR')")
    public ResponseEntity<List<CitaDTO>> listarPorPaciente(@PathVariable Long pacienteId) {
        return ResponseEntity.ok(agendamientoFacade.listarCitasPorPaciente(pacienteId));
    }

    /** profesionalId es el Usuario.id del profesional; misma protección que arriba. */
    @GetMapping("/profesional/{profesionalId}")
    @PreAuthorize("#profesionalId == authentication.principal or hasAnyRole('AGENDADOR','ADMINISTRADOR')")
    public ResponseEntity<List<CitaDTO>> listarPorProfesional(@PathVariable Long profesionalId) {
        return ResponseEntity.ok(agendamientoFacade.listarCitasPorProfesional(profesionalId));
    }

    @GetMapping("/profesional/{profesionalId}/fecha")
    @PreAuthorize("#profesionalId == authentication.principal or hasAnyRole('AGENDADOR','ADMINISTRADOR')")
    public ResponseEntity<List<CitaDTO>> listarPorProfesionalYFecha(
            @PathVariable Long profesionalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

        List<CitaDTO> citas = agendamientoFacade.listarCitasPorProfesionalYFecha(profesionalId, fecha);
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(citas.size()))
                .body(citas);
    }

    /**
     * HU-1.2: exporta en CSV las citas de un médico/terapista en una fecha.
     * Mismos permisos que la consulta (el propio profesional, agendador o administrador).
     */
    @GetMapping(value = "/profesional/{profesionalId}/fecha/exportar", produces = "text/csv;charset=UTF-8")
    @PreAuthorize("#profesionalId == authentication.principal or hasAnyRole('AGENDADOR','ADMINISTRADOR')")
    public ResponseEntity<byte[]> exportarCitasCsv(
            @PathVariable Long profesionalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

        byte[] contenido = agendamientoFacade.exportarCitasCsv(profesionalId, fecha);
        String nombreArchivo = "citas_" + fecha + "_profesional_" + profesionalId + ".csv";

        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nombreArchivo).build().toString())
                .body(contenido);
    }

    /**
     * HU-4.1: historial de citas de un paciente a partir de su cédula.
     * Disponible para médico/terapista, agendador y administrador.
     */
    @GetMapping("/paciente/cedula/{cedula}")
    @PreAuthorize("hasAnyRole('PROFESIONAL','AGENDADOR','ADMINISTRADOR')")
    public ResponseEntity<HistorialCitasPacienteDTO> consultarHistorialPorCedula(
            @PathVariable String cedula) {
        return ResponseEntity.ok(agendamientoFacade.consultarHistorialPorCedula(cedula));
    }

    @GetMapping("/profesional/{profesionalId}/disponibilidad")
    public ResponseEntity<List<ZonedDateTime>> obtenerHorariosDisponibles(
            @PathVariable Long profesionalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(agendamientoFacade.obtenerHorariosDisponibles(profesionalId, fecha));
    }

    // ── Transiciones de estado ────────────────────────────────────────────────
    // Gestión operativa de citas: reservada a agendador/administrador.
    // (Cancelar la propia cita como paciente puede habilitarse más adelante
    // con una regla adicional que compare el pacienteId de la cita.)

    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('AGENDADOR','ADMINISTRADOR')")
    public ResponseEntity<CitaDTO> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(agendamientoFacade.cancelarCita(id));
    }

    @PatchMapping("/{id}/completar")
    @PreAuthorize("hasAnyRole('AGENDADOR','ADMINISTRADOR')")
    public ResponseEntity<CitaDTO> completar(@PathVariable Long id) {
        return ResponseEntity.ok(agendamientoFacade.completarCita(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENDADOR','ADMINISTRADOR')")
    public ResponseEntity<CitaDTO> actualizar(
            @PathVariable Long id,
            @RequestBody CitaDTO dto) {
        return ResponseEntity.ok(agendamientoFacade.actualizarCita(id, dto));
    }

    // ── Conteo ────────────────────────────────────────────────────────────────

    @GetMapping("/contar")
    @PreAuthorize("hasAnyRole('AGENDADOR','ADMINISTRADOR')")
    public ResponseEntity<Long> contarPorEstado(@RequestParam EstadoCita estado) {
        return ResponseEntity.ok(agendamientoFacade.contarCitasPorEstado(estado));
    }
}