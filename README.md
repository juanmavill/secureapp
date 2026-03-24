# SecureApp - Taller Arquitectura Segura

**Escuela Colombiana de Ingeniería Julio Garavito**  

---

## Descripción

Aplicación web segura desplegada en AWS que implementa comunicación cifrada mediante TLS en dos servidores independientes. El sistema garantiza integridad, autenticación y autorización tanto a nivel de usuario como a nivel de servidores mediante Mutual TLS.

---

##  Arquitectura

```
Browser
   │
   │ HTTPS (TLS)
   ▼
┌─────────────────────────────┐
│  Servidor 1 - Apache        │
│  jmvtdse.duckdns.org        │
│  - Sirve frontend (HTML/JS) │
│  - Proxy inverso hacia Spring│
│  - KeyStore: Let's Encrypt  │
└─────────────┬───────────────┘
              │
              │ HTTPS + Certificado cliente (Mutual TLS)
              ▼
┌─────────────────────────────┐
│  Servidor 2 - Spring Boot   │
│  jmvtdse-backend.duckdns.org│
│  - REST API /api/login      │
│  - BCrypt para passwords    │
│  - TrustStore con cert Apache│
└─────────────────────────────┘
```

### Componentes

| Componente | Tecnología | Descripción |
|---|---|---|
| Frontend | HTML + JavaScript | Cliente asíncrono con fetch API |
| Servidor Web | Apache HTTP Server | Sirve estáticos y hace proxy inverso |
| Backend | Spring Boot 3 | API REST con Spring Security |
| Cifrado | TLS / Let's Encrypt | Certificados para ambos servidores |
| Seguridad | BCrypt + Mutual TLS | Autenticación de usuarios y servidores |
| Infraestructura | AWS EC2 | Dos instancias con Elastic IP |

---

##  Características de Seguridad

- **TLS en ambos servidores**: Certificados generados con Let's Encrypt via Certbot
- **Mutual TLS**: Apache presenta su certificado al llamar al backend Spring. Spring verifica el certificado contra su TrustStore
- **BCrypt**: Las contraseñas de usuarios nunca se almacenan en texto plano
- **12-Factor App**: Configuración sensible (contraseñas del keystore) leída desde variables de entorno, no hardcodeada en el código
- **Proxy inverso**: El backend Spring queda oculto detrás de Apache

---

##  Estructura del Proyecto

```
secureapp/
├── frontend/
│   └── index.html              # Cliente HTML + JS asíncrono
├── src/
│   └── main/
│       ├── java/co/edu/escuelaing/secureapp/
│       │   ├── SecureappApplication.java
│       │   ├── AuthController.java     # Endpoint /api/login y /api/health
│       │   └── SecurityConfig.java     # Configuración Spring Security + TrustStore
│       └── resources/
│           └── application.properties  # Configuración SSL con variables de entorno
├── pom.xml
└── README.md
```

---

##  Despliegue en AWS

### Requisitos

- Dos instancias EC2 con Amazon Linux 2023
- Elastic IP asignada a cada instancia
- Dominio apuntando a cada Elastic IP (ej: DuckDNS)
- Java 17 instalado en el servidor backend
- Apache HTTP Server instalado en el servidor frontend

### Servidor 1 - Apache (Frontend)

**1. Instalar Apache y Certbot:**
```bash
sudo yum install -y httpd mod_ssl certbot
sudo systemctl enable httpd
sudo systemctl start httpd
```

**2. Generar certificado TLS:**
```bash
sudo certbot certonly --standalone -d tu-dominio.duckdns.org
```

**3. Generar KeyStore PKCS12:**
```bash
sudo openssl pkcs12 -export \
  -in /etc/letsencrypt/live/tu-dominio.duckdns.org/fullchain.pem \
  -inkey /etc/letsencrypt/live/tu-dominio.duckdns.org/privkey.pem \
  -out /home/ec2-user/apache-keystore.p12 \
  -name apache \
  -password pass:TU_PASSWORD
```

**4. Exportar certificado público:**
```bash
sudo openssl x509 \
  -in /etc/letsencrypt/live/tu-dominio.duckdns.org/fullchain.pem \
  -out /home/ec2-user/apache-cert.cer
```

**5. Configurar proxy inverso hacia Spring:**

Crear `/etc/httpd/conf.d/proxy.conf`:
```apache
SSLProxyEngine on
SSLProxyVerify none

ProxyPass /api https://tu-backend.duckdns.org/api
ProxyPassReverse /api https://tu-backend.duckdns.org/api

SSLProxyMachineCertificateFile /home/ec2-user/apache-keystore.p12
```

**6. Colocar el frontend:**
```bash
sudo cp index.html /var/www/html/index.html
sudo systemctl restart httpd
```

---

### Servidor 2 - Spring Boot (Backend)

**1. Instalar Java 17:**
```bash
sudo yum install -y java-17-amazon-corretto
```

**2. Generar certificado TLS:**
```bash
sudo certbot certonly --standalone -d tu-backend.duckdns.org
```

**3. Convertir certificado a PKCS12:**
```bash
sudo openssl pkcs12 -export \
  -in /etc/letsencrypt/live/tu-backend.duckdns.org/fullchain.pem \
  -inkey /etc/letsencrypt/live/tu-backend.duckdns.org/privkey.pem \
  -out /home/ec2-user/keystore.p12 \
  -name tomcat \
  -password pass:TU_PASSWORD
```

**4. Importar certificado del Apache al TrustStore:**
```bash
keytool -import -file /home/ec2-user/apache-cert.cer \
  -alias apache \
  -keystore /home/ec2-user/truststore.p12 \
  -storetype PKCS12 \
  -storepass TU_PASSWORD \
  -noprompt
```

**5. Empacar y subir el jar:**
```bash
mvn clean package -DskipTests
scp -i "tu-llave.pem" target/secureapp-0.0.1-SNAPSHOT.jar ec2-user@IP-BACKEND:/home/ec2-user/
```

**6. Configurar como servicio permanente:**

Crear `/etc/systemd/system/secureapp.service`:
```ini
[Unit]
Description=SecureApp Spring Boot Backend
After=network.target

[Service]
Type=simple
User=root
Environment="SSL_PASSWORD=TU_PASSWORD"
ExecStart=/usr/bin/java -jar /home/ec2-user/secureapp-0.0.1-SNAPSHOT.jar --server.port=443
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable secureapp
sudo systemctl start secureapp
```

---

##  Variables de Entorno

| Variable | Descripción |
|---|---|
| `SSL_PASSWORD` | Contraseña del KeyStore y TrustStore |

El proyecto implementa el principio III de 12-Factor App: la configuración sensible se lee del entorno y nunca se hardcodea en el código fuente.

---

##  Prueba de Funcionamiento

1. Abre `https://tu-dominio.duckdns.org` en el browser
2. Ingresa usuario `admin` y contraseña `1234`
3. Debes ver el mensaje: **Bienvenido, admin!**

También puedes verificar el health check del backend:
```
https://tu-backend.duckdns.org/api/health
```

---

##  Tecnologías Usadas

- Java 17
- Spring Boot 4
- Spring Security
- BCrypt
- Apache HTTP Server 2.4
- Let's Encrypt / Certbot
- AWS EC2
- DuckDNS
- Maven

---

##  Autor

Juan Manuel Villegas  
Escuela Colombiana de Ingeniería Julio Garavito
