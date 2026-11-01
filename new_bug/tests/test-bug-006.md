# ✅ Tests: Bug 006 — Integridad datos BPO

> Tests de verificación TDD. Deben fallar ANTES de implementar y pasar DESPUÉS.

---

## Tests

### Test 1 — Migración V27 existe
```bash
Test-Path "src/main/resources/db/migration/V27__hash_integridad_bpo.sql"
```
**Antes:** False
**Después:** True
**Tipo:** Bash

### Test 2 — CuentaBPO tiene hashSha256
```bash
Select-String -Path "src/main/java/com/lms/model/bpo/CuentaBPO.java" -Pattern "hashSha256|hash_sha256"
```
**Antes:** 0
**Después:** 1+
**Tipo:** Grep

### Test 3 — UsuarioCuentaBPO tiene deletedAt
```bash
Select-String -Path "src/main/java/com/lms/model/bpo/UsuarioCuentaBPO.java" -Pattern "deletedAt|deleted_by|deletedBy"
```
**Antes:** 0
**Después:** 1+
**Tipo:** Grep

### Test 4 — UsuarioCuentaBPO tiene @SQLRestriction
```bash
Select-String -Path "src/main/java/com/lms/model/bpo/UsuarioCuentaBPO.java" -Pattern "SQLRestriction"
```
**Antes:** 0
**Después:** 1
**Tipo:** Grep

### Test 5 — eliminarCuenta cascada a subservicios
```bash
Select-String -Path "src/main/java/com/lms/service/bpo/CuentaBPOService.java" -Pattern "eliminarSubcarpeta|setDeletedAt" | Select-String -Pattern "eliminarCuenta" -Context 0,10
```
**Antes:** Solo `gestor.setDeletedAt()` en eliminarCuenta
**Después:** También setDeletedAt en subservicios/contenidos
**Tipo:** Grep

### Test 6 — Compilación sin errores
```bash
mvn compile -q
```
**Antes/Después:** BUILD SUCCESS
**Tipo:** Bash

---

## Checklist de ejecución

- [ ] Test 1 (Bash — migración V27): ❌ / ✅
- [ ] Test 2 (Grep — hashSha256 en CuentaBPO): ❌ / ✅
- [ ] Test 3 (Grep — deletedAt en UsuarioCuentaBPO): ❌ / ✅
- [ ] Test 4 (Grep — @SQLRestriction): ❌ / ✅
- [ ] Test 5 (Grep — cascada eliminarCuenta): ❌ / ✅
- [ ] Test 6 (Bash — compilación): ❌ / ✅
