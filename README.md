# Hidrología UdeA

Aplicación web académica para apoyar la materia de Hidrología de la Universidad de Antioquia. El sitio permite consultar publicaciones por talleres, parciales y hashtags; enviar preguntas públicas; administrar contenido como profesor; gestionar enlaces de interés; y consultar estadísticas propias del sitio.

## Stack

- Frontend: Vue 3, TypeScript, Vite, Vue Router, Pinia, Axios, Tailwind CSS, Tiptap y Vitest.
- Backend: Java 17, Spring Boot 4.1.x, Maven, Spring Web, Spring Security, Spring Data JPA, Flyway, Actuator y OpenAPI.
- Datos: PostgreSQL local con Docker Compose; Neon previsto para producción.
- Servicios externos: Firebase Authentication para el profesor, Cloudinary para imágenes y Cloudflare Turnstile para reducir abuso en preguntas públicas.

## Estructura

```text
frontend/   SPA Vue
backend/    API REST Spring Boot
docs/       especificación y arquitectura
infra/      infraestructura local
```

## Configuración local

1. Copia `.env.example` como `.env`.
2. Cambia los valores locales necesarios, especialmente `POSTGRES_PASSWORD`, `DB_PASSWORD`, Firebase, Cloudinary y Turnstile si vas a probar esas integraciones.
3. No guardes secretos reales en archivos versionados.

Si el puerto `5432` está ocupado, cambia `POSTGRES_HOST_PORT` y usa el mismo puerto dentro de `DB_URL`:

```text
POSTGRES_HOST_PORT=<PUERTO_LIBRE>
DB_URL=jdbc:postgresql://127.0.0.1:<PUERTO_LIBRE>/hidrologia_udea
```

## PostgreSQL local

Desde la raíz del repositorio:

```powershell
docker compose --env-file .env -f infra/docker-compose.yml up -d
docker compose --env-file .env -f infra/docker-compose.yml ps
```

El servicio debe aparecer como `healthy`.

Para detenerlo sin borrar datos:

```powershell
docker compose --env-file .env -f infra/docker-compose.yml stop
```

Para eliminar contenedor y red sin borrar el volumen:

```powershell
docker compose --env-file .env -f infra/docker-compose.yml down
```

Evita `docker compose down -v` salvo que quieras borrar la base local.

## Backend

Docker Compose lee `.env` cuando se pasa con `--env-file`, pero Maven/Spring Boot no leen automáticamente ese archivo. Carga las variables en PowerShell antes de iniciar el backend:

```powershell
Get-Content ..\.env | Where-Object { $_ -and -not $_.TrimStart().StartsWith('#') } | ForEach-Object {
    $name, $value = $_.Split('=', 2)
    Set-Item -Path "Env:$name" -Value $value
}
$env:SPRING_PROFILES_ACTIVE='local'
```

Luego:

```powershell
cd backend
mvn spring-boot:run
```

URLs útiles:

- API técnica: `http://localhost:8080/api/v1/health`
- Actuator: `http://localhost:8080/actuator/health`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## Frontend

```powershell
cd frontend
npm install
npm run dev
```

Vite sirve la aplicación normalmente en `http://localhost:5173`. En desarrollo, el proxy de Vite envía `/api` hacia Spring Boot en `http://localhost:8080`.

## Validaciones

Backend:

```powershell
cd backend
mvn test
mvn package
```

Frontend:

```powershell
cd frontend
npm run test:unit
npm run lint
npm run build
```

## Seguridad local

- `.env` está ignorado por Git.
- `.env.example` solo contiene placeholders.
- El JSON de service account de Firebase debe vivir fuera del repositorio.
- `CLOUDINARY_API_SECRET`, `FIREBASE_ADMIN_UID`, `GOOGLE_APPLICATION_CREDENTIALS`, `TURNSTILE_SECRET_KEY` y tokens no deben llegar al frontend ni al control de versiones.
- Turnstile protege `POST /api/v1/questions` cuando `TURNSTILE_ENABLED=true`. El frontend usa solo `VITE_TURNSTILE_SITE_KEY`; el backend valida el token con Cloudflare Siteverify usando `TURNSTILE_SECRET_KEY`.
- Para pruebas locales con dummy keys oficiales de Cloudflare, deja `TURNSTILE_EXPECTED_ACTION=` y `TURNSTILE_EXPECTED_HOSTNAMES=` vacíos: esas credenciales sirven para comprobar `success=true`, pero pueden devolver metadata dummy no equivalente al entorno real.
- Para producción, configura también `TURNSTILE_EXPECTED_ACTION=student_question` y `TURNSTILE_EXPECTED_HOSTNAMES=<hostname público del frontend>` para activar validación estricta de metadata.

## Estado preproducción

El producto ya cuenta con módulos públicos, autenticación del profesor, administración de contenido, imágenes en publicaciones, analíticas propias y protección anti-abuso con Cloudflare Turnstile para el envío público de preguntas. Antes de producción quedan pendientes la configuración final de despliegue, secretos cloud, monitoreo y backups.
