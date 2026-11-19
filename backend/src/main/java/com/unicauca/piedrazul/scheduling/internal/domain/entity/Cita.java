package com.unicauca.piedrazul.scheduling.internal.domain.entity;

import com.unicauca.piedrazul.scheduling.internal.domain.entity.enums.EstadoCita;
import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;


@Entity
@Table(name = "citas")
// Removed @UniqueConstraint on (profesional_id, fecha_hora).
// A DB-level unique index prevented cancelled/completed rows from ever being
// reused, permanently destroying slots. Uniqueness for *active* bookings is
// now enforced at the service layer via existsSolapamientoProgramado().
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // IDs locales — no FK a microservicios externos
    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;

    @Column(name = "paciente_nombre", nullable = false, length = 150)
    private String pacienteNombre;

    @Column(name = "profesional_id", nullable = false)
    private Long profesionalId;

    @Column(name = "profesional_nombre", nullable = false, length = 150)
    private String profesionalNombre;

    @Column(name = "fecha_hora", nullable = false)
    private ZonedDateTime fechaHora;

    // Stored so the overlap query can reason about the end of this appointment
    // without joining back to disponibilidad_semanal.
    @Column(name = "duracion_minutos", nullable = false)
    private Integer duracionMinutos;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", columnDefinition = "varchar(50)")
    private EstadoCita estado = EstadoCita.programada;

    @Column(name = "creado_en")
    private ZonedDateTime creadoEn;

    // HU-2.3: justificación registrada cuando el paciente cancela su cita.
    @Column(name = "motivo_cancelacion", length = 500)
    private String motivoCancelacion;

    @Column(name = "cancelada_en")
    private ZonedDateTime canceladaEn;
}
