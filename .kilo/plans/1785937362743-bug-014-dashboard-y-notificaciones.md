# Bug-014: Tarjetas Dashboard (Certificaciones/Exportaciones) + Notificaciones de inscripción

## Contexto

Sistema LMS (Spring Boot 3.3 + Thymeleaf + HTMX). Se reportan 3 bugs del dashboard y del sistema de notificaciones. Las decisiones fueron confirmadas por el usuario:

1. Tarjeta "Exportaciones" → enlazar a `/dashboard/exportaciones` (NO a certificaciones).
2. Acceso a `/admin/certificaciones/**` → ampliar a `SUPERADMIN`, `ADMIN`, `INSTRUCTOR` (solo ese sub-path).
3. Notificaciones → cubrir 3 eventos: inscripción a curso, creación de evaluación en curso, inscripción a certificación.

Nota: la generación de `new_bug/reportes/bug-014.md` (reporte formal del bug) se hace como Tarea 1 de este plan, ya que el agente de planificación no puede escribir fuera de `plans/`.

---

## Causas raíz

- **Bug 1:** `dashboard/index.html:94` enlaza "Mis Certificaciones" a `/certificaciones` para todos los autenticados, pero `CertificacionAlumnoController` tiene `@PreAuthorize("hasRole('ESTUDIANTE')")` a nivel de clase → no-estudiantes reciben 403. El `GlobalExceptionHandler` (`@RestControllerAdvice`) responde el 403 como JSON.
- **Bug 2:** `dashboard/index.html:72-79` la tarjeta "Exportaciones" es un `<div class="kpi-card">` sin `<a href>`, no clicable.
- **Bug 3:** Solo `NotificacionScheduler` envía notificaciones. `InscripcionService.inscribirUsuarioConFirma()`, `EvaluacionService.crearEvaluacion()` y `CertificacionService.inscribirUsuario()` no llaman a `NotificacionService.enviarNotificacion()`. El frontend de la campanita (`header.html` + `NotificacionController`) ya funciona.

---

## Tareas de implementación

### Tarea 1 — Crear el reporte formal del bug
- Crear `new_bug/reportes/bug-014.md` (carpeta `new_bug/reportes` ya existe) con el contenido del reporte (secciones: Qué pasó, Qué debería pasar, Análisis de código, Decisiones, Estado). El contenido base está en este plan (sección "Contenido del reporte" abajo).
- Crear también `new_bug/planes/plan-bug-014.md` con una copia de este plan (convención del repo: reporte + plan separados).

### Tarea 2 — Bug 1: Redirección por rol en tarjeta "Mis Certificaciones"
**Archivos:**
- `src/main/java/com/lms/controller/HomeController.java` — en `dashboard(Authentication, Model)` (líneas 74-84):
  - Determinar rol: si tiene `ROLE_ESTUDIANTE` → `certificacionesUrl = "/certificaciones"`; si tiene `ROLE_SUPERADMIN`, `ROLE_ADMIN` o `ROLE_INSTRUCTOR` → `certificacionesUrl = "/admin/certificaciones"` (mismo patrón ya usado en HomeController para `esEstudiante`, project-memo T16).
  - `model.addAttribute("certificacionesUrl", certificacionesUrl)`.
- `src/main/resources/templates/dashboard/index.html:94` — cambiar `<a href="/certificaciones" ...>` por `<a th:href="${certificacionesUrl}" ...>` (mantener `sec:authorize="isAuthenticated()"`).

### Tarea 3 — Bug 2: Tarjeta "Exportaciones" clicable
**Archivo:** `src/main/resources/templates/dashboard/index.html:72-79`
- Convertir el `<div class="kpi-card" sec:authorize="hasAnyRole('SUPERADMIN', 'INSTRUCTOR')"> ... </div>` en `<a href="/dashboard/exportaciones" class="kpi-card" style="text-decoration:none;display:block;cursor:pointer;" sec:authorize="hasAnyRole('SUPERADMIN', 'INSTRUCTOR')"> ... </a>`.
- Mantener el `sec:authorize` existente. La página `/dashboard/exportaciones` ya está restringida a esos roles en `SecurityConfig`.

### Tarea 4 — Bug 1+2 (acceso): Ampliar seguridad para /admin/certificaciones
**Archivo:** `src/main/java/com/lms/security/SecurityConfig.java:63`
- Agregar ANTES de `.requestMatchers("/admin/**").hasAnyRole("SUPERADMIN")`:
  ```java
  .requestMatchers("/admin/certificaciones", "/admin/certificaciones/**")
      .hasAnyRole("SUPERADMIN", "ADMIN", "INSTRUCTOR")
  ```
- El orden importa: las reglas se evalúan en orden y gana la primera que matchea. El resto de `/admin/**` permanece solo SUPERADMIN.

### Tarea 5 — Bug 3: Notificaciones al inscribir a un curso
**Archivo:** `src/main/java/com/lms/service/InscripcionService.java`
- Inyectar `NotificacionService` por constructor (no hay ciclo de dependencias: NotificacionService no depende de InscripcionService).
- En `inscribirUsuarioConFirma()` (después del `inscripcionRepository.save(inscripcion)`, antes del `return`):
  ```java
  notificacionService.enviarNotificacion(usuarioId, "sistema",
      "Inscripción a curso",
      "Has sido inscrito al curso " + curso.getTitulo(),
      "/cursos");
  ```
- Nota: NO notificar en el flujo masivo línea a línea duplicado si usa el mismo método (inscribirMasivo ya reutiliza `inscribirUsuario` → notifica una vez por inscripción exitosa, que es el comportamiento deseado).

### Tarea 6 — Bug 3: Notificación al crear evaluación en un curso
**Archivo:** `src/main/java/com/lms/service/EvaluacionService.java`
- Inyectar `NotificacionService` y `InscripcionRepository` por constructor.
- En `crearEvaluacion()` (después de guardar la evaluación, antes del `return`):
  - Obtener inscritos: `inscripcionRepository.findByCursoIdAndDeletedAtIsNull(cursoId)` → para cada inscripción `getUsuario().getId()`.
  - `notificacionService.enviarNotificacion(uidInscrito, "sistema", "Nueva evaluación disponible", "Se ha publicado la evaluación " + titulo + " (" + codigo + ") en el curso " + curso.getTitulo(), "/cursos")`.
  - Proteger el loop con try/catch por notificación para no romper la creación de la evaluación si falla un envío (log.error).
  - Evitar notificar al autor si está inscrito (opcional; no es crítico).

### Tarea 7 — Bug 3: Notificación al inscribir en certificación
**Archivo:** `src/main/java/com/lms/service/CertificacionService.java`
- Inyectar `NotificacionService` por constructor (no hay ciclo: NotificacionService no depende de CertificacionService).
- En `inscribirUsuario()` (después de `certUsuarioRepo.save(cu)`, antes del audit):
  ```java
  notificacionService.enviarNotificacion(usuarioId, "sistema",
      "Certificación asignada",
      "Has sido inscrito en la certificación " + cert.getTitulo() + " (" + cert.getCodigo() + ")",
      "/certificaciones");
  ```
- `inscribirMasivo()` ya reutiliza `inscribirUsuario()` → notifica una vez por éxito.

### Tarea 8 — Compilación y tests
- `mvn compile -DskipTests` → BUILD SUCCESS.
- `mvn test` (suite existente: 31+ tests). Verificar especialmente que no rompan `InscripcionServiceTest`, `EvaluacionServiceTest`, `HomeControllerTest` (los constructores cambian → actualizar mocks/`@MockBean` si los tests instancian los servicios).
- Si hay tests que construyen los servicios manualmente, agregar el mock de `NotificacionService` (y `InscripcionRepository` en EvaluacionServiceTest).

---

## Flujo de datos (notificaciones)

Inscripción curso → `InscripcionService` → `NotificacionService.enviarNotificacion` → tabla `notificaciones` (leida=false) → `header.html` campanita hace GET `/api/v1/notificaciones/no-leidas` y `/contar` cada 60s → badge rojo + dropdown.

---

## Modos de fallo y mitigaciones

| Fallo | Mitigación |
|-------|------------|
| `NotificacionService` causa dependencia circular | Verificado: no depende de los servicios modificados. |
| Notificar al crear evaluación rompe el guardado | try/catch por usuario en el loop; log.error. |
| Tests de servicios con constructores modificados | Agregar mocks; ejecutar suite completa. |
| Regla `/admin/certificaciones/**` pisada por `/admin/**` | Insertar la regla ANTES de la genérica en SecurityConfig. |
| Inyección duplicada en constructores | Revisar que no existan ya los campos inyectados antes de agregarlos. |

---

## Validación manual

1. **Bug 1:** Login `admin@lms.cl` (SUPERADMIN) → `/dashboard` → clic "Mis Certificaciones" → debe abrir `/admin/certificaciones` sin 403. Login `estudiante@lms.cl` → clic "Mis Certificaciones" → `/certificaciones` (sin cambios).
2. **Bug 1 extra:** Login `instructor@lms.cl` → clic "Mis Certificaciones" → `/admin/certificaciones` (ahora permitido por Tarea 4).
3. **Bug 2:** Login `admin@lms.cl` → clic "Exportaciones" → `/dashboard/exportaciones`.
4. **Bug 3:** Con admin, inscribir a `estudiante@lms.cl` en un curso publicado → login del estudiante → campanita muestra "Inscripción a curso". Crear una evaluación en ese curso → el estudiante recibe "Nueva evaluación disponible". Inscribir al estudiante en una certificación → recibe "Certificación asignada".
5. Verificar que el badge de la campanita se actualice (refresh de 60s o recarga de página).

---

## Entorno

- Levantar: `docker compose -f docker/docker-compose.java.yml up -d` y ver `http://localhost:8080` (perfil development, DevTools hot-reload).
- Usuarios seed: `admin@lms.cl` / `Demo1234!` (SUPERADMIN), `instructor@lms.cl` / `Demo1234!`, `estudiante@lms.cl` / `Demo1234!` (configurables por variables de entorno `SEED_*_PASSWORD`).

---

## Contenido del reporte (para `new_bug/reportes/bug-014.md`)

# Bug-014: Tarjetas del Dashboard (Certificaciones/Exportaciones) y notificaciones de inscripción

## 1. Que paso?

### Bug 1: Tarjeta "Mis Certificaciones" responde 403 Forbidden (JSON) en lugar de una pagina HTML

En `/dashboard`, la tarjeta **"Mis Certificaciones"** lleva a `/certificaciones`. Al hacer clic, el servidor responde un JSON de error en vez de la interfaz:

```json
{"status":403,"error":"Forbidden","message":"No tiene permisos para acceder a este recurso","path":"/certificaciones","timestamp":"2026-08-05T09:47:50.81562409","details":[]}
```

**Causa:** la tarjeta usa `sec:authorize="isAuthenticated()"` y apunta a `/certificaciones`, pero `CertificacionAlumnoController` tiene `@PreAuthorize("hasRole('ESTUDIANTE')")`. Todo no-estudiante recibe `AccessDeniedException`, que `GlobalExceptionHandler` devuelve como JSON 403. Para alumnos funciona.

### Bug 2: Tarjeta "Exportaciones" no hace nada al hacer clic

La tarjeta "Exportaciones" es un `<div class="kpi-card">` sin `<a href>` en `dashboard/index.html` (líneas 72-79), a diferencia del resto de tarjetas. No es clicable. Debe enlazar a `/dashboard/exportaciones` (página de reportes XLSX), visible solo para SUPERADMIN/INSTRUCTOR.

### Bug 3: Inscripción a curso / asignación de evaluación / certificación no genera notificación

Solo `NotificacionScheduler` envía notificaciones. `InscripcionService.inscribirUsuarioConFirma()`, `EvaluacionService.crearEvaluacion()` y `CertificacionService.inscribirUsuario()` no llaman a `enviarNotificacion()`. La campanita del header ya funciona; falta generar las notificaciones en backend.

## 2. Que deberia pasar?

| Bug | Comportamiento esperado |
|-----|--------------------------|
| 1 | Tarjeta "Mis Certificaciones": ESTUDIANTE → `/certificaciones`; SUPERADMIN/ADMIN/INSTRUCTOR → `/admin/certificaciones`. Sin 403. |
| 2 | Tarjeta "Exportaciones" clicable → `/dashboard/exportaciones` (solo SUPERADMIN/INSTRUCTOR). |
| 3 | Notificaciones tipo "sistema" con enlace: inscripción a curso (link `/cursos`), nueva evaluación en curso (link `/cursos`), certificación asignada (link `/certificaciones`). |

## 3. Archivos afectados

- `src/main/resources/templates/dashboard/index.html` (tarjetas líneas 72-79 y 94)
- `src/main/java/com/lms/controller/HomeController.java` (`dashboard()`, líneas 74-84)
- `src/main/java/com/lms/security/SecurityConfig.java` (regla `/admin/certificaciones/**`, línea 63)
- `src/main/java/com/lms/service/InscripcionService.java` (`inscribirUsuarioConFirma`, línea 161)
- `src/main/java/com/lms/service/EvaluacionService.java` (`crearEvaluacion`, línea 52)
- `src/main/java/com/lms/service/CertificacionService.java` (`inscribirUsuario`, línea 193)

## 4. Estado

| Bug | Estado |
|-----|--------|
| 1 | Pendiente de implementación |
| 2 | Pendiente de implementación |
| 3 | Pendiente de implementación |
