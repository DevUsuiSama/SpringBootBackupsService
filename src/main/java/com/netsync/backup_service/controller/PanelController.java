package com.netsync.backup_service.controller;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.netsync.backup_service.dto.BackupConfigDTO;
import com.netsync.backup_service.dto.DeviceDTO;
import com.netsync.backup_service.dto.ErrorResponseDTO;
import com.netsync.backup_service.dto.SSHTestResponseDTO;
import com.netsync.backup_service.model.BackupConfig;
import com.netsync.backup_service.model.Device;
import com.netsync.backup_service.repository.BackupConfigRepository;
import com.netsync.backup_service.repository.DeviceRepository;
import com.netsync.backup_service.service.BackupService;
import com.netsync.backup_service.service.PanelService;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/api/devices")
public class PanelController {
    private final PanelService panelService;
    private final DeviceRepository deviceRepo;
    private final BackupService backupService;
    private final BackupConfigRepository cfgRepo;
    private static final AtomicBoolean firstRun = new AtomicBoolean(true);

    public PanelController(PanelService panelService, DeviceRepository deviceRepo, BackupService backupService, BackupConfigRepository cfgRepo) {
        this.panelService = panelService;
        this.deviceRepo = deviceRepo;
        this.backupService = backupService;
        this.cfgRepo = cfgRepo;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<List<DeviceDTO>> getDevices() {
        List<DeviceDTO> devices = panelService.getDevices().stream()
                .map(device -> new DeviceDTO(
                        device.getId(),
                        device.getName(),
                        device.getIpAddress(),
                        device.getType(),
                        device.getUsername(),
                        device.getPassword(),
                        device.getSshPort(),
                        device.getVersion() // <-- ahora incluimos la versión
                ))
                .toList();

        return ResponseEntity.ok(devices);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<DeviceDTO> createDevice(@RequestBody DeviceDTO dto) {
        // mapea DTO → entidad (sin version)
        Device entity = new Device(
                null, dto.name(), dto.ipAddress(), dto.type(),
                dto.username(), dto.password(), dto.sshPort(), null);
        Device saved = panelService.createDevice(entity);
        // mapea entidad → DTO (con id y version)
        DeviceDTO out = new DeviceDTO(
                saved.getId(), saved.getName(), saved.getIpAddress(),
                saved.getType(), saved.getUsername(), saved.getPassword(),
                saved.getSshPort(), saved.getVersion());
        return ResponseEntity.status(HttpStatus.CREATED).body(out);
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping
    public ResponseEntity<?> updateDevice(@RequestBody DeviceDTO deviceDTO) {
        try {
            Optional<Device> updatedOpt = panelService.updateDevice(deviceDTO);

            if (updatedOpt.isEmpty()) {
                // Llega vacío cuando hay un "Nuevo Dispositivo" pendiente
                return ResponseEntity
                        .badRequest()
                        .body(new ErrorResponseDTO(
                                "No se puede actualizar: existe un 'Nuevo Dispositivo' pendiente",
                                "400"));
            }

            Device d = updatedOpt.get();
            // Mapeo de entidad a DTO
            DeviceDTO result = new DeviceDTO(
                    d.getId(),
                    d.getName(),
                    d.getIpAddress(),
                    d.getType(),
                    d.getUsername(),
                    d.getPassword(),
                    d.getSshPort(),
                    d.getVersion());

            return ResponseEntity.ok(result);

        } catch (ObjectOptimisticLockingFailureException ex) {
            // Cuando falla por locking optimista
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(new ErrorResponseDTO(
                            "Conflicto de edición: otro usuario modificó este dispositivo.",
                            "409"));
        }
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable String id) {
        panelService.deleteDevice(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/backup/{deviceId}")
    public ResponseEntity<Void> performBackup(@PathVariable String deviceId) {
        panelService.performBackup(deviceId);
        return ResponseEntity.ok().build();
    }

    /**
     * Prueba conexión SSH al dispositivo identificado por {id}.
     * Devuelve 200 OK si fue exitosa, 502 si falló, 404 si no existe.
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}/ssh-test")
    public ResponseEntity<SSHTestResponseDTO> testSSHConnection(@PathVariable String id) throws InterruptedException, IOException {
        Device device = deviceRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Dispositivo no encontrado: " + id
            ));

        SSHTestResponseDTO dto = panelService.testSSHConnection(device);

        // Si success==true → 200 OK, si no → 502 Bad Gateway
        return ResponseEntity.ok(dto);
    }

    /**
     * Devuelve la configuración única de backup.
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/backup-config")
    public ResponseEntity<BackupConfigDTO> getBackupConfig() {
        BackupConfigDTO config = panelService.getBackupConfig();
        return ResponseEntity.ok(config);
    }

    /**
     * Actualiza (o crea la primera vez) la configuración de backup.
     */
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/backup-config")
    public ResponseEntity<BackupConfigDTO> saveBackupConfig(
            @RequestBody BackupConfigDTO dto) {

        BackupConfigDTO out = panelService.saveBackupConfig(dto);
        return ResponseEntity.ok(out);
    }

    /** 🔹 Se ejecuta una vez cuando la aplicación está completamente lista */
    @EventListener(ApplicationReadyEvent.class)
    public void runInitialBackup() {
        System.out.println("🚀 Servidor iniciado, ejecutando primer backup...");
        scheduleBackups(true);
    }

    /** 🔹 Se ejecuta cada minuto */
    @Scheduled(cron = "0 */1 * * * *")
    public void scheduleBackups() {
        if (firstRun.get()) {
            System.out.println("⚠️ Backup aún en ejecución, esperando...");
            return;
        }
        firstRun.set(true);
        try {
            scheduleBackups(false);
        } finally {
            firstRun.set(false);
        }
    }

    private void scheduleBackups(boolean isFirstRun) {
        LocalDateTime now = LocalDateTime.now();
        LocalTime hora = now.toLocalTime().truncatedTo(ChronoUnit.MINUTES);
        DayOfWeek dow = now.getDayOfWeek();
        int dom = now.getDayOfMonth();

        // Obtener configuración global del backup
        BackupConfig backupConfig = cfgRepo.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new RuntimeException("No backup configuration found in DB!"));

        // Ejecutar backup en la primera ejecución o si toca según programación
        if (isFirstRun || shouldRunBackup(backupConfig, hora, dow, dom)) {
            firstRun.set(false); // 🔹 Desactivar flag después de la primera ejecución
            executeBackupForAllDevices(backupConfig);
        }
    }

    /** Define si el respaldo debe ejecutarse según la configuración en BD */
    private boolean shouldRunBackup(BackupConfig cfg, LocalTime hora, DayOfWeek dow, int dom) {
        return switch (cfg.getFrequency().toUpperCase()) {
            case "DAILY"   -> hora.equals(cfg.getTime());
            case "WEEKLY"  -> hora.equals(cfg.getTime()) && dow.name().equalsIgnoreCase(cfg.getWeeklyDay());
            case "MONTHLY" -> hora.equals(cfg.getTime()) && dom == Integer.parseInt(cfg.getMonthlyDay());
            default        -> false;
        };
    }

    private void executeBackupForAllDevices(BackupConfig backupConfig) {
        List<Device> devices = deviceRepo.findAll();

        // 🔹 Validación: Si no hay dispositivos, detener ejecución
        if (devices.isEmpty()) {
            System.out.println("⚠️ No hay dispositivos disponibles para realizar el backup. Operación cancelada.");
            return;
        }

        System.out.println("🔹 Total de dispositivos a procesar: " + devices.size());

        // 🔹 Mostrar lista de dispositivos para verificar que todos están presentes
        devices.forEach(dev -> System.out.println("   ➡ " + dev.getIpAddress()));

        for (Device dev : devices) {
            try {
                // 🔹 Probar conexión SSH antes de realizar el backup
                System.out.println("🔹 Probando conexión SSH para: " + dev.getIpAddress());
                long start = System.currentTimeMillis();
                SSHTestResponseDTO test = panelService.testSSHConnection(dev);
                
                System.out.printf("🔹 Resultado conexión [%s]: %s (%d ms)%n", dev.getIpAddress(), test.success(), (System.currentTimeMillis() - start));

                if (!test.success()) {
                    System.err.printf("❌ Error: SSH falló para dispositivo %s. Motivo: %s%n", dev.getName(), test.message());
                    continue; // No intentar el backup si SSH falla
                }

                // 🔹 Ejecutar respaldo según la carpeta definida en BackupConfig
                System.out.println("✅ Ejecutando backup para dispositivo: " + dev.getIpAddress());
                backupService.performBackup(backupConfig.getLocalFolder(), dev);

                System.out.println("✅ Backup completado para: " + dev.getIpAddress());

            } catch (Exception e) {
                System.err.printf("❌ Error al procesar el dispositivo %s: %s%n", dev.getName(), e.getMessage());
            }

            System.out.println("🔹 Finalizado procesamiento de dispositivo: " + dev.getIpAddress());
        }

        System.out.println("✅ Proceso de backup finalizado.");
    }
}
