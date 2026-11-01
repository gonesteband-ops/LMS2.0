# 🧾 Plan: Bug 005 — Seguridad BPO: endpoints expuestos, authorities rotas, sin @PreAuthorize

> Generado del reporte `reportes/bug-005.md`

---

## 1. Resumen del problema

6 fallos de seguridad en el módulo BPO que permiten a estudiantes acceder a funciones administrativas, authorities rotas que nunca matchean, y endpoints sin control de acceso.

---

## 2. Archivos a modificar

| Archivo | Cambio | Tipo |
|---|---|---|
| `src/main/java/com/lms/security/SecurityConfig.java:63` | Agregar matcher para `/bpo/admin/**` con `hasAnyRole('SUPERADMIN', 'BPO_ADMIN')` | Config |
| `src/main/java/com/lms/security/UserPrincipal.java:50-51` | Cambiar `getAuthorities()` para usar `permiso.getNombre()` en lugar de `recurso + "_" + accion` | Security |
| `src/main/java/com/lms/controller/bpo/ContenidoBPOApiController.java:40` | Cambiar `@PreAuthorize` a `hasAuthority('BPO_CONTENIDO_IMPORTAR')` (ahora coincidirá con el fix de UserPrincipal) | Controller |
| `src/main/java/com/lms/controller/bpo/GestorAlumnoController.java:72-78` | Agregar verificación de asignación usuario-cuenta en `verPublicacion()` | Controller |
| `src/main/java/com/lms/controller/bpo/AdminBPOController.java:337-343` | Mover lógica de `removerCompartir()` a `CuentaBPOService` con auditoría | Controller |
| `src/main/java/com/lms/service/bpo/CuentaBPOService.java` | Nuevo método `removerAsignacionUsuario()` con auditoría | Service |
| `src/main/java/com/lms/controller/bpo/AdminBPOController.java:73-76` | Cambiar `listarGestores()` para pasar todas las cuentas activas al modelo en vez de redirigir | Controller |

---

## 3. Orden de implementación

1. `UserPrincipal.getAuthorities()` — cambiar a `permiso.getNombre()`
2. `ContenidoBPOApiController` — el `@PreAuthorize` ahora funciona automáticamente con el fix anterior
3. `SecurityConfig` — agregar matcher `/bpo/admin/**`
4. `CuentaBPOService` — nuevo método `removerAsignacionUsuario()`
5. `AdminBPOController.removerCompartir()` — delegar al servicio
6. `AdminBPOController.listarGestores()` — pasar cuentas activas directamente
7. `GestorAlumnoController.verPublicacion()` — agregar ownership check

---

## 4. Criterios de aceptación

- [ ] **AC1:** ESTUDIANTE no puede acceder a `POST /bpo/admin/contenidos`
- [ ] **AC2:** `@PreAuthorize("hasAuthority('BPO_CONTENIDO_IMPORTAR')")` funciona correctamente
- [ ] **AC3:** `removerCompartir()` pasa por servicio con auditoría
- [ ] **AC4:** `verPublicacion()` retorna 404 si el usuario no tiene acceso
- [ ] **AC5:** `mvn compile` sin errores
- [ ] **AC6:** Todos los tests en `tests/test-bug-005.md` pasan
