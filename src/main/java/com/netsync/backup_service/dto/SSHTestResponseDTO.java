package com.netsync.backup_service.dto;

// DTO para unificar respuesta
public record SSHTestResponseDTO(
    boolean success,
    String message
) {}
