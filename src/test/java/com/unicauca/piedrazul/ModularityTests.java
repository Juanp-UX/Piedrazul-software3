package com.unicauca.piedrazul;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {
    ApplicationModules modules = ApplicationModules.of(PiedrazulApplication.class);

    @Test
    void verifiesModularStructure() {
        modules.verify();
    }
}