# 🧾 Plan: Bug 002 — UI de incidentes pobre + roles redundantes

> Generado del reporte `reportes/bug-002.md`

---

## 1. Resumen del problema

Dos tareas independientes:
- **A:** Rediseñar `/admin/incidentes` con tabla, filtros y botón Resolver
- **B:** Limpiar roles: eliminar ADMIN, BPO_ADMIN, BPO_SUPERVISOR, ROL DE PRUEBA; migrar sus usuarios a SUPERADMIN; agregar permisos de certificaciones a ESTUDIANTE

---

## 2. Archivos a modificar

### Tarea A — Incidentes UI

| Archivo | Cambio | Tipo |
|---|---|---|
| `src/main/resources/templates/admin/incidentes.html` | Rediseño completo con tabla, filtros y botón Resolver | Template |

### Tarea B — Limpieza de Roles

| Archivo | Cambio | Tipo |
|---|---|---|
| `src/main/resources/db/migration/V30__limpieza_roles_y_permisos.sql` | Migración: eliminar 4 roles, agregar permisos certificaciones | SQL |
| `src/main/java/com/lms/security/SecurityConfig.java:63-69` | Cambiar `hasAnyRole("ADMIN", ...)` por `hasAnyRole("SUPERADMIN", ...)` | Config |
| ~25 controladores Java | Cambiar `'ADMIN'` por `'SUPERADMIN'` en `@PreAuthorize` | Controller |
| ~50 templates HTML | Cambiar `hasAnyRole('ADMIN',...)` por `hasAnyRole('SUPERADMIN',...)` | Template |
| `src/main/resources/templates/fragments/sidebar-gestor.html:1` | Eliminar referencias a BPO_ADMIN, BPO_SUPERVISOR, BPO_AGENTE | Template |
| 5 archivos de test | Cambiar `@WithMockUser(roles = "ADMIN")` por `roles = "SUPERADMIN"` | Test |

---

## 3. Orden de implementación

1. Crear migración `V30__limpieza_roles_y_permisos.sql`
2. Actualizar `SecurityConfig.java` (4 matchers)
3. Actualizar ~25 controladores (`ADMIN` → `SUPERADMIN`)
4. Actualizar ~50 templates (`sec:authorize`)
5. Actualizar `sidebar-gestor.html` (eliminar BPO roles)
6. Rediseñar `admin/incidentes.html`
7. Actualizar 5 archivos de test
8. Ejecutar tests de verificación

---

## 4. Especificación de la migración V30

```sql
-- 4.1 Migrar usuarios de ADMIN, BPO_ADMIN, BPO_SUPERVISOR a SUPERADMIN
INSERT INTO usuarios_roles (usuario_id, rol_id)
SELECT ur.usuario_id, (SELECT id FROM roles WHERE nombre = 'SUPERADMIN')
FROM usuarios_roles ur JOIN roles r ON r.id = ur.rol_id
WHERE r.nombre IN ('ADMIN', 'BPO_ADMIN', 'BPO_SUPERVISOR')
  AND NOT EXISTS (
    SELECT 1 FROM usuarios_roles ur2
    WHERE ur2.usuario_id = ur.usuario_id AND ur2.rol_id = (SELECT id FROM roles WHERE nombre = 'SUPERADMIN')
  );

-- 4.2 Eliminar roles_permisos de roles a eliminar
DELETE FROM roles_permisos WHERE rol_id IN (SELECT id FROM roles WHERE nombre IN ('ADMIN', 'BPO_ADMIN', 'BPO_SUPERVISOR', 'ROL DE PRUEBA'));

-- 4.3 Eliminar usuarios_roles de roles a eliminar
DELETE FROM usuarios_roles WHERE rol_id IN (SELECT id FROM roles WHERE nombre IN ('ADMIN', 'BPO_ADMIN', 'BPO_SUPERVISOR', 'ROL DE PRUEBA'));

-- 4.4 Eliminar roles
DELETE FROM roles WHERE nombre IN ('ADMIN', 'BPO_ADMIN', 'BPO_SUPERVISOR', 'ROL DE PRUEBA');

-- 4.5 Crear permisos de certificaciones
INSERT INTO permisos (id, nombre, recurso, accion, descripcion)
SELECT gen_random_uuid(), v.nombre, v.recurso, v.accion, v.descripcion
FROM (VALUES
    ('CERTIFICACION_LEER', 'certificacion', 'LEER', 'Leer certificaciones'),
    ('CERTIFICACION_RENDIR', 'certificacion', 'RENDIR', 'Rendir certificaciones'),
    ('CERTIFICACION_ADMIN', 'certificacion', 'ADMIN', 'Administrar certificaciones')
) AS v(nombre, recurso, accion, descripcion)
WHERE NOT EXISTS (SELECT 1 FROM permisos p WHERE p.nombre = v.nombre);

-- 4.6 Asignar permisos a SUPERADMIN
INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p
WHERE r.nombre = 'SUPERADMIN' AND p.nombre IN ('CERTIFICACION_LEER', 'CERTIFICACION_RENDIR', 'CERTIFICACION_ADMIN')
  AND NOT EXISTS (SELECT 1 FROM roles_permisos rp WHERE rp.rol_id = r.id AND rp.permiso_id = p.id);

-- 4.7 Asignar permisos a ESTUDIANTE
INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p
WHERE r.nombre = 'ESTUDIANTE' AND p.nombre IN ('CERTIFICACION_LEER', 'CERTIFICACION_RENDIR')
  AND NOT EXISTS (SELECT 1 FROM roles_permisos rp WHERE rp.rol_id = r.id AND rp.permiso_id = p.id);

-- 4.8 Asignar permisos a INSTRUCTOR
INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p
WHERE r.nombre = 'INSTRUCTOR' AND p.nombre IN ('CERTIFICACION_LEER', 'CERTIFICACION_ADMIN')
  AND NOT EXISTS (SELECT 1 FROM roles_permisos rp WHERE rp.rol_id = r.id AND rp.permiso_id = p.id);
```

---

## 5. Criterios de aceptación

- [ ] **AC-A1:** `/admin/incidentes` muestra tabla con columnas IP, Usuario, Descripción, Estado, Fecha
- [ ] **AC-A2:** Los filtros por estado (Todos/Pendientes/Resueltos) funcionan
- [ ] **AC-A3:** El botón "Resolver" cambia el estado del incidente
- [ ] **AC-B1:** Roles eliminados (ADMIN, BPO_ADMIN, BPO_SUPERVISOR, ROL DE PRUEBA) = 0 en BD
- [ ] **AC-B2:** SUPERADMIN tiene ≥28 permisos
- [ ] **AC-B3:** ESTUDIANTE tiene CERTIFICACION_LEER y CERTIFICACION_RENDIR
- [ ] **AC-B4:** admin@lms.cl tiene SUPERADMIN (sin ADMIN ni BPO_ADMIN)
- [ ] **AC-B5:** No queda `hasRole('ADMIN')` en controladores (solo ESTUDIANTE y SUPERADMIN)
- [ ] **AC-B6:** No quedan referencias a BPO_ADMIN, BPO_SUPERVISOR, BPO_AGENTE en templates
- [ ] **AC-B7:** `mvn compile` sin errores
- [ ] **AC-B8:** Todos los tests en `tests/test-bug-002.md` pasan
