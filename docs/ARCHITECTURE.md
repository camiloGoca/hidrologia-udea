# Architecture — Hidrología UdeA

## 1. Vista general

Hidrología UdeA usa una arquitectura simple:

```text
Vue 3 SPA
   ↓ /api/v1
Spring Boot 4.1.x monolito modular
   ↓
PostgreSQL + Flyway

Servicios externos:
- Firebase Authentication / Firebase Admin SDK para el profesor.
- Cloudinary para almacenamiento de imágenes.
- Cloudflare Turnstile para reducir abuso en el envío público de preguntas.
```

No se usan microservicios, colas, Redis, Elasticsearch ni Kubernetes para el MVP.

## 2. Repositorio

```text
frontend/   aplicación Vue
backend/    API REST Spring Boot
docs/       documentación funcional y técnica
infra/      infraestructura local
```

## 3. Frontend

Stack:

- Vue 3;
- TypeScript;
- Vite;
- Vue Router;
- Pinia;
- Axios;
- Tailwind CSS;
- Tiptap / ProseMirror;
- Vitest.

Organización principal:

```text
frontend/src/
assets/
components/
composables/
layouts/
router/
services/
stores/
types/
utils/
views/
```

Reglas:

- las views consumen servicios, no Axios directamente;
- `httpClient` usa `VITE_API_BASE_URL` con fallback `/api/v1`;
- `adminHttpClient` obtiene el ID Token desde Firebase Auth y agrega `Authorization: Bearer` solo a requests admin;
- en producción Firebase Hosting sirve la SPA y el navegador llama al backend desplegado en Northflank mediante `VITE_API_BASE_URL`;
- solo variables `VITE_*` llegan al bundle;
- no se persisten manualmente ID Tokens;
- no se usa `v-html`.

## 4. Backend

Stack:

- Java 17;
- Spring Boot 4.1.x;
- Maven;
- Spring Web MVC;
- Spring Security;
- Spring Data JPA / Hibernate;
- Bean Validation;
- Flyway;
- Actuator;
- SpringDoc OpenAPI;
- Firebase Admin SDK;
- Cloudinary Java SDK;
- Cloudflare Turnstile Siteverify mediante cliente HTTP de Spring.

El backend es un monolito modular por funcionalidad:

```text
analytics/
admin/
link/
post/
question/
section/
shared/
tag/
```

Controllers devuelven DTOs. Las entidades JPA no se exponen directamente por HTTP.

## 5. Base de datos

Base principal: PostgreSQL.

Desarrollo local:

- Docker Compose;
- imagen oficial PostgreSQL;
- puerto host configurable con `POSTGRES_HOST_PORT`.

Producción prevista:

- Neon PostgreSQL.
- Spring Boot se ejecutará con el perfil `prod`, tomando `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` desde variables de entorno.
- El puerto HTTP usa `PORT` con fallback `8080`, compatible con contenedores y Cloud Run.

Flyway controla el esquema. Hibernate valida con `ddl-auto=validate`; no crea ni actualiza tablas.

Migraciones actuales:

- `V1__create_sections.sql`;
- `V2__create_interesting_links.sql`;
- `V3__create_posts_and_tags.sql`;
- `V4__create_student_questions.sql`;
- `V5__create_question_attachments.sql`;
- `V6__link_posts_to_source_questions.sql`;
- `V7__add_post_content_document.sql`;
- `V8__create_post_images.sql`;
- `V9__create_analytics.sql`.

Las migraciones V1-V9 ya aplicadas son inmutables.

## 6. API pública

Prefijo:

```text
/api/v1
```

Rutas públicas principales:

```text
GET  /sections
GET  /sections/{slug}/posts
GET  /posts/{id}
GET  /posts/search?q=...
GET  /tags/{slug}/posts
GET  /links
POST /questions
POST /analytics/visit
POST /analytics/sections/{slug}/view
POST /analytics/posts/{id}/view
GET  /analytics/visits/count
```

Las rutas públicas de publicaciones solo exponen contenido `PUBLISHED`.

## 7. API administrativa

Todas las rutas administrativas viven bajo:

```text
/api/v1/admin/**
```

Requieren rol `ADMIN`.

Rutas principales:

```text
GET    /admin/me
GET    /admin/questions
GET    /admin/questions/pending
GET    /admin/questions/{id}
POST   /admin/questions/{id}/draft
DELETE /admin/questions/{id}/draft
POST   /admin/questions/{id}/reject
POST   /admin/questions/{id}/archive
POST   /admin/questions/{id}/reopen

GET    /admin/posts
POST   /admin/posts
GET    /admin/posts/{id}
PATCH  /admin/posts/{id}
DELETE /admin/posts/{id}
POST   /admin/posts/{id}/publish
POST   /admin/posts/{id}/archive
POST   /admin/posts/{id}/restore
POST   /admin/posts/{postId}/images
PATCH  /admin/posts/{postId}/images/{imageId}
DELETE /admin/posts/{postId}/images/{imageId}

GET    /admin/tags
POST   /admin/tags
PATCH  /admin/tags/{id}
DELETE /admin/tags/{id}

GET    /admin/links
POST   /admin/links
PATCH  /admin/links/{id}
DELETE /admin/links/{id}

GET    /admin/analytics/summary
```

## 8. Seguridad

Spring Security mantiene:

- API stateless;
- rutas públicas explícitas;
- `/api/v1/admin/**` con rol `ADMIN`;
- `anyRequest().denyAll()`.

CORS:

- se configura exclusivamente con `CORS_ALLOWED_ORIGINS`;
- no se usa wildcard de origins;
- si no hay origins configurados, el backend no abre CORS globalmente;
- permite solo métodos y headers necesarios para la SPA y el panel admin;
- no permite credenciales porque la autenticación admin viaja por `Authorization: Bearer`.

Firebase:

- frontend usa Firebase Web SDK solo para login;
- backend usa Firebase Admin SDK para verificar ID Tokens;
- `verifyIdToken(token, true)` valida revocación;
- el UID se compara contra `FIREBASE_ADMIN_UID`;
- email no se usa como autoridad.

Con `FIREBASE_ENABLED=false`, tests y entornos locales sin credenciales siguen funcionando.

Turnstile:

- `POST /api/v1/questions` sigue siendo público en Spring Security;
- cuando `TURNSTILE_ENABLED=true`, el backend valida el token antes de validar imagen, subir a Cloudinary o persistir;
- el backend envía `secret` y `response` a Cloudflare Siteverify, sin `remoteip`;
- se exige `success=true`;
- si `TURNSTILE_EXPECTED_ACTION` está configurado, la action devuelta debe coincidir exactamente;
- si `TURNSTILE_EXPECTED_HOSTNAMES` está configurado, el hostname devuelto debe pertenecer a esa lista;
- si Cloudflare falla o no responde, el envío falla cerrado;
- no se almacenan IP, fingerprint, datos Turnstile ni tokens Turnstile.

Flujo:

```text
Student
   ↓
Turnstile browser challenge
   ↓
POST /api/v1/questions
   ↓
Spring Boot
   ↓
Cloudflare Siteverify
   ↓
validación de pregunta / imagen / persistencia
```

## 9. Contenido académico estructurado

`posts.content_document` es JSONB y es la fuente editorial de verdad.

`posts.content` es texto plano derivado para búsquedas y compatibilidad.

La entidad JPA persiste el JSON como tipo Java neutral para evitar acoplarla a una versión concreta de Jackson. La validación/canonicalización del documento usa componentes de contenido del backend.

El documento permite únicamente nodos, marcas y atributos whitelisted:

- párrafos;
- H2/H3;
- listas;
- citas;
- bloques académicos;
- enlaces HTTP/HTTPS/mailto seguros;
- estilos semánticos controlados;
- imágenes por `postImageId`, caption y `displaySize`.

Se rechazan HTML libre, estilos CSS arbitrarios, clases arbitrarias y atributos no permitidos.

## 10. Imágenes

Cloudinary almacena binarios; PostgreSQL guarda metadata.

Preguntas:

- `QuestionAttachment`;
- máximo una imagen;
- JPEG/PNG;
- privada para admin;
- no se vuelve pública automáticamente.

Publicaciones:

- `PostImage`;
- pertenece a un Post;
- folder Cloudinary independiente;
- formato canónico `jpg` o `png`;
- `public_id` no se expone en DTOs;
- el documento público referencia imágenes por metadata controlada;
- una imagen referenciada no puede eliminarse;
- una imagen no utilizada sí puede eliminarse;
- al descartar un borrador con imágenes se limpia Cloudinary antes de metadata y Post;
- `NOT_FOUND` remoto se trata como idempotente.

## 11. Hashtags

`tags` y `post_tags` existen desde V3.

Reglas técnicas:

- slug generado automáticamente al crear;
- slug inmutable al renombrar;
- unicidad por slug;
- unicidad case-insensitive por nombre;
- sin `CascadeType.REMOVE` ni `CascadeType.ALL` destructivo entre Post y Tag;
- `usageCount` se calcula con query agregada para evitar N+1.

## 12. Analytics

Analytics V1 usa tablas propias:

- `site_visits`;
- `section_views`;
- `post_views`.

El frontend genera un UUID anónimo por sesión de navegador mediante `sessionStorage`. No se usan cookies ni localStorage para analytics.

El backend evita duplicados por sesión:

- una visita por `session_id`;
- una consulta de sección por `session_id + section_id`;
- una consulta de publicación por `session_id + post_id`.

No se guardan IP, ubicación, fingerprint, correo, Firebase UID, user agent ni referrer.

## 13. Configuración

Variables principales:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
CORS_ALLOWED_ORIGINS
CLOUDINARY_ENABLED
CLOUDINARY_CLOUD_NAME
CLOUDINARY_API_KEY
CLOUDINARY_API_SECRET
FIREBASE_ENABLED
FIREBASE_PROJECT_ID
FIREBASE_ADMIN_UID
GOOGLE_APPLICATION_CREDENTIALS
TURNSTILE_ENABLED
TURNSTILE_SECRET_KEY
TURNSTILE_EXPECTED_HOSTNAMES
TURNSTILE_EXPECTED_ACTION
VITE_API_BASE_URL
VITE_TURNSTILE_SITE_KEY
VITE_FIREBASE_*
```

`.env.example` contiene placeholders. `.env` está ignorado.

`GOOGLE_APPLICATION_CREDENTIALS` lo descubre Google Application Default Credentials; la aplicación no abre ni parsea manualmente el JSON de service account.

En ejecución local esa variable puede apuntar a un JSON fuera del repositorio. En Cloud Run se debe usar la service identity del servicio para que ADC resuelva credenciales sin subir ni referenciar un JSON de service account.

## 14. Preparación de contenedor y perfil productivo

El backend tiene un `Dockerfile` multi-stage en `backend/`:

- etapa builder con Maven y Java 17;
- etapa runtime con JRE Java 17;
- ejecución del JAR como usuario no-root;
- sin copiar `.env`, credenciales ni código fuente al runtime.

El perfil `prod`:

- reactiva DataSource, JPA y Flyway;
- usa PostgreSQL mediante variables de entorno;
- mantiene `ddl-auto=validate` y `open-in-view=false`;
- deshabilita SpringDoc/Swagger;
- expone Actuator `health` sin detalles;
- evita incluir stack traces, exception class y binding internals en respuestas de error estándar.

## 15. Testing

Backend:

- JUnit;
- Mockito;
- Spring MVC tests;
- pruebas JPA con H2 cuando cruzan persistencia;
- sin dependencia obligatoria de PostgreSQL Docker para la suite normal.

Frontend:

- Vitest;
- Vue Test Utils;
- mocks de servicios API/Firebase;
- tests sin backend real.

## 16. Gaps preproducción

Riesgo principal pendiente:

- monitoreo operativo de abuso y errores antes de abrir tráfico real de producción.

Pendiente de infraestructura:

- configuración final de Firebase Hosting;
- Cloud Run;
- Neon;
- secretos cloud;
- CI/CD;
- monitoreo y backups.
