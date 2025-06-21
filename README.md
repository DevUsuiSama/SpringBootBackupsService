# 🚀 BackupService – Sistema de Respaldo Remoto vía SSH

Proyecto backend desarrollado en Spring Boot que permite realizar **backups programados de configuraciones almacenadas en contenedores Docker** mediante acceso remoto por SSH. El sistema expone una API REST documentada con Swagger, cuenta con autenticación JWT y puede ser desplegado en entornos Docker.

---

## 📦 Estructura del Proyecto
```
.
┣ docker/
┃ ┗ dockerfile               → Imagen para entorno SSH de prueba
┣ src/
┃ ┣ main/
┃ ┃ ┣ java/com/netsync/backup_service/
┃ ┃ ┃ ┣ config/              → Swagger y configuración general
┃ ┃ ┃ ┣ controller/          → Controladores REST (auth, panel)
┃ ┃ ┃ ┣ dto/                 → Clases de transferencia (DTOs)
┃ ┃ ┃ ┣ model/               → Entidades persistentes (Device, BackupConfig)
┃ ┃ ┃ ┣ repository/          → JPA Repositories
┃ ┃ ┃ ┣ security/            → JWT, configuración de seguridad
┃ ┃ ┃ ┣ service/             → Servicios principales de login, backups y conexión SSH
┃ ┃ ┃ ┗ BackupServiceApplication.java
┃ ┣ resources/
┃ ┃ ┣ application.properties → Configuración de la aplicación
┃ ┃ ┣ static/, templates/    → (Reservado para vistas si se usara)
┃ ┗ test/
┃ ┃ ┗ BackupServiceApplicationTests.java
┣ .gitignore
┣ pom.xml
┣ README.md
┗ HELP.md
```

---

## 🔐 Seguridad

El sistema implementa:

- Autenticación vía JWT con `/api/auth/login`
- Protección de rutas mediante `SecurityConfig.java`
- Permite acceso público a Swagger UI, login y APIs públicas

---

## 📄 Documentación Swagger

Swagger UI está disponible al levantar la aplicación en:

```
http://localhost:8080/swagger-ui/index.html
```

---

## 🐳 Docker - Nodo SSH de prueba

Puedes crear un contenedor para pruebas de respaldo SSH con los siguientes pasos:

1. Guarda el siguiente contenido como `docker/dockerfile`.

2. Construye la imagen:
   ```bash
   docker build -t ssh-node docker/
   ```

3. Crea una red de prueba y corre el contenedor:
   ```bash
   docker network create --subnet=172.18.0.0/16 ssh-net

   docker run -d \
     --name ssh-node-1 \
     --network ssh-net \
     --ip 172.18.0.10 \
     -h nodo1 \
     -e SSH_PASSWORD=123 \
     -p 2222:22 \
     ssh-node
   ```

4. Prueba la conexión:
   ```bash
   ssh usui@localhost -p 2222     # contraseña: 123
   ```

---

## 🔄 Funcionamiento general

1. El backend detecta dispositivos registrados en base de datos
2. Valida conectividad vía SSH (`PanelService`)
3. Ejecuta comandos de respaldo como `tar -czf`
4. Descarga los archivos al servidor local (`BackupService`)
5. Guarda registros asociados

---

## 📦 Tecnologías utilizadas

- Java 17
- Spring Boot 3
- Spring Security + JWT
- Spring Data JPA
- OpenAPI / Swagger
- Docker + JSch SSH client

---

## 📌 Notas

- Asegúrate de que los nodos remotos tengan `sudo` y acceso a `/etc`, `/home`, `/var`
- La contraseña SSH del contenedor debe estar disponible por variable de entorno: `SSH_PASSWORD`
- La frecuencia y horarios de ejecución de backups se configuran vía `BackupConfig` en base de datos

---

## ✍️ Autor

Desarrollado por **DevUsui-San**  
_“Respalda con estilo. Automatiza con propósito.”_