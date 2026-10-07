# Taxi-Backend

API Spring Boot 3.3 (Java 21) + PostgreSQL + Flyway + WebSocket (STOMP/SockJS).
Frontend: repo `Taxi-Frontend`.

## Despliegue en Hostinger (VPS) + Coolify

### DNS (Hostinger -> Dominios -> nexorasistemas.com.mx -> DNS)

| Tipo | Nombre     | Apunta a     |
|------|------------|--------------|
| A    | `api-taxi` | 2.24.112.116 |
| A    | `taxi`     | 2.24.112.116 |

### 1. Base de datos (Coolify -> New Resource -> PostgreSQL)

Crear la base vacia (ej. `taxiapp_db`). **No ejecutar `init.sql`**: Flyway crea todo el esquema
solo (V1..V12) al primer arranque. Si se corriera `init.sql` a mano, Flyway haria baseline en V1 e
intentaria aplicar V2..V12 encima de un esquema que ya los tiene, y el arranque fallaria.

### 2. Backend (Coolify -> New Resource -> repo Taxi-Backend, Build strategy: Dockerfile)

- Base directory `/`, Dockerfile location `/Dockerfile`
- Domain: `https://api-taxi.nexorasistemas.com.mx`
- Exposed ports: `8081`
- **Persistent Storage** (para que las fotos no se borren en cada redeploy):
  Volume, destination path `/app/uploads` (el Dockerfile ya define `UPLOADS_DIR=/app/uploads`)

Variables de entorno:

```
DB_HOST=<host interno del Postgres de Coolify>
DB_PORT=5432
DB_NAME=taxiapp_db
DB_USER=<usuario>
DB_PASSWORD=<password>

APP_JWT_SECRET=<cadena aleatoria larga, minimo 64 caracteres>
APP_CORS_ALLOWED_ORIGINS=https://taxi.nexorasistemas.com.mx
APP_FRONTEND_URL=https://taxi.nexorasistemas.com.mx
APP_COOKIE_SECURE=true
APP_COOKIE_SAMESITE=None

SEED_ENABLED=true                # crea el SUPER_ADMIN la primera vez; despues puede quedar en false
SEED_ADMIN_PHONE=<telefono>
SEED_ADMIN_PASSWORD=<password fuerte, NO dejar el default Admin123!>
SEED_ADMIN_EMAIL=<correo>

MAIL_ENABLED=true                # opcional; con false los correos solo se registran en el log
MAIL_HOST=smtp.hostinger.com
MAIL_PORT=587
MAIL_USERNAME=<cuenta>
MAIL_PASSWORD=<password>
```

Nota: con `SEED_ENABLED=true` tambien se siembra una organizacion/operador/taxi de demostracion.

### 3. Frontend (Coolify -> New Resource -> repo Taxi-Frontend, Build strategy: Dockerfile)

- Base directory `/`, Dockerfile location `/Dockerfile` (Node 22 compila, nginx sirve `dist`)
- Domain: `https://taxi.nexorasistemas.com.mx`
- Exposed ports: `80`
- Las URLs del backend ya van en `.env.production` (`VITE_API_URL`, `VITE_WS_URL`), no hace falta
  ponerlas en Coolify.

### Verificacion

- `https://api-taxi.nexorasistemas.com.mx/` responde.
- Subir una foto de operador, hacer **Redeploy** del backend y comprobar que la foto sigue visible.
