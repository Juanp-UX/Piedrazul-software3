package com.unicauca.piedrazul.users.internal.domain.repository;



import com.unicauca.piedrazul.shared.RolUsuario;
import com.unicauca.piedrazul.users.internal.domain.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByLogin(String login);
    List<Usuario>     findByRol(RolUsuario rol);
    List<Usuario>     findByActivoTrue();
    boolean           existsByLogin(String login);
    long countByActivoTrue();
}