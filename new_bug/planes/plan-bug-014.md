# Plan Bug-014: Tarjetas Dashboard (Certificaciones/Exportaciones) + Notificaciones de inscripción

---

## Objetivo

Corregir 3 bugs del dashboard y del sistema de notificaciones del LMS:

1. **Bug 1:** Tarjeta "Mis Certificaciones" responde 403 JSON a no-estudiantes. Debe redirigir según rol: ESTUDIANTE → `/certificaciones`; SUPERADMIN/ADMIN/INSTRUCTOR → `/admin/certificaciones`.
2. **Bug 2:** Tarjeta "Exportaciones" no es clicable. Debe enlazar a `/dashboard/exportaciones`.
3. **Bug 3:** No llegan notificaciones a la campanita al inscribir en curso, crear evaluación o inscribir en certificación.

---

## Causas raíz

- **Bug 1:** `dashboard/index.html:94` enlaza "Mis Certificaciones" a `/certificaciones` para todos los autenticados, pero `CertificacionAlumnoController` tiene `@PreAuthorize("hasRole('ESTUDIANTE')")` a nivel de clase → no-estudiantes reciben 403. El `GlobalExceptionHandler` (`@RestControllerAdvice`) responde el 403 como JSON.
- **Bug 2:** `dashboard/index.html:72-79` la tarjeta "Exportaciones" es un `<div class="kpi-card">` sin `<a href>`, no clicable.
- **Bug 3:** Solo `NotificacionScheduler` envía notificaciones. `InscripcionService.inscribirUsuarioConFirma()`, `EvaluacionService.crearEvaluacion()` y `CertificacionService.inscribirUsuario()` no llaman a `NotificacionService.enviarNotificacion()`. El frontend de la campanita (`header.html` + `NotificacionController`) ya funciona.

---

## Tareas de implementación

### Tarea 1 — Bug 1: Redirección por rol en tarjeta "Mis Certificaciones"
**Archivos:**
- `src/main/java/com/lms/controller/HomeController.java` — en `dashboard(Authentication, Model)`:
  - Determinar rol desde `authentication.getAuthorities()` (mismo patrón ya usado para `esEstudiante` en HomeController, project-memo T16).
  - `ROLE_ESTUDIANTE` → `/certificaciones`; `ROLE_SUPERADMIN`/`ROLE_ADMIN`/`ROLE_INSTRUCTOR` → `/admin/certificaciones`.
  - `model.addAttribute("certificacionesUrl", certificacionesUrl)`.
- `src/main/resources/templates/dashboard/index.html:94` — `<a th:href="${certificacionesUrl}">` manteniendo `sec:authorize="isAuthenticated()"`.

### Tarea 2 — Bug 2: Tarjeta "Exportaciones" clicable
**Archivo:** `src/main/resources/templates/dashboard/index.html:72-79`
- Convertir `<div class="kpi-card" ...>` en `<a href="/dashboard/exportaciones" class="kpi-card" style="text-decoration:none;display:block;cursor:pointer;" ...>` y cerrar con `</a>`.
- Mantener `sec:authorize="hasAnyRole('SUPERADMIN', 'INSTRUCTOR')"`.

### Tarea 3 — Bug 1+2 (acceso): Ampliar seguridad /admin/certificaciones
**Archivo:** `src/main/java/com/lms/security/SecurityConfig.java:63`
- Agregar ANTES de `.requestMatchers("/admin/**").hasAnyRole("SUPERADMIN")`:
  ```java
  .requestMatchers("/admin/certificaciones", "/admin/certificaciones/**")
      .hasAnyRole("SUPERADMIN", "ADMIN", "INSTRUCTOR")
  ```
- El orden importa: gana la primera regla que matchea.

### Tarea 4 — Bug 3: Notificación al inscribir a un curso
**Archivo:** `src/main/java/com/lms/service/InscripcionService.java`
- Inyectar `NotificacionService` por constructor (sin ciclo de dependencias).
- En `inscribirUsuarioConFirma()` tras el `save(inscripcion)`:
  - `notificacionService.enviarNotificacion(usuarioId, "sistema", "Inscripción a curso", "Has sido inscrito al curso " + curso.getTitulo(), "/cursos")`.
- `inscribirMasivo()` reutiliza `inscribirUsuario()` → notifica una vez por inscripción exitosa (deseado).

### Tarea 5 — Bug 3: Notificación al crear evaluación
**Archivo:** `src/main/java/com/lms/service/EvaluacionService.java`
- Inyectar `NotificacionService` y `InscripcionRepository` por constructor.
- En `crearEvaluacion()` tras guardar: obtener `inscripcionRepository.findByCursoIdAndDeletedAtIsNull(cursoId)`, notificar a cada inscrito con try/catch por usuario (no romper la creación).
  - Mensaje: "Se ha publicado la evaluación {titulo} ({codigo}) en el curso {curso}".

### Tarea 6 — Bug 3: Notificación al inscribir en certificación
**Archivo:** `src/main/java/com/lms/service/CertificacionService.java`
- Inyectar `NotificacionService` por constructor (sin ciclo de dependencias).
- En `inscribirUsuario()` tras `certUsuarioRepo.save(cu)`:
  - `notificacionService.enviarNotificacion(usuarioId, "sistema", "Certificación asignada", "Has sido inscrito en la certificación {titulo} ({codigo})", "/certificaciones")`.

### Tarea 7 — Compilación y tests
- `mvn compile -DskipTests` → BUILD SUCCESS.
- `mvn test` — revisar `InscripcionServiceTest`, `EvaluacionServiceTest`, `HomeControllerTest`; actualizar mocks si construyen servicios con los constructores modificados.
- Reiniciar contenedor `docker compose -f docker/docker-compose.java.yml restart api` para recargar.

---

## Validación manual

1. **Bug 1:** Login `admin@lms.cl` → `/dashboard` → "Mis Certificaciones" → `/admin/certificaciones` sin 403. Login `estudiante@lms.cl` → "Mis Certificaciones" → `/certificaciones`.
2. **Bug 1 extra:** Login `instructor@lms.cl` → "Mis Certificaciones" → `/admin/certificaciones` (permitido por Tarea 3).
3. **Bug 2:** Login `admin@lms.cl` → "Exportaciones" → `/dashboard/exportaciones`.
4. **Bug 3:** Inscribir a `estudiante@lms.cl` en curso publicado → campanita "Inscripción a curso". Crear evaluación en ese curso → "Nueva evaluación disponible". Inscribir en certificación → "Certificación asignada".

---

## Riesgos y mitigaciones

| Riesgo | Mitigación |
|--------|------------|
| Dependencia circular con NotificacionService | Verificado: no depende de los servicios modificados |
| Notificación falla y rompe creación de evaluación | try/catch por usuario, log.error |
| Tests con constructores modificados | Agregar mocks; correr suite completa |
| Regla `/admin/certificaciones/**` pisada por `/admin/**` | Insertar regla ANTES de la genérica |

---

## Estado de implementación (05-08-2026)

**COMPLETADO.** Todos los cambios aplicados y verificados:

- **Bug 1:** `HomeController.dashboard()` calcula `certificacionesUrl` por rol; tarjeta usa `th:href`.
- **Bug 2:** Tarjeta "Exportaciones" convertida a `<a href="/dashboard/exportaciones">`.
- **Acceso:** SecurityConfig permite `/admin/certificaciones/**` a SUPERADMIN/ADMIN/INSTRUCTOR.
- **Bug 3:** Notificaciones en inscripcion a curso, creacion de evaluacion (a todos los inscritos) e inscripcion a certificacion, con try/catch por usuario.
- **Tests:** `EvaluacionServiceTest` + `InscripcionServiceTest` = 24/24 OK. Suite completa identica a la linea base (sin regresiones).

**Pendiente:** validacion manual en navegador tras reiniciar el contenedor API.
