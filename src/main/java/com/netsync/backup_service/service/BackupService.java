package com.netsync.backup_service.service;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.netsync.backup_service.model.Device;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

@Service
public class BackupService {

    private static final int SSH_SESSION_TIMEOUT   = 7_000;
    private static final int SSH_COMMAND_TIMEOUT   = 7_000;  // ms para connect y ejecución
    private static final int SFTP_TIMEOUT          = 7_000;  // ms para SFTP connect

    public void performBackup(String localFolderPath, Device device) {
        Session session = null;
        try {
            System.out.printf("🔐 Conectando a %s para respaldo SSH…%n", device.getIpAddress());
            session = createSession(device);
            System.out.println("✅ Conexión SSH establecida con: " + device.getIpAddress());

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String remotePath = "/tmp/backup-" + device.getName() + "-" + timestamp + ".tar.gz";
            String remoteCmd  = "sudo tar -czf " + remotePath + " /etc /home /var";

            execRemoteCommand(session, remoteCmd);

            Path localPath = Paths.get(localFolderPath, device.getName() + "-" + timestamp + ".tar.gz");
            Files.createDirectories(localPath.getParent());

            downloadFile(session, remotePath, localPath);

            System.out.println("✅ Backup completado y guardado en: " + localPath);

        } catch (Exception e) {
            System.err.printf("❌ ERROR en backup de %s: %s%n",
                    device.getIpAddress(), e.getMessage());
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
                System.out.println("🔹 Sesión SSH cerrada para: " + device.getIpAddress());
            }
        }
    }

    private Session createSession(Device device) throws JSchException {
        JSch jsch = new JSch();
        Session session = jsch.getSession(
                device.getUsername(),
                device.getIpAddress(),
                device.getSshPort());
        session.setPassword(device.getPassword());
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect(SSH_SESSION_TIMEOUT);
        return session;
    }

    private void execRemoteCommand(Session session, String command) throws Exception {
        ChannelExec channel = null;
        InputStream in      = null;
        try {
            System.out.println("🔹 Ejecutando comando remoto: " + command);
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            channel.setInputStream(null);
            channel.setErrStream(System.err);
            in = channel.getInputStream();
            channel.connect(SSH_COMMAND_TIMEOUT);

            long start = System.currentTimeMillis();
            byte[] buf = new byte[1024];

            // Bucle de lectura con timeout global
            while (true) {
                while (in.available() > 0) {
                    int len = in.read(buf, 0, buf.length);
                    if (len < 0) break;
                    System.out.print(new String(buf, 0, len));
                }
                if (channel.isClosed()) {
                    int status = channel.getExitStatus();
                    if (status != 0) {
                        throw new RuntimeException(
                            "Comando remoto terminó con error (exitStatus=" + status + ")");
                    }
                    break;
                }
                if (System.currentTimeMillis() - start > SSH_COMMAND_TIMEOUT) {
                    throw new RuntimeException("Timeout ejecutando comando remoto");
                }
                Thread.sleep(100);
            }
            System.out.println("✅ Comando remoto ejecutado correctamente.");
        } finally {
            if (in != null) in.close();
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
        }
    }

    private void downloadFile(Session session, String remoteFilePath, Path localPath) throws Exception {
        ChannelSftp sftp = null;
        try {
            System.out.println("🔹 Iniciando descarga SFTP de: " + remoteFilePath);
            sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect(SFTP_TIMEOUT);

            try (FileOutputStream fos = new FileOutputStream(localPath.toFile())) {
                sftp.get(remoteFilePath, fos);
            }
            System.out.println("✅ Archivo descargado: " + localPath);
        } catch (SftpException e) {
            throw new RuntimeException("Error SFTP: " + e.getMessage(), e);
        } finally {
            if (sftp != null && sftp.isConnected()) {
                sftp.disconnect();
                System.out.println("🔹 Canal SFTP cerrado.");
            }
        }
    }
}
