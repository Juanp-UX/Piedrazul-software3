package com.unicauca.piedrazul.users;



import com.unicauca.piedrazul.users.dto.ProfesionalDTO;
import com.unicauca.piedrazul.users.internal.domain.entity.Profesional;
import com.unicauca.piedrazul.users.internal.domain.entity.Usuario;


import java.util.List;

public interface IProfesionalService {
    Profesional crearProfesional(Usuario usuario, ProfesionalDTO dto);
    List<ProfesionalDTO> listarActivos();
    ProfesionalDTO buscarPorId(Long id);
    List<ProfesionalDTO> listarActivosPorEspecialidad(String especialidadNombre);
}