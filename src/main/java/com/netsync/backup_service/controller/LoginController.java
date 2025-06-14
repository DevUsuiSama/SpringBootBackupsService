package com.netsync.backup_service.controller;

import com.netsync.backup_service.dto.AuthResponseDTO;
import com.netsync.backup_service.dto.ErrorResponseDTO;
import com.netsync.backup_service.model.LoginRequest;
import com.netsync.backup_service.service.LoginService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class LoginController {
    private final LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        String token = loginService.authenticate(request.getUsername(), request.getPassword());

        if (token != null) {
            return ResponseEntity.ok(new AuthResponseDTO(token)); // ✅ Respuesta estructurada con DTO
        }

        return ResponseEntity.status(401).body(new ErrorResponseDTO("(╯°□°）╯︵ ┻━┻ CREDENCIALES INCORRECTAS", "401")); // ✅ Código correcto para error
    }
}

