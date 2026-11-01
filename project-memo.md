# Project Memo â€” LMS LegalTech

## ðŸ“‹ DescripciÃ³n General

Sistema de GestiÃ³n de Aprendizaje (LMS) con enfoque en cumplimiento legal, integridad de datos y versionado. Construido con Java 17 + Spring Boot 3.3.0, Thymeleaf, HTMX, PostgreSQL, Redis y MinIO.

---

## ðŸ—ï¸ Stack TecnolÃ³gico

| Capa | TecnologÃ­a |
|------|-----------|
| **Backend** | Java 17, Spring Boot 3.3.0, Spring Security, Spring Data JPA/Redis |
| **Frontend** | Thymeleaf, HTMX, JavaScript vanilla, CSS3 |
| **Base de Datos** | PostgreSQL 16 (vÃ­a Docker) |
| **Cache** | Redis |
| **Storage** | MinIO (S3-compatible) |
| **Migraciones** | Flyway (V1â€“V8) |
| **DevOps** | Docker, Docker Compose, DevTools live reload |
| **Testing** | JUnit 5, Spring Boot Test, Mockito |

---

## ðŸ§± Arquitectura General

### Capas
1. **Controladores MVC** (`HomeController`) â€” Sirven templates Thymeleaf
2. **Controladores REST** (`EvaluacionController`, `CursoController`) â€” APIs JSON con `@PreAuthorize`
3. **Servicios** (`EvaluacionService`, `CursoService`) â€” LÃ³gica de negocio
4. **Repositorios JPA** â€” Acceso a datos con soft-delete (`deletedAt IS NULL`)
5. **Modelos/Entidades** â€” JPA con `@Entity`, soft-delete, `@OneToMany`, `@ManyToOne`

### Seguridad
- Doble autenticaciÃ³n: JWT (cookie) + Spring Security Session
- `JwtAuthenticationFilter` extrae email del token JWT
- Roles: `ROLE_ESTUDIANTE`, `ROLE_ADMIN`, `ROLE_INSTRUCTOR`, `ROLE_SUPERADMIN`
- Thymeleaf `sec:authorize` para rendering condicional en templates

---

## ðŸ“ Estructura de Archivos Clave

```
src/main/java/com/lms/
â”œâ”€â”€ controller/
â”‚   â”œâ”€â”€ HomeController.java          â†’ Sirve pÃ¡ginas MVC (Thymeleaf)
â”‚   â”œâ”€â”€ EvaluacionController.java    â†’ API REST de evaluaciones
â”‚   â”œâ”€â”€ CursoController.java         â†’ API REST de cursos
â”‚   â””â”€â”€ CursoHtmxController.java     â†’ Endpoints HTMX
â”œâ”€â”€ service/
â”‚   â””â”€â”€ EvaluacionService.java       â†’ LÃ³gica de evaluaciones
â”œâ”€â”€ repository/
â”‚   â””â”€â”€ EvaluacionRepository.java    â†’ Queries con soft-delete
â”œâ”€â”€ model/
â”‚   â”œâ”€â”€ Evaluacion.java              â†’ Entidad con soft-delete, @OneToMany a Pregunta
â”‚   â””â”€â”€ Pregunta.java                â†’ Entidad con opciones JSON, respuesta_correcta
â”œâ”€â”€ config/
â”‚   â””â”€â”€ DataInitializer.java         â†’ Semilla de usuarios por defecto
â””â”€â”€ security/
    â””â”€â”€ JwtAuthenticationFilter.java â†’ Filtro JWT
src/main/resources/templates/
â””â”€â”€ evaluaciones/
    â”œâ”€â”€ listado.html                 â†’ Listado de evaluaciones (JS + Thymeleaf)
    â”œâ”€â”€ editor.html                  â†’ Editor/creaciÃ³n de evaluaciones
    â””â”€â”€ rendir.html                  â†’ Interfaz para rendir evaluaciÃ³n
```

---

## ðŸ”§ Cambios Recientes (Julio 2026)

### Fix: Evaluaciones no cargaban para estudiantes
**Problema:** Estudiante `estudiante.demo@example.com` veÃ­a "No hay evaluaciones para este curso" aunque existÃ­a "examen de prueba" (ID: `ef2ac871-...`).

**DiagnÃ³stico:**
1. TypeError en consola: `Cannot set properties of null (setting 'href')` en `listado.html:204`
2. El elemento `btn-crear-evaluacion` tiene `sec:authorize="hasAnyRole('ADMIN', 'INSTRUCTOR', 'SUPERADMIN')"` y no se renderiza para estudiantes
3. El error JS detenÃ­a toda ejecuciÃ³n, impidiendo que `cargarEvaluaciones()` se ejecutara

**Fix 1** â€” [`listado.html:202-207`](src/main/resources/templates/evaluaciones/listado.html:202)
```javascript
var btnCrear = document.getElementById('btn-crear-evaluacion');
if (btnCrear) btnCrear.href = crearUrl;
```

### Fix: Estudiante veÃ­a UI de Admin (Editar/Eliminar)
**Problema:** DespuÃ©s del Fix 1, el estudiante veÃ­a botÃ³n "Editar" y respuestas correctas visibles.

**DiagnÃ³stico:**
- `#authorization.expression('hasRole(''ROLE_ESTUDIANTE'')')` en Thymeleaf retornaba `false` para estudiantes
- JS detectaba `esEstudiante = false` â†’ renderizaba template de Admin

**Fix 2** â€” [`HomeController.java:173-181`](src/main/java/com/lms/controller/HomeController.java:173)
```java
boolean esEstudiante = authentication.getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().equals("ROLE_ESTUDIANTE"));
model.addAttribute("esEstudiante", esEstudiante);
```

**Fix 3** â€” [`listado.html:142-146`](src/main/resources/templates/evaluaciones/listado.html:142)
```html
<span id="user-role-data" th:data-es-estudiante="${esEstudiante}"></span>
```

### Fix: Respuestas correctas visibles para estudiantes
**Problema:** El flag `correcta` dentro del JSON `opciones` se mostraba en verde via `renderOpcionesVisuales()`.

**Fix 4** â€” [`EvaluacionController.java:60-82`](src/main/java/com/lms/controller/EvaluacionController.java:60)
```java
// Sanitizar flag 'correcta' del JSON de opciones para estudiantes
try {
    List<Map<String, Object>> opcionesList = mapper.readValue(
            p.getOpciones(), new TypeReference<>() {});
    for (Map<String, Object> opt : opcionesList) {
        if (opt.containsKey("correcta") && Boolean.TRUE.equals(opt.get("correcta"))) {
            opt.put("correcta", false);
        }
    }
    p.setOpciones(mapper.writeValueAsString(opcionesList));
} catch (JsonProcessingException e) { ... }
```

### Fix: Estudiante veÃ­a "Ver X preguntas" expandible
**Problema:** Bajo la tarjeta del examen, habÃ­a un elemento `<details>` con "Ver 4 preguntas" que los estudiantes podÃ­an expandir para previsualizar todas las preguntas antes de rendir la evaluaciÃ³n.

**DiagnÃ³stico:**
- El bloque de preview de preguntas se renderizaba para todos los usuarios sin verificar el rol
- La condiciÃ³n `!esEstudiante` existÃ­a en el JS pero el navegador servÃ­a HTML cachead

**Fix 5** â€” [`listado.html:380-419`](src/main/resources/templates/evaluaciones/listado.html:380)
```javascript
'<!-- Preguntas (solo para admin/instructor, estudiantes no ven preview) -->' +
(!esEstudiante
    ? (preguntasCount > 0
        ? '<details>...Ver ' + preguntasCount + ' preguntas...</details>'
        : '<p>Sin preguntas configuradas.</p>'
      )
    : ''   // â† ESTUDIANTES no ven NADA aquÃ­
) +
```

### Fix: CachÃ© del navegador impedÃ­a ver cambios en template
**Problema:** DespuÃ©s de aplicar Fix 5, el navegador seguÃ­a mostrando el HTML viejo (con "Ver 4 preguntas") porque cacheaba la pÃ¡gina.

**Fix 6** â€” [`listado.html:7-9`](src/main/resources/templates/evaluaciones/listado.html:7)
```html
<meta http-equiv="Cache-Control" content="no-cache, no-store, must-revalidate">
<meta http-equiv="Pragma" content="no-cache">
<meta http-equiv="Expires" content="0">
```

### Fix: SelecciÃ³n mÃºltiple no funcionaba en pÃ¡gina "Rendir EvaluaciÃ³n"
**Problema:** En la pÃ¡gina de rendir evaluaciÃ³n, las preguntas de tipo `opcion_multiple` no permitÃ­an seleccionar mÃºltiples opciones. El usuario reportÃ³: *"la pregunta de selecciÃ³n multiple, 'NO' permite hacer la selecciÃ³n multiple"*.

**DiagnÃ³stico â€” Dos problemas identificados:**

**Problema A (Backend):** El endpoint `GET /{evaluacionId}/rendir` en [`EvaluacionController.java:173`](src/main/java/com/lms/controller/EvaluacionController.java:173) enviaba `p.getOpciones()` (un String JSON crudo con flags `correcta`) directamente al frontend. El frontend recibÃ­a un string en lugar de un array JSON, y ademÃ¡s exponÃ­a las respuestas correctas al estudiante.

**Problema B (Frontend):** El CSS global en [`components.css:272-277`](src/main/resources/static/css/components.css:272) oculta TODOS los checkboxes nativos:
```css
input[type="checkbox"] {
    position: absolute;
    opacity: 0;
    width: 0;
    height: 0;
}
```
Esto hacÃ­a que los checkboxes en `renderOpcionMultiple()` fueran invisibles y no clickeables. Los intentos de anular con inline styles `!important` fallaron porque el CSS externo tiene precedencia impredecible sobre inline styles cuando hay reglas de especificidad involucradas.

**Fix 7 (Backend)** â€” [`EvaluacionController.java:209-227`](src/main/java/com/lms/controller/EvaluacionController.java:209)
```java
// Procesar opciones: parsear JSON string y sanitizar flag 'correcta' para estudiantes
String rawOpciones = p.getOpciones();
if (rawOpciones != null && !rawOpciones.isEmpty()) {
    try {
        List<Map<String, Object>> opcionesList = mapper.readValue(
                rawOpciones,
                new TypeReference<List<Map<String, Object>>>() {});
        // Sanitizar: ocultar flag 'correcta' al estudiante
        for (Map<String, Object> opt : opcionesList) {
            opt.remove("correcta");
        }
        pMap.put("opciones", opcionesList); // Enviar como array JSON, no como string
    } catch (JsonProcessingException e) {
        pMap.put("opciones", rawOpciones); // Fallback
    }
}
```

**Fix 8 (Frontend - v3 - reemplazado por Fix 10)** â€” [`rendir.html`](src/main/resources/templates/evaluaciones/rendir.html:1)
Se intentÃ³ enfoque JavaScript (`element.style.setProperty()`) pero luego se reemplazÃ³ por `<style>` block.

**Fix 9 (Frontend - v4 - reemplazado por Fix 10)** â€” [`rendir.html`](src/main/resources/templates/evaluaciones/rendir.html:1)
Se reemplazÃ³ completamente el bloque `<style>` con selectores de alta especificidad y se aÃ±adieron `!important` inline. **No funcionÃ³** porque `components.css` se carga en `<head>` y gana la batalla de especificidad incluso contra `!important` inline en ciertos contextos de renderizado del navegador.

**Fix 10 (Frontend - v5 definitiva - JavaScript `style.setProperty` con `'important'`)** â€” [`rendir.html:330-331`](src/main/resources/templates/evaluaciones/rendir.html:330)

**Problema:** El CSS global `input[type="checkbox"] { position:absolute; opacity:0; width:0; height:0; }` de [`components.css:272`](src/main/resources/static/css/components.css:272) oculta checkboxes/radios. Ni el `<style>` block con `!important` ni los inline `!important` en JS bastaron porque `components.css` se carga en `<head>` (vÃ­a [`head.html`](src/main/resources/templates/fragments/head.html:20)) y tiene prioridad sobre estilos en `<body>`.

**SoluciÃ³n â€” Abandonar CSS, forzar desde JavaScript post-renderizado:**

1. **Eliminado bloque `<style>`** del `<body>` â€” Ya no hay CSS de override, todo se maneja desde JS.

2. **Nueva funciÃ³n [`forzarVisibilidadInputs(contenedor)`](src/main/resources/templates/evaluaciones/rendir.html:515)** â€” Definida en el Ã¡mbito del IIFE, antes de `DOMContentLoaded`. Usa `querySelectorAll('input[type="checkbox"], input[type="radio"]')` y aplica `style.setProperty(prop, value, 'important')` para cada input dentro del contenedor.

3. **Llamada post-renderizado** â€” En [`renderizarPreguntas()`](src/main/resources/templates/evaluaciones/rendir.html:266), despuÃ©s de `container.appendChild(card)` se invoca `forzarVisibilidadInputs(card)`.

4. **Propiedades forzadas:** `position: relative`, `opacity: 1`, `width: 1rem`, `height: 1rem`, `pointer-events: auto`.

**Diferencia clave con Fix 9:** `style.setProperty(prop, value, 'important')` se ejecuta imperativamente despuÃ©s de que el DOM estÃ¡ listo, garantizando que el valor se aplique por encima de cualquier CSS (incluyendo `components.css`), mientras que el `<style>` block en `<body>` podÃ­a ser ignorado por el navegador segÃºn orden de carga y especificidad.

**Archivo modificado:** [`rendir.html`](src/main/resources/templates/evaluaciones/rendir.html)

---

## ðŸš€ Mejoras CrÃ­ticas v1.0 (Julio 2026)

### Tarea 1: Switch "Requiere Firma Digital de Cumplimiento" en Cursos

**Objetivo arquitectÃ³nico:** Agregar un toggle en el formulario de creaciÃ³n/ediciÃ³n de cursos que, cuando estÃ¡ activo, obliga al estudiante a aceptar una declaraciÃ³n jurada con captura de IP y timestamp al momento de inscribirse. Esto cumple con los requisitos de trazabilidad de la Ley 19.799.

#### ImplementaciÃ³n:

1. **Modelo Curso.java** â€” Agregado campo `requiereFirmaCumplimiento` (booleano, default false)
2. **Modelo Inscripcion.java** â€” Agregados 4 campos: `declaracionJuradaAceptada`, `ipFirma`, `timestampFirma`, `firmaHash`
3. **DTO CursoRequest.java** â€” Agregado campo `requiereFirmaCumplimiento`
4. **CursoService.java** â€” Actualizados mÃ©todos `crearCurso` y `actualizarCurso` para persistir el nuevo campo
5. **InscripcionRequest.java** â€” Agregado campo `declaracionJuradaAceptada`
6. **InscripcionService.java** â€” Nuevo mÃ©todo `inscribirUsuarioConFirma` con validaciÃ³n y captura de datos de firma
7. **InscripcionController.java** â€” Actualizado endpoint `/cursos/{cursoId}/inscribir` para recibir y validar declaraciÃ³n jurada
8. **editor.html** â€” Agregado switch visual con explicaciÃ³n y estilos CSS personalizados
9. **participantes.html** â€” Agregado badge "ðŸ” Firma de Cumplimiento requerida" en el tÃ­tulo del curso si corresponde

### Tarea 2: BotÃ³n "Forzar Completado" con trazabilidad para ADMIN

**Objetivo arquitectÃ³nico:** Permitir que un usuario con rol ADMIN o SUPERADMIN marque una inscripciÃ³n como "completada" de forma forzosa, registrando motivo, comentario, quiÃ©n lo hizo y una etiqueta especial. Esto cubre el caso donde SCORM falla tÃ©cnicamente y el estudiante no puede completar el curso por causas ajenas.

#### ImplementaciÃ³n:

1. **Modelo Inscripcion.java** â€” Agregados 6 campos: `forzadoPorSupervisor`, `motivoForzado`, `comentarioForzado`, `forzadoPorUsuarioId`, `forzadoEn`, `etiquetaForzado`
2. **InscripcionService.java** â€” Nuevo mÃ©todo `forzarCompletado` con lÃ³gica de actualizaciÃ³n y registro de auditorÃ­a
3. **InscripcionController.java** â€” Nuevo endpoint `POST /inscripciones/{id}/forzar-completado` con validaciÃ³n de roles y motivos
4. **participantes.html** â€” 
   - Agregado modal con formulario de forzado (motivo, comentario)
   - Agregado botÃ³n "Forzar" en tabla de participantes (solo visible para ADMIN/SUPERADMIN y solo en inscripciones no completadas)
   - Agregado badge "âš ï¸ Forzado" junto al estado "âœ… Completado" cuando corresponde
   - Agregada variable global `window.userRoles` para verificar roles del usuario actual

#### Detalles TÃ©cnicos de ImplementaciÃ³n

**[`InscripcionService.forzarCompletado()`](src/main/java/com/lms/service/InscripcionService.java:461)**

```java
@Transactional
public InscripcionResponse forzarCompletado(UUID inscripcionId, UUID adminUserId, 
                                           String motivo, String comentario) {
    Inscripcion inscripcion = inscripcionRepository.findById(inscripcionId)
            .orElseThrow(() -> new IllegalArgumentException("InscripciÃ³n no encontrada: " + inscripcionId));

    // Previene duplicidad de forzado
    if (inscripcion.isCompletado()) {
        throw new IllegalStateException("La inscripciÃ³n ya estÃ¡ completada");
    }

    inscripcion.setCompletado(true);
    inscripcion.setFechaCompletado(LocalDateTime.now());
    inscripcion.setForzadoPorSupervisor(true);
    inscripcion.setMotivoForzado(motivo);
    inscripcion.setComentarioForzado(comentario);
    inscripcion.setForzadoPorUsuarioId(adminUserId);
    inscripcion.setForzadoEn(LocalDateTime.now());
    inscripcion.setEtiquetaForzado("APROBADO_POR_SUPERVISOR_FALLA_SCORM");
    
    // Asignar nota mÃ­nima de aprobaciÃ³n (4.0) si es null
    if (inscripcion.getNota() == null) {
        inscripcion.setNota(4.0);
    }

    // Recalcular hash con evento discriminador COMPLETADO_FORZADO
    String hashSha256 = HashUtils.sha256(
        inscripcion.getId().toString() + "|" +
        inscripcion.getCurso().getId().toString() + "|" +
        inscripcion.getUsuario().getId().toString() + "|" +
        "COMPLETADO_FORZADO" + "|" +
        inscripcion.getNota().toString() + "|" +
        LocalDateTime.now()
    );
    inscripcion.setHashSha256(hashSha256);

    // Registro de auditorÃ­a tipo INSCRIPCION_COMPLETADA_FORZADA
    auditLogger.registrarEvento("INSCRIPCION_COMPLETADA_FORZADA", adminUserId, ...);
    return InscripcionResponse.fromEntity(inscripcion);
}
```

**LÃ³gica de seguridad del endpoint:**
- `@PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")` â€” acceso restringido a supervisores
- Body JSON: `{"motivo": "string (obligatorio)", "comentario": "string (opcional)"}`
- ValidaciÃ³n server-side: retorna `400 Bad Request` si `motivo` es null/vacÃ­o
- `adminUserId` obtenido vÃ­a `securityUtils.getCurrentUserId(authentication)` (no confÃ­a en el body)

**[`modal-forzar-completado` en participantes.html](src/main/resources/templates/cursos/participantes.html:96)**

| Elemento | DescripciÃ³n |
|----------|-------------|
| Overlay | `position:fixed; z-index:9999; background:rgba(0,0,0,0.5)` |
| Dropdown motivos | "Falla tÃ©cnica SCORM", "Error de procesamiento LMS", "MigraciÃ³n de datos", "Otro" |
| Advertencia visual | Banner naranja: "âš ï¸ Solo usar por falla tÃ©cnica SCORM" |
| Evento confirmaciÃ³n | `fetch POST /api/v1/inscripciones/{id}/forzar-completado` con JSON body |
| Auto-refresco | `cargarInscritos()` tras confirmaciÃ³n exitosa |

**Variable global [`window.userRoles`](src/main/resources/templates/cursos/participantes.html:212)**

```javascript
window.userRoles = [];
var rolesData = document.getElementById('user-roles-data');
if (rolesData && rolesData.dataset.roles) {
    window.userRoles = rolesData.dataset.roles
        .replace(/\[|\]|ROLE_/g, '')
        .split(', ')
        .map(r => r.trim())
        .filter(r => r.length > 0);
}
```

- Fuente: `th:data-roles="${#authentication.principal.authorities}"` (Thymeleaf + Spring Security)
- SanitizaciÃ³n: elimina `ROLE_`, corchetes `[]`, espacios vacÃ­os
- Uso: controla visibilidad del botÃ³n "Forzar" â€” `window.userRoles.includes('ROLE_ADMIN') || window.userRoles.includes('ROLE_SUPERADMIN')`

**Lecciones aprendidas (Tarea 2):**
- El botÃ³n "Forzar" no debe mostrarse basÃ¡ndose en `sec:authorize` de Thymeleaf directamente sobre cada fila, porque el renderizado por fila se hace vÃ­a JavaScript (dinÃ¡mico). Por eso se usa `window.userRoles` global, calculada una sola vez al cargar la pÃ¡gina.
- La etiqueta `"APROBADO_POR_SUPERVISOR_FALLA_SCORM"` es fija e intencionalmente especÃ­fica: sirve como filtro en reportes de auditorÃ­a XLSX (columna `etiquetaForzado`) y permite detectar patrones de abuso si un admin fuerza demasiadas completaciones.
- La nota por defecto `4.0` es el mÃ­nimo aprobatorio, no un valor arbitrario. Previene que el reporte de curso aparezca con `nota = null` tras el forzado.

### MigraciÃ³n Flyway V10

Creada migraciÃ³n `V10__mejoras_criticas.sql` con todos los nuevos campos:
- `cursos.requiere_firma_cumplimiento`
- `inscripciones.declaracion_jurada_aceptada`, `ip_firma`, `timestamp_firma`, `firma_hash`
- `inscripciones.forzado_por_supervisor`, `motivo_forzado`, `comentario_forzado`, `forzado_por_usuario_id`, `forzado_en`, `etiqueta_forzado`

---

### Docker: Reinicio post-Mejoras CrÃ­ticas (14-Jul-2026)
**Contexto:** Luego de aplicar todas las mejoras crÃ­ticas, se ejecutÃ³ `docker compose restart api` para recargar la aplicaciÃ³n con los nuevos cambios.

**Resultado:** Contenedor `lms-api` reiniciado exitosamente. Spring Boot iniciÃ³ con perfil `development`, Flyway aplicÃ³ la migraciÃ³n V10, y el health check pasÃ³ a estado healthy.

**Estado final:** `Up (healthy)` âœ…

### Docker: Reinicio post-Fix 10 (11-Jul-2026)
**Contexto:** Luego de aplicar Fix 10 (cambios en `rendir.html`), se ejecutÃ³ `docker compose restart api` para recompilar el proyecto Java y recargar los templates Thymeleaf.

**Resultado:** Contenedor `lms-api` recompilÃ³ 104 source files + 21 test files mediante el script `hot-reload.sh` ejecutando `mvn spring-boot:run`. Spring Boot se iniciÃ³ correctamente con Tomcat en puerto 8080 y health check superado.

**Estado final:** `Up (healthy)` âœ…

---
### T8: Feedback visual en formulario de exportaciones

**Problema:** El formulario en [`exportaciones.html`](src/main/resources/templates/dashboard/exportaciones.html:86) usaba `hx-post="/api/v1/exportaciones"` que enviaba la solicitud al endpoint REST `POST /api/v1/exportaciones`, el cual retorna `ResponseEntity<Exportacion>` (JSON). HTMX renderizaba el JSON crudo como `[object Object]` en el div `#export-response`, sin feedback visual para el usuario.

**SoluciÃ³n â€” Reemplazar HTMX con JavaScript fetch:**

1. **Eliminados atributos HTMX** del `<form>` â€” ya no usa `hx-post`, `hx-target`, `hx-swap`. Ahora usa `onsubmit="return solicitarExportacion(event)"` con `id="export-form"`.

2. **Nueva funciÃ³n [`window.solicitarExportacion(event)`](src/main/resources/templates/dashboard/exportaciones.html:221)** â€” Previene el envÃ­o normal del formulario, construye `FormData`, lo envÃ­a vÃ­a `fetch('POST', '/api/v1/exportaciones')`, y:
   - **Ã‰xito:** Muestra banner verde con badge "Pendiente" y auto-refresca la lista de exportaciones tras 500ms
   - **Error:** Muestra banner rojo con el mensaje de error. Intenta parsear JSON de error para extraer mensaje legible.

3. **Nueva funciÃ³n [`mostrarFeedback(tipo, mensaje)`](src/main/resources/templates/dashboard/exportaciones.html:165)** â€” Renderiza un banner animado (slide-in) verde para Ã©xito o rojo para error. El mensaje de Ã©xito se auto-oculta tras 6 segundos.

4. **Nueva funciÃ³n [`setLoading(loading)`](src/main/resources/templates/dashboard/exportaciones.html:200)** â€” Deshabilita el botÃ³n durante la solicitud, cambia el icono a spinner SVG y el texto a "Solicitando...".

5. **AnimaciÃ³n CSS [`@keyframes fadeSlideIn`](src/main/resources/templates/dashboard/exportaciones.html:7)** â€” Agregada inline en el `<head>` para la transiciÃ³n suave del feedback banner.

**Mejora posterior â€” Filtro dinÃ¡mico:** El campo `filtros` era un `<input type="text">` donde el usuario escribÃ­a JSON libre (ej: "activo" sin JSON vÃ¡lido), causando errores. Se reemplazÃ³ por un `<select>` dinÃ¡mico que muestra opciones predefinidas segÃºn el tipo de reporte seleccionado. Cada opciÃ³n envÃ­a un JSON vÃ¡lido como valor.

| Reporte | Opciones de filtro |
|---------|-------------------|
| Cursos | Todos, Solo activos, Solo inactivos |
| Usuarios | Todos, Solo activos, Estudiantes, Instructores, Administradores |
| Evaluaciones | Todas, Pendientes, Completadas |
| SCORM | Todos, Con progreso registrado |
| Firmas | Todas, Pendientes, Completadas |
| AuditorÃ­a | Todos, Inicios de sesiÃ³n, Exportaciones |

El `<select id="tipoReporte">` ahora tiene `onchange="actualizarFiltros()"` que actualiza las opciones del `<select id="filtros">`. Se agregÃ³ el mapa `FILTER_OPTIONS` en JS y la funciÃ³n `actualizarFiltros()` se llama en la carga inicial de la pÃ¡gina.

**Archivo modificado:** [`exportaciones.html`](src/main/resources/templates/dashboard/exportaciones.html)

---

### T9: Filtro por cÃ³digo en reportes de evaluaciones y SCORM

**Requerimiento:** Los reportes de evaluaciones y SCORM deben permitir filtrar por el cÃ³digo alfanumÃ©rico Ãºnico (`EVA001`, `SCO001`, etc.) que se asigna automÃ¡ticamente al crear cada evaluaciÃ³n o subir cada paquete SCORM.

**Contexto:** Ambos modelos ya tenÃ­an campo `codigo`:
- [`Evaluacion.java`](src/main/java/com/lms/model/Evaluacion.java:48): `@Column(length = 20) private String codigo;`
- [`PaqueteScorm.java`](src/main/java/com/lms/model/PaqueteScorm.java:46): `@Column(length = 20) private String codigo;`

El [`CodigoGeneratorService`](src/main/java/com/lms/service/CodigoGeneratorService.java) ya auto-generaba cÃ³digos con prefijos `"EVA"` y `"SCO"` usando `generar_codigo_alfanumerico()` de PostgreSQL con fallback en memoria. Sin embargo, el cÃ³digo no se mostraba en las UI de creaciÃ³n/ediciÃ³n.

**Cambios realizados (4 tareas atÃ³micas):**

**Tarea A â€” Mostrar `codigo` en [`editor.html`](src/main/resources/templates/evaluaciones/editor.html:127):**
- Agregado campo de solo lectura `<input type="text" id="codigo" readonly>` despuÃ©s del tÃ­tulo
- Placeholder: "Se generarÃ¡ automÃ¡ticamente al guardar"
- Mostrado con estilo monospace y opacidad reducida para indicar que es autogenerado
- Actualizada funciÃ³n `cargarEvaluacion()` para poblar el campo al editar

**Tarea B â€” Mostrar `codigo` en [`scorm-upload.html`](src/main/resources/templates/cursos/scorm-upload.html:126):**
- Resultado de subida ahora muestra `<strong>CÃ³digo:</strong>` con monospace + color de acento
- El listado de paquetes ya incluÃ­a `codigo` en la tabla

**Tarea C â€” Filtro por cÃ³digo en [`exportaciones.html`](src/main/resources/templates/dashboard/exportaciones.html:110):**
- Agregadas opciones `"Por cÃ³digo (EVA###)"` y `"Por cÃ³digo (SCO###)"` en `FILTER_OPTIONS` para evaluaciones y scorm respectivamente
- Nuevo `<div id="codigo-filtro-container">` con `<input type="text" id="codigo-filtro">` (oculto por defecto)
- Nueva funciÃ³n `window.onFiltroChange()`: cuando se selecciona `{"tipo":"por_codigo"}`, muestra el input de cÃ³digo; si se selecciona otra opciÃ³n, lo oculta y limpia
- Actualizada `solicitarExportacion()`: si el filtro es `por_codigo`, valida que se haya ingresado un cÃ³digo y reemplaza el JSON por `{"codigo":"EVA001"}`
- Actualizada `actualizarFiltros()` para invocar `onFiltroChange()` tras poblar opciones

**Tarea D â€” Backend [`ExportacionService.java`](src/main/java/com/lms/service/ExportacionService.java):**
- Agregado `import com.fasterxml.jackson.databind.ObjectMapper` y `import com.fasterxml.jackson.core.JsonProcessingException`
- Nueva constante `private static final ObjectMapper JSON_MAPPER = new ObjectMapper();`
- Nueva inyecciÃ³n: `EvaluacionRepository` y `PaqueteScormRepository` (ya existÃ­an)
- Cambio de firma: `generarReporteEvaluaciones(UUID, String codigo)` â€” si `codigo` no es null/blank, busca por `evaluacionRepository.findByCodigo(codigo.trim().toUpperCase())`, si no encuentra retorna lista vacÃ­a; si es null, usa `findAll()`
- Cambio de firma: `generarReporteScorm(UUID, String codigo)` â€” mismo patrÃ³n con `paqueteScormRepository.findByCodigoAndDeletedAtIsNull()`
- Nuevo mÃ©todo `extraerCodigoDeFiltro(String filtros)`: parsea el JSON de filtros y extrae el campo `"codigo"`
- Actualizado `procesarExportacion()`: lee `exportacion.getFiltros()`, extrae `codigo` y lo pasa a los generadores de evaluaciones y scorm

**Tarea E â€” Tests actualizados ([`ExportacionServiceTest.java`](src/test/java/com/lms/service/ExportacionServiceTest.java)):**
- 4 tests actualizados para pasar `null` como segundo argumento a los mÃ©todos con nueva firma
- Tests de ruteo (`procesarExportacionRuteoEvaluaciones`, `procesarExportacionRuteoScorm`) no requieren cambios porque el exportacion mock no tiene `filtros` (null â†’ codigo null â†’ comportamiento original)
- Suite completa: **31 tests, 0 fallos, 0 errores**

**Archivos modificados:**
- [`exportaciones.html`](src/main/resources/templates/dashboard/exportaciones.html) â€” Frontend: filtro por cÃ³digo
- [`editor.html`](src/main/resources/templates/evaluaciones/editor.html) â€” Frontend: campo cÃ³digo readonly
- [`scorm-upload.html`](src/main/resources/templates/cursos/scorm-upload.html) â€” Frontend: mostrar cÃ³digo en resultado
- [`ExportacionService.java`](src/main/java/com/lms/service/ExportacionService.java) â€” Backend: filtrado por cÃ³digo
- [`ExportacionServiceTest.java`](src/test/java/com/lms/service/ExportacionServiceTest.java) â€” Tests: nuevas firmas

---

## ðŸ³ Entorno Docker

```bash
# Stack completo
docker compose -f docker/docker-compose.java.yml up -d

# Servicios: postgres, minio, redis, lms-api
# Hot-reload: bind mount de src/ y pom.xml
```

**Contenedor `lms-api`:**
- Puerto: `8080`
- LiveReload: `35729`
- Health check: `GET /actuator/health`
- Perfil activo: `development`
- DevTools reinicia automÃ¡ticamente al detectar cambios en `.class`

---

## ðŸ“ Convenciones y Reglas

1. **Soft-delete obligatorio**: Toda entidad debe tener `deletedAt`, y todas las queries deben filtrar `WHERE deletedAt IS NULL`
2. **Hash SHA256**: Cada entidad tiene `hashSha256` para integridad de datos
3. **AuditorÃ­a**: `createdAt`, `updatedAt`, `deletedAt`, `deletedBy`, `reason`
4. **Roles**: Preferir pasar datos del usuario autenticado desde el controlador al template, no usar `#authorization.expression()` de Thymeleaf (es unreliable)
5. **REST API**: Prefijo `/api/v1/`, usar `@PreAuthorize`, retornar `ResponseEntity`
6. **Frontend JS**: Siempre agregar null checks para elementos que usan `sec:authorize` (pueden no existir en DOM)

---

## ðŸ§ª Datos de Prueba

| Email | Rol | Password |
|-------|-----|----------|
| `admin@lms.cl` | ADMIN | â€” |
| `instructor@lms.cl` | INSTRUCTOR | â€” |
| `estudiante@lms.cl` | ESTUDIANTE | â€” |
| `estudiante.demo@example.com` | ESTUDIANTE | â€” |

---

### T10: Reprocesar exportaciones atascadas en "pendiente" al iniciar el servidor

**Problema:** 9 exportaciones quedaron atascadas en estado "pendiente" porque los mensajes de Redis fueron publicados en el canal `"exportaciones"` antes de que el `RedisMessageListenerContainer` estuviera completamente inicializado (race condition al arrancar). Sin un consumidor Redis activo, los mensajes se perdieron y las exportaciones nunca se procesaron.

**SÃ­ntomas:**
- Exportaciones visibles en la UI con estado `pendiente` y sin progreso
- Sin errores en logs â€” simplemente nunca se procesaron
- El filtro JSON contenÃ­a valores como `"activo"` (formato antiguo, no JSON vÃ¡lido)

**SoluciÃ³n â€” Reprocesador automÃ¡tico al inicio:**

1. **Nuevo mÃ©todo [`reprocesarPendientes()`](src/main/java/com/lms/service/ExportacionService.java:117)** con `@EventListener(ApplicationReadyEvent.class)`:
   ```java
   @EventListener(ApplicationReadyEvent.class)
   public void reprocesarPendientes() {
       try {
           List<Exportacion> pendientes = exportacionRepository.findByEstadoOrderByCreatedAtDesc("pendiente");
           if (!pendientes.isEmpty()) {
               log.warn("âš  Se encontraron {} exportaciones en estado 'pendiente'. Reprocesando...", pendientes.size());
               for (Exportacion exp : pendientes) {
                   redisTemplate.convertAndSend(REDIS_CHANNEL_EXPORTACIONES, exp.getId().toString());
                   log.info("â³ Re-publicada exportaciÃ³n pendiente: {} (tipo: {})", exp.getId(), exp.getTipoReporte());
               }
           }
       } catch (Exception e) {
           log.error("Error al reprocesar exportaciones pendientes al inicio", e);
       }
   }
   ```

2. **`ApplicationReadyEvent` garantiza** que el contexto Spring estÃ© completamente inicializado (incluyendo `RedisMessageListenerContainer`) antes de ejecutar el reprocesador.

3. **Compatibilidad con filtros antiguos:** Las exportaciones con `filtros` en formato antiguo (ej: `"activo"` sin JSON) son manejadas por [`extraerCodigoDeFiltro()`](src/main/java/com/lms/service/ExportacionService.java:752) que retorna `null` al no poder parsear el JSON, cayendo en el comportamiento original (`findAll()`).

**Resultado:** 9/9 exportaciones pendientes reprocesadas y completadas exitosamente en el siguiente reinicio:
| ID | Tipo | Resultado |
|----|------|-----------|
| `c3e5c8ee` | firmas | âœ… Completado |
| `cc15c4d6` | usuarios | âœ… Completado |
| `bfcdc615` | usuarios | âœ… Completado |
| `94777908` | usuarios | âœ… Completado |
| `61f3310c` | cursos | âœ… Completado |
| `a82cb757` | evaluaciones | âœ… Completado |
| `7c4dbb4a` | scorm | âœ… Completado |
| `1065f050` | evaluaciones | âœ… Completado |
| `d7fa746f` | usuarios | âœ… Completado |

**Archivo modificado:** [`ExportacionService.java`](src/main/java/com/lms/service/ExportacionService.java)

### T11: Race condition en `solicitarExportacion()` â€” `save()` vs `convertAndSend()` (fix INCOMPLETO, reemplazado por T14)

**Problema:** Al crear una nueva exportaciÃ³n, el mÃ©todo [`solicitarExportacion()`](src/main/java/com/lms/service/ExportacionService.java:136) llamaba a `exportacionRepository.save(exportacion)` y acto seguido publicaba el mensaje en Redis con `redisTemplate.convertAndSend()`. Sin embargo, `save()` no garantiza que el INSERT estÃ© commiteado en PostgreSQL antes de que el mensaje llegue al `ExportacionWorker`. El worker recibÃ­a el mensaje, intentaba `findById()` y no encontraba el registro porque la transacciÃ³n aÃºn no habÃ­a hecho commit.

**SÃ­ntomas:**
- Exportaciones reciÃ©n creadas quedaban en estado "pendiente" indefinidamente
- En logs se veÃ­a `ExportacionWorker procesando` y `completado` inmediatamente (2ms), pero sin el log intermedio `Procesando exportaciÃ³n tipo 'X'` â€” indicando que `findById()` retornÃ³ `Optional.empty()`
- OcurrÃ­a esporÃ¡dicamente, dependiendo del timing entre el hilo del controller y el hilo del worker

**Intento de soluciÃ³n â€” `saveAndFlush()`:**
```java
exportacion = exportacionRepository.saveAndFlush(exportacion);
redisTemplate.convertAndSend(REDIS_CHANNEL_EXPORTACIONES, exportacion.getId().toString());
```

**Por quÃ© NO funcionÃ³:** La clase [`ExportacionService`](src/main/java/com/lms/service/ExportacionService.java:63) tiene `@Transactional` a nivel de clase. `saveAndFlush()` ejecuta el SQL `INSERT` pero **no hace COMMIT** â€” el commit ocurre cuando el proxy transaccional retorna del mÃ©todo `solicitarExportacion()`. El `ExportacionWorker` inicia una transacciÃ³n SEPARADA (`@Async @Transactional`) que no puede ver datos no commiteados de otra transacciÃ³n. La race condition persistiÃ³ porque el mensaje Redis se publicaba antes del commit real.

**Archivos modificados (revertidos en T14):**
- [`ExportacionService.java`](src/main/java/com/lms/service/ExportacionService.java:144) â€” `save()` â†’ `saveAndFlush()`
- [`ExportacionServiceTest.java`](src/test/java/com/lms/service/ExportacionServiceTest.java:100) â€” Mock actualizado de `save()` a `saveAndFlush()`

---

### T12: Auto-descarga al completar exportaciÃ³n

**Problema:** Al solicitar una exportaciÃ³n, el frontend creaba la tarea con estado "pendiente" y el polling `iniciarPollingDescarga()` consultaba el estado cada 2s. Sin embargo, cuando detectaba estado `completado`, solo mostraba un enlace de descarga en un mensaje de feedback, **sin iniciar la descarga automÃ¡ticamente**. El usuario debÃ­a hacer clic manualmente en el enlace.

**DiagnÃ³stico:**
- La funciÃ³n [`iniciarPollingDescarga()`](src/main/resources/templates/dashboard/exportaciones.html:428) ya existÃ­a y hacÃ­a polling correctamente
- En el bloque `if (data.estado === 'completado' && data.urlResultado)` solo se actualizaba el feedback con un `<a>` link
- El flujo Redis pub/sub + ExportacionWorker ya procesaba correctamente (fixes T10 y T11)

**SoluciÃ³n v1** â€” `window.location.href` (reemplazado por v2):
Inicialmente se usÃ³ `window.location.href = data.urlResultado`, pero los navegadores bloquean esta navegaciÃ³n automÃ¡tica por ser considerada _popup_ no solicitado.

**SoluciÃ³n v2 (definitiva)** â€” [`exportaciones.html:439-450`](src/main/resources/templates/dashboard/exportaciones.html:439):
```javascript
if (data.estado === 'completado' && data.urlResultado) {
    clearInterval(pollInterval);
    // Disparar descarga automÃ¡tica creando un <a> temporal
    var link = document.createElement('a');
    link.href = data.urlResultado;
    link.style.display = 'none';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    mostrarFeedback('success',
        'ExportaciÃ³n completada â€” la descarga deberÃ­a iniciar automÃ¡ticamente.');
    setTimeout(loadExports, 500);
}
```

**Cambio:** Se crea un `<a>` temporal en el DOM, se asigna `href` con la URL de descarga (`GET /api/v1/exportaciones/{id}/descargar?object=...` que retorna `Content-Disposition: attachment`), se ejecuta `click()` programÃ¡tico y se elimina del DOM. Esto imita exactamente un clic real del usuario, evitando el bloqueo del navegador contra `window.location.href`.

**Archivo modificado:**
- [`exportaciones.html`](src/main/resources/templates/dashboard/exportaciones.html) â€” `iniciarPollingDescarga()` ahora descarga automÃ¡ticamente

---

### T13: Reinicio del servidor (11-Jul-2026 23:25 CLT)
**AcciÃ³n:** Se ejecutÃ³ `docker compose -f docker/docker-compose.java.yml restart api` para reiniciar el contenedor `lms-api`.

**Resultado:**
- Contenedor `lms-api` reiniciado exitosamente
- Maven recompilÃ³ sin cambios (104 source files up-to-date)
- Spring Boot iniciÃ³ con perfil `development`
- Flyway verificÃ³ schema versiÃ³n 8 â€” sin migraciones pendientes
- Tomcat iniciado en puerto 8080 con LiveReload en 35729
- `ExportacionService.reprocesarPendientes()` se ejecutÃ³ (bÃºsqueda de exportaciones pendientes)
- Health check: **healthy** âœ… â€” Up ~1 minuto

**Estado final:** `Up (healthy)` âœ…

---

### T14: Fix definitivo â€” diferir publicaciÃ³n Redis via `TransactionSynchronizationManager.afterCommit()` (12-Jul-2026)

**Problema:** El fix T11 (`saveAndFlush()`) asumiÃ³ incorrectamente que flush = commit. En realidad, `@Transactional` a nivel de clase ([`ExportacionService.java:63`](src/main/java/com/lms/service/ExportacionService.java:63)) hace que el commit ocurra cuando el proxy Spring retorna del mÃ©todo `solicitarExportacion()`. El mensaje Redis se publicaba antes del commit, y el `ExportacionWorker` (transacciÃ³n separada) no podÃ­a ver el INSERT no commiteado.

**Evidencia en logs (PID 170, antes del fix):**
```
ExportacionWorker RECIBIÃ“ mensaje de Redis: '68911264-...'
ExportacionWorker procesando: 68911264-...
ExportacionService.procesarExportacion() buscando ID: 68911264-...
âš  NO encontrÃ³ registro con ID: 68911264-... (race condition: mensaje Redis llegÃ³ antes del commit)
ExportacionWorker completado: 68911264-...
```
Mismo patrÃ³n para `6357bafd-...` y todas las exportaciones creadas vÃ­a UI en tiempo real.

**DiagnÃ³stico final:**
1. `@Transactional` a nivel de clase (lÃ­nea 63) â€” todos los mÃ©todos pÃºblicos ejecutan dentro de una transacciÃ³n
2. `saveAndFlush()` flushes SQL a la conexiÃ³n pero **no hace COMMIT** â€” el commit ocurre cuando el proxy retorna
3. `redisTemplate.convertAndSend()` se ejecuta DENTRO de la transacciÃ³n, ANTES del commit
4. `ExportacionWorker` tiene `@Async @Transactional` â€” inicia su PROPIA transacciÃ³n (PROPAGATION_REQUIRED default)
5. `findById()` en la transacciÃ³n del worker no ve datos no commiteados de otra transacciÃ³n â†’ `Optional.empty()`
6. Worker logea warning y termina sin procesar â†’ exportaciÃ³n queda "pendiente" para siempre

**Por quÃ© solo "cursos" funcionaba:** El `reprocesarPendientes()` (ApplicationReadyEvent) ejecuta FUERA del proxy transaccional (no es llamado a travÃ©s del proxy, es un callback de evento). Los mensajes que publica encuentran el registro porque `solicitarExportacion()` ya habÃ­a commiteado para entonces. "cursos" probablemente se completÃ³ en un reinicio anterior via `reprocesarPendientes()`, no por el flujo en tiempo real.

**SoluciÃ³n â€” `TransactionSynchronizationManager.registerSynchronization()`:**

```java
// ANTES (race condition):
exportacion = exportacionRepository.saveAndFlush(exportacion);
redisTemplate.convertAndSend(REDIS_CHANNEL_EXPORTACIONES, exportacion.getId().toString());

// DESPUÃ‰S (publicaciÃ³n diferida post-commit):
exportacion = exportacionRepository.save(exportacion); // save() es suficiente

final UUID exportacionId = exportacion.getId();

if (TransactionSynchronizationManager.isSynchronizationActive()) {
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
            redisTemplate.convertAndSend(REDIS_CHANNEL_EXPORTACIONES, exportacionId.toString());
            log.debug("Mensaje publicado en Redis (post-commit): {}", exportacionId);
        }
        @Override
        public void afterCompletion(int status) {
            if (status == STATUS_ROLLED_BACK) {
                log.warn("Rollback para exportaciÃ³n: {} - mensaje NO publicado", exportacionId);
            }
        }
    });
} else {
    // Sin transacciÃ³n activa (ej: tests) â€” publicar inmediatamente
    redisTemplate.convertAndSend(REDIS_CHANNEL_EXPORTACIONES, exportacionId.toString());
}
```

**Mecanismo:**
1. `TransactionSynchronizationManager.registerSynchronization()` registra un callback en la transacciÃ³n activa actual
2. `afterCommit()` se invoca automÃ¡ticamente cuando la transacciÃ³n hace commit exitosamente
3. Para entonces, el INSERT es visible para TODAS las conexiones (aislamiento READ_COMMITTED)
4. El `ExportacionWorker` encuentra el registro inmediatamente
5. `afterCompletion()` con `STATUS_ROLLED_BACK` logea advertencia si la transacciÃ³n se revierte

**VerificaciÃ³n â€” Logs de PID 168 (post-fix) tras reinicio del contenedor:**

Todas las exportaciones pendientes fueron reprocesadas exitosamente via `reprocesarPendientes()` al iniciar:

| ID (suffix) | Tipo | Resultado |
|-------------|------|-----------|
| `68911264` | firmas | âœ… Procesado |
| `6357bafd` | auditoria | âœ… Procesado |
| `54c65d2b` | scorm | âœ… Procesado |
| `a791cec7` | usuarios | âœ… Procesado |
| `21e079af` | evaluaciones | âœ… Procesado |
| `22b704c9` | evaluaciones | âœ… Procesado |

Cada una generÃ³ su reporte, subiÃ³ a MinIO y cambiÃ³ estado a "completado".

**Archivos modificados:**
- [`ExportacionService.java`](src/main/java/com/lms/service/ExportacionService.java:138) â€” `saveAndFlush()` â†’ `save()`; aÃ±adido `TransactionSynchronizationManager.registerSynchronization()` con `afterCommit()` y `afterCompletion()`
- [`ExportacionService.java`](src/main/java/com/lms/service/ExportacionService.java:54-55) â€” AÃ±adidos imports: `TransactionSynchronization`, `TransactionSynchronizationManager`
- [`ExportacionServiceTest.java`](src/test/java/com/lms/service/ExportacionServiceTest.java:100) â€” Mock revertido de `saveAndFlush()` a `save()`

**Tests:** 31/31 pasando âœ…

**Defense-in-depth recomendado:** Agregar reintento en [`ExportacionWorker.procesarExportacion()`](src/main/java/com/lms/service/ExportacionWorker.java:32): si `findById()` retorna vacÃ­o, esperar 100ms y reintentar 3 veces antes de abandonar.

---

## ðŸš€ Deploy: Render + Docker Local (12-Jul-2026)

### Archivos Creados

| Archivo | PropÃ³sito |
|---------|-----------|
| [`Dockerfile`](Dockerfile) | Multi-stage build production, optimizado para Render Free Tier (512MB RAM) |
| [`docker-compose.yml`](docker-compose.yml) | Stack completo para deploy local (PostgreSQL + MinIO + Redis + API) |
| [`LMS_BASE.sql`](LMS_BASE.sql) | Schema consolidado (V1â€“V8) para crear BD externa (Render, Railway, etc.) |

### [`Dockerfile`](Dockerfile) â€” Detalles TÃ©cnicos

**Build Stage:**
- `maven:3.9.6-eclipse-temurin-17` â†’ compila con `mvn clean package -DskipTests`
- Cache de dependencias con `dependency:go-offline` antes de copiar src

**Runtime Stage:**
- `eclipse-temurin:17-jre-alpine` â€” imagen JRE minimalista
- Usuario no-root `lms` (seguridad)
- JVM: `-Xms128m -Xmx256m -XX:+UseG1GC -XX:MaxMetaspaceSize=128m -XX:+ExitOnOutOfMemoryError`
  - Deja ~256MB libres para el SO Alpine dentro del lÃ­mite de 512MB RAM de Render
  - `ExitOnOutOfMemoryError` asegura reinicio automÃ¡tico si la JVM se queda sin memoria
- Healthcheck con `wget` al `/actuator/health`
- `SPRING_PROFILES_ACTIVE=production` (thymeleaf cache activo, ddl-auto: validate)

**Variables de entorno (sobrescribir en Render Dashboard):**
| Variable | Default | DescripciÃ³n |
|----------|---------|-------------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/lms_db` | URL de PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | `lms_user` | Usuario BD |
| `SPRING_DATASOURCE_PASSWORD` | `cambiar_esto` | Password BD |
| `SPRING_DATA_REDIS_HOST` | `localhost` | Host Redis |
| `APP_MINIO_ENDPOINT` | `http://localhost:9000` | Endpoint MinIO/S3 |
| `JWT_SECRET` | `dev-secret-key-...` | **Â¡Cambiar obligatoriamente!** |

### [`docker-compose.yml`](docker-compose.yml) â€” Stack Local

Mismo stack que `docker/docker-compose.java.yml` pero usando el Dockerfile de producciÃ³n (sin DevTools, sin bind mounts).

```yaml
services:
  postgres:  # PostgreSQL 16-alpine, puerto 5432
  minio:     # MinIO S3, puertos 9000 (API) + 9001 (console)
  redis:     # Redis 7-alpine, puerto 6379
  api:       # Spring Boot, build: ./Dockerfile, puerto 8080
```

**Uso:**
```bash
docker compose up -d          # Iniciar todo el stack
docker compose logs -f api    # Ver logs de la API
docker compose down -v        # Detener y limpiar volÃºmenes
```

### [`LMS_BASE.sql`](LMS_BASE.sql) â€” Schema para BD Externa

- ConsolidaciÃ³n de todas las migraciones Flyway (V1â€“V8) en un solo archivo
- `CREATE TABLE IF NOT EXISTS` para todas las tablas (21 tablas)
- `CREATE INDEX IF NOT EXISTS` para todos los Ã­ndices
- Incluye funciones: `prevent_event_mutation()`, `generar_codigo_alfanumerico()`
- Incluye seed data: roles (ESTUDIANTE, INSTRUCTOR, ADMIN, SUPERADMIN), permisos, roles_permisos, secuencias_codigos
- Listo para importar en Render, Railway, Aiven, o cualquier PostgreSQL externo

### Render Deployment Steps

1. **Crear BD PostgreSQL** en Render Dashboard â†’ copiar "Internal Connection String"
2. **Crear Servicio Web** en Render:
   - Repositorio: `https://github.com/gonesteband-ops/LMS_APP.git`
   - Build Command: (usar Dockerfile automÃ¡tico)
   - Start Command: (no necesario con Docker)
   - Plan: **Free** (512MB RAM)
3. **Variables de entorno en Render:**
   ```
   SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:<port>/<db>?sslmode=require
   SPRING_DATASOURCE_USERNAME=<user>
   SPRING_DATASOURCE_PASSWORD=<password>
   JWT_SECRET=<generar-secreto-fuerte-de-256-bits>
   SPRING_PROFILES_ACTIVE=production
   ```
4. **Nota:** Render Free no incluye Redis ni MinIO. Las exportaciones quedarÃ¡n en estado "pendiente" a menos que se configure un Redis externo (Redis Cloud, Upstash) y un S3 compatible (Cloudflare R2, Backblaze B2, o el mismo MinIO en otro servidor).

---

---
### T15: 5 mejoras â€” Inscripciones, Documentos, Evaluaciones y SCORM (12-Jul-2026)

**Contexto:** El usuario solicitÃ³ 5 mejoras puntuales en las secciones de Inscripciones (carga CSV y badge de estado), Documentos (habilitar/deshabilitar), Evaluaciones (mostrar cÃ³digo EVA al guardar) y SCORM (habilitar/deshabilitar).

---

#### Fix 1 â€” CSV Bulk Enrollment: parÃ¡metro `"file"` â†’ `"archivo"`

**Problema:** La carga masiva de participantes via CSV fallaba silenciosamente. Al subir un archivo CSV con columna `email`, los participantes no se inscribÃ­an.

**DiagnÃ³stico:**
- El frontend en [`participantes.html`](src/main/resources/templates/cursos/participantes.html) enviaba el archivo con `formData.append('archivo', file)`
- El backend en [`InscripcionController.java:60`](src/main/java/com/lms/controller/InscripcionController.java:60) esperaba `@RequestParam("file")` â€” el nombre del parÃ¡metro no coincidÃ­a
- Spring MVC no puede hacer binding cuando los nombres no coinciden â†’ `MultipartFile file` era `null` â†’ el servicio recibÃ­a `null` y no procesaba nada

**Fix 1a â€” [`InscripcionController.java:60`](src/main/java/com/lms/controller/InscripcionController.java:60):**
```java
// ANTES:
@RequestParam("file") MultipartFile file
// DESPUÃ‰S:
@RequestParam("archivo") MultipartFile archivo
```

**Fix 1b â€” [`InscripcionService.java:213`](src/main/java/com/lms/service/InscripcionService.java:213):**
```java
// ANTES (no filtra soft-deleted):
Usuario usuario = usuarioRepository.findByEmail(linea).orElseThrow(...)
// DESPUÃ‰S (excluye usuarios eliminados):
Usuario usuario = usuarioRepository.findByEmailAndDeletedAtIsNull(linea).orElseThrow(...)
```

El repositorio [`UsuarioRepository.java:16`](src/main/java/com/lms/repository/UsuarioRepository.java:16) ya tenÃ­a `findByEmailAndDeletedAtIsNull()` pero no se estaba utilizando en el mÃ©todo `inscribirMasivo()`.

**Archivos modificados:**
- [`InscripcionController.java`](src/main/java/com/lms/controller/InscripcionController.java) â€” `@RequestParam` name fix
- [`InscripcionService.java`](src/main/java/com/lms/service/InscripcionService.java) â€” `findByEmailAndDeletedAtIsNull()` fix

---

#### Fix 2 â€” Badge "Pendiente" â†’ "Inscrito"

**Problema:** Al inscribir un nuevo participante de forma unitaria, el estado visual mostraba "â³ Pendiente", lo que daba la impresiÃ³n de que la inscripciÃ³n no se habÃ­a completado o estaba en un estado intermedio.

**DiagnÃ³stico:**
- Las inscripciones nuevas se crean con `completado = false` (diseÃ±o correcto)
- El frontend en [`participantes.html:211`](src/main/resources/templates/cursos/participantes.html:211) renderizaba `completado` como badge: `true` â†’ "âœ… Completado", `false` â†’ "â³ Pendiente"
- El tÃ©rmino "Pendiente" generaba confusiÃ³n porque sugerÃ­a que algo faltaba, cuando en realidad el participante ya estÃ¡ inscrito y puede acceder al curso

**Fix â€” [`participantes.html:211`](src/main/resources/templates/cursos/participantes.html:211):**
```javascript
// ANTES:
'<span class="badge badge-warning">â³ Pendiente</span>'
// DESPUÃ‰S:
'<span class="badge badge-info">ðŸ“ Inscrito</span>'
```

Se cambiÃ³ el badge de color `warning` (amarillo) a `info` (azul) para reforzar visualmente que no es un estado de advertencia, sino informativo.

**Archivo modificado:**
- [`participantes.html`](src/main/resources/templates/cursos/participantes.html) â€” Badge text + color

---

#### Feature 3 â€” Documentos: habilitar/deshabilitar

**Requerimiento:** Los documentos cargados no pueden eliminarse (por auditorÃ­a), pero deben poder deshabilitarse para que los estudiantes no los vean en el curso.

**ImplementaciÃ³n â€” 4 cambios:**

**3a. Modelo [`Documento.java:36`](src/main/java/com/lms/model/Documento.java:36):**
```java
@Column(nullable = false)
private boolean habilitado = true;
```
Nuevo campo booleano con default `true`. Los documentos existentes se migran automÃ¡ticamente como habilitados.

**3b. Servicio [`DocumentoService.java:164`](src/main/java/com/lms/service/DocumentoService.java:164):**
```java
public Documento toggleHabilitado(UUID id) {
    Documento doc = documentoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado: " + id));
    doc.setHabilitado(!doc.isHabilitado());
    return documentoRepository.save(doc);
}
```

**3c. Controlador [`DocumentoController.java:153`](src/main/java/com/lms/controller/DocumentoController.java:153):**
```java
@PatchMapping("/{id}/toggle-habilitado")
@PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR', 'SUPERADMIN')")
public ResponseEntity<Map<String, Object>> toggleHabilitado(@PathVariable UUID id) {
    com.lms.model.Documento doc = documentoService.toggleHabilitado(id);
    return ResponseEntity.ok(Map.of("id", doc.getId(), "habilitado", doc.isHabilitado()));
}
```

**3d. Frontend [`documentos.html`](src/main/resources/templates/cursos/documentos.html):**
- `<span id="user-role-admin" style="display:none" sec:authorize="...">` â€” indicador oculto de rol admin para JS
- Columna "Estado" en el table header (entre Nombre y TamaÃ±o)
- Badge "âœ… Habilitado" (verde) o "ðŸš« Deshabilitado" (rojo/gris) por cada documento
- BotÃ³n toggle "Deshabilitar" o "Habilitar" visible solo para ADMIN/INSTRUCTOR/SUPERADMIN
- Event delegation con `querySelectorAll('.btn-toggle-doc')` y PATCH a `/api/v1/documentos/{id}/toggle-habilitado`
- Estudiantes solo ven documentos con `habilitado = true`

**Archivos modificados:**
- [`Documento.java`](src/main/java/com/lms/model/Documento.java) â€” Nuevo campo `habilitado`
- [`DocumentoService.java`](src/main/java/com/lms/service/DocumentoService.java) â€” Nuevo mÃ©todo `toggleHabilitado()`
- [`DocumentoController.java`](src/main/java/com/lms/controller/DocumentoController.java) â€” Nuevo endpoint `PATCH /toggle-habilitado`
- [`documentos.html`](src/main/resources/templates/cursos/documentos.html) â€” Estado column + toggle button

---

#### Feature 4 â€” Mostrar cÃ³digo EVA al guardar evaluaciÃ³n

**Requerimiento:** Al crear una nueva evaluaciÃ³n, el sistema genera automÃ¡ticamente un cÃ³digo alfanumÃ©rico (`EVA001`, `EVA005`, etc.). Este cÃ³digo debe visualizarse inmediatamente al hacer clic en "Guardar EvaluaciÃ³n".

**Contexto:** El [`CodigoGeneratorService`](src/main/java/com/lms/service/CodigoGeneratorService.java) ya generaba el cÃ³digo automÃ¡ticamente al persistir la evaluaciÃ³n (vÃ­a `generar_codigo_alfanumerico()` de PostgreSQL). El campo [`Evaluacion.java:48`](src/main/java/com/lms/model/Evaluacion.java:48) `private String codigo;` ya existÃ­a y se mostraba como readonly en el formulario. Sin embargo, al guardar una nueva evaluaciÃ³n, el cÃ³digo generado no se mostraba en el mensaje de feedback.

**Fix â€” [`editor.html`](src/main/resources/templates/evaluaciones/editor.html):**
El handler de `guardarEvaluacion()` captura la respuesta del endpoint `POST /api/v1/cursos/{cursoId}/evaluaciones` y extrae el campo `codigo`:

```javascript
.then(function (data) {
    var codigoGenerado = data && data.codigo ? data.codigo : '';
    var codigoEl = document.getElementById('codigo');
    if (codigoEl && codigoGenerado) {
        codigoEl.value = codigoGenerado;
        codigoEl.placeholder = codigoGenerado;
    }
    var msg = '\u2705 Evaluaci\u00f3n guardada correctamente';
    if (codigoGenerado) {
        msg += ' \u2014 C\u00f3digo: <strong>' + codigoGenerado + '</strong>';
    }
    mostrarFeedback(msg, 'success');
    // ...
})
```

**Comportamiento resultante:**
1. Usuario llena tÃ­tulo, descripciÃ³n, preguntas, etc.
2. Hace clic en "Guardar EvaluaciÃ³n"
3. El endpoint retorna `{ id, codigo: "EVA005", ... }`
4. El campo `#codigo` se actualiza con el cÃ³digo generado
5. El feedback muestra: âœ… EvaluaciÃ³n guardada correctamente â€” CÃ³digo: **EVA005**
6. Tras 2.5s redirige al listado de evaluaciones

**Archivo modificado:**
- [`editor.html`](src/main/resources/templates/evaluaciones/editor.html) â€” Save handler now shows EVA code in feedback

---

#### Feature 5 â€” SCORM: habilitar/deshabilitar

**Requerimiento:** Del mismo modo que Documentos, los paquetes SCORM cargados deben poder habilitarse/deshabilitarse para controlar su visibilidad a los estudiantes. Sigue el mismo patrÃ³n que Feature 3.

**ImplementaciÃ³n â€” 4 cambios:**

**5a. Modelo [`PaqueteScorm.java:50`](src/main/java/com/lms/model/PaqueteScorm.java:50):**
```java
@Column(nullable = false)
private boolean habilitado = true;
```

**5b. Servicio [`ScormService.java:305`](src/main/java/com/lms/service/ScormService.java:305):**
```java
public PaqueteScorm toggleHabilitado(UUID paqueteId) {
    PaqueteScorm paquete = paqueteScormRepository.findByIdAndDeletedAtIsNull(paqueteId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Paquete SCORM no encontrado: " + paqueteId));
    paquete.setHabilitado(!paquete.isHabilitado());
    return paqueteScormRepository.save(paquete);
}
```

**5c. Controlador [`ScormController.java`](src/main/java/com/lms/controller/ScormController.java):**
- Agregado `"habilitado", p.isHabilitado()` al Map de respuesta en `listarPaquetesScorm()` (lÃ­nea 97)
- Nuevo endpoint:
```java
@PatchMapping("/scorm/{paqueteId}/toggle-habilitado")
@PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR', 'SUPERADMIN')")
public ResponseEntity<Map<String, Object>> toggleHabilitado(@PathVariable UUID paqueteId) {
    PaqueteScorm paquete = scormService.toggleHabilitado(paqueteId);
    return ResponseEntity.ok(Map.of(
        "id", paquete.getId().toString(),
        "habilitado", paquete.isHabilitado()
    ));
}
```

**5d. Frontend [`scorm-upload.html`](src/main/resources/templates/cursos/scorm-upload.html):**
- `<span id="user-role-admin-scorm" style="display:none" sec:authorize="...">` â€” indicador oculto de rol admin
- Columna "Estado" en el table header (entre TÃ­tulo y VersiÃ³n)
- Badge "âœ… Habilitado" (verde) o "ðŸš« Deshabilitado" (rojo/gris) por cada paquete
- BotÃ³n toggle visible solo para ADMIN/INSTRUCTOR/SUPERADMIN
- Event delegation con `querySelectorAll('.btn-toggle-scorm')` y PATCH a `/api/v1/scorm/{paqueteId}/toggle-habilitado`
- Estudiantes solo ven paquetes con `habilitado = true`

**Archivos modificados:**
- [`PaqueteScorm.java`](src/main/java/com/lms/model/PaqueteScorm.java) â€” Nuevo campo `habilitado`
- [`ScormService.java`](src/main/java/com/lms/service/ScormService.java) â€” Nuevo mÃ©todo `toggleHabilitado()`
- [`ScormController.java`](src/main/java/com/lms/controller/ScormController.java) â€” `habilitado` en respuesta + nuevo endpoint `PATCH /toggle-habilitado`
- [`scorm-upload.html`](src/main/resources/templates/cursos/scorm-upload.html) â€” Estado column + toggle button

---

#### Resumen de archivos modificados (12-Jul-2026)

| Archivo | Cambio |
|---------|--------|
| [`InscripcionController.java`](src/main/java/com/lms/controller/InscripcionController.java) | `@RequestParam("file")` â†’ `@RequestParam("archivo")` |
| [`InscripcionService.java`](src/main/java/com/lms/service/InscripcionService.java) | `findByEmail()` â†’ `findByEmailAndDeletedAtIsNull()` |
| [`participantes.html`](src/main/resources/templates/cursos/participantes.html) | Badge "â³ Pendiente" â†’ "ðŸ“ Inscrito" |
| [`Documento.java`](src/main/java/com/lms/model/Documento.java) | Nuevo campo `habilitado` |
| [`DocumentoService.java`](src/main/java/com/lms/service/DocumentoService.java) | Nuevo mÃ©todo `toggleHabilitado()` |
| [`DocumentoController.java`](src/main/java/com/lms/controller/DocumentoController.java) | Nuevo endpoint `PATCH /toggle-habilitado` |
| [`documentos.html`](src/main/resources/templates/cursos/documentos.html) | Columna Estado + botÃ³n toggle |
| [`editor.html`](src/main/resources/templates/evaluaciones/editor.html) | Feedback muestra cÃ³digo EVA al guardar |
| [`PaqueteScorm.java`](src/main/java/com/lms/model/PaqueteScorm.java) | Nuevo campo `habilitado` |
| [`ScormService.java`](src/main/java/com/lms/service/ScormService.java) | Nuevo mÃ©todo `toggleHabilitado()` |
| [`ScormController.java`](src/main/java/com/lms/controller/ScormController.java) | `habilitado` en respuesta + `PATCH /toggle-habilitado` |
| [`scorm-upload.html`](src/main/resources/templates/cursos/scorm-upload.html) | Columna Estado + botÃ³n toggle |

---

### T16: CorrecciÃ³n de 6 tareas del archivo `mejoras_12-07.md` (12-Jul-2026)

**Contexto:** El usuario reportÃ³ que tras la implementaciÃ³n de T15 (5 mejoras), 4 de las 6 funcionalidades presentaban problemas. Se creÃ³ el archivo [`mejoras_12-07.md`](mejoras_12-07.md) como checklist. Esta task (T16) cubre la correcciÃ³n de esos 6 puntos.

---

#### Task 1 â€” Reporte detallado en carga masiva CSV

**Problema:** La carga masiva de participantes vÃ­a CSV mostraba solo un contador simple (`data.inscritos || 0`). El usuario necesitaba un reporte por pantalla con 3 escenarios: exitosos, no encontrados en BD, y ya inscritos en el curso.

**DiagnÃ³stico:**
- Backend [`InscripcionService.inscribirMasivo()`](src/main/java/com/lms/service/InscripcionService.java:196) atrapaba TODAS las excepciones con un `catch (Exception e)` genÃ©rico y solo logueaba con `log.warn()`
- No habÃ­a distinciÃ³n entre `IllegalStateException` (ya inscrito) e `IllegalArgumentException` (usuario no encontrado)
- El mÃ©todo retornaba `List<InscripcionResponse>` â€” solo lista de Ã©xitos, sin estructura de errores

**SoluciÃ³n:**

**1a. Nuevo DTO [`InscripcionMasivaReport.java`](src/main/java/com/lms/dto/InscripcionMasivaReport.java):**
```java
public record InscripcionMasivaReport(
        int totalProcesados, int exitosos, int noEncontrados,
        int yaInscritos, int errores,
        List<InscripcionResponse> inscripciones,
        List<String> detalleNoEncontrados,
        List<String> detalleYaInscritos,
        List<String> detalleErrores
) {
    public static InscripcionMasivaReport vacio() { ... }
}
```

**1b. Refactor de [`InscripcionService.inscribirMasivo()`](src/main/java/com/lms/service/InscripcionService.java:196):**
- Return type cambiado: `List<InscripcionResponse>` â†’ `InscripcionMasivaReport`
- CategorizaciÃ³n por tipo de excepciÃ³n:
  - `IllegalStateException` â†’ `yaInscritos` (participante ya inscrito)
  - `IllegalArgumentException` â†’ `noEncontrados` (usuario no existe)
  - `Exception` genÃ©rica â†’ `errores` (error inesperado)

**1c. ActualizaciÃ³n de [`InscripcionController.inscribirMasivo()`](src/main/java/com/lms/controller/InscripcionController.java:57):**
- Return type cambiado a `ResponseEntity<InscripcionMasivaReport>`

**1d. Frontend [`participantes.html`](src/main/resources/templates/cursos/participantes.html:283):**
- Reporte visual detallado con contadores y listas para cada escenario
- Muestra: `âœ… X inscritos`, `âŒ X no encontrados`, `âš ï¸ X ya inscritos`, `â— X errores`

---

#### Task 2 â€” Mensaje detalle en inscripciÃ³n individual

**Problema:** Al inscribir un participante de forma individual, los mensajes de error no se mostraban en pantalla â€” el bloque `catch` leÃ­a `d.mensaje` pero el backend envÃ­a `message`.

**DiagnÃ³stico:**
- Frontend [`participantes.html:253`](src/main/resources/templates/cursos/participantes.html:253): `var errorMsg = d.mensaje || 'Error al inscribir';`
- Backend [`ErrorResponse.java`](src/main/java/com/lms/dto/ErrorResponse.java:12): `String message` â€” el campo se llama `message`, no `mensaje`
- El valor de `d.mensaje` era siempre `undefined`

**Fix:** Cambiado `d.mensaje` â†’ `d.message` en el handler de error de inscripciÃ³n individual y tambiÃ©n en el handler de error de carga masiva.

**Archivo modificado:**
- [`participantes.html`](src/main/resources/templates/cursos/participantes.html) â€” LÃ­neas 253 y 295

---

#### Task 3 â€” Estado "Inscrito" inmediato (ya funcionaba)

Sin cambios â€” la inscripciÃ³n ya pasaba al estado "ðŸ“ Inscrito" inmediatamente desde T15 (Fix 2).

---

#### Task 4 â€” Documentos: "Cargando documentos..." y SCORM HTTP 500

**Problema:** Al agregar el campo `habilitado` a las entidades [`Documento.java`](src/main/java/com/lms/model/Documento.java:61) y [`PaqueteScorm.java`](src/main/java/com/lms/model/PaqueteScorm.java:62) con `@Column(nullable = false) private boolean habilitado = true;`, NO se creÃ³ una migraciÃ³n Flyway para agregar la columna en PostgreSQL. Al consultar estas entidades, Hibernate generaba SQL referenciando una columna inexistente â†’ SQLException â†’ HTTP 500 â†’ frontend mostraba "Cargando documentos..." o "âš ï¸ Error al cargar los paquetes SCORM (HTTP 500)".

**SoluciÃ³n:**

**4a. Nueva migraciÃ³n Flyway [`V9__Add_habilitado_to_documento_and_scorm.sql`](src/main/resources/db/migration/V9__Add_habilitado_to_documento_and_scorm.sql):**
```sql
ALTER TABLE documentos ADD COLUMN IF NOT EXISTS habilitado BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE paquetes_scorm ADD COLUMN IF NOT EXISTS habilitado BOOLEAN NOT NULL DEFAULT TRUE;
```

**4b. Error handler en [`documentos.html`](src/main/resources/templates/cursos/documentos.html):**
- Agregado bloque `else` para status no-200 en `cargarDocumentos()`

**Archivos modificados/creados:**
- [`V9__Add_habilitado_to_documento_and_scorm.sql`](src/main/resources/db/migration/V9__Add_habilitado_to_documento_and_scorm.sql) â€” **CREADO**
- [`documentos.html`](src/main/resources/templates/cursos/documentos.html) â€” Error handler para status no-200

---

#### Task 5 â€” EVA ID no se visualiza correctamente

**Problema:** En [`listado.html`](src/main/resources/templates/evaluaciones/listado.html:354), el cÃ³digo se mostraba como `ID: ef2ac871â€¦` (truncated UUID) en lugar del cÃ³digo EVA-XXX generado automÃ¡ticamente.

**DiagnÃ³stico:**
- La lÃ­nea usaba `ev.id.substring(0, 8) + 'â€¦'` que muestra los primeros 8 caracteres del UUID
- El campo `ev.codigo` (EVA-001, EVA-005, etc.) no se estaba utilizando en el display

**Fix â€” [`listado.html:354`](src/main/resources/templates/evaluaciones/listado.html:354):**
```javascript
// ANTES:
'ID: ' + (ev.id ? ev.id.substring(0, 8) + 'â€¦' : 'â€”')
// DESPUÃ‰S:
(ev.codigo ? '<strong style="color:var(--color-accento);">' + escapeHtml(ev.codigo) + '</strong>'
    : 'ID: ' + (ev.id ? ev.id.substring(0, 8) + 'â€¦' : 'â€”'))
```

**Archivo modificado:**
- [`listado.html`](src/main/resources/templates/evaluaciones/listado.html) â€” EVA ID ahora muestra `ev.codigo` con color de acento

---

#### Task 6 â€” SCORM HTTP 500 al listar paquetes

**Problema:** Misma causa raÃ­z que Task 4 â€” la columna `habilitado` no existÃ­a en la tabla `paquetes_scorm` de PostgreSQL.

**SoluciÃ³n:** La migraciÃ³n Flyway V9 creada en Task 4 tambiÃ©n agrega la columna a `paquetes_scorm`, resolviendo este problema.

**Archivo creado (compartido con Task 4):**
- [`V9__Add_habilitado_to_documento_and_scorm.sql`](src/main/resources/db/migration/V9__Add_habilitado_to_documento_and_scorm.sql)

---

#### Resumen de archivos modificados/creados (T16)

| Archivo | Cambio |
|---------|--------|
| [`mejoras_12-07.md`](mejoras_12-07.md) | Marcadas todas las tareas como [x] completadas |
| [`InscripcionMasivaReport.java`](src/main/java/com/lms/dto/InscripcionMasivaReport.java) | **CREADO** â€” Nuevo DTO para reporte categorizado de carga masiva |
| [`InscripcionService.java`](src/main/java/com/lms/service/InscripcionService.java) | `inscribirMasivo()` retorna `InscripcionMasivaReport` con categorizaciÃ³n por excepciÃ³n |
| [`InscripcionController.java`](src/main/java/com/lms/controller/InscripcionController.java) | Return type cambiado a `InscripcionMasivaReport` |
| [`participantes.html`](src/main/resources/templates/cursos/participantes.html) | Reporte detallado en carga masiva + fix `d.mensaje` â†’ `d.message` |
| [`V9__Add_habilitado_to_documento_and_scorm.sql`](src/main/resources/db/migration/V9__Add_habilitado_to_documento_and_scorm.sql) | **CREADO** â€” Flyway migration: columnas `habilitado` en documentos y paquetes_scorm |
| [`documentos.html`](src/main/resources/templates/cursos/documentos.html) | Error handler para status no-200 en `cargarDocumentos()` |
| [`listado.html`](src/main/resources/templates/evaluaciones/listado.html) | EVA ID muestra `ev.codigo` en lugar de UUID truncado |
| [`EvaluacionControllerTest.java`](src/test/java/com/lms/controller/EvaluacionControllerTest.java) | Agregado `@MockBean InscripcionService` + `@WithMockUser` en 5 tests |
| [`InscripcionControllerTest.java`](src/test/java/com/lms/controller/InscripcionControllerTest.java) | Mock actualizado a `InscripcionMasivaReport` |

**Tests:** 46 tests relevantes ejecutados (EvaluacionControllerTest, InscripcionControllerTest, InscripcionServiceTest, DocumentoControllerTest, ScormControllerTest) â€” **0 fallos, 0 errores** âœ…

---

### T17: Fix "Mis Documentos" muestra UUIDs truncados en lugar de cÃ³digos DOC-XXX (12-Jul-2026)

**Problema:** En la vista "Mis Documentos" ([`dashboard/documentos.html`](src/main/resources/templates/dashboard/documentos.html)), la columna "CÃ³digo" mostraba UUIDs truncados (ej: `c4bdb1a3`, `f68437d7`) en lugar de los cÃ³digos auto-generados como `DOC001`, `DOC002`, `DOC003`. El usuario confirmÃ³ que "Documentos Vinculados" ([`cursos/documentos.html`](src/main/resources/templates/cursos/documentos.html)) mostraba correctamente los cÃ³digos.

**DiagnÃ³stico â€” Error de precedencia de operadores en JavaScript:**

En [`dashboard/documentos.html:244`](src/main/resources/templates/dashboard/documentos.html:244):
```javascript
// BUG: doc.codigo || doc.id ? ... : ...
// Por precedencia de operadores, || tiene mayor prioridad que ?:
// Esto se evalÃºa como: (doc.codigo || doc.id) ? (doc.id).substring(0,8) : 'â€”'
// Como doc.id (UUID string) siempre es truthy, NUNCA usa doc.codigo
var codigo = doc.codigo || doc.id ? (doc.id).substring(0, 8) : 'â€”';
```

**Fix â€” Agregar parÃ©ntesis para corregir precedencia:**
```javascript
// AHORA: doc.codigo tiene prioridad sobre el fallback de UUID
var codigo = doc.codigo || (doc.id ? doc.id.substring(0, 8) + 'â€¦' : 'â€”');
```

**Comportamiento resultante:**
1. Si `doc.codigo` existe (ej: `"DOC001"`, `"DOC002"`), se muestra completo en monospace con color de acento
2. Si `doc.codigo` no existe pero `doc.id` sÃ­, muestra primeros 8 caracteres del UUID como fallback
3. Si ambos son null/undefined, muestra `'â€”'`

**Archivo modificado:**
- [`dashboard/documentos.html`](src/main/resources/templates/dashboard/documentos.html) â€” LÃ­nea 244: corregida precedencia de operadores

---

## ðŸ“Œ Pendientes / Mejoras

- [x] Remover logs `[DEBUG]` de producciÃ³n (`EvaluacionController`, `EvaluacionService`, `listado.html`)
- [x] Verificar que el botÃ³n "Rendir EvaluaciÃ³n" funcione correctamente para estudiantes
- [x] Fix checkboxes/radios invisibles en rendir.html (select multiple, opciÃ³n Ãºnica, V/F)
- [x] Probar en navegador que la selecciÃ³n mÃºltiple funcione correctamente despuÃ©s del Fix 10
- [x] Fix carga masiva CSV (parÃ¡metro `"archivo"` + `findByEmailAndDeletedAtIsNull`)
- [x] Badge "Pendiente" â†’ "Inscrito" para nuevas inscripciones
- [x] Documentos: columna Estado + habilitar/deshabilitar
- [x] Mostrar cÃ³digo EVA al guardar evaluaciÃ³n
- [x] SCORM: columna Estado + habilitar/deshabilitar
- [x] Tareas 1 y 2 implementadas y verificadas. Trazabilidad configurada y roles de ADMIN actualizados
- [ ] Considerar si estudiantes deben ver evaluaciones de otros cursos en los que estÃ¡n inscritos
- [ ] Mejorar manejo de errores en frontend (toast notifications)
- [ ] Agregar `Cache-Control` headers desde el controlador Spring (no solo meta tags HTML) para prevenir cachÃ© en proxies/CDN

---

## Historial de Decisiones Tecnicas (Julio 2026)

### Auditoria (14-Jul)
- **Filtros jerarquicos:** 2 combos (categoria -> subfiltro) con enum AuditCategoria en backend (reunion.md)
- **Codigos amigables:** DOC001/SCO001/EVA001 visibles en columna Recurso en vez de UUID (reunion.md)
- **Busqueda en tiempo real:** campo con debounce 300ms, busca por codigo/RUT/email (reunion.md)
- **Actor muestra RUT:** via mapa batch en AdminHtmxController (reunion.md)

### Firma Digital y Forzar Completado (14-Jul)
- **Switch requiereFirmaCumplimiento** en editor de cursos (mejoras1y2.md)
- **Forzar completado por ADMIN:** modal con motivo y trazabilidad en participantes.html (mejoras1y2.md)
- **Event Sourcing:** V10 y V11 migraciones Flyway (mejoras1y2.md)

### Roles y Permisos (15-Jul)
- **Checkboxes de permisos** agrupados por recurso con seleccionar todos (reunion.md)
- **Endpoint POST /hx/admin/roles** con creacion HTMX (reunion.md)
- **hashSha256** en builder de RolService (reunion.md)

### Evaluaciones (16-17 Jul)
- **Intento unico** con reinicio autorizado por ADMIN (evaluaciones.md)
- **Correccion automatica** comparando opciones JSON (evaluaciones.md)
- **Escala 0-100%** para nota y notaMinima (evaluaciones.md)
- **Detalle por pregunta** (CORRECTA/INCORRECTA) en rendir.html y listado.html (detalle_evaluacion.md)
- **Reporte XLSX 'resultados'** con buena/mala por pregunta (tareas17.07.md)
- **Entidad IntentoEvaluacion** + migracion V12 (tareas17.07.md)

### UX y Visual (17-Jul)
- **Ojito toggle password** en crear usuario (usuarios-crear-form.html)
- **Sidebar no solapa footer:** z-index + padding-bottom en layout.css
- **Refactor visual:** 55 emojis reemplazados por SVGs monocromaticos (paleta azul/gris)

### Limpieza y Organizacion (17-Jul)
- **documentos de decision** movidos a carpeta decisiones/
- **archivos temporales** movidos a carpeta temp/
- **UX_ejemplo** eliminado (no usado)
- **DB reseteada** a cero para codigos DOC001/SCO001/EVA001

### Perfil - Mis Cursos (18-Jul)
- Duplicado de div mis-cursos-container corregido
- Endpoint creado: GET /hx/admin/perfil/cursos con @PreAuthorize(isAuthenticated)
- Fragmento cursos-fragment.html con lista de cursos del usuario
- XHR directo (sin HTMX) en perfil.html para cargar cursos

- Script vacio eliminado

---
### Bug-004: Inscripcion de usuarios — RUTs en CSV masivo, error 403 y plantilla (22-Jul)

**Problemas:**
- `@PreAuthorize("hasAuthority('INSCRIPCION_CREAR')")` bloqueaba admins sin ese permiso granular
- Carga masiva CSV usaba `UUID.fromString()` en vez de `resolverIdentificador()` → los RUTs fallaban como "no encontrados"
- Sin plantilla CSV descargable para el usuario
- Boton "Forzar Completado" daba 403 (CSRF)
- Modal "Forzar" cortado visualmente

**Fix 1 — InscripcionController.java:50:** hasAuthority → hasAnyRole('ADMIN','INSTRUCTOR','SUPERADMIN')

**Fix 2 — InscripcionService.java:253:** Reemplazado UUID.fromString(linea) por resolverIdentificador(linea) (soporta RUT, email y UUID)

**Fix 3 — PlantillaController.java (NUEVO):** Endpoint GET /api/v1/plantilla-inscripcion descarga CSV ejemplo con columna usuarioId

**Fix 4 — participantes.html:** Boton "Descargar plantilla CSV" + nombre de archivo en negrita y mas grande

**Archivos modificados:** InscripcionController.java, InscripcionService.java, participantes.html, PlantillaController.java (NUEVO)

---
### Bug-005: Solucion definitiva CSRF + modal Forzar (22-Jul)

**Problema raiz:** CSRF causaba 403 intermitentes en toda la API REST. Cada nuevo endpoint requeria excluirse manualmente de CSRF o agregar el token.

**Diagnostico:** El JWT viaja en cookie access_token con SameSite=Lax + HttpOnly=true. SameSite=Lax bloquea ataques CSRF cross-site porque el navegador no envia la cookie en POST/PUT/DELETE desde otro origen. La proteccion CSRF de Spring Security es REDUNDANTE.

**Solucion definitiva — SecurityConfig.java:50:**
```java
.ignoringRequestMatchers("/hx/admin/**", "/api/v1/**")
```

**Impacto:**
- Elimina TODOS los 403 CSRF de la API REST
- Ya no se necesita handler htmx:configRequest en head.html
- Ya no se necesita input hidden _csrf en formularios
- Ya no hay que acordarse de CSRF al crear nuevos endpoints
- Seguro porque: JWT + SameSite=Lax + HttpOnly + @PreAuthorize

**Otros fixes:**
- head.html — Limpiado handler CSRF duplicado
- participantes.html — Modal "Forzar" con z-index: 9999; position: fixed; para evitar corte visual
- InscripcionController.java — Eliminado metodo descargarPlantilla() duplicado (ahora en PlantillaController.java)
- PlantillaController.java — Endpoint funcional, compilado y verificado

**Estado:** Container healthy, BUILD SUCCESS, 0 errores CSRF en toda la app.

---

## Modulo Gestor de Contenidos BPO

### Resumen
Se implemento el modulo "Gestor de Contenidos BPO" para administrar contenidos (procedimientos, noticias, ofertas) asociados a cuentas de clientes (Cliente A, Cliente B, etc.) con asignacion a usuarios.

### Estructura del modulo

**Modelos:**
- `CuentaBPO` — Representa un cliente/empresa (Cliente A, Cliente B, etc.). Campos: id, nombre, codigo, activa
- `SubServicioBPO` — Subdivision de una cuenta (Facturacion, Soporte, Ventas). FK a CuentaBPO. Incluye campo `codigo` para URLs limpias
- `ContenidoBPO` — Contenido del gestor. Campos: tipo (procedimiento/noticia/oferta), titulo, descripcion_corta, contenido_html, metadatos JSONB, etiquetas JSONB, estado (borrador/publicado/archivado), vigencia, archivo (url, nombre, mime, tamano). FK a CuentaBPO y SubServicioBPO
- `UsuarioCuentaBPO` — Tabla puente usuario-cuenta con subservicio opcional

**Controladores:**
- `AdminBPOController.java` — CRUD de gestores y publicaciones desde el admin
- `ContenidoBPOController.java` — Vista de contenidos para alumnos/agentes BPO
- `ContenidoBPOApiController.java` — API REST para importacion de contenidos
- `GestorAlumnoController.java` — Vista de gestores para alumnos

**Servicios:**
- `CuentaBPOService.java` — Gestion de cuentas BPO y subservicios
- `ContenidoBPOService.java` — Gestion de contenidos con subida de archivos y auditoria
- `FileUploadService.java` — Subida de archivos a MinIO

**Templates (admin):**
- `gestores.html` — Listado de gestores con boton "+ Nuevo Gestor"
- `formulario-gestor.html` — Formulario para crear/editar gestor
- `gestor-detalle.html` — Detalle del gestor con sus subcarpetas
- `subcarpeta-publicaciones.html` — Listado de publicaciones en una subcarpeta
- `formulario-publicacion.html` — Formulario para crear publicacion con archivo adjunto
- `formulario-contenido.html` — Formulario de contenido BPO (admin legacy)

**Templates (alumno):**
- `gestor-contenidos.html` — Pagina principal del gestor para el alumno
- `gestor-detalle.html` — Detalle del gestor para el alumno
- `gestores.html` — Listado de gestores del alumno
- `sidebar-gestor.html` — Sidebar del gestor
- `fragmentos/` — Fragmentos: tarjetas-resumen, lista-contenidos, detalle-contenido, columnas-publicaciones

**Migraciones Flyway:**
- `V20__crear_modulo_bpo.sql` — Tablas cuentas_bpo, subservicios_bpo, contenidos_bpo, usuarios_cuenta_bpo
- `V21__seed_roles_bpo.sql` — Roles BPO (BPO_AGENTE, BPO_SUPERVISOR, BPO_ADMIN)
- `V22__agregar_archivos_contenido_bpo.sql` — Columnas archivo en contenidos_bpo
- `V23__permisos_bpo_roles_existentes.sql` — Permisos para roles existentes
- `V24__seed_datos_ejemplo_bpo.sql` — Datos de ejemplo (Gestor de ejemplo con publicaciones)

### Bugs y correcciones realizadas

**Bug-012:** Seed data incorrecta y error 500 en formulario-gestor.html
- Se corrigio V24 para mostrar solo "Gestor de ejemplo"
- Se mejoro `obtenerCuentasActivas()` en CuentaBPOService
- Se agrego sidebar para admin con enlace a /admin/bpo/gestores

**Bug-013:** Metodos faltantes en ContenidoBPOService (compilacion fallida)
- Se agregaron metodos: `obtenerResumen`, `buscarConFiltros`, `crearContenido`, `cambiarEstado`
- Se corrigio tipo "publicacion" a tipos validos (procedimiento/noticia/oferta) en V24 y servicio

**Bug-014:** Errores en admin de Gestor de Contenidos (COMPLETADO)
- Pagina en blanco en /gestores/nuevo → Se agrego objeto gestor al modelo
- Boton Volver con estilo btn-ghost incorrecto → Cambiado a btn-primary
- Error 500 en + Nueva Publicacion (sintaxis Thymeleaf en 4 URLs) → Corregidas
- Publicaciones sin estructura de tarjeta → Redisenadas como tarjetas con sombra
- Formulario de publicacion con th:object vacio y th:href duplicado → Corregidos
- @ModelAttribute sin nombre en el controlador → Agregado nombre "publicacion"
- Botones Guardar/Publicar sin clase btn-primary → Migrados a btn-primary
- Formularios sin diseño card → Ambos formularios ahora usan card layout
- Falta metodo crearCuenta en Service → Agregado
- Tests de validacion creados (6 tests) → Todos pasan

**Bug-015:** Mejoras en admin del Gestor de Contenidos (COMPLETADO)
- ✅ **Error A:** Agregado boton "+ Crear nueva carpeta" + Endpoints GET/POST + Template formulario-subcarpeta.html
- ✅ **Error B:** Agregado boton "Editar" + Endpoints GET/POST + Metodo actualizarPublicacion
- ✅ **Error C:** Token CSRF agregado al formulario multipart (enctype="multipart/form-data")
- ✅ **Error D:** URLs limpias con codigo en lugar de UUID + Fallback a UUID para compatibilidad backward
- Plan implementado completamente en 4 fases: V1-CSRF, F2-Subcarpeta, F3-Editar, F4-RutasLimpias

### Tests de validacion
Los tests para verificar las correcciones del Bug-014 estan en `bugs/test/` y se ejecutan con:
```powershell
cd <RUTA_DEL_PROYECTO>
powershell -File bugs/test/run-all-tests.ps1
```

**Tests disponibles (10):**

**Bug-014 (6 tests):**
- `test-01-crear-cuenta.ps1` — Verifica metodo crearCuenta en Service
- `test-02-urls-thymeleaf.ps1` — Verifica sintaxis correcta en 4 URLs
- `test-03-controller.ps1` — Verifica @ModelAttribute y POST handler
- `test-04-botones.ps1` — Verifica consistencia de estilos btn-primary
- `test-05-formulario-publicacion.ps1` — Verifica correcciones del formulario
- `test-06-funcional.ps1` — Verifica endpoints HTTP 200 + estilos CSS + rutas

**Bug-015 (4 tests - FASE 1-4):**
- `test-07-fase1-csrf.ps1` — ✅ Token CSRF en formulario multipart
- `test-08-fase2-crear-subcarpeta.ps1` — ✅ Template + Endpoints + Boton + Migracion V25
- `test-09-fase3-editar-publicacion.ps1` — ✅ Boton Editar + Endpoints + Metodo Service
- `test-10-fase4-rutas-limpias.ps1` — ✅ URLs con codigo + Fallback UUID + Metodos resolve*

**Bug-016 (en investigacion — 1 test):**
- `test-11-bug016-csrf-post.ps1` — 🔄 CSRF en formulario-subcarpeta.html + verificar codigo en publicaciones

### Infraestructura
- `docker/docker-compose.java.yml` — Comando de la API cambiado de `hot-reload.sh` a `mvn spring-boot:run` directo para evitar errores de entrypoint
- `SubServicioBPO.java` — Agregado campo `codigo` para soportar URLs limpias en subcarpetas
- `V26__agregar_deleted_at_documentos.sql` — Agregada columna `deleted_at` a tabla `documentos` (faltaba, causaba error 500 en dashboard)

### Bugs registrados
| Bug | Estado | Descripcion |
|-----|--------|-------------|
| 012 | Completado | Seed data incorrecta y error 500 en formulario-gestor.html |
| 013 | Completado | Metodos faltantes en ContenidoBPOService (compilacion fallida) |
| 014 | Completado | Errores en admin de Gestor de Contenidos (botones, URLs, formularios) |
| 015 | Completado | Mejoras en admin: crear carpeta, editar, CSRF, rutas limpias (4 FASES implementadas) |
| 016 | En investigacion | CSRF 403 en POST de publicaciones y subcarpetas (residual de BUG-015) |

### Resumen de implementacion BUG-015
**FASE 1 (CSRF)** ✅
- [x] Token CSRF agregado a formulario-publicacion.html (enctype multipart)
- [x] mvn compile BUILD SUCCESS

**FASE 2 (Crear subcarpeta)** ✅
- [x] Migracion V25 creada (columna codigo en subservicios_bpo)
- [x] Metodo crearSubcarpeta en CuentaBPOService
- [x] Endpoints GET /subcarpetas/nuevo y POST /subcarpetas
- [x] Template formulario-subcarpeta.html (NUEVO)
- [x] Boton "+ Crear nueva carpeta" agregado a gestor-detalle.html
- [x] mvn compile BUILD SUCCESS

**FASE 3 (Editar publicacion)** ✅
- [x] Metodo actualizarPublicacion en ContenidoBPOService
- [x] Endpoints GET y POST para editar publicacion
- [x] Boton "Editar" agregado a subcarpeta-publicaciones.html
- [x] mvn compile BUILD SUCCESS

**FASE 4 (Rutas limpias)** ✅
- [x] Plantillas actualizadas: gestores.html, gestor-detalle.html, subcarpeta-publicaciones.html, formulario-publicacion.html
- [x] Metodos resolveCuentaId y resolveSubservicioId en AdminBPOController
- [x] PathVariable cambiados a String para aceptar codigo o UUID
- [x] Fallback UUID implementado (intenta UUID primero, luego busca por codigo)
- [x] mvn compile BUILD SUCCESS

**Tests** ✅ (10 tests totales, 4 nuevos para BUG-015)
- [x] test-07-fase1-csrf.ps1 PASS
- [x] test-08-fase2-crear-subcarpeta.ps1 PASS
- [x] test-09-fase3-editar-publicacion.ps1 PASS
- [x] test-10-fase4-rutas-limpias.ps1 PASS

