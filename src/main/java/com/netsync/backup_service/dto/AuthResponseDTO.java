package com.netsync.backup_service.dto;

public record AuthResponseDTO(String token) {
    public AuthResponseDTO {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("El token no puede estar vacío");
        }
    }
}