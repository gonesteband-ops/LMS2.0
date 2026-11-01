# Plan Arquitectónico — Mejoras Críticas v1.0

## ⚠️ Reglas de Integridad (NO VIOLAR)

1. **NO modificar ni refactorizar código existente que funcione.** Solo agregar nuevas entidades, campos o archivos.
2. **NO tocar** archivos no listados explícitamente en las secciones de cada tarea.
3. **NO cambiar firmas de endpoints existentes**, rutas de templates, ni lógica de negocio actual.
4. **NO alterar el modelo de seguridad actual** (`@PreAuthorize`, `SecurityUtils`).
5. **Mantener retrocompatibilidad total** — los tests existentes deben seguir pasando sin cambios.
6. **Después de cada tarea**, compilar con `mvn clean compile -q` para verificar integridad.
7. **Cada tarea completada se marca con `[X]`**.

---

## Tarea 1: Switch "Requiere Firma Digital de Cumplimiento" en Cursos

**Objetivo arquitectónico:** Agregar un toggle en el formulario de creación/edición de cursos que, cuando está activo, obliga al estudiante a aceptar una declaración jurada con captura de IP y timestamp al momento de inscribirse. Esto cumple con los requisitos de trazabilidad de la Ley 19.799.

### Paso 1.1 — Extender modelo Curso [X]

| Elemento | Detalle |
|----------|---------|
| **Archivo** | `src/main/java/com/lms/model/Curso.java` |
| **Cambio** | Agregar un campo booleano `requiereFirmaCumplimiento` con valor por defecto `false`, anotado como columna en base de datos no nulleable. |
| **Regla** | No modificar ningún campo existente ni relaciones. Solo agregar el nuevo campo. |

### Paso 1.2 — Extender modelo Inscripcion [X]

| Elemento | Detalle |
|----------|---------|
| **Archivo** | `src/main/java/com/lms/model/Inscripcion.java` |
| **Cambio** | Agregar cuatro campos: (1) booleano `declaracionJuradaAceptada` default `false`, (2) String `ipFirma` (máx 45 caracteres, nullable), (3) `LocalDateTime timestampFirma` (nullable), (4) String `firmaHash` (máx 64 caracteres, nullable). |
| **Regla** | No modificar campos existentes ni anotaciones `@Builder`, `@Getter`, etc. Solo agregar los cuatro campos nuevos. |

### Paso 1.3 — Extender DTO de request/response de Curso [X]

| Elemento | Detalle |
|----------|---------|
| **Archivo** | Buscar si existe `CursoRequest.java` o similar en el paquete dto. Si no existe, omitir. |
| **Cambio** | Si existe un DTO de request para crear/actualizar cursos, agregar el campo `requiereFirmaCumplimiento` (booleano). Si el controlador usa `@RequestParam` o `Map<String, Object>`, asegurarse de que el nuevo campo sea opcional (no romper peticiones existentes). |
| **Regla** | Si no existe DTO, no crearlo. Trabajar con la estructura existente. |

### Paso 1.4 — Actualizar CursoController (serialización del nuevo campo) [X]

| Elemento | Detalle |
|----------|---------|
| **Archivo** | `src/main/java/com/lms/controller/CursoController.java` |
| **Cambio** | En el endpoint `POST /api/v1/cursos` y `PUT /api/v1/cursos/{id}`, leer el campo booleano `requiereFirmaCumplimiento` del body (opcional, default `false`). Pasarlo al servicio `CursoService` para que lo almacene en la entidad. |
| **Regla** | No modificar la firma de los métodos del servicio si no es necesario. Si el servicio ya acepta un `CursoRequest`, solo agregar el campo allí. |

### Paso 1.5 — Agregar switch visual en editor.html [X]

| Elemento | Detalle |
|----------|---------|
| **Archivo** | `src/main/resources/templates/cursos/editor.html` |
| **Cambio** | Agregar un componente switch (checkbox estilizado) en el formulario, después del campo "Fecha Límite de Realización". El switch debe tener la etiqueta "Requiere Firma Digital de Cumplimiento (Ley 19.799)" con un texto explicativo debajo. |

**Contrato visual esperado:**
- Un contenedor con fondo y borde (como tarjeta secundaria)
- Texto explicativo: "Si se activa, los estudiantes deberán aceptar una declaración jurada con captura de IP y timestamp al inscribirse"
- Switch toggle (on/off) alineado a la derecha
- En modo edición, el switch debe reflejar el valor guardado del curso

### Paso 1.6 — Actualizar JS del editor [X]

| Elemento | Detalle |
|----------|---------|
| **Archivo** | `src/main/resources/templates/cursos/editor.html` (sección `<script>`) |
| **Cambio** | Agregar lógica para: (a) al guardar, incluir `requiereFirmaCumplimiento` en el payload JSON; (b) al cargar un curso existente, sincronizar el switch con el valor del curso; (c) no romper la creación de cursos nuevos (el campo debe ser opcional, default `false`). |

### Paso 1.7 — Endpoint de inscripción con validación de firma [X]

| Elemento | Detalle |
|----------|---------|
| **Archivo** | Verificar si existe `InscripcionController.java`. Si no existe, crearlo. |
| **Nuevo endpoint** | `POST /api/v1/cursos/{cursoId}/inscribir` |
| **Lógica de alto nivel** | (1) Recibir ID del curso y datos del usuario autenticado. (2) Verificar si el curso existe y está publicado. (3) Verificar si `curso.requiereFirmaCumplimiento == true`. (4) Si requiere firma, validar que el body contenga `declaracionJuradaAceptada = true`. (5) Si no se envía o es `false`, rechazar con error. (6) Si es válido, capturar `request.getRemoteAddr()` como IP, generar `LocalDateTime.now()` como timestamp, y calcular `firmaHash` como SHA-256 de la concatenación `cursoId + usuarioId + timestamp`. (7) Crear la inscripción con todos los campos de firma poblados. |
| **Regla** | Si el curso NO requiere firma, el endpoint debe comportarse exactamente como antes (inscripción normal sin campos de firma). |

### Paso 1.8 — Badge visual en participantes.html [X]

| Elemento | Detalle |
|----------|---------|
| **Archivo** | `src/main/resources/templates/cursos/participantes.html` |
| **Cambio** | En el encabezado o descripción del curso dentro de la página, si `curso.requiereFirmaCumplimiento == true`, mostrar un badge o etiqueta visual que diga "🔏 Firma de Cumplimiento requerida". |

---

## Tarea 2: Botón "Forzar Completado" con trazabilidad para ADMIN

**Objetivo arquitectónico:** Permitir que un usuario con rol ADMIN o SUPERADMIN marque una inscripción como "completada" de forma forzosa, registrando motivo, comentario, quién lo hizo y una etiqueta especial. Esto cubre el caso donde SCORM falla técnicamente y el estudiante no puede completar el curso por causas ajenas.

### Paso 2.1 — Extender modelo Inscripcion (campos de trazabilidad) [X]

| Elemento | Detalle |
|----------|---------|
| **Archivo** | `src/main/java/com/lms/model/Inscripcion.java` |
| **Campos a agregar** | (1) booleano `forzadoPorSupervisor` default `false`, (2) String `motivoForzado` (TEXT, nullable), (3) String `comentarioForzado` (TEXT, nullable), (4) UUID `forzadoPorUsuarioId` (nullable), (5) `LocalDateTime forzadoEn` (nullable), (6) String `etiquetaForzado` (máx 100 caracteres, nullable). |
| **Regla** | No tocar campos existentes de la Tarea 1 ni los originales. Sumar estos seis campos a los cuatro de la Tarea 1. |

### Paso 2.2 — Agregar método en InscripcionService [X]

| Elemento | Detalle |
|----------|---------|
| **Archivo** | Verificar si existe `InscripcionService.java`. Si no existe, crearlo en `src/main/java/com/lms/service/InscripcionService.java`. |
| **Nuevo método** | `forzarCompletado(UUID inscripcionId, UUID adminUserId, String motivo, String comentario)` |
| **Lógica de alto nivel** | (1) Buscar la inscripción por ID, lanzar error si no existe. (2) Validar que no esté ya completada (evitar duplicidad). (3) Establecer `completado = true`, `fechaCompletado = LocalDateTime.now()`, `forzadoPorSupervisor = true`, `motivoForzado = motivo`, `comentarioForzado = comentario`, `forzadoPorUsuarioId = adminUserId`, `forzadoEn = LocalDateTime.now()`, `etiquetaForzado = "APROBADO_POR_SUPERVISOR_FALLA_SCORM"`. (4) Si `nota` es null, asignar `4.0` como nota mínima de aprobación. (5) Persistir y retornar la inscripción actualizada. |
| **Regla** | El método debe ser `@Transactional`. No modificar otros métodos existentes del servicio. |

### Paso 2.3 — Endpoint REST para forzar completado [X]

| Elemento | Detalle |
|----------|---------|
| **Archivo** | `InscripcionController.java` (mismo archivo del Paso 1.7 o creado allí) |
| **Nuevo endpoint** | `POST /api/v1/inscripciones/{id}/forzar-completado` |
| **Seguridad** | `@PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")` |
| **Lógica de alto nivel** | (1) Recibir ID de inscripción como path variable. (2) Recibir body JSON con campos `motivo` (obligatorio) y `comentario` (opcional). (3) Validar que `motivo` no sea nulo ni vacío, retornar 400 si lo es. (4) Obtener ID del admin autenticado vía `SecurityUtils`. (5) Llamar a `inscripcionService.forzarCompletado(...)`. (6) Retornar la inscripción actualizada. |
| **Regla** | No modificar endpoints existentes del controlador. Solo agregar este nuevo. |

### Paso 2.4 — Modal y botón en participantes.html [X]

| Elemento | Detalle |
|----------|---------|
| **Archivo** | `src/main/resources/templates/cursos/participantes.html` |
| **Cambios** | (1) Agregar un modal oculto (HTML + CSS overlay) que contenga un formulario con: selector de motivo (dropdown con opciones predefinidas), campo de comentario (textarea opcional), y botones de confirmar/cancelar. (2) En cada fila de la tabla de participantes, si el usuario autenticado tiene rol ADMIN/SUPERADMIN y la inscripción NO está completada, agregar un botón "Forzar" que abra el modal. |

**Contrato visual del modal:**
- Overlay semitransparente centrado
- Título: "Forzar Completado"
- Texto de advertencia explicando que solo usar por falla técnica SCORM
- Dropdown de motivos: "Falla técnica SCORM", "Error de procesamiento LMS", "Migración de datos", "Otro"
- Textarea para comentario adicional (opcional)
- Botón "Cancelar" y "Confirmar Forzar Completado"

### Paso 2.5 — Badge de estado "Forzado" en tabla [X]

| Elemento | Detalle |
|----------|---------|
| **Archivo** | `src/main/resources/templates/cursos/participantes.html` |
| **Cambio** | En la columna de estado de cada fila, si `inscripcion.forzadoPorSupervisor == true`, mostrar un badge con estilo de advertencia (warning) que diga "Forzado" y con tooltip "Aprobado por Supervisor (Falla de SCORM)". |
| **Regla** | El badge debe ser adicional al badge existente de "Completado". Si está forzado y completado, mostrar el badge forzado. |

---

## Resumen de Archivos Afectados

| Archivo | Tarea | Tipo de Cambio |
|---------|-------|---------------|
| `src/main/java/com/lms/model/Curso.java` | 1.1 | +1 campo booleano |
| `src/main/java/com/lms/model/Inscripcion.java` | 1.2, 2.1 | +10 campos totales |
| DTO de curso (si existe) | 1.3 | +1 campo booleano |
| `CursoController.java` | 1.4 | +lectura del nuevo campo en create/update |
| `editor.html` | 1.5, 1.6 | +switch visual + JS asociado |
| `InscripcionController.java` | 1.7, 2.3 | +2 nuevos endpoints |
| `participantes.html` | 1.8, 2.4, 2.5 | +badge firma + modal forzar + botón forzar + badge forzado |
| `InscripcionService.java` | 2.2 | +1 nuevo método |

## Verificación Final (Checklist)

- [ ] `mvn clean compile -q` sin errores
- [ ] `mvn test` — todos los tests existentes pasan (especialmente `CursoServiceTest`, `ExportacionServiceTest`)
- [X] Al crear/editar un curso, el switch "Requiere Firma Digital" aparece y persiste
- [X] Al inscribirse en un curso con firma requerida, se solicita aceptación y se capturan IP/timestamp/hash
- [X] Como ADMIN, en participantes aparece botón "Forzar" en inscripciones no completadas
- [X] Al forzar, se registra motivo, comentario, quién, cuándo y etiqueta
- [X] El badge "Forzado" se muestra correctamente en la tabla
