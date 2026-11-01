# 🧾 Contrato de Mejora: Bug 021 — Incidentes UI + Limpieza de Roles

> Generado del reporte `bugs/reportes/bug-021.md`

---

## 1. Error detectado

**Síntoma A:** `/admin/incidentes` muestra JSON crudo sin tabla ni filtros.

**Síntoma B:** Existen 8 roles, varios redundantes o sin usuarios. `ADMIN` duplica a `SUPERADMIN`, `BPO_ADMIN` y `BPO_SUPERVISOR` no aportan valor, `ROL DE PRUEBA` es sobrante.

**Causa raíz A:** El template `incidentes.html` es ultra básico y el HTMX recibe JSON en vez de HTML.

**Causa raíz B:** Se crearon roles BPO sin una revisión de si realmente hacían falta. `ADMIN` quedó como rol fantasma (0 usuarios).

**Impacto:**
- A: El admin no puede gestionar incidentes de seguridad
- B: Modelo de roles inflado, confuso y con riesgo de seguridad (roles sin supervisión)

---

## 2. Archivos involucrados

### Tarea A — UI Incidentes

| Archivo | Rol | Cambio |
|---|---|---|
| `src/main/resources/templates/admin/incidentes.html` | Frontend | Rediseño completo con tabla, filtros, botón Resolver |

### Tarea B — Limpieza de Roles

| Archivo | Rol | Cambio |
|---|---|---|
| `src/main/resources/db/migration/V30__limpieza_roles_y_permisos_certificaciones.sql` | Migración | Eliminar 4 roles, crear permisos de certificaciones |
| `src/main/java/com/lms/security/SecurityConfig.java` | Config | Cambiar `hasAnyRole("ADMIN",...)` por `hasAnyRole("SUPERADMIN",...)` |
| ~25 controladores Java | Controller | Cambiar `hasRole('ADMIN')` → `hasRole('SUPERADMIN')` |
| ~50 templates HTML | Frontend | Cambiar `sec:authorize="hasAnyRole('ADMIN',...)"` por `hasAnyRole('SUPERADMIN',...)` |
| `src/main/resources/templates/fragments/sidebar-gestor.html` | Frontend | Eliminar referencias a BPO_ADMIN, BPO_SUPERVISOR, BPO_AGENTE |
| 5 archivos de test | Test | Cambiar `@WithMockUser(roles = "ADMIN")` por `roles = "SUPERADMIN"` |

---

## 3. Criterios de aceptación (checklist)

### Tarea A
- [ ] **AC-A1:** `/admin/incidentes` muestra tabla con columnas: IP, Usuario, Descripción, Estado, Fecha
- [ ] **AC-A2:** Los filtros por estado (Todos/Pendientes/Resueltos) funcionan
- [ ] **AC-A3:** El botón "Resolver" cambia el estado del incidente vía PUT

### Tarea B
- [ ] **AC-B1:** Roles eliminados: `ADMIN`, `BPO_ADMIN`, `BPO_SUPERVISOR`, `ROL DE PRUEBA` → 0 registros en BD
- [ ] **AC-B2:** `SUPERADMIN` tiene ≥28 permisos (todos los del sistema)
- [ ] **AC-B3:** `ESTUDIANTE` tiene `CERTIFICACION_LEER` y `CERTIFICACION_RENDIR`
- [ ] **AC-B4:** admin@lms.cl tiene `SUPERADMIN` y no tiene `ADMIN` ni `BPO_ADMIN`
- [ ] **AC-B5:** No queda `hasRole('ADMIN')` en ningún controlador (solo `ESTUDIANTE` y `SUPERADMIN`)
- [ ] **AC-B6:** No quedan referencias a `BPO_ADMIN`, `BPO_SUPERVISOR`, `BPO_AGENTE` en templates
- [ ] **AC-B7:** `mvn compile` sin errores
- [ ] **AC-B8:** Todos los tests unitarios pasan
- [ ] **AC-B9:** Smoke test: login admin, login estudiante, rutas admin protegidas

---

## 4. Tests TDD (deben fallar antes y pasar después)

### Test 1 — BD: Roles eliminados
```sql
SELECT COUNT(*) FROM roles WHERE nombre IN ('ADMIN', 'BPO_ADMIN', 'BPO_SUPERVISOR', 'ROL DE PRUEBA');
-- Antes: 4 | Después: 0
```

### Test 2 — BD: SUPERADMIN tiene todos los permisos
```sql
SELECT COUNT(*) FROM roles_permisos rp JOIN roles r ON r.id = rp.rol_id WHERE r.nombre = 'SUPERADMIN';
-- Antes: ~24 | Después: >= 28
```

### Test 3 — BD: ESTUDIANTE tiene permisos de certificaciones
```sql
SELECT p.nombre FROM roles_permisos rp JOIN roles r ON r.id = rp.rol_id
JOIN permisos p ON p.id = rp.permiso_id
WHERE r.nombre = 'ESTUDIANTE' AND p.nombre LIKE 'CERTIFICACION%';
-- Antes: 0 filas | Después: CERTIFICACION_LEER, CERTIFICACION_RENDIR
```

### Test 4 — BD: admin@lms.cl es SUPERADMIN
```sql
SELECT r.nombre FROM usuarios_roles ur JOIN roles r ON r.id = ur.rol_id
JOIN usuarios u ON u.id = ur.usuario_id WHERE u.email = 'admin@lms.cl';
-- Antes: ADMIN, SUPERADMIN, BPO_ADMIN | Después: SUPERADMIN
```

### Test 5 — Código: No queda `hasRole('ADMIN')` en controladores
```bash
grep -rn "hasRole('ADMIN')" src/main/java/com/lms/controller/ | grep -v "hasRole('ESTUDIANTE')" | grep -v "hasRole('SUPERADMIN')"
# Antes: múltiples ocurrencias | Después: 0
```

### Test 6 — Código: No quedan referencias BPO en templates
```bash
grep -rn "BPO_ADMIN\|BPO_SUPERVISOR\|BPO_AGENTE" src/main/resources/templates/
# Antes: 1 ocurrencia (sidebar-gestor.html) | Después: 0
```

### Test 7 — Endpoint admin con SUPERADMIN → 200
```java
@Test @WithMockUser(roles = "SUPERADMIN")
public void adminEndpoint_ConSuperadmin_Retorna200() {
    mockMvc.perform(get("/admin/usuarios")).andExpect(status().isOk());
}
```

### Test 8 — Endpoint admin con ESTUDIANTE → 403
```java
@Test @WithMockUser(roles = "ESTUDIANTE")
public void adminEndpoint_ConEstudiante_Retorna403() {
    mockMvc.perform(get("/admin/usuarios")).andExpect(status().isForbidden());
}
```

### Test 9 — Compilación y tests
```bash
mvn compile -q && mvn test -q
# BUILD SUCCESS
```

---

## 5. Tareas para el agente

### 5.1 Antes de implementar
- [ ] Ejecutar Tests 1-6 (SQL y grep) con el código actual para verificar que FALLAN
- [ ] Escribir Tests 7 y 8 (JUnit) que fallen con el código actual
- [ ] Leer `SecurityConfig.java` y todos los controladores listados en la radiografía
- [ ] Leer el template `sidebar-gestor.html`

### 5.2 Implementación — Orden estricto
- [ ] **Paso 1:** Crear `V30__limpieza_roles_y_permisos_certificaciones.sql`
- [ ] **Paso 2:** Actualizar `SecurityConfig.java` (4 matchers)
- [ ] **Paso 3:** Actualizar ~25 controladores (`ADMIN` → `SUPERADMIN`)
- [ ] **Paso 4:** Actualizar ~50 templates (`sec:authorize`)
- [ ] **Paso 5:** Actualizar `sidebar-gestor.html` (eliminar BPO roles)
- [ ] **Paso 6:** Rediseñar `admin/incidentes.html` (tabla + filtros + botón Resolver)
- [ ] **Paso 7:** Actualizar 5 archivos de test (`@WithMockUser`)

### 5.3 Verificación (el agente debe mostrar evidencia)
- [ ] Ejecutar Tests 1-6 (SQL y grep) — deben pasar
- [ ] Ejecutar Tests 7 y 8 (JUnit) — deben pasar
- [ ] `mvn compile -q` sin errores
- [ ] `mvn test -q` verde
- [ ] Smoke test: acceso a rutas admin con SUPERADMIN y ESTUDIANTE

---

## 6. Evidencia de finalización

*El agente llena al terminar:*

```
### Migración aplicada:
[V30__limpieza_roles_y_permisos_certificaciones.sql] — OK

### Controladores modificados (25):
[listar archivos y líneas]

### Templates modificados (~50):
[listar archivos representativos]

### Tests:
Test 1 (BD roles): antes 4, después 0 ✅
Test 2 (BD permisos SUPERADMIN): >= 28 ✅
Test 3 (BD ESTUDIANTE certificaciones): 2 permisos ✅
Test 4 (BD admin@lms.cl): SUPERADMIN ✅
Test 5 (grep hasRole ADMIN): 0 ocurrencias ✅
Test 6 (grep BPO roles): 0 ocurrencias ✅
Test 7 (SUPERADMIN → 200): ✅
Test 8 (ESTUDIANTE → 403): ✅
Test 9 (mvn compile + test): BUILD SUCCESS ✅

### Smoke test:
- Login admin@lms.cl → OK
- Login estudiante@lms.cl → OK
- /admin/usuarios con admin → carga ✅
- /admin/usuarios con estudiante → 403 ✅
```
