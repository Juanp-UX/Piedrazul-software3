package com.unicauca.piedrazul.users.controller;

import com.unicauca.piedrazul.users.dto.UsuarioDTO;
import com.unicauca.piedrazul.users.internal.application.interfaces.IRegistroService;
import com.unicauca.piedrazul.users.internal.domain.dto.request.RegistroPacienteRequest;
import com.unicauca.piedrazul.users.internal.domain.dto.request.RegistroProfesionalRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/registro")
public class RegistroController {

    private final IRegistroService registroService;

    public RegistroController(IRegistroService registroService) {
        this.registroService = registroService;
    }

    @PostMapping("/usuario")
    public ResponseEntity<UsuarioDTO> registrarUsuario(@RequestBody UsuarioDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registroService.registrarUsuario(dto));
    }

    @PostMapping("/paciente")
    public ResponseEntity<UsuarioDTO> registrarPaciente(@RequestBody RegistroPacienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registroService.registrarPaciente(request.getUsuario(), request.getPaciente()));
    }

    @PostMapping("/profesional")
    public ResponseEntity<UsuarioDTO> registrarProfesional(@RequestBody RegistroProfesionalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registroService.registrarProfesional(request.getUsuario(), request.getProfesional()));
    }
}