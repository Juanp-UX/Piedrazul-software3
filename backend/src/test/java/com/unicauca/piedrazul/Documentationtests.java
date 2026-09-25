package com.unicauca.piedrazul;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Genera la documentación arquitectónica (diagramas C4 y canvas por módulo)
 * a partir de la estructura real de los módulos de Spring Modulith.
 *
 * Salida: target/spring-modulith-docs/
 *
 * Se ejecuta con: mvn test -Dtest=DocumentationTests
 */
class DocumentationTests {

    ApplicationModules modules = ApplicationModules.of(PiedrazulApplication.class);

    @Test
    void writeDocumentationSnippets() {
        new Documenter(modules)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml();
    }
}