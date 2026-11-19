package com.unicauca.piedrazul.scheduling.dto;

import lombok.*;

import java.util.List;

/** HU-5.1: cantidad de citas por mes (12 posiciones, enero..diciembre) y por médico en un año. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReporteCitasMensualDTO {
    private int anio;
    private long totalCitas;
    private List<SerieMedico> series;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SerieMedico {
        private Long profesionalId;
        private String profesionalNombre;
        /** Siempre 12 valores; índice 0 = enero. */
        private List<Long> citasPorMes;
        private long total;
    }
}
