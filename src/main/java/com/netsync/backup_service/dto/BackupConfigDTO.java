package com.netsync.backup_service.dto;

public record BackupConfigDTO(
    String frequency,    // "daily" | "weekly" | "monthly"
    String time,         // "08:00"  (solo para daily/weekly)
    String weeklyDay,    // "monday" … "sunday" (solo para weekly)
    String monthlyDay,   // "1" … "31" (solo para monthly)
    String localFolder   // ruta absoluta
) {}