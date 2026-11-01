# ✅ Tests: Bug 002 — UI de incidentes pobre + roles redundantes

> Tests de verificación TDD. Deben fallar ANTES de implementar y pasar DESPUÉS.
> El agente NO entrega hasta que TODOS los tests pasen.

---

## Tests — Tarea A (Incidentes)

### Test A1 — Tabla de incidentes visible
```bash
grep -c "incidentes-table\|<table" src/main/resources/templates/admin/incidentes.html
```

**Antes:** 1 (solo un div con id="incidentes-table")
**Después:** >= 2 (tabla con filas y columnas)
**Tipo:** Bash

### Test A2 — Filtros por estado existen
```bash
grep -c "Pendiente\|Resuelto\|Todos\|filter" src/main/resources/templates/admin/incidentes.html
```

**Antes:** 0
**Después:** >= 3
**Tipo:** Bash

### Test A3 — Botón Resolver existe
```bash
grep -c "Resolver\|resolver\|RESUELTO" src/main/resources/templates/admin/incidentes.html
```

**Antes:** 0
**Después:** >= 1
**Tipo:** Bash

---

## Tests — Tarea B (Roles)

### Test B1 — Roles eliminados no existen en BD
```sql
SELECT COUNT(*) FROM roles WHERE nombre IN ('ADMIN', 'BPO_ADMIN', 'BPO_SUPERVISOR', 'ROL DE PRUEBA');
```

**Antes:** 4
**Después:** 0
**Tipo:** SQL

### Test B2 — SUPERADMIN tiene todos los permisos
```sql
SELECT COUNT(*) FROM roles_permisos rp JOIN roles r ON r.id = rp.rol_id WHERE r.nombre = 'SUPERADMIN';
```

**Antes:** ~24
**Después:** >= 28
**Tipo:** SQL

### Test B3 — ESTUDIANTE tiene permisos de certificaciones
```sql
SELECT p.nombre FROM roles_permisos rp JOIN roles r ON r.id = rp.rol_id
JOIN permisos p ON p.id = rp.permiso_id
WHERE r.nombre = 'ESTUDIANTE' AND p.nombre LIKE 'CERTIFICACION%';
```

**Antes:** 0 filas
**Después:** CERTIFICACION_LEER, CERTIFICACION_RENDIR (2 filas)
**Tipo:** SQL

### Test B4 — admin@lms.cl es SUPERADMIN (sin ADMIN ni BPO_ADMIN)
```sql
SELECT r.nombre FROM usuarios_roles ur JOIN roles r ON r.id = ur.rol_id
JOIN usuarios u ON u.id = ur.usuario_id WHERE u.email = 'admin@lms.cl';
```

**Antes:** ADMIN, SUPERADMIN, BPO_ADMIN
**Después:** SUPERADMIN (solo)
**Tipo:** SQL

### Test B5 — No queda `hasRole('ADMIN')` en controladores
```bash
grep -rn "hasRole('ADMIN')" src/main/java/com/lms/controller/ | grep -v "hasRole('ESTUDIANTE')" | grep -v "hasRole('SUPERADMIN')" | wc -l
```

**Antes:** >10
**Después:** 0
**Tipo:** Bash

### Test B6 — No quedan referencias BPO en templates
```bash
grep -rn "BPO_ADMIN\|BPO_SUPERVISOR\|BPO_AGENTE" src/main/resources/templates/ | wc -l
```

**Antes:** 1 (sidebar-gestor.html)
**Después:** 0
**Tipo:** Bash

---

## Tests — Compilación y smoke

### Test C1 — Compilación sin errores
```bash
mvn compile -q
echo $?
```

**Después:** 0
**Tipo:** Bash

### Test C2 — Tests unitarios pasan
```bash
mvn test -q
echo $?
```

**Después:** 0
**Tipo:** Bash

### Test C3 — Smoke test manual
1. Login como admin@lms.cl → debe cargar dashboard
2. Ir a `/admin/incidentes` → debe mostrar tabla con datos
3. Login como estudiante@lms.cl → debe cargar sin secciones admin
4. Ir a `/admin/usuarios` con estudiante → debe dar 403

**Después:** Todo funciona
**Tipo:** Smoke

---

## Checklist de ejecución

- [ ] Test A1 (tabla incidentes): ❌ / ✅
- [ ] Test A2 (filtros): ❌ / ✅
- [ ] Test A3 (botón Resolver): ❌ / ✅
- [ ] Test B1 (roles eliminados BD): ❌ / ✅
- [ ] Test B2 (permisos SUPERADMIN): ❌ / ✅
- [ ] Test B3 (ESTUDIANTE certificaciones): ❌ / ✅
- [ ] Test B4 (admin@lms.cl): ❌ / ✅
- [ ] Test B5 (grep hasRole ADMIN): ❌ / ✅
- [ ] Test B6 (grep BPO referencias): ❌ / ✅
- [ ] Test C1 (mvn compile): ❌ / ✅
- [ ] Test C2 (mvn test): ❌ / ✅
- [ ] Test C3 (smoke manual): ❌ / ✅

> **Regla:** Si algún test queda como ❌, el agente NO entrega. Debe corregir y repetir.
