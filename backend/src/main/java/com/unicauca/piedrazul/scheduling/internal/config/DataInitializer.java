package com.unicauca.piedrazul.scheduling.internal.config;

import com.unicauca.piedrazul.scheduling.internal.application.interfaces.IConfiguracionAgendamientoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Inicializa datos requeridos en el arranque del microservicio.
 * El servicio ya maneja la creación si no existe, pero este runner
 * hace el seed de forma explícita y registra en el log el estado inicial.
 */
@Component
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final IConfiguracionAgendamientoService configuracionService;

    public DataInitializer(IConfiguracionAgendamientoService configuracionService) {
        this.configuracionService = configuracionService;
    }

    @Override
    public void run(ApplicationArguments args) {
        var config = configuracionService.obtener();
        log.info("✔ Configuración de agendamiento inicializada: {} semana(s) habilitadas. " +
                 "Fecha máxima de agendamiento: {}",
                config.getSemanasHabilitadas(),
                configuracionService.obtenerFechaMaximaAgendamiento());
    }
}
