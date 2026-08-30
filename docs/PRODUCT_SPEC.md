# Product Spec — Hidrología UdeA

## 1. Visión

Hidrología UdeA es una plataforma académica para la materia de Hidrología de la Universidad de Antioquia. Centraliza recursos del curso, publicaciones del profesor, respuestas a preguntas frecuentes, enlaces de interés y estadísticas de uso.

El producto debe crecer semestre tras semestre a partir de las preguntas reales de los estudiantes y del trabajo editorial del profesor.

## 2. Usuarios

### Estudiante

No necesita cuenta ni inicio de sesión.

Puede:

- consultar la página principal;
- navegar por Talleres y Parciales;
- leer publicaciones públicas;
- buscar publicaciones por título, contenido y hashtags;
- explorar páginas públicas de hashtags;
- consultar enlaces de interés activos;
- enviar una pregunta con nickname opcional o de forma anónima;
- adjuntar una imagen JPEG/PNG a una pregunta;
- generar métricas anónimas de uso por sesión.

### Profesor / Administrador

Es el único usuario autenticado. La autenticación usa Firebase Authentication y la autorización final depende del UID configurado en el backend.

Puede:

- iniciar sesión en el panel privado;
- revisar preguntas recibidas;
- convertir preguntas en borradores de publicación;
- crear publicaciones manuales sin pregunta de origen;
- editar contenido académico con editor estructurado;
- subir y gestionar imágenes propias de publicaciones;
- publicar, archivar, restaurar y descartar borradores según reglas de dominio;
- crear, renombrar y eliminar hashtags no usados;
- asignar hashtags a publicaciones;
- administrar enlaces de interés;
- consultar estadísticas privadas.

## 3. Página principal

La página inicial es el punto de entrada público. Debe mantener una identidad académica asociada a Hidrología UdeA y ofrecer acceso claro a:

- Enlaces de interés;
- Talleres;
- Parciales;
- Agregar una pregunta;
- búsqueda pública desde la navegación.

## 4. Talleres y parciales

Las secciones públicas son:

- Taller 1: Morfometría de cuencas.
- Taller 2: Estadística y balance hídrico.
- Taller 3: Curva de duración de caudales.
- Parcial 1.
- Parcial 2.
- Parcial 3.

Cada sección muestra únicamente publicaciones con estado `PUBLISHED` asociadas a esa sección. Si no hay publicaciones, se muestra un estado vacío, no un error.

## 5. Publicaciones

Una publicación puede nacer de una pregunta estudiantil o ser creada manualmente por el profesor.

Una publicación contiene:

- título;
- contenido textual derivado;
- documento estructurado editorial;
- sección;
- estado;
- fechas de creación, actualización y publicación;
- hashtags;
- imágenes embebibles propias del Post;
- referencia opcional a una pregunta de origen.

Estados:

- `DRAFT`: editable, no público.
- `PUBLISHED`: visible para estudiantes.
- `ARCHIVED`: conservado en admin, no público.

Reglas:

- crear manualmente una publicación siempre produce un `DRAFT`;
- publicar un borrador lo vuelve visible;
- archivar oculta una publicación sin borrarla y, si nació de una pregunta, sincroniza esa pregunta como `ARCHIVED`;
- restaurar vuelve a publicar una publicación archivada y, si nació de una pregunta, sincroniza esa pregunta como `PUBLISHED`;
- descartar por endpoint está limitado a borradores manuales sin pregunta de origen;
- no existe hard-delete público de publicaciones publicadas como funcionalidad de producto.

## 6. Editor académico

El editor administrativo es visual y estructurado. No expone Markdown al profesor ni acepta HTML libre.

Soporta:

- párrafos;
- H2 y H3;
- negrita, cursiva y subrayado;
- listas;
- citas;
- enlaces seguros;
- alineaciones controladas;
- tamaños controlados;
- colores y resaltados controlados;
- bloques académicos: Nota, Ejemplo e Importante;
- imágenes insertables con texto alternativo, caption y tamaño visual controlado;
- videos embebidos mediante una acción explícita de Video;
- vista previa con el estado local del formulario, incluso sin guardar;
- autosave para publicaciones `DRAFT` y `ARCHIVED`;
- guardado manual para publicaciones `PUBLISHED`.

Autosave observa título, contenido estructurado, sección y hashtags. El editor informa los estados `Guardado`, `Cambios sin guardar`, `Guardando...` y `No se pudo guardar`, permite reintentar si falla y bloquea acciones que dependen del contenido persistido mientras hay cambios pendientes o un guardado en curso.

El botón Video permite insertar recursos embebidos validados:

- YouTube: `youtube.com/watch?v=VIDEO_ID`, `youtu.be/VIDEO_ID`, `youtube.com/shorts/VIDEO_ID`, `youtube.com/embed/VIDEO_ID` y `youtube.com/live/VIDEO_ID`;
- TikTok: enlaces completos `https://www.tiktok.com/@usuario/video/POST_ID` y `https://www.tiktok.com/player/v1/POST_ID`;
- video directo HTTPS terminado en `.mp4` o `.webm`.

Los parámetros de consulta de YouTube, como `?si=...`, pueden existir pero no forman parte del ID. Los enlaces cortos de TikTok (`vt.tiktok.com`, `vm.tiktok.com`) no se resuelven automáticamente: el profesor debe abrir el video y usar el enlace completo/canónico. La acción Enlace sigue creando enlaces normales, no embeds. No existen previews automáticas genéricas de páginas web.

El renderer público usa el documento estructurado validado por backend y no usa `v-html`.

## 7. Imágenes

Cloudinary almacena los binarios. PostgreSQL guarda únicamente metadata y referencias.

Preguntas:

- permiten máximo una imagen JPEG/PNG;
- la imagen permanece privada al flujo administrativo;
- `QuestionAttachment` no se copia automáticamente a publicaciones.

Publicaciones:

- usan `PostImage`, separado de `QuestionAttachment`;
- las imágenes pertenecen a un Post;
- las imágenes no utilizadas pueden eliminarse explícitamente;
- las imágenes referenciadas por el documento no pueden eliminarse;
- descartar un borrador con imágenes limpia Cloudinary, metadata y Post de forma controlada;
- un `NOT_FOUND` remoto durante limpieza se trata como resultado idempotente.

## 8. Hashtags

Solo el profesor gestiona hashtags.

Un hashtag tiene:

- `id`;
- `name`;
- `slug`.

Reglas:

- el slug se genera al crear desde el nombre;
- el slug es inmutable al renombrar para conservar URLs públicas;
- nombres duplicados case-insensitive no se aceptan;
- colisiones de slug no se aceptan;
- `usageCount` cuenta relaciones con publicaciones `DRAFT`, `PUBLISHED` y `ARCHIVED`;
- un hashtag usado no se elimina hasta quitarlo de las publicaciones.

## 9. Enlaces de interés

El profesor administra enlaces externos con:

- título;
- descripción opcional;
- URL HTTP/HTTPS;
- orden;
- estado activo/inactivo.

La página pública muestra únicamente enlaces activos ordenados por `display_order`.

## 10. Preguntas de estudiantes

El formulario público permite enviar preguntas sin cuenta.

Campos:

- nickname opcional;
- sección obligatoria;
- pregunta obligatoria;
- imagen opcional JPEG/PNG;
- verificación anti-abuso mediante Cloudflare Turnstile.

Reglas:

- nickname vacío se guarda como `null` y representa anonimato;
- toda pregunta nace con estado `PENDING`;
- el estudiante no elige estado;
- preguntas pendientes no son contenido público;
- no existe listado público de preguntas;
- si Turnstile falla, la pregunta no se crea y la imagen no se sube.

Privacidad:

- no se almacenan IP, fingerprint, datos Turnstile ni tokens Turnstile;
- Turnstile se usa exclusivamente como barrera anti-abuso del formulario público.

Estados:

- `PENDING`: espera revisión.
- `PUBLISHED`: fue procesada/publicada mediante un Post.
- `ARCHIVED`: se conserva fuera del flujo activo.
- `REJECTED`: no será publicada.

## 11. Búsqueda pública

La búsqueda pública consulta únicamente publicaciones `PUBLISHED`.

Debe buscar por:

- título;
- contenido textual derivado;
- hashtags.

La búsqueda es case-insensitive, acepta coincidencias parciales y conserva `q` en la URL para recarga y navegación back/forward.

## 12. Estadísticas

Las estadísticas son propias del backend y se almacenan en PostgreSQL.

Se registra:

- visita pública de sitio por sesión anónima;
- consulta de sección por sesión;
- consulta de publicación por sesión.

No se recolectan IP, ubicación, huella digital, correo, UID de Firebase, user agent ni referrer.

El panel privado muestra:

- visitas totales;
- visitas de hoy, semana y mes;
- visitas diarias;
- secciones más consultadas;
- taller y parcial más consultado;
- publicaciones más consultadas;
- conteo de preguntas totales, pendientes y publicadas.

## 13. Autenticación y administración

El panel administrativo requiere login del profesor mediante Firebase Web SDK en frontend y Firebase Admin SDK en backend.

La autorización no depende del email. El backend compara el UID del token con `FIREBASE_ADMIN_UID`.

Las rutas administrativas viven bajo `/api/v1/admin/**` y requieren rol `ADMIN`.

## 14. Fuera de alcance actual

No son parte del producto actual:

- registro público de estudiantes;
- creación de hashtags por estudiantes;
- comentarios;
- versionado editorial;
- búsqueda avanzada/autocomplete;
- imágenes de publicaciones reutilizadas automáticamente desde preguntas.
