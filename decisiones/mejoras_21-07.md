# Plan de Mejoras — 21-Jul-2026

**Estado actual:** Commit `2d8f616` en rama `main`
**Problemas confirmados:** 3 bugs reales (de 5 identificados inicialmente)

---

## Bug 1 — Conflicto de beans RedisTemplate ✅ RESUELTO

### Causa
Spring encuentra 2 beans del tipo `RedisTemplate<String, String>`:
- `redisStringTemplate` → definido en `RedisConfig.java`
- `stringRedisTemplate` → definido en `RedisAutoConfiguration`

### Solucion aplicada
**Archivo:** `src/main/java/com/lms/config/RedisConfig.java`
**Linea 36:** Se agrego `@Primary` al metodo `redisStringTemplate()` para que Spring lo use por defecto.

---

## Bug 2 — Exportaciones no se procesan (quedan "pendiente") <PENDIENTE>

### Causa
`ExportacionService.solicitarExportacion()` guarda la exportacion con estado "pendiente" y publica un mensaje en Redis. Si Redis no esta disponible (produccion o local sin Redis), el mensaje nunca se publica y la exportacion queda "pendiente" para siempre. No hay fallback sincrono.

### Archivo
`src/main/java/com/lms/service/ExportacionService.java` (linea ~162-188)

### Solucion propuesta
Agregar `boolean redisDisponible = false;` antes del bloque Redis. Marcar `redisDisponible = true` en cada rama donde se publique exitosamente. Despues del bloque, si `!redisDisponible`, llamar a `procesarExportacion(exportacionId)` de forma sincrona.

---

## Bug 3 — CSRF residual en head.html <PENDIENTE>

### Causa
`SecurityConfig.java` tiene `csrf.disable()`, pero `head.html` aun contiene:
- Meta tags CSRF (lineas 35-36)
- Handler `htmx:configRequest` que intenta setear header CSRF (lineas 45-58)

### Archivo
`src/main/resources/templates/fragments/head.html` (lineas 34-58)

### Solucion propuesta
Eliminar las lineas 34-36 (meta tags) y las lineas 45-58 (handler JS).

---

## Bugs descartados (no existen en el codigo actual)

| Bug | Motivo |
|-----|--------|
| **4. Age validation admin** | `crearUsuarioAdmin()` ya tiene la validacion de edad (lineas 82-88). Fue implementada en sesion anterior y sobrevivio al reset. |
| **5. Checkbox SVG** | `consentimiento.html` usa checkboxes nativos de HTML (`<input type="checkbox">`), no hay SVG dinamico ni errores de renderizado. |

---

## Arbol completo del proyecto (src/main)

```
src/main/
+-- java/com/lms/
|   +-- config/
|   |   +-- DataInitializer.java
|   |   +-- FilterConfig.java
|   |   +-- GlobalControllerAdvice.java
|   |   +-- MinioConfig.java
|   |   +-- OpenTelemetryConfig.java
|   |   +-- RedisConfig.java             ← Bug 1 (RESUELTO)
|   |   +-- ThymeleafConfig.java
|   |   +-- WebConfig.java
|   +-- controller/
|   |   +-- AdminHtmxController.java
|   |   +-- ArcopController.java
|   |   +-- AuditoriaController.java
|   |   +-- AuthController.java
|   |   +-- ConsentimientoController.java
|   |   +-- CursoController.java
|   |   +-- CursoHtmxController.java
|   |   +-- DashboardController.java
|   |   +-- DocumentoController.java
|   |   +-- EvaluacionController.java
|   |   +-- ExportacionController.java
|   |   +-- FirmaController.java
|   |   +-- HomeController.java
|   |   +-- IncidentesController.java
|   |   +-- InscripcionController.java
|   |   +-- NotificacionController.java
|   |   +-- PublicController.java
|   |   +-- RolController.java
|   |   +-- ScormController.java
|   |   +-- UsuarioController.java
|   +-- dto/ (20 archivos)
|   +-- exception/
|   |   +-- GlobalExceptionHandler.java
|   +-- model/ (18 archivos)
|   +-- repository/ (19 archivos)
|   +-- security/
|   |   +-- DatabaseUserDetailsService.java
|   |   +-- FormLoginSuccessHandler.java
|   |   +-- JwtAuthenticationFilter.java
|   |   +-- RateLimitFilter.java
|   |   +-- SecurityConfig.java
|   |   +-- UserPrincipal.java
|   +-- service/
|   |   +-- ArcopService.java
|   |   +-- AuditoriaCleanupService.java
|   |   +-- AuthService.java
|   |   +-- BreachDetectionService.java
|   |   +-- CodigoGeneratorService.java
|   |   +-- ConsentimientoService.java
|   |   +-- CursoService.java
|   |   +-- DocumentoService.java
|   |   +-- EliminacionCleanupService.java
|   |   +-- EvaluacionService.java
|   |   +-- ExportacionService.java       ← Bug 2
|   |   +-- ExportacionWorker.java
|   |   +-- FirmaService.java
|   |   +-- IncidenteSeguridadService.java
|   |   +-- InscripcionService.java
|   |   +-- IntentoEvaluacionService.java
|   |   +-- JwtService.java
|   |   +-- MfaService.java
|   |   +-- NotificacionScheduler.java
|   |   +-- NotificacionService.java
|   |   +-- RolService.java
|   |   +-- ScormRuntimeService.java
|   |   +-- ScormService.java
|   |   +-- UsuarioService.java
|   +-- util/ (6 archivos)
+-- resources/
    +-- application.yml
    +-- db/migration/ (18 archivos V1-V19)
    +-- static/css/ (5 archivos)
    +-- static/js/ (3 archivos)
    +-- templates/
        +-- admin/ (7 archivos)
        +-- auth/ (3 archivos)
        +-- cursos/ (6 archivos)
        +-- dashboard/ (9 archivos)
        +-- evaluaciones/ (3 archivos)
        +-- fragments/
        |   +-- head.html                  ← Bug 3
        |   +-- header.html
        |   +-- footer.html
        +-- public/ (3 archivos)
        +-- index.html
```

---

## Orden de ejecucion

1. ~~Bug 1 (RESUELTO)~~
2. Bug 3 — CSRF residual (facil, solo eliminar lineas)
3. Bug 2 — Exportaciones (logica de negocio, requiere prueba)

## Bug 4 — Sidebar inconsistente entre páginas (NUEVO)

### Causa
Cada template HTML tiene su propia copia del sidebar (dashboard/index.html, dashboard/perfil.html, dashboard/firmas.html, dashboard/privacidad.html, dashboard/documentos.html, dashboard/arcop.html, cursos/listado.html, evaluaciones/listado.html, admin/usuarios.html, admin/roles.html, admin/auditoria.html, admin/incidentes.html).

Cuando se agrega un nuevo link (ej: "Mis Datos (ARCOP)"), solo se actualiza en algunas páginas. Al navegar entre vistas, los links aparecen/desaparecen.

### Solucion propuesta
**Crear un fragmento compartido** `fragments/sidebar.html` con el sidebar UNA SOLA VEZ, y que cada template lo incluya via `th:replace`.

### Archivos a modificar

**NUEVO:** `src/main/resources/templates/fragments/sidebar.html`
- Contiene TODO el HTML del sidebar (navigation links, seccion administracion)
- Recibe un parametro `seccion` para marcar el link activo

**MODIFICAR:** los siguientes templates para REEMPLAZAR su sidebar inline por `th:replace`:
- `dashboard/index.html`
- `dashboard/perfil.html`
- `dashboard/firmas.html`
- `dashboard/privacidad.html`
- `dashboard/documentos.html`
- `dashboard/arcop.html`
- `dashboard/exportaciones.html`
- `cursos/listado.html`
- `cursos/participantes.html`
- `evaluaciones/listado.html`
- `admin/usuarios.html`
- `admin/roles.html`
- `admin/auditoria.html`
- `admin/incidentes.html`

### Que hacer exactamente

1. Crear `fragments/sidebar.html` con el bloque completo del sidebar (desde `<aside class="dashboard-sidebar">` hasta `</aside>`). Todos los links fijos, con `sec:authorize` segun corresponda. El link activo se marca comparando `${seccion}` con el nombre de la seccion.

2. En cada template, REEMPLAZAR el bloque `<aside class="dashboard-sidebar">...</aside>` por:
   ```html
   <div th:replace="~{fragments/sidebar :: sidebar(seccion='dashboard')}"></div>
   ```
   (cambiando `'dashboard'` por el nombre de la seccion correspondiente: `'cursos'`, `'evaluaciones'`, `'admin-usuarios'`, etc.)

3. Verificar que `sec:authorize="hasAnyRole(...)"` se mantenga en el fragmento.

4. Eliminar las sidebar-section-title duplicadas.

### Criterios de aceptacion
- [ ] "Mis Datos (ARCOP)" aparece en TODAS las paginas del dashboard
- [ ] "Exportaciones" aparece solo para ADMIN/INSTRUCTOR/SUPERADMIN
- [ ] "Administracion" aparece solo para ADMIN/SUPERADMIN
- [ ] El link activo se marca correctamente en cada pagina
- [ ] No hay regresion visual (los estilos se mantienen)


## Bug 5 — Editor de curso: estado siempre "borrador" y datos se borran al editar

### Descripcion del problema

**Problema 1:** En el editor de curso (`/cursos/editar?id=X`), al cambiar el estado a "Publicado" o "Archivado" y guardar, el curso siempre vuelve a "Borrador". El backend ignora el campo estado.

**Problema 2:** Al editar un curso existente, los datos se borran porque el frontend no envia el campo `estado` en el payload JSON al backend. El backend recibe `estado = null` y no lo actualiza. Ademas, algunos campos que se cargan correctamente en el formulario no se reenvian al guardar.

### Causa raiz (2 archivos)

**Frontend — `editor.html`**
- Linea 659: se lee `var estado = document.getElementById(''estado'').value;`
- Lineas 685-693: el payload JSON NO incluye `estado`:
```javascript
var payload = {
    titulo: titulo,
    descripcion: descripcion,
    categoria: categoria,
    fechaLimite: ...,
    contenido: contenido,
    motivoCambio: motivoCambio,
    requiereFirmaCumplimiento: ...
};
// FALTA: estado: estado
```

**Backend — `CursoRequest.java`**
- El record no tiene campo `estado`. Solo tiene: titulo, descripcion, categoria, contenido, motivoCambio, fechaLimite, requiereFirmaCumplimiento.
- Aunque el frontend enviara `estado`, el backend lo ignoraria porque el DTO no lo define.

**Backend — `CursoService.java`**
- `actualizarCurso()` (lineas 120-126): actualiza titulo, descripcion, categoria, fechaLimite, requiereFirma, hashSha256. **NO actualiza estado** porque el DTO no tiene ese campo.
- `crearCurso()` (linea 70): hardcodea `estado("borrador")` en vez de leer del DTO.

### Archivos a modificar (3 archivos)

```
src/main/resources/templates/cursos/editor.html              ← Frontend payload
src/main/java/com/lms/dto/CursoRequest.java                   ← DTO falta campo estado
src/main/java/com/lms/service/CursoService.java               ← Logica ignora estado
```

### Paso a paso para el agente

#### Paso 1 — CursoRequest.java (DTO)
**Ruta:** `src/main/java/com/lms/dto/CursoRequest.java`

**Que hacer:** Agregar el campo `String estado` al record, despues de `boolean requiereFirmaCumplimiento`.

**Codigo exacto (record completo):**
```java
public record CursoRequest(
        @NotBlank(message = "El titulo es obligatorio")
        @Size(min = 3, max = 255, message = "El titulo debe tener entre 3 y 255 caracteres")
        String titulo,

        @Size(max = 2000, message = "La descripcion no debe exceder 2000 caracteres")
        String descripcion,

        @NotBlank(message = "La categoria es obligatoria")
        String categoria,

        String contenido,

        String motivoCambio,

        String fechaLimite,

        boolean requiereFirmaCumplimiento,

        String estado    // ← NUEVO: "borrador", "publicado" o "archivado"
) {}
```

**Validacion:** No poner `@NotBlank` ni otras anotaciones en `estado` porque es opcional (si es null, se usa "borrador" por defecto).

#### Paso 2 — CursoService.java (logica de negocio)
**Ruta:** `src/main/java/com/lms/service/CursoService.java`

**Sub-paso 2a — Metodo `crearCurso()` (linea ~61):**
Cambiar la linea 70 de:
```java
.estado("borrador")
```
a:
```java
.estado(request.estado() != null ? request.estado() : "borrador")
```

**Sub-paso 2b — Metodo `actualizarCurso()` (linea ~108):**
Agregar `curso.setEstado(request.estado());` DESPUES de la linea 123 (`curso.setVersionActual(nuevaVersion);`) y ANTES de la linea 125 (`curso.setRequiereFirmaCumplimiento(...)`).

El bloque debe quedar asi:
```java
curso.setTitulo(request.titulo().trim());
curso.setDescripcion(request.descripcion() != null ? request.descripcion().trim() : null);
curso.setVersionActual(nuevaVersion);
curso.setEstado(request.estado());                              // ← AGREGAR
curso.setCategoria(request.categoria());
curso.setFechaLimite(parseFechaLimite(request.fechaLimite()));
curso.setRequiereFirmaCumplimiento(request.requiereFirmaCumplimiento());
curso.setHashSha256(HashUtils.sha256(request.titulo() + "|" + nuevaVersion));
```

#### Paso 3 — editor.html (frontend)
**Ruta:** `src/main/resources/templates/cursos/editor.html`

**Sub-paso 3a — Buscar la variable `var payload` (linea ~685):**
Agregar `estado: estado,` en el objeto payload, justo despues de `categoria: categoria,`.

El payload debe quedar asi:
```javascript
var payload = {
    titulo: titulo,
    descripcion: descripcion,
    categoria: categoria,
    estado: estado,
    fechaLimite: document.getElementById('fechaLimite').value || null,
    contenido: contenido,
    motivoCambio: motivoCambio,
    requiereFirmaCumplimiento: document.getElementById('requiereFirmaCumplimiento').checked
};
```

**Sub-paso 3b — Verificar que la linea 659 (`var estado = ...`) EXISTE.**
Si no existe, agregarla DESPUES de `var categoria = document.getElementById(''categoria'').value;`:
```javascript
var estado = document.getElementById(''estado'').value;
```

### Criterios de aceptacion
- [ ] Al crear un curso con estado "publicado", aparece como "Publicado" inmediatamente
- [ ] Al editar un curso y cambiar estado a "Archivado", se guarda como "Archivado"
- [ ] Al editar un curso y NO cambiar el estado, el estado existente NO se pierde
- [ ] `mvn compile -DskipTests` sin errores
- [ ] Los datos del curso (titulo, descripcion, contenido) NO se borran al editar

### Archivos NO modificados
- CursoController.java (no necesita cambios, recibe el DTO y lo pasa al service)
- Curso.java (el modelo ya tiene campo `estado`, no se toca)

