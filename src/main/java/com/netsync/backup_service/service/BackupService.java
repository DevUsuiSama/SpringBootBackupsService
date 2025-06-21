package com.netsync.backup_service.service;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.netsync.backup_service.model.Device;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.stereotype.Service;

@Service
public class BackupService {
    private static final int SSH_SESSION_TIMEOUT = 7_000;
    private static final int SFTP_TIMEOUT = 7_000;

    public void performBackup(String localFolderPath, Device device) {
        Session session = null;
        ChannelSftp sftpChannel = null;

        // ✅ Lista de rutas a descargar directamente desde el servidor
        String[] pathsToDownload = {
            "/etc/ssh",
            "/etc/passwd",
            "/etc/group",
            "/etc/hostname",
            "/etc/hosts"
        };

        try {
            System.out.printf("🔐 Conectando a %s para descargar archivos…%n", device.getIpAddress());
            session = createSession(device);

            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect(SFTP_TIMEOUT);
            System.out.println("✅ Conexión SFTP establecida.");

            Path localBaseDir = Paths.get(localFolderPath, device.getName());
            Files.createDirectories(localBaseDir);

            for (String remotePath : pathsToDownload) {
                try {
                    SftpATTRS attrs = sftpChannel.lstat(remotePath);
                    String cleanName = remotePath.replaceFirst("^/+", "").replaceAll("/", "_");
                    Path localTargetPath = localBaseDir.resolve(cleanName);

                    if (attrs.isDir()) {
                        System.out.println("📁 Descargando directorio: " + remotePath);
                        Files.createDirectories(localTargetPath);
                        // Puedes implementar un método recursivo aquí si querés una copia profunda
                    } else {
                        System.out.println("📄 Descargando archivo: " + remotePath);
                        sftpChannel.get(remotePath, localTargetPath.toString());
                    }

                    System.out.println("✅ Guardado en: " + localTargetPath);

                } catch (Exception e) {
                    System.err.println("⚠️ Fallo al descargar " + remotePath + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.printf("❌ ERROR durante la sesión con %s: %s%n", device.getIpAddress(), e.getMessage());
        } finally {
            if (sftpChannel != null && sftpChannel.isConnected()) {
                try {
                    sftpChannel.disconnect();
                    System.out.println("🔹 Canal SFTP cerrado.");
                } catch (Exception ignored) {}
            }
            if (session != null && session.isConnected()) {
                try {
                    session.disconnect();
                    System.out.println("🔹 Sesión SSH cerrada.");
                } catch (Exception ignored) {}
            }
        }
    }

    private Session createSession(Device device) throws JSchException {
        JSch jsch = new JSch();
        Session session = jsch.getSession(device.getUsername(),
                device.getIpAddress(),
                device.getSshPort());
        session.setPassword(device.getPassword());
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect(SSH_SESSION_TIMEOUT);
        return session;
    }
}
