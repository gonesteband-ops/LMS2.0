# 🧾 Plan: Bug 004 — Gestor de Contenidos: unificar vistas, toggle activa y navegación

> Generado del reporte `reportes/bug-004.md`

---

## 1. Resumen del problema

2 issues en el módulo Gestor de Contenidos BPO:

**Issue A — Sidebar unificado:** Existen dos flujos paralelos (`/bpo/gestores` para alumnos, `/admin/bpo/gestores` para admin) que deberían ser uno solo siguiendo el patrón "Mis Cursos". El sidebar solo enlaza a la vista de alumno.

**Issue B — Toggle activa:** El modelo `CuentaBPO` tiene campo `activa` pero no hay toggle rápido desde la UI. Solo se puede cambiar editando el gestor.

---

## 2. Archivos a modificar

| Archivo | Cambio | Tipo |
|---|---|---|
| `src/main/resources/templates/bpo/gestores.html` | Agregar `sec:authorize` para "+ Nuevo Gestor" + badge de estado + botón toggle activa | Template |
| `src/main/resources/templates/bpo/gestor-detalle.html` | Agregar badge de estado (Activo/Inactivo) visible para admin | Template |
| `src/main/java/com/lms/controller/bpo/GestorAlumnoController.java` | Agregar atributo `esAdmin` al modelo (opcional, se puede usar `sec:authorize` directo) | Controller |
| `src/main/java/com/lms/service/bpo/CuentaBPOService.java` | Nuevo método `toggleActiva(UUID id)` | Service |
| `src/main/java/com/lms/controller/bpo/AdminBPOController.java` | Nuevo endpoint `PATCH /gestores/{id}/toggle-activa` + redirigir `/admin/bpo/gestores` → `/bpo/gestores` | Controller |
| `src/main/resources/templates/fragments/sidebar.html` | Agregar link a `/admin/bpo/contenidos` en sección Admin (opcional post-unificación) | Template |

---

## 3. Orden de implementación

1. **CuentaBPOService** — agregar `toggleActiva(UUID id)`
2. **AdminBPOController** — agregar endpoint `PATCH /gestores/{id}/toggle-activa`
3. **bpo/gestores.html** — agregar badge de estado (Activo/Inactivo) + botón toggle (solo admin) + "+ Nuevo Gestor" con `sec:authorize`
4. **bpo/gestor-detalle.html** — agregar badge de estado del gestor
5. **AdminBPOController** — redirigir `/admin/bpo/gestores` → `/bpo/gestores` (o mantener como redirect)
6. Ejecutar tests

---

## 4. Criterios de aceptación

- [ ] **AC1:** Admin ve "+ Nuevo Gestor" en `/bpo/gestores`
- [ ] **AC2:** Admin puede habilitar/deshabilitar gestor con un clic (toggle)
- [ ] **AC3:** El badge de estado (Activo/Inactivo) se ve en el listado y detalle
- [ ] **AC4:** Alumno NO ve botones admin en `/bpo/gestores`
- [ ] **AC5:** `mvn compile` sin errores
- [ ] **AC6:** Todos los tests en `tests/test-bug-004.md` pasan
