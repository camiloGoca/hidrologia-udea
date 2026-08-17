# Hidrologia UdeA

Aplicacion web academica para apoyar la materia de Hidrologia de la Universidad de Antioquia.

## PostgreSQL local

El entorno local usa PostgreSQL 17 mediante Docker Compose. No es necesario instalar PostgreSQL directamente en Windows.

1. Copia `.env.example` como `.env`.
2. Cambia `POSTGRES_PASSWORD` y `DB_PASSWORD` en `.env` por el mismo valor local.
3. Si el puerto `5432` ya esta ocupado en Windows, cambia `POSTGRES_HOST_PORT` y actualiza el puerto dentro de `DB_URL` con el mismo valor.

Ejemplo:

```text
POSTGRES_HOST_PORT=5433
DB_URL=jdbc:postgresql://127.0.0.1:5433/hidrologia_udea
```

## Levantar PostgreSQL

Desde la raiz del repositorio:

```powershell
docker compose --env-file .env -f infra/docker-compose.yml up -d
docker compose --env-file .env -f infra/docker-compose.yml ps
```

El contenedor debe aparecer como `healthy`.

## Iniciar Spring Boot con perfil local

Docker Compose lee `.env` porque se lo pasamos con `--env-file`. Maven y Spring Boot no leen ese archivo automaticamente.

Antes de iniciar el backend, carga las variables de `.env` en la sesion actual de PowerShell:

```powershell
Get-Content ..\.env | Where-Object { $_ -and -not $_.TrimStart().StartsWith('#') } | ForEach-Object {
    $name, $value = $_.Split('=', 2)
    Set-Item -Path "Env:$name" -Value $value
}
$env:SPRING_PROFILES_ACTIVE='local'
```

Luego, desde la carpeta `backend/`, ejecuta:

```powershell
mvn spring-boot:run
```

## Detener PostgreSQL local

Detener sin borrar datos:

```powershell
docker compose --env-file .env -f infra/docker-compose.yml stop
```

Eliminar contenedor/red sin borrar el volumen de datos:

```powershell
docker compose --env-file .env -f infra/docker-compose.yml down
```

Evita `docker compose down -v` salvo que quieras borrar los datos locales de PostgreSQL y recrear la base desde cero.
