package com.netsync.backup_service.service;

import org.springframework.stereotype.Service;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.netsync.backup_service.dto.BackupConfigDTO;
import com.netsync.backup_service.dto.DeviceDTO;
import com.netsync.backup_service.dto.SSHTestResponseDTO;
import com.netsync.backup_service.model.BackupConfig;
import com.netsync.backup_service.model.Device;
import com.netsync.backup_service.repository.BackupConfigRepository;
import com.netsync.backup_service.repository.DeviceRepository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class PanelService {
    private final DeviceRepository deviceRepository;
    private final BackupConfigRepository cfgRepo;

    public PanelService(DeviceRepository deviceRepository, BackupConfigRepository cfgRepo) {
        this.deviceRepository = deviceRepository;
        this.cfgRepo = cfgRepo;
    }

    public List<Device> getDevices() {
        return deviceRepository.findAll();
    }

    public Device createDevice(Device device) {
        return deviceRepository.save(device);
    }

    public Optional<Device> updateDevice(DeviceDTO dto) {
        // Bloquear si "Nuevo Dispositivo" ya existe
        if (deviceRepository.existsByName("Nuevo Dispositivo")) {
            return Optional.empty();
        }

        return deviceRepository.findById(dto.id())
                .map(entity -> {
                    // Copio solo los campos permitidos
                    entity.setName(dto.name());
                    entity.setIpAddress(dto.ipAddress());
                    entity.setType(dto.type());
                    entity.setUsername(dto.username());
                    entity.setPassword(dto.password());
                    entity.setSshPort(dto.sshPort());
                    // No toco entity.getVersion(): Hibernate lo gestionará
                    return deviceRepository.save(entity);
                });
    }

    public void deleteDevice(String id) {
        deviceRepository.deleteById(id);
    }

    public void performBackup(String deviceId) {
        System.out.println("Backup realizado para dispositivo " + deviceId);
    }

    /** Recupera la única configuración, o valores por defecto si no existe */
    public BackupConfigDTO getBackupConfig() {
        return cfgRepo.findFirstByOrderByIdAsc()
                .map(cfg -> new BackupConfigDTO(
                        cfg.getFrequency(),
                        cfg.getTime() != null ? cfg.getTime().toString() : null,
                        cfg.getWeeklyDay(),
                        cfg.getMonthlyDay(),
                        cfg.getLocalFolder()))
                .orElse(new BackupConfigDTO(
                        "daily", "08:00", "monday", "1", "C:/backups/"));
    }

    /**
     * Guarda o actualiza la configuración de backup.
     */
    public BackupConfigDTO saveBackupConfig(BackupConfigDTO dto) {
        // 1️⃣ Cargar la única configuración si ya existe
        BackupConfig cfg = cfgRepo
                .findFirstByOrderByIdAsc()
                .orElseGet(BackupConfig::new);

        // 2️⃣ Mapear campos del DTO a la entidad existente
        cfg.setFrequency(dto.frequency());
        if (!"monthly".equals(dto.frequency())) {
            cfg.setTime(LocalTime.parse(dto.time()));
        } else {
            cfg.setTime(null);
        }
        cfg.setWeeklyDay(dto.weeklyDay());
        cfg.setMonthlyDay(dto.monthlyDay());
        cfg.setLocalFolder(dto.localFolder());

        // 3️⃣ Guardar (insert o update según cfg.id)
        BackupConfig saved = cfgRepo.save(cfg);

        // 4️⃣ Devolver DTO con los valores persistidos
        return new BackupConfigDTO(
                saved.getFrequency(),
                saved.getTime() != null ? saved.getTime().toString() : null,
                saved.getWeeklyDay(),
                saved.getMonthlyDay(),
                saved.getLocalFolder());
    }

    public SSHTestResponseDTO testSSHConnection(Device device) {
        Session session = null;
        final int SSH_TIMEOUT_MS = 7_000;

        try {
            System.out.println("🔍 Iniciando prueba SSH:");
            System.out.printf("   Host: %s:%d%n", device.getIpAddress(), device.getSshPort());
            System.out.println("   Usuario: " + device.getUsername());

            JSch jsch = new JSch();
            session = jsch.getSession(device.getUsername(),
                                    device.getIpAddress(),
                                    device.getSshPort());
            session.setPassword(device.getPassword());
            session.setConfig("StrictHostKeyChecking", "no");

            long t0 = System.currentTimeMillis();
            session.connect(SSH_TIMEOUT_MS);
            long dt = System.currentTimeMillis() - t0;

            System.out.printf("✅ Conexión SSH establecida en %d ms%n", dt);
            return new SSHTestResponseDTO(true, "Conexión exitosa");

        } catch (JSchException sshEx) {
            // timeout, host inaccesible, etc.
            System.err.println("❌ Error de SSH: " + sshEx.getMessage());
            return new SSHTestResponseDTO(false,
                "Error de conexión SSH: " + sshEx.getMessage());

        } catch (Exception ex) {
            // cualquier otro fallo inesperado
            System.err.println("❌ Error inesperado al probar SSH: " + ex.getMessage());
            ex.printStackTrace();
            return new SSHTestResponseDTO(false,
                "Error inesperado: " + ex.getMessage());

        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
                System.out.println("🔹 Sesión SSH cerrada tras prueba.");
            }
        }
    }
}