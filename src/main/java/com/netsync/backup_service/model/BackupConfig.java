package com.netsync.backup_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Entity
@Table(name = "backup_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BackupConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String frequency;
    private LocalTime time;
    private String weeklyDay;
    private String monthlyDay;
    private String localFolder;
}