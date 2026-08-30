# Hidrología UdeA

Aplicación web académica para apoyar la materia de Hidrología de la Universidad de Antioquia. El sitio permite consultar publicaciones por talleres, parciales y hashtags; enviar preguntas públicas; administrar contenido como profesor; gestionar enlaces de interés; y consultar estadísticas propias del sitio.

## Stack

- Frontend: Vue 3, TypeScript, Vite, Vue Router, Pinia, Axios, Tailwind CSS, Tiptap y Vitest.
- Backend: Java 17, Spring Boot 4.1.x, Maven, Spring Web, Spring Security, Spring Data JPA, Flyway, Actuator y OpenAPI.
- Datos: PostgreSQL local con Docker Compose; Neon PostgreSQL en producción.
- Servicios externos: Firebase Authentication para el profesor, Cloudinary para imágenes y Cloudflare Turnstile para reducir abuso en preguntas públicas.
- Producción y automatización: Firebase Hosting para la SPA, Northflank para el backend y GitHub Actions para CI/CD.

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

## Backend en contenedor

El backend incluye un `Dockerfile` multi-stage en `backend/`. La imagen compila con Maven y Java 17, y ejecuta solo el JAR final con JRE Java 17 y un usuario no-root.

Para construir la imagen localmente:

```powershell
cd backend
docker build -t hidrologia-backend:local .
```

La aplicación escucha el puerto indicado por `PORT` y usa `8080` como fallback local. El perfil `prod` reactiva DataSource, JPA y Flyway mediante variables de entorno (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`) y mantiene Hibernate en `ddl-auto=validate`.

Firebase Admin usa Application Default Credentials. En local, `GOOGLE_APPLICATION_CREDENTIALS` puede apuntar a un JSON de service account fuera del repositorio. En producción, la credencial debe entregarse de forma segura mediante la configuración del entorno de hosting, sin versionar JSON ni rutas personales.

En producción, SpringDoc/Swagger queda deshabilitado y Actuator expone solo `health` sin detalles.

## Frontend

```powershell
cd frontend
npm install
npm run dev
```

Vite sirve la aplicación normalmente en `http://localhost:5173`. En desarrollo, el proxy de Vite envía `/api` hacia Spring Boot en `http://localhost:8080`.

En producción, Firebase Hosting sirve la SPA estática y `VITE_API_BASE_URL` debe apuntar al backend desplegado en Northflank. Como el navegador llama a un origen distinto, el backend solo acepta CORS desde los origins configurados explícitamente en `CORS_ALLOWED_ORIGINS`; no se usa wildcard.

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
npm run lint:check
npm run build
```

`npm run lint` existe para desarrollo y puede aplicar fixes automáticamente. Para verificación sin modificar archivos usa `npm run lint:check`.

## Seguridad local

- `.env` está ignorado por Git.
- `.env.example` solo contiene placeholders.
- El JSON de service account de Firebase debe vivir fuera del repositorio.
- `CORS_ALLOWED_ORIGINS` define los origins del frontend autorizados para llamar al backend desde navegador. Para Firebase Hosting deben incluirse los dominios públicos de la SPA separados por coma.
- `CLOUDINARY_API_SECRET`, `FIREBASE_ADMIN_UID`, `GOOGLE_APPLICATION_CREDENTIALS`, `TURNSTILE_SECRET_KEY` y tokens no deben llegar al frontend ni al control de versiones.
- Turnstile protege `POST /api/v1/questions` cuando `TURNSTILE_ENABLED=true`. El frontend usa solo `VITE_TURNSTILE_SITE_KEY`; el backend valida el token con Cloudflare Siteverify usando `TURNSTILE_SECRET_KEY`.
- Para pruebas locales con dummy keys oficiales de Cloudflare, deja `TURNSTILE_EXPECTED_ACTION=` y `TURNSTILE_EXPECTED_HOSTNAMES=` vacíos: esas credenciales sirven para comprobar `success=true`, pero pueden devolver metadata dummy no equivalente al entorno real.
- Para producción, configura también `TURNSTILE_EXPECTED_ACTION=student_question` y `TURNSTILE_EXPECTED_HOSTNAMES=<hostname público del frontend>` para activar validación estricta de metadata.

## Producción y CI/CD

El despliegue actual usa:

- Frontend: Firebase Hosting.
- Backend: Northflank.
- Base de datos: Neon PostgreSQL.
- CI/CD: GitHub Actions.

En Pull Requests internos del mismo repositorio, GitHub Actions ejecuta pruebas backend/frontend y publica un Firebase Hosting Preview Channel de solo lectura. Ese preview no habilita admin, envío de preguntas ni escrituras de analytics, y no despliega backend de preview.

En push/merge a `main`, GitHub Actions ejecuta CI, despliega el frontend live en Firebase Hosting y dispara/verifica el despliegue del backend en Northflank para el commit exacto.

La especificación funcional vive en `docs/PRODUCT_SPEC.md` y las decisiones técnicas en `docs/ARCHITECTURE.md`.
