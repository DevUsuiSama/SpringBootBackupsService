## Swagger Doc

http://localhost:8080/swagger-ui/index.html

Cómo usarlo en Docker-CLI:

1) Guarda ese contenido como `Dockerfile` en tu carpeta de proyecto.  
2) Desde la UI de Docker Desktop crea un nuevo **Build** apuntando a esa carpeta (o, por CLI):  
   ```
   docker build -t ssh-test .
   ```
3) Al levantar el contenedor pásale la contraseña por ENV, el puerto, IP, host-name y red que necesites:
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
4) Prueba tu acceso SSH:
   ```
   ssh usui@localhost -p 2222   # contraseña: 123
   ```