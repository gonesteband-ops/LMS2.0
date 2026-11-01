# ✅ Tests: Bug 004 — Gestor de Contenidos: unificar vistas, toggle activa y navegación

> Tests de verificación TDD. Deben fallar ANTES de implementar y pasar DESPUÉS.
> El agente NO entrega hasta que TODOS los tests pasen.

---

## Tests

### Test 1 — CuentaBPOService tiene método toggleActiva
```bash
grep -c "public CuentaBPO toggleActiva" src/main/java/com/lms/service/bpo/CuentaBPOService.java
```

**Antes:** `0`
**Después:** `1`
**Tipo:** Grep

### Test 2 — AdminBPOController tiene endpoint PATCH toggle-activa
```bash
grep -c "toggle-activa" src/main/java/com/lms/controller/bpo/AdminBPOController.java
```

**Antes:** `0`
**Después:** `1`
**Tipo:** Grep

### Test 3 — bpo/gestores.html tiene sec:authorize para botón admin
```bash
grep -c 'sec:authorize.*ADMIN.*SUPERADMIN' src/main/resources/templates/bpo/gestores.html
```

**Antes:** `0`
**Después:** `1` o más
**Tipo:** Grep

### Test 4 — bpo/gestores.html tiene badge de estado
```bash
grep -c 'activa\|Inactivo\|Activo' src/main/resources/templates/bpo/gestores.html
```

**Antes:** `0`
**Después:** `1` o más
**Tipo:** Grep

### Test 5 — bpo/gestor-detalle.html tiene badge de estado
```bash
grep -c 'activa\|Inactivo\|Activo' src/main/resources/templates/bpo/gestor-detalle.html
```

**Antes:** `0`
**Después:** `1` o más
**Tipo:** Grep

### Test 6 — Compilación sin errores
```bash
mvn compile -q
```

**Antes:** BUILD SUCCESS
**Después:** BUILD SUCCESS
**Tipo:** Bash

### Test 7 — Smoke test manual: admin ve + Nuevo Gestor
1. Iniciar sesión como admin
2. Ir a `http://localhost:8080/bpo/gestores`
3. Verificar que aparece botón "+ Nuevo Gestor"

**Antes:** No aparece
**Después:** Aparece
**Tipo:** Smoke

### Test 8 — Smoke test manual: toggle activa funciona
1. Como admin, ir a `/bpo/gestores`
2. Hacer clic en toggle de un gestor activo
3. Verificar que cambia a inactivo (badge gris)
4. Hacer clic de nuevo y verifica que vuelve a activo

**Antes:** No hay toggle
**Después:** Toggle funciona en ambos sentidos
**Tipo:** Smoke

### Test 9 — Smoke test manual: alumno no ve botones admin
1. Iniciar sesión como estudiante
2. Ir a `http://localhost:8080/bpo/gestores`
3. Verificar que NO aparece "+ Nuevo Gestor" ni toggle

**Antes:** (se verifica que no hay)
**Después:** No debe haber botones admin
**Tipo:** Smoke

---

## Checklist de ejecución

- [ ] Test 1 (Grep — toggleActiva en Service): ❌ / ✅
- [ ] Test 2 (Grep — endpoint PATCH): ❌ / ✅
- [ ] Test 3 (Grep — sec:authorize en template): ❌ / ✅
- [ ] Test 4 (Grep — badge de estado listado): ❌ / ✅
- [ ] Test 5 (Grep — badge de estado detalle): ❌ / ✅
- [ ] Test 6 (Bash — compilación): ❌ / ✅
- [ ] Test 7 (Smoke — admin ve + Nuevo Gestor): ❌ / ✅
- [ ] Test 8 (Smoke — toggle activa funciona): ❌ / ✅
- [ ] Test 9 (Smoke — alumno no ve botones admin): ❌ / ✅

> **Regla:** Si algún test queda como ❌, el agente NO entrega. Debe corregir y repetir.
