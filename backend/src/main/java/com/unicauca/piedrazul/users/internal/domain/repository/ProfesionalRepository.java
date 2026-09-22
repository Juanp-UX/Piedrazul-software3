package com.unicauca.piedrazul.users.internal.domain.repository;



import com.unicauca.piedrazul.users.internal.domain.entity.Profesional;
import com.unicauca.piedrazul.users.internal.domain.entity.enums.TipoProfesional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfesionalRepository extends JpaRepository<Profesional, Long> {
    List<Profesional> findByActivoTrue();
    List<Profesional> findByTipoAndActivoTrue(TipoProfesional tipo);
    boolean           existsByLicenciaProfesional(String licencia);
    List<Profesional> findByEspecialidadNombreAndActivoTrue(String nombre);

    /**
     * Busca por el id del Usuario asociado (no por el id propio de la fila
     * "profesionales"). En todo el sistema (listar(), scheduling, citas)
     * "profesionalId" se refiere siempre al Usuario.id, así que las
     * búsquedas por id deben ser consistentes con eso.
     */
    Optional<Profesional> findByUsuarioId(Long usuarioId);
}