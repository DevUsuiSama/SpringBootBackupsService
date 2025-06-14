package com.netsync.backup_service.dto;

public record DeviceDTO(
  String id,
  String name,
  String ipAddress,
  String type,
  String username,
  String password,
  int sshPort,
  Long version
) {}
