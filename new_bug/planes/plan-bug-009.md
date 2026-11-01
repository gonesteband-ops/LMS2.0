# 🧾 Plan: Bug 009 — Certificaciones: acceso post-completado, sin seguridad, sin integridad

> Generado del reporte `reportes/bug-009.md`

---

## 1. Resumen del problema

7 fallos en Certificaciones que requieren cambios en controladores, modelos, template y migración.

---

## 2. Archivos a modificar

| Archivo | Cambio | Tipo |
|---|---|---|
| `src/main/java/com/lms/controller/CertificacionAlumnoController.java:47-67` | Agregar `@PreAuthorize("hasRole('ESTUDIANTE')")` + verificar inscripción y estado COMPLETADO en `rendir()` | Controller |
| `src/main/java/com/lms/model/CertificacionUsuario.java` | Agregar `deletedAt`, `deletedBy`, `reason`, `hashSha256` + `@SQLRestriction` | Model |
| `src/main/resources/db/migration/V31__certificaciones_soft_delete_hash.sql` | **CREAR** — Agregar columnas soft-delete + hash a `certificaciones_usuarios` | Migración |
| `src/main/resources/templates/certificaciones/mis-certificaciones.html:48-51` | Separar ruta "Ver Resultado" a `/{id}/resultado` | Template |
| `src/main/java/com/lms/controller/AdminCertificacionController.java:36` | `listar()` mostrar todas las certificaciones (no solo activas) | Controller |

---

## 3. Orden de implementación

1. Migración V31 — columnas soft-delete + hash en `certificaciones_usuarios`
2. `CertificacionUsuario` — agregar campos + `@SQLRestriction`
3. `AdminCertificacionController.listar()` — mostrar todas
4. `CertificacionAlumnoController` — `@PreAuthorize` + verificación en `rendir()`
5. `mis-certificaciones.html` — separar "Ver Resultado"

---

## 4. Criterios de aceptación

- [ ] **AC1:** `rendir()` retorna 403 si el usuario no está inscrito
- [ ] **AC2:** `rendir()` muestra resultado si el usuario ya completó (no permite rendir de nuevo)
- [ ] **AC3:** `CertificacionAlumnoController` tiene `@PreAuthorize("hasRole('ESTUDIANTE')")`
- [ ] **AC4:** `CertificacionUsuario` tiene soft-delete + hash
- [ ] **AC5:** Admin ve todas las certificaciones (activas e inactivas)
- [ ] **AC6:** "Ver Resultado" va a ruta separada
- [ ] **AC7:** `mvn compile` sin errores
- [ ] **AC8:** Todos los tests en `tests/test-bug-009.md` pasan
