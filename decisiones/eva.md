# Análisis de Problemas y Plan de Acción: Visibilidad de Evaluaciones y Cursos para Rol Estudiante

## Resumen de Problemas Detectados

### Problema 1: ESTUDIANTE no puede acceder a evaluaciones de un curso al que está inscrito

**Síntoma**: Un usuario con rol ESTUDIANTE, inscrito en un curso que tiene evaluaciones cargadas, no puede rendir la evaluación desde la interfaz. Al hacer clic en "Evaluaciones" desde la tarjeta del curso, la vista `evaluaciones/listado.html` se carga pero la lógica JavaScript determina el rol y **oculta el botón de "Rendir"** porque no existe. El código actual en `evaluaciones/listado.html` muestra editar/eliminar para administradores pero no ofrece un botón de "Rendir" o "Comenzar evaluación" para estudiantes.

**Causa raíz**: 
1. En `fragmentos.html` (línea 108-117), el enlace a evaluaciones lleva a `/cursos/evaluaciones?cursoId=X` sin validación de inscripción.
2. En `evaluaciones/listado.html` (línea ~555), el JavaScript `renderizarEvaluaciones()` evalúa `esEstudiante` verificando si existe un elemento `#user-role-estudiante` en el DOM (**que NUNCA se crea porque el span usa `sec:authorize="hasRole('ESTUDIANTE')"`** — el prefijo correcto en BD es `ROLE_ESTUDIANTE`). Esto hace que `esEstudiante` siempre sea `false` para estudiantes.
3. Aunque `esEstudiante` funcionara, en el renderizado solo se muestra `editar`/`eliminar` para no-estudiantes, pero NO hay un botón de "Rendir evaluación" para estudiantes.
4. No hay un endpoint o servicio que verifique si un estudiante puede acceder a una evaluación específica.

### Problema 2: ESTUDIANTE ve cursos en los que NO está inscrito

**Síntoma**: Un estudiante ve 2 cursos adicionales en la lista de cursos (`/cursos`) a los que no fue inscrito.

**Causa raíz**:
1. En `CursoService.listarCursos()` (línea 101-103) y `CursoRepository.findByDeletedAtIsNull()`, el listado de cursos retorna **TODOS** los cursos activos del sistema, no solo aquellos en los que el usuario está inscrito.
2. En `CursoHtmxController.listadoFragment()` (línea 31), se llama a `cursoService.listarCursos(pageable)` sin filtrar por usuario.
3. En `fragmentos.html` (plantilla Thymeleaf), los cursos se renderizan sin verificar si el usuario autenticado está inscrito.

---

## Plan de Acción Detallado

A continuación se presentan las tareas divididas en mini-tareas para que cada una sea implementada de forma independiente.

---

## Tarea 1: Crear endpoint y servicio para evaluaciones de estudiante

### Subtarea 1.1: Agregar método en `InscripcionRepository`

**Archivo**: `src/main/java/com/lms/repository/InscripcionRepository.java`

Agregar un método que verifique si un usuario está inscrito en un curso específico, retornando la inscripción si existe:

```java
import java.util.Optional;

// Ya existe: Optional<Inscripcion> findByUsuarioIdAndCursoIdAndDeletedAtIsNull(UUID usuarioId, UUID cursoId);
// Ya está presente en el archivo (línea ~18)
```

### Subtarea 1.2: Agregar método de validación en `InscripcionService`

**Archivo**: `src/main/java/com/lms/service/InscripcionService.java`

Agregar un método público para verificar si un estudiante está inscrito en un curso:

```java
/**
 * Verifica si un usuario está inscrito en un curso activo.
 *
 * @param usuarioId ID del usuario
 * @param cursoId   ID del curso
 * @return true si el usuario está inscrito y la inscripción no fue eliminada
 */
@Transactional(readOnly = true)
public boolean isUsuarioInscritoEnCurso(UUID usuarioId, UUID cursoId) {
    return inscripcionRepository.existsByUsuarioIdAndCursoIdAndDeletedAtIsNull(usuarioId, cursoId);
}
```

### Subtarea 1.3: Modificar `EvaluacionController` para estudiantes

**Archivo**: `src/main/java/com/lms/controller/EvaluacionController.java`

Agregar un nuevo endpoint que permita a un estudiante **inscrito** listar las evaluaciones de un curso:

```java
// Al inicio, inyectar InscripcionService y SecurityUtils (ya inyectados en otros)
// Agregar este método:

/**
 * Endpoint para estudiantes: lista evaluaciones del curso SÓLO si el estudiante está inscrito.
 */
@GetMapping("/estudiante")
@PreAuthorize("hasRole('ESTUDIANTE')")
public ResponseEntity<List<Evaluacion>> listarParaEstudiante(
        @PathVariable UUID cursoId,
        Authentication authentication) {
    UUID usuarioId = securityUtils.getCurrentUserId(authentication);
    
    // Verificar inscripción
    if (!inscripcionService.isUsuarioInscritoEnCurso(usuarioId, cursoId)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    
    // Retornar evaluaciones sin respuestas correctas (solo estructura)
    List<Evaluacion> evaluaciones = evaluacionService.listarPorCurso(cursoId);
    
    // Limpiar respuestas correctas de las preguntas para que el estudiante no las vea
    for (Evaluacion ev : evaluaciones) {
        if (ev.getPreguntas() != null) {
            for (Pregunta p : ev.getPreguntas()) {
                p.setRespuestaCorrecta(null); // Ocultar respuesta correcta
            }
        }
    }
    
    return ResponseEntity.ok(evaluaciones);
}
```

### Subtarea 1.4: Agregar endpoint para rendir evaluación (obtener preguntas)

**Archivo**: `src/main/java/com/lms/controller/EvaluacionController.java`

Agregar un endpoint que permita a un estudiante obtener las preguntas de una evaluación específica (para "rendirla"):

```java
/**
 * Endpoint para estudiante: obtiene preguntas de evaluación (sin respuestas correctas).
 */
@GetMapping("/{evaluacionId}/rendir")
@PreAuthorize("hasRole('ESTUDIANTE')")
public ResponseEntity<Map<String, Object>> rendirEvaluacion(
        @PathVariable UUID cursoId,
        @PathVariable UUID evaluacionId,
        Authentication authentication) {
    UUID usuarioId = securityUtils.getCurrentUserId(authentication);
    
    // Verificar inscripción en el curso
    if (!inscripcionService.isUsuarioInscritoEnCurso(usuarioId, cursoId)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "No estás inscrito en este curso"));
    }
    
    // Obtener evaluación completa
    Evaluacion evaluacion = evaluacionService.buscarPorId(evaluacionId)
            .orElseThrow(() -> new IllegalArgumentException("Evaluación no encontrada"));
    
    // Verificar que la evaluación pertenezca al curso
    if (!evaluacion.getCurso().getId().equals(cursoId)) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", "La evaluación no pertenece al curso especificado"));
    }
    
    // Obtener preguntas sin respuestas correctas
    List<Pregunta> preguntas = evaluacionService.obtenerPreguntas(evaluacionId);
    List<Map<String, Object>> preguntasParaEstudiante = new ArrayList<>();
    for (Pregunta p : preguntas) {
        Map<String, Object> pMap = new LinkedHashMap<>();
        pMap.put("id", p.getId());
        pMap.put("tipo", p.getTipo());
        pMap.put("contenido", p.getContenido());
        pMap.put("peso", p.getPeso());
        pMap.put("orden", p.getOrden());
        pMap.put("opciones", p.getOpciones()); // Opciones SÍ se muestran (para seleccionar)
        pMap.put("tiempoRespuestaMs", p.getTiempoRespuestaMs());
        // NO incluir respuestaCorrecta
        preguntasParaEstudiante.add(pMap);
    }
    
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("evaluacion", Map.of(
        "id", evaluacion.getId(),
        "titulo", evaluacion.getTitulo(),
        "descripcion", evaluacion.getDescripcion(),
        "pesoTotal", evaluacion.getPesoTotal(),
        "tiempoEstimadoMinutos", evaluacion.getTiempoEstimadoMinutos(),
        "configuracion", evaluacion.getConfiguracion()
    ));
    response.put("preguntas", preguntasParaEstudiante);
    
    return ResponseEntity.ok(response);
}
```

**Nota**: Importar `java.util.ArrayList`, `java.util.LinkedHashMap`, `java.util.Map`, `org.springframework.http.HttpStatus`.

---

## Tarea 2: Modificar la vista de evaluaciones para estudiantes

### Subtarea 2.1: Arreglar detección de rol ESTUDIANTE en `evaluaciones/listado.html`

**Archivo**: `src/main/resources/templates/evaluaciones/listado.html`

**Cambio 1**: Reemplazar el span oculto (línea ~230):

```html
<!-- REEMPLAZAR ESTO: -->
<span id="user-role-estudiante" style="display:none" sec:authorize="hasRole('ESTUDIANTE')"></span>

<!-- POR ESTO: -->
<span id="user-role-data" 
      style="display:none"
      data-es-estudiante="${#authorization.expression('hasRole(''ROLE_ESTUDIANTE'')')}"
      data-es-admin="${#authorization.expression('hasAnyRole(''ROLE_ADMIN'', ''ROLE_INSTRUCTOR'', ''ROLE_SUPERADMIN'')')}"></span>
```

**Cambio 2**: Modificar la función `renderizarEvaluaciones()` para detectar correctamente el rol y agregar botón "Rendir evaluación":

```javascript
// En lugar de:
// var esEstudiante = !!document.getElementById('user-role-estudiante');

// Usar:
var roleData = document.getElementById('user-role-data');
var esEstudiante = roleData ? roleData.getAttribute('data-es-estudiante') === 'true' : false;
```

**Cambio 3**: En el HTML generado dentro de `renderizarEvaluaciones()`, reemplazar:

```javascript
// Donde dice:
(esEstudiante
    ? ''
    : '<!-- Acciones -->' + ... )

// Reemplazar por:
(esEstudiante
    ? '<!-- Acciones para estudiante -->' +
      '<div style="display:flex;align-items:center;justify-content:flex-end;gap:var(--spacing-sm);padding-top:var(--spacing-md);border-top:1px solid var(--border-default);">' +
          '<button type="button" class="btn btn-accent btn-sm btn-rendir-ev" ' +
             'style="display:inline-flex;align-items:center;gap:var(--spacing-xs);" ' +
             'data-evaluacion-id="' + ev.id + '" ' +
             'data-curso-id="' + CURSO_ID + '">' +
              '<svg style="width:1rem;height:1rem;flex-shrink:0;" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M5.25 5.653c0-.856.917-1.398 1.667-.986l11.54 6.348a1.125 1.125 0 010 1.971l-11.54 6.347a1.125 1.125 0 01-1.667-.985V5.653z"/></svg>' +
              '<span>Rendir Evaluación</span>' +
          '</button>' +
      '</div>'
    : '<!-- Acciones Admin/Instructor -->' +
      '<div style="display:flex;align-items:center;justify-content:flex-end;gap:var(--spacing-sm);padding-top:var(--spacing-md);border-top:1px solid var(--border-default);">' +
          // ... (el código existente de editar/eliminar)
      '</div>'
)
```

**Cambio 4**: Agregar event listener al final del script (antes del cierre del IIFE) para manejar clic en "Rendir Evaluación":

```javascript
// Event delegation para botones "Rendir Evaluación"
document.getElementById('evaluaciones-list').addEventListener('click', function(e) {
    var btn = e.target.closest('.btn-rendir-ev');
    if (btn) {
        var evaluacionId = btn.getAttribute('data-evaluacion-id');
        var cursoId = btn.getAttribute('data-curso-id');
        if (evaluacionId && cursoId) {
            window.location.href = '/cursos/evaluaciones/' + evaluacionId + '/rendir?cursoId=' + cursoId;
        }
    }
});
```

### Subtarea 2.2: Crear la vista de "Rendir Evaluación" para estudiantes

**Archivo nuevo**: `src/main/resources/templates/evaluaciones/rendir.html`

Crear una nueva vista Thymeleaf que cargue la evaluación y presente las preguntas al estudiante de forma interactiva. Esta vista debe:

1. Mostrar título, descripción y tiempo estimado de la evaluación.
2. Renderizar cada pregunta según su tipo (opción única, múltiple, verdadero/falso, respuesta corta, desarrollo).
3. Para preguntas de opción única/múltiple: mostrar las opciones como radio buttons / checkboxes.
4. Para verdadero/falso: mostrar dos botones.
5. Para respuesta corta/desarrollo: mostrar un textarea.
6. Incluir un botón "Enviar respuestas" que haga POST a un endpoint `/api/v1/cursos/{cursoId}/evaluaciones/{evaluacionId}/responder`.
7. Mostrar un temporizador si la evaluación tiene tiempo estimado.
8. Confirmación antes de enviar.

**Endpoint para enviar respuestas** (a crear en `EvaluacionController`):

```java
@PostMapping("/{evaluacionId}/responder")
@PreAuthorize("hasRole('ESTUDIANTE')")
public ResponseEntity<Map<String, Object>> responderEvaluacion(
        @PathVariable UUID cursoId,
        @PathVariable UUID evaluacionId,
        @RequestBody Map<String, Object> respuestas,
        Authentication authentication) {
    UUID usuarioId = securityUtils.getCurrentUserId(authentication);
    
    // Verificar inscripción
    if (!inscripcionService.isUsuarioInscritoEnCurso(usuarioId, cursoId)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    
    // TODO: Implementar lógica de guardado de respuestas y corrección
    // Por ahora, solo registrar el intento
    
    return ResponseEntity.ok(Map.of(
        "resultado", "success",
        "mensaje", "Respuestas registradas correctamente"
    ));
}
```

---

## Tarea 3: Filtrar cursos visibles para estudiantes (solo los inscritos)

### Subtarea 3.1: Agregar método en `CursoRepository`

**Archivo**: `src/main/java/com/lms/repository/CursoRepository.java`

No es necesario modificar el repositorio de cursos. La lógica debe ir en el servicio.

### Subtarea 3.2: Agregar método en `InscripcionRepository`

**Archivo**: `src/main/java/com/lms/repository/InscripcionRepository.java`

Agregar un método que retorne los cursos en los que un usuario está inscrito:

```java
@Query("SELECT i.curso FROM Inscripcion i WHERE i.usuario.id = :usuarioId AND i.deletedAt IS NULL AND i.curso.deletedAt IS NULL")
List<Curso> findCursosInscritosByUsuarioId(@Param("usuarioId") UUID usuarioId);
```

### Subtarea 3.3: Agregar método en `CursoService`

**Archivo**: `src/main/java/com/lms/service/CursoService.java`

Agregar un método para listar cursos filtrados por rol:

```java
/**
 * Lista cursos según el rol del usuario:
 * - ESTUDIANTE: solo los cursos en los que está inscrito
 * - ADMIN/INSTRUCTOR/SUPERADMIN: todos los cursos activos
 */
@Transactional(readOnly = true)
public Page<Curso> listarCursosSegunRol(UUID usuarioId, Collection<? extends GrantedAuthority> authorities, Pageable pageable) {
    boolean isAdminOrInstructor = authorities.stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || 
                          a.getAuthority().equals("ROLE_INSTRUCTOR") ||
                          a.getAuthority().equals("ROLE_SUPERADMIN"));
    
    if (isAdminOrInstructor) {
        return listarCursos(pageable);
    } else {
        // Estudiante: solo cursos inscritos
        // Necesitamos una page de cursos inscritos
        List<Curso> cursosInscritos = inscripcionRepository.findCursosInscritosByUsuarioId(usuarioId);
        // Convertir a Page
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), cursosInscritos.size());
        List<Curso> pageContent = start > cursosInscritos.size() ? List.of() : cursosInscritos.subList(start, end);
        return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, cursosInscritos.size());
    }
}
```

**Nota**: Importar `org.springframework.security.core.GrantedAuthority`, `java.util.Collection`, `java.util.List`.

### Subtarea 3.4: Modificar `CursoHtmxController` para filtrar por rol

**Archivo**: `src/main/java/com/lms/controller/CursoHtmxController.java`

Modificar `listadoFragment()` para pasar el usuario autenticado y filtrar:

```java
// Inyectar SecurityUtils y agregar Authentication como parámetro
private final SecurityUtils securityUtils;

// Actualizar constructor para incluir SecurityUtils

@GetMapping
public String listadoFragment(Model model, 
                              @PageableDefault(size = 20) Pageable pageable,
                              Authentication authentication) {
    UUID usuarioId = securityUtils.getCurrentUserId(authentication);
    Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
    model.addAttribute("cursos", cursoService.listarCursosSegunRol(usuarioId, authorities, pageable));
    return "cursos/fragmentos :: listado";
}
```

### Subtarea 3.5: Modificar el Dashboard KPI de cursos para estudiantes

**Archivo**: `src/main/java/com/lms/controller/DashboardController.java`

Modificar el endpoint `/api/v1/dashboard/resumen` para que el conteo de cursos refleje los inscritos si el usuario es estudiante:

```java
// En obtenerResumen(), reemplazar:
long cursosActivos = cursoRepository.countByDeletedAtIsNull();

// Por:
String role = authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .filter(a -> a.equals("ROLE_ESTUDIANTE"))
        .findFirst().orElse(null);
long cursosActivos;
if ("ROLE_ESTUDIANTE".equals(role)) {
    cursosActivos = inscripcionRepository.countByUsuarioIdAndDeletedAtIsNull(usuarioId);
} else {
    cursosActivos = cursoRepository.countByDeletedAtIsNull();
}
```

**Nota**: Necesitarás importar `org.springframework.security.core.GrantedAuthority` e inyectar `InscripcionRepository`.

### Subtarea 3.6: Modificar el Dashboard KPI de actividad para estudiantes

**Archivo**: `src/main/java/com/lms/controller/DashboardController.java`

El endpoint `/api/v1/dashboard/actividad` ya filtra por `usuarioId`, por lo que para estudiantes solo mostrará sus propios eventos. Esto está correcto. Sin embargo, estudiantes que nunca hicieron acciones no verán nada. No requiere cambios.

---

## Tarea 4: Agregar navegación desde la tarjeta de curso para estudiante

### Subtarea 4.1: Modificar `fragmentos.html` para estudiantes

**Archivo**: `src/main/resources/templates/cursos/fragmentos.html`

Si se implementa la Tarea 3 (filtrar solo cursos inscritos), los estudiantes verán los cursos correctos. Pero adicionalmente, en las acciones de cada tarjeta de curso, los estudiantes no ven botón "Editar", "Inscribir", etc. Sin embargo, actualmente **todas las acciones están protegidas con `sec:authorize`** excepto "Documentos", "Evaluaciones" y "SCORM", que son visibles para todos.

**Problema**: Un estudiante, aunque vea el enlace a Evaluaciones (correcto), al hacer clic no puede acceder a las evaluaciones porque el JavaScript actual lista evaluaciones pero no permite "rendirlas".

**Solución**: Como las tarjetas ya muestran los enlaces correctos y la Tarea 2.1 ya agrega el botón "Rendir Evaluación" para estudiantes, no se requiere modificar `fragmentos.html` para este propósito. La visibilidad de enlaces está controlada por `sec:authorize`.

---

## Tarea 5: Verificar y corregir posibles errores de seguridad

### Subtarea 5.1: Proteger endpoints de evaluaciones para no-estudiantes

**Archivo**: `src/main/java/com/lms/controller/EvaluacionController.java`

Los endpoints existentes `GET /api/v1/cursos/{cursoId}/evaluaciones` y `GET /api/v1/cursos/{cursoId}/evaluaciones/{evaluacionId}/preguntas` actualmente **no tienen protección de roles**. Cualquier usuario autenticado puede ver las evaluaciones (y sus respuestas correctas). Agregar:

```java
// En listar() y preguntas(), agregar:
@PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR', 'SUPERADMIN')")
```

Y reemplazar con los nuevos endpoints para estudiantes que no exponen respuestas.

Alternativamente, mantener el endpoint existente pero filtrar respuestas según rol. Opción recomendada:

```java
@GetMapping
@PreAuthorize("isAuthenticated()")
public ResponseEntity<?> listar(@PathVariable UUID cursoId, Authentication authentication) {
    UUID usuarioId = securityUtils.getCurrentUserId(authentication);
    String role = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(a -> a.startsWith("ROLE_"))
            .findFirst().orElse("");
    
    List<Evaluacion> evaluaciones = evaluacionService.listarPorCurso(cursoId);
    
    // Si es estudiante, limpiar respuestas correctas
    if ("ROLE_ESTUDIANTE".equals(role)) {
        for (Evaluacion ev : evaluaciones) {
            if (ev.getPreguntas() != null) {
                for (Pregunta p : ev.getPreguntas()) {
                    p.setRespuestaCorrecta(null);
                }
            }
        }
    }
    
    return ResponseEntity.ok(evaluaciones);
}
```

### Subtarea 5.2: Proteger `@DeleteMapping` de evaluaciones

Ya tiene `@PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR', 'SUPERADMIN')")` — correcto.

---

## Resumen de Archivos a Modificar/Crear

| # | Archivo | Acción | Descripción |
|---|---|---|---|
| 1 | `src/main/java/com/lms/service/InscripcionService.java` | **MODIFICAR** | Agregar `isUsuarioInscritoEnCurso()` |
| 2 | `src/main/java/com/lms/repository/InscripcionRepository.java` | **MODIFICAR** | Agregar `findCursosInscritosByUsuarioId()` |
| 3 | `src/main/java/com/lms/controller/EvaluacionController.java` | **MODIFICAR** | Agregar endpoints: `/estudiante`, `/{id}/rendir`, `/{id}/responder`. Modificar `listar()` para filtrar respuestas según rol. |
| 4 | `src/main/java/com/lms/service/CursoService.java` | **MODIFICAR** | Agregar `listarCursosSegunRol()` |
| 5 | `src/main/java/com/lms/controller/CursoHtmxController.java` | **MODIFICAR** | Filtrar cursos según rol del usuario autenticado |
| 6 | `src/main/java/com/lms/controller/DashboardController.java` | **MODIFICAR** | Ajustar KPI de cursos según rol |
| 7 | `src/main/resources/templates/evaluaciones/listado.html` | **MODIFICAR** | Corregir detección de rol y agregar botón "Rendir Evaluación" |
| 8 | `src/main/resources/templates/evaluaciones/rendir.html` | **CREAR** | Vista para que el estudiante rinda la evaluación |
| 9 | `src/main/resources/templates/cursos/fragmentos.html` | **REVISAR** | Verificar que acciones estén correctamente protegidas por rol |
| 10 | `src/main/java/com/lms/dto/RespuestaRequest.java` | **CREAR** (opcional) | DTO para recibir respuestas del estudiante |

## Orden de Implementación Sugerido

1. **Paso 1 (Prioridad Alta)**: Subtarea 1.1 + 1.2 — Agregar validación de inscripción en `InscripcionService`
2. **Paso 2 (Prioridad Alta)**: Subtarea 5.1 — Proteger endpoint de evaluaciones existente y crear endpoint para estudiantes
3. **Paso 3 (Prioridad Alta)**: Subtarea 2.1 — Arreglar detección de rol en `listado.html` y agregar botón "Rendir"
4. **Paso 4 (Prioridad Media)**: Subtarea 2.2 — Crear vista `rendir.html`
5. **Paso 5 (Prioridad Alta)**: Subtarea 3.2 + 3.3 + 3.4 — Filtrar cursos visibles para estudiantes
6. **Paso 6 (Prioridad Media)**: Subtarea 3.5 — Ajustar Dashboard KPI
7. **Paso 7 (Prioridad Baja)**: Subtarea 1.3 + 1.4 — Endpoints `/rendir` y `/responder`
8. **Paso 8 (Prioridad Baja)**: Crear DTOs si es necesario

## Notas Adicionales

- **IMPORTANTE**: La anotación `sec:authorize="hasRole('ESTUDIANTE')"` en Thymeleaf verifica el prefijo `ROLE_` automáticamente, por lo que buscará `ROLE_ESTUDIANTE`. En el código de detección JavaScript por span oculto, el span **no se renderiza** si el usuario no tiene el rol. Sin embargo, el problema era que la variable `esEstudiante` dependía de que el span existiera en el DOM y era evaluada con `!!document.getElementById('user-role-estudiante')`. **Esto debería funcionar correctamente**. Si no funciona, puede deberse a que el span se carga después de que el JavaScript se ejecuta. La solución propuesta (Subtarea 2.1 - Cambio 2) usa atributos `data-*` más confiables.

- Para evitar race conditions, usar `data-*` attributes en lugar de verificar existencia de elementos en el DOM.
