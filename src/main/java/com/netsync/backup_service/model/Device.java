package com.netsync.backup_service.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

@Entity
@Data // ✅ Genera automáticamente getters, setters, equals, hashCode y toString
@NoArgsConstructor // ✅ Genera un constructor sin argumentos
@AllArgsConstructor // ✅ Genera un constructor con todos los atributos
public class Device {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // ✅ Generación automática de ID
    private String id;
    private String name;
    private String ipAddress;
    private String type;
    private String username;
    private String password;
    private int sshPort;

    @Version // ✅ Manejo de concurrencia en Hibernate
    private Long version;

    public Device(String id, String name, String ipAddress, String type, String username, String password, int sshPort) {
        this.id = id;
        this.name = name;
        this.ipAddress = ipAddress;
        this.type = type;
        this.username = username;
        this.password = password;
        this.sshPort = sshPort;
    }
}