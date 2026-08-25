# PRODUCT_SPEC.md — Hidrología UdeA

## 1. Visión del producto

Hidrología UdeA será una plataforma web académica destinada a servir como base de conocimiento para los estudiantes de la materia de Hidrología de la Universidad de Antioquia.

El profesor podrá documentar preguntas frecuentes, procedimientos y soluciones utilizando texto, imágenes y capturas de pantalla.

Los estudiantes podrán consultar ese conocimiento y enviar nuevas preguntas cuando no encuentren una respuesta.

La plataforma deberá crecer semestre tras semestre a partir de las preguntas reales de los estudiantes.

---

# 2. Usuarios

## Estudiante

No necesita registrarse ni iniciar sesión.

Puede:

* consultar contenido;
* navegar entre talleres;
* navegar entre parciales;
* consultar enlaces de interés;
* buscar publicaciones;
* explorar hashtags;
* enviar preguntas;
* utilizar nickname;
* preguntar anónimamente;
* adjuntar una imagen a una pregunta;
* consultar el contador público de visitas.

## Profesor / Administrador

Es el único usuario que necesita autenticación.

Puede:

* iniciar sesión;
* consultar preguntas recibidas;
* responder preguntas;
* publicar preguntas y soluciones;
* crear publicaciones directamente;
* editar publicaciones;
* eliminar publicaciones;
* asignar hashtags;
* crear hashtags;
* administrar enlaces de interés;
* consultar estadísticas privadas.

---

# 3. Página principal

La página inicial funcionará como punto de entrada al sitio.

Debe conservar un carácter académico relacionado con Hidrología y la Universidad de Antioquia.

Debe mostrar tres accesos principales:

## Enlaces de interés

Acceso a recursos generales seleccionados por el profesor.

## Talleres

Acceso a:

### Taller 1

Morfometría de cuencas.

### Taller 2

Estadística y balance hídrico.

### Taller 3

Curva de duración de caudales.

## Parciales

Acceso a:

* Parcial 1.
* Parcial 2.
* Parcial 3.

La página también deberá ofrecer acceso visible a:

**Agregar una pregunta**

---

# 4. Talleres y parciales

Cada taller o parcial tendrá su propia sección.

Dentro de una sección se mostrarán las publicaciones asociadas.

Ejemplo:

`Talleres > Taller 1 > Publicaciones`

Una publicación podrá contener:

* título o pregunta;
* solución;
* contenido con formato;
* imágenes;
* hashtags;
* fecha de publicación;
* número de visualizaciones si posteriormente se decide mostrarlo.

---

# 5. Publicaciones

El profesor podrá crear publicaciones independientemente de las preguntas de los estudiantes.

Una publicación deberá tener como mínimo:

* título;
* contenido;
* sección;
* fecha de creación;
* estado.

Podrá tener:

* una o más imágenes;
* uno o más hashtags;
* referencia a una pregunta de estudiante que la originó.

El profesor podrá:

* crear;
* consultar;
* editar;
* publicar;
* eliminar.

---

# 6. Hashtags

Los hashtags ayudan a organizar preguntas relacionadas.

Ejemplo:

`#Morfometría`

`#Cuencas`

`#BalanceHídrico`

`#Caudales`

Una publicación puede contener varios hashtags.

Al seleccionar un hashtag, el estudiante debe poder consultar las publicaciones relacionadas.

## Regla

Solo el profesor puede crear y administrar hashtags.

Los estudiantes no pueden crear hashtags cuando envían una pregunta.

Al editar una publicación, el sistema debe sugerir hashtags existentes para favorecer su reutilización y evitar duplicados.

Los hashtags deben manejarse de manera que no existan duplicados únicamente por diferencias de mayúsculas/minúsculas.

---

# 7. Buscador

La aplicación tendrá un buscador público.

Debe permitir localizar contenido utilizando al menos:

* título;
* texto de la publicación;
* hashtags.

Posteriormente podrá incorporar filtros por sección.

Ejemplo de búsqueda:

`cuenca`

Puede devolver:

* publicaciones que contengan "cuenca";
* publicaciones con hashtags relacionados.

---

# 8. Agregar una pregunta

Los estudiantes podrán enviar preguntas sin registrarse.

El formulario deberá contener:

## Identificación

Dos posibilidades:

### Nickname

El estudiante escribe voluntariamente un apodo.

### Anónimo

El estudiante selecciona que no desea identificarse.

No debe requerirse nombre real, correo electrónico ni cuenta.

## Categoría

El estudiante deberá indicar dónde se relaciona la pregunta.

Opciones:

* Taller 1.
* Taller 2.
* Taller 3.
* Parcial 1.
* Parcial 2.
* Parcial 3.

## Pregunta

Campo obligatorio.

## Imagen

Opcional.

Permitirá adjuntar una captura de pantalla o imagen relacionada con la duda.

---

# 9. Estado de las preguntas

Una pregunta enviada por un estudiante NO se publica automáticamente.

Inicialmente queda en estado:

`PENDING`

El profesor podrá revisarla.

Estados oficiales:

* `PENDING`: pendiente de revisión o resolución.
* `REJECTED`: revisada y descartada; no se elimina físicamente ni se elimina su imagen adjunta.
* `ARCHIVED`: pregunta válida cerrada sin crear una nueva publicación.
* `PUBLISHED`: pregunta que originó una publicación.

Eliminar definitivamente una pregunta será una operación administrativa separada y futura.

---

# 10. Conversión de pregunta en publicación

Flujo:

Estudiante envía pregunta.

↓

Pregunta queda pendiente.

↓

Profesor inicia sesión.

↓

Profesor consulta la pregunta.

↓

Profesor escribe la solución.

↓

Profesor asigna hashtags.

↓

Profesor decide publicarla.

↓

Se genera o vincula una publicación.

↓

La solución queda disponible públicamente.

La pregunta original debe conservarse para mantener trazabilidad interna.

Una pregunta de estudiante puede originar como máximo una publicación.

Durante la preparación editorial, el profesor puede crear un borrador de publicación asociado a una pregunta pendiente. En ese estado:

* la pregunta continúa en `PENDING`;
* el borrador puede estar incompleto;
* la pregunta no se considera publicada todavía;
* archivar o rechazar la pregunta queda bloqueado mientras exista el borrador;
* descartar el borrador elimina únicamente la publicación en borrador;
* la pregunta y su imagen adjunta permanecen intactas.

Implementado en la fase Admin Questions V2B2A:

* el borrador asociado a una pregunta se edita manualmente;
* guardar cambios es una acción explícita, sin autosave;
* la sección editorial del Post puede cambiar sin modificar la sección original de la StudentQuestion;
* publicar requiere título y contenido;
* publicar actualiza atómicamente el Post y la StudentQuestion;
* `StudentQuestion.PUBLISHED` significa que la pregunta originó una publicación;
* la StudentQuestion continúa siendo privada y administrativa;
* las preguntas publicadas aparecen en una pestaña administrativa propia;
* el Post publicado queda visible en la API pública;
* los Posts publicados quedan en solo lectura durante esta fase.

Implementado en la fase Admin Publications V1:

* el panel administrativo incluye un módulo de publicaciones;
* las publicaciones se organizan en pestañas de borradores, publicadas y archivadas;
* los Posts `DRAFT`, `PUBLISHED` y `ARCHIVED` pueden editarse con guardado manual;
* los cambios guardados sobre un Post `PUBLISHED` se reflejan inmediatamente en la web pública;
* los Posts `ARCHIVED` se conservan administrativamente y no son visibles públicamente;
* archivar y restaurar publicaciones son acciones explícitas;
* `publishedAt` conserva la fecha de primera publicación al archivar y restaurar;
* `StudentQuestion.PUBLISHED` no cambia cuando se archiva o restaura el Post asociado.

Implementado en la fase Admin Hashtags V1:

* los hashtags son gestionados únicamente por el profesor;
* el panel administrativo incluye `Admin > Hashtags`;
* el profesor puede crear, renombrar y eliminar hashtags cuando no tienen usos;
* el slug se genera automáticamente al crear un hashtag;
* el slug permanece estable e inmutable al renombrar para conservar URLs públicas;
* el listado administrativo muestra `usageCount`;
* un Post puede tener múltiples hashtags;
* la asignación de hashtags se realiza desde el editor de publicaciones;
* guardar hashtags, contenido y sección ocurre en una única operación editorial;
* los cambios guardados sobre un Post `PUBLISHED` se reflejan inmediatamente en la web pública.

Implementado en la fase Admin Publications V2:

* el profesor puede crear publicaciones `DRAFT` manualmente desde `Admin > Publicaciones`;
* una publicación manual nace sin `StudentQuestion` de origen;
* crear una publicación manual no la publica automáticamente;
* el editor existente permite completar título, contenido, sección y hashtags;
* descartar un borrador manual elimina únicamente ese Post y no afecta preguntas.

Las imágenes adjuntas por estudiantes son referencias privadas de la pregunta. No se copian ni se reutilizan automáticamente en una publicación. Si posteriormente el profesor decide usar una imagen de una pregunta en una publicación, deberá crearse una copia independiente propiedad de la publicación.

Futuro:

* búsqueda avanzada o autocompletado de hashtags;
* redirects si alguna vez se permite cambiar slugs;
* imágenes propias de Posts;
* copia controlada de una QuestionAttachment hacia una imagen de Post;
* versionado o historial de cambios;
* autosave.

---

# 11. Panel administrativo

El panel administrativo es privado.

Debe requerir autenticación.

Pantalla principal propuesta:

## Resumen

* preguntas pendientes;
* publicaciones;
* visitas;
* actividad reciente.

## Preguntas

Listado de preguntas enviadas por estudiantes.

Debe permitir:

* consultar;
* filtrar por estado;
* abrir;
* responder;
* publicar;
* rechazar.

## Publicaciones

Debe permitir:

* crear;
* editar;
* eliminar;
* publicar.

## Hashtags

Debe permitir:

* consultar;
* crear;
* editar cuando sea seguro;
* eliminar si no genera inconsistencias.

## Enlaces de interés

Debe permitir:

* crear;
* editar;
* eliminar.

## Estadísticas

Panel privado de métricas.

---

# 12. Enlaces de interés

La sección pública mostrará recursos seleccionados por el profesor.

Cada enlace puede contener:

* título;
* descripción;
* URL;
* fecha de creación;
* estado activo/inactivo.

Solo el profesor puede administrarlos.

---

# 13. Contador público de visitas

La parte pública mostrará únicamente una estadística:

**Visitas al sitio: X**

No se mostrarán públicamente estadísticas detalladas.

Una recarga constante de la página no debería incrementar indefinidamente el contador.

Se utilizará un concepto de sesión anónima para aproximar las visitas.

---

# 14. Estadísticas privadas

Solo el profesor podrá consultar las estadísticas detalladas.

Como mínimo:

* visitas totales;
* visitas del día;
* visitas de la semana;
* visitas del mes;
* secciones más consultadas;
* taller más consultado;
* parcial más consultado;
* publicaciones más consultadas;
* total de preguntas recibidas;
* preguntas pendientes;
* preguntas respondidas/publicadas.

No recopilar información personal que no sea necesaria.

---

# 15. Navegación esperada

## Consulta

Inicio

→ Talleres

→ Taller 1

→ Publicaciones

→ Publicación

## Hashtags

Publicación

→ `#Cuencas`

→ Publicaciones relacionadas

## Pregunta

Inicio

→ Agregar una pregunta

→ Seleccionar categoría

→ Nickname o Anónimo

→ Escribir pregunta

→ Adjuntar imagen opcional

→ Enviar

→ Confirmación

## Administración

Login

→ Dashboard

→ Preguntas pendientes

→ Pregunta

→ Responder

→ Asignar hashtags

→ Publicar

---

# 16. Diseño

La interfaz deberá tomar como inspiración las referencias visuales proporcionadas por el profesor.

Características deseadas:

* limpia;
* académica;
* sencilla;
* moderna;
* responsive;
* fácil de utilizar;
* sin sobrecarga visual.

La página principal utilizará tarjetas o accesos visuales grandes para:

* Enlaces de interés;
* Talleres;
* Parciales.

La experiencia móvil debe considerarse desde el inicio.

---

# 17. Seguridad y privacidad

Los estudiantes no requieren cuenta.

No solicitar información personal innecesaria.

Las operaciones administrativas requieren autenticación.

Las preguntas públicas nunca deben permitir ejecutar HTML o scripts introducidos por usuarios.

Las imágenes deben validarse.

El formulario público debe tener mecanismos para reducir spam y abuso.

---

# 18. Fuera del alcance inicial

No implementar inicialmente:

* cuentas de estudiantes;
* perfiles de estudiantes;
* comentarios públicos;
* chats;
* mensajería privada;
* calificaciones;
* entrega de talleres;
* sistema de notas;
* múltiples profesores administradores;
* aplicación móvil nativa;
* microservicios.

Estas funcionalidades requieren nueva aprobación antes de incorporarse.

---

# 19. Objetivo del MVP

El MVP será considerado funcional cuando un estudiante pueda:

1. entrar a la página;
2. navegar por Talleres y Parciales;
3. consultar publicaciones;
4. buscar contenido;
5. navegar mediante hashtags;
6. consultar Enlaces de interés;
7. enviar una pregunta como nickname o Anónimo;
8. adjuntar opcionalmente una imagen;
9. ver el contador de visitas.

Y cuando el profesor pueda:

1. iniciar sesión;
2. consultar preguntas pendientes;
3. responderlas;
4. convertirlas en publicaciones;
5. crear publicaciones;
6. editarlas y eliminarlas;
7. crear y asignar hashtags;
8. administrar enlaces;
9. consultar estadísticas.

---

# 20. Principio del producto

La aplicación debe ser suficientemente sencilla para que el profesor pueda administrar su contenido sin conocimientos de programación.

La tecnología debe permanecer invisible para el usuario final.

El producto existe para facilitar la consulta y acumulación progresiva del conocimiento de la materia.
