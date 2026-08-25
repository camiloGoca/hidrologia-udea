# ARCHITECTURE.md — Hidrología UdeA

## 1. Arquitectura general

La aplicación utilizará una arquitectura:

**SPA + REST API + PostgreSQL + servicios cloud administrados**

El backend será un:

**Monolito modular**

No se utilizarán microservicios para el MVP.

---

# 2. Diagrama general

```text
                        USUARIO
                           │
                           ▼
                ┌────────────────────┐
                │ Firebase Hosting   │
                │                    │
                │ Vue 3 + TypeScript │
                └─────────┬──────────┘
                          │
                     /api/v1
                          │
                          ▼
                ┌────────────────────┐
                │ Google Cloud Run   │
                │                    │
                │ Spring Boot 4.1.x  │
                │ Java 17            │
                └───┬────────┬───────┘
                    │        │
          ┌─────────┘        └─────────┐
          ▼                            ▼
┌──────────────────┐         ┌──────────────────┐
│ PostgreSQL       │         │ Cloudinary       │
│ Neon             │         │                  │
│                  │         │ imágenes         │
└──────────────────┘         └──────────────────┘

            Firebase Authentication
                      │
                      ▼
               Profesor/Admin
```

---

# 3. Repositorio

Se utilizará un monorepo.

Estructura inicial:

```text
hidrologia-udea/
│
├── frontend/
│
├── backend/
│
├── docs/
│   ├── PRODUCT_SPEC.md
│   └── ARCHITECTURE.md
│
├── infra/
│
├── .github/
│   └── workflows/
│
├── AGENTS.md
├── README.md
├── .gitignore
└── .env.example
```

---

# 4. Frontend

Tecnologías:

* Vue 3
* TypeScript
* Vite
* Composition API
* Vue Router
* Pinia
* Axios
* Tailwind CSS

Estructura orientativa:

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
views/
utils/
```

## Responsabilidades

Frontend:

* interfaz;
* navegación;
* formularios;
* validaciones de experiencia de usuario;
* consumo de API;
* manejo de sesión del administrador.

Frontend NO es la autoridad de seguridad.

Toda operación sensible debe validarse nuevamente en el backend.

---

# 5. Backend

Tecnologías:

* Java 17
* Spring Boot 4.1.x
* Maven
* Spring Web
* Spring Security
* Spring Data JPA
* Hibernate
* Bean Validation
* Flyway
* Spring Boot Actuator
* OpenAPI / Swagger

Arquitectura por funcionalidad.

Ejemplo:

```text
backend/src/main/java/.../

auth/
analytics/
attachment/
link/
post/
question/
section/
tag/
shared/
```

Cada módulo podrá contener:

```text
controller/
service/
repository/
entity/
dto/
```

según sea necesario.

---

# 6. API REST

Prefijo:

```text
/api/v1
```

Ejemplos conceptuales:

```text
GET    /api/v1/sections
GET    /api/v1/posts
GET    /api/v1/posts/{id}
GET    /api/v1/tags
GET    /api/v1/search

POST   /api/v1/questions

GET    /api/v1/admin/questions
POST   /api/v1/admin/posts
PUT    /api/v1/admin/posts/{id}
DELETE /api/v1/admin/posts/{id}

POST   /api/v1/admin/tags
GET    /api/v1/admin/analytics
```

Las rutas exactas se definirán durante la implementación.

---

# 7. PostgreSQL

Base de datos relacional principal: PostgreSQL 17.

## Desarrollo

PostgreSQL 17 ejecutado localmente con Docker Compose mediante la imagen oficial `postgres:17.10-alpine`.

## Producción

PostgreSQL administrado mediante Neon.

La aplicación debe utilizar configuración intercambiable mediante variables de entorno.

Ejemplo conceptual:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
```

---

# 8. Migraciones

Flyway será responsable del esquema.

Ejemplo:

```text
backend/src/main/resources/db/migration/

V1__initial_schema.sql
V2__add_tags.sql
V3__add_analytics.sql
```

Nunca modificar una migración que ya haya sido ejecutada en producción.

Crear una nueva migración.

---

# 9. Modelo conceptual

## Section

Representa:

* Taller 1
* Taller 2
* Taller 3
* Parcial 1
* Parcial 2
* Parcial 3

Campos aproximados:

```text
id
type
name
slug
description
display_order
active
```

---

## StudentQuestion

```text
id
nickname
anonymous
content
status
section_id
created_at
updated_at
```

---

## Post

```text
id
title
content
section_id
source_question_id
status
created_at
updated_at
published_at
```

`source_question_id` es nullable: una publicación puede originarse en una pregunta de estudiante o ser creada directamente por el profesor en una fase futura.

Cuando existe, la relación es conceptualmente:

```text
StudentQuestion 1 -> 0..1 Post
```

La base de datos debe proteger esta regla con una restricción única sobre `posts.source_question_id`.

El vínculo usa `ON DELETE RESTRICT` para evitar perder trazabilidad editorial si una pregunta ya originó una publicación o borrador. La eliminación definitiva de preguntas deberá resolver explícitamente esta relación en una fase futura.

Los posts en estado `DRAFT` pueden tener título y contenido incompletos. Los posts públicos o archivados deben conservar título y contenido válidos.

---

## Tag

```text
id
name
slug
created_at
```

---

## PostTag

Tabla many-to-many:

```text
post_id
tag_id
```

---

## Attachment

```text
id
cloudinary_public_id
url
resource_type
question_id
post_id
created_at
```

La implementación puede utilizar asociaciones alternativas si mejoran la integridad del modelo.

---

## InterestingLink

```text
id
title
description
url
active
created_at
updated_at
```

---

## AnalyticsEvent

```text
id
session_id
event_type
resource_type
resource_id
created_at
```

---

## AdminUser

Como mínimo deberá existir una forma segura de asociar:

```text
firebase_uid
role
active
```

El diseño final podrá variar.

---

# 10. Relaciones principales

```text
Section
  │
  ├── StudentQuestion
  │
  └── Post

StudentQuestion
  │
  └── Post opcional

Post
  │
  ├── Attachment
  │
  └── Tag
       many-to-many

InterestingLink
  independiente

AnalyticsEvent
  referencia recursos cuando corresponde
```

Mientras el Post asociado esté en `DRAFT`, la StudentQuestion permanece en `PENDING`. Ese borrador bloquea archivar o rechazar la pregunta hasta que sea descartado. Descartar un borrador elimina solo el Post; no elimina la pregunta ni su attachment.

En Admin Questions V2B2A el borrador se edita con guardado manual. La sección del Post puede cambiar de forma independiente a la sección original de la StudentQuestion. La publicación exige título y contenido no vacíos y se realiza en una única transacción: `Post.DRAFT -> Post.PUBLISHED`, `published_at = now` y, si existe pregunta origen, `StudentQuestion.PENDING -> StudentQuestion.PUBLISHED`. La pregunta publicada sigue siendo un recurso privado/admin; el contenido público es el Post publicado.

En Admin Publications V1 el panel administrativo lista Posts por estado (`DRAFT`, `PUBLISHED`, `ARCHIVED`) con paginación y orden editorial por `updated_at DESC, id DESC`. Los tres estados permiten edición con guardado manual. Los Posts publicados requieren título y contenido válidos, y sus cambios guardados impactan inmediatamente la API pública. Los Posts archivados no aparecen en endpoints públicos, pero pueden editarse y restaurarse. Las transiciones `PUBLISHED -> ARCHIVED` y `ARCHIVED -> PUBLISHED` preservan `published_at` y no modifican la StudentQuestion de origen.

En Admin Hashtags V1 los hashtags se gestionan únicamente desde el panel del profesor. El slug se genera al crear el Tag y queda inmutable al renombrar para conservar URLs públicas como `/hashtags/morfometria`. El listado administrativo calcula `usageCount` sobre todas las relaciones `post_tags`, incluyendo Posts `DRAFT`, `PUBLISHED` y `ARCHIVED`, porque ese conteo determina si el Tag puede eliminarse. El editor de Posts guarda `title`, `content`, `sectionSlug` y `tagIds` en una única operación transaccional. Editar tags actualiza `posts.updated_at`; en Posts publicados el cambio se refleja inmediatamente en la API pública, sin modificar `published_at` ni la StudentQuestion de origen.

Quedan para fases futuras: búsqueda avanzada/autocomplete de hashtags, redirects si alguna vez se permite cambiar slugs, creación manual de publicaciones, imágenes propias de Posts, copia explícita de QuestionAttachment hacia Post, versionado/historial y autosave.

---

# 11. Autenticación

Proveedor:

Firebase Authentication.

Método inicial:

Email + contraseña.

Solo el profesor utiliza login.

Flujo conceptual:

```text
Profesor
   │
   ▼
Vue Login
   │
   ▼
Firebase Authentication
   │
   ▼
ID Token
   │
   ▼
Spring Security
   │
   ▼
Validación
   │
   ▼
Autorización ADMIN
```

El backend nunca confiará únicamente en que Firebase haya autenticado al usuario.

Debe verificar que corresponda al administrador autorizado.

---

# 12. Imágenes

Proveedor:

Cloudinary.

Uso:

* capturas adjuntas por estudiantes;
* imágenes utilizadas en soluciones/publicaciones.

El backend controla las reglas de subida.

Validar:

* tamaño máximo;
* tipos MIME permitidos;
* errores de carga.

PostgreSQL guarda referencias, no el archivo binario.

Las imágenes adjuntas a preguntas permanecen privadas del flujo administrativo. No se copian automáticamente a Posts. Si una publicación futura reutiliza una imagen de pregunta, deberá hacerlo mediante una copia independiente propiedad del Post.

---

# 13. Estadísticas

Las estadísticas principales serán propias del sistema.

Eventos posibles:

```text
SITE_VISIT
SECTION_VIEW
POST_VIEW
SEARCH
QUESTION_SUBMITTED
```

No todos tienen que implementarse desde la primera iteración.

---

# 14. Sesiones anónimas para visitas

Para evitar que cada refresh cuente como una visita:

Frontend crea o conserva un identificador de sesión anónimo.

Ejemplo conceptual:

```text
localStorage
session_id = UUID
```

El backend determina cuándo registrar una nueva visita siguiendo la política que se defina durante implementación.

No utilizar fingerprinting invasivo.

No recolectar datos personales innecesarios.

---

# 15. Búsqueda

Primera implementación:

PostgreSQL.

Campos considerados:

* título;
* contenido;
* hashtags.

No agregar motores externos de búsqueda inicialmente.

Si el volumen de datos crece extraordinariamente en el futuro, la arquitectura podrá revisarse.

---

# 16. Hosting

## Frontend

Firebase Hosting.

Contendrá el build generado por Vue/Vite.

## Backend

Google Cloud Run.

Spring Boot será desplegado como aplicación contenerizada.

Firebase Hosting podrá redirigir las solicitudes de API hacia Cloud Run si se decide utilizar una ruta compartida.

---

# 17. Docker

Docker se utilizará para:

* PostgreSQL local;
* reproducibilidad del entorno;
* backend cuando sea necesario para despliegue.

Durante desarrollo se utilizará:

```text
docker compose
```

para servicios auxiliares.

El desarrollador podrá ejecutar Vue y Spring Boot directamente desde su máquina para facilitar debugging.

---

# 18. Configuración por ambientes

Ambientes iniciales:

```text
local
test
production
```

Backend:

```text
application.yml
application-local.yml
application-test.yml
application-prod.yml
```

Frontend:

```text
.env.development
.env.production
```

Los secretos reales nunca estarán versionados.

---

# 19. Testing

## Backend

JUnit.

Mockito cuando corresponda.

Testcontainers para pruebas de integración donde resulte útil verificar comportamiento real con PostgreSQL.

## Frontend

Vitest.

## End-to-end

Playwright para flujos críticos.

Flujos E2E prioritarios:

* navegación pública;
* búsqueda;
* envío de pregunta;
* login administrador;
* publicación de respuesta.

---

# 20. CI/CD

Proveedor:

GitHub Actions.

Pipeline futuro:

```text
PUSH / PULL REQUEST
        │
        ├── Frontend
        │     ├── install
        │     ├── lint
        │     ├── test
        │     └── build
        │
        └── Backend
              ├── compile
              ├── test
              └── package
```

Para `main`, posteriormente:

```text
Frontend
   ↓
Firebase Hosting

Backend
   ↓
Cloud Run
```

La automatización de despliegues se implementará después de que el proyecto pueda desplegarse manualmente de forma confiable.

---

# 21. Seguridad

Principios:

* mínimo privilegio;
* backend como autoridad;
* validación de toda entrada;
* secretos mediante variables de entorno;
* autenticación para administración;
* rate limiting para formularios públicos;
* archivos validados;
* respuestas HTTP seguras;
* manejo centralizado de errores.

No almacenar información sensible innecesaria.

---

# 22. Observabilidad

Spring Boot Actuator proporcionará health checks básicos.

Inicialmente no se requiere una plataforma avanzada de observabilidad.

Logs:

* útiles;
* estructurados cuando sea práctico;
* nunca incluir contraseñas, tokens ni secretos.

---

# 23. Principios de arquitectura

## Simplicidad

No agregar infraestructura hasta necesitarla.

## Separación de responsabilidades

Frontend, backend, almacenamiento y base de datos tienen responsabilidades diferentes.

## Seguridad

Nunca confiar exclusivamente en el cliente.

## Evolución incremental

Primero hacer funcionar el flujo básico.

Después incorporar servicios externos.

## Comprensión

Toda tecnología nueva debe introducirse acompañada de explicación y pasos claros para el desarrollador.

---

# 24. Stack congelado para MVP

```text
Frontend
Vue 3
TypeScript
Vite
Vue Router
Pinia
Axios
Tailwind CSS

Backend
Java 17
Spring Boot 4.1.x
Maven
Spring Web
Spring Security
Spring Data JPA
Hibernate
Bean Validation
Flyway
Actuator
OpenAPI

Database
PostgreSQL 17
Neon en producción

Authentication
Firebase Authentication

Image Storage
Cloudinary

Frontend Hosting
Firebase Hosting

Backend Hosting
Google Cloud Run

Infrastructure
Docker

Repository
GitHub

CI/CD
GitHub Actions

Testing
JUnit
Mockito
Testcontainers
Vitest
Playwright
```
