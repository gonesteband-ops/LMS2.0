# ✅ Tests: Bug 009 — Certificaciones

> Tests de verificación TDD. Deben fallar ANTES de implementar y pasar DESPUÉS.

---

## Tests

### Test 1 — CertificacionAlumnoController tiene @PreAuthorize
```bash
Select-String -Path "src/main/java/com/lms/controller/CertificacionAlumnoController.java" -Pattern "@PreAuthorize"
```
**Antes:** 0
**Después:** 1+
**Tipo:** Grep

### Test 2 — rendir() verifica inscripción
```bash
Select-String -Path "src/main/java/com/lms/controller/CertificacionAlumnoController.java" -Pattern "certUsuarioRepo|findByCertificacionIdAndUsuarioId|inscrito|noInscrito"
```
**Antes:** 0 en rendir()
**Después:** 1+ en rendir()
**Tipo:** Grep

### Test 3 — CertificacionUsuario tiene deletedAt
```bash
Select-String -Path "src/main/java/com/lms/model/CertificacionUsuario.java" -Pattern "deletedAt|deletedBy|reason"
```
**Antes:** 0
**Después:** 1+ cada uno
**Tipo:** Grep

### Test 4 — CertificacionUsuario tiene @SQLRestriction
```bash
Select-String -Path "src/main/java/com/lms/model/CertificacionUsuario.java" -Pattern "SQLRestriction"
```
**Antes:** 0
**Después:** 1
**Tipo:** Grep

### Test 5 — CertificacionUsuario tiene hashSha256
```bash
Select-String -Path "src/main/java/com/lms/model/CertificacionUsuario.java" -Pattern "hashSha256"
```
**Antes:** 0
**Después:** 1
**Tipo:** Grep

### Test 6 — Migración V31 existe
```bash
Test-Path "src/main/resources/db/migration/V31__certificaciones_soft_delete_hash.sql"
```
**Antes:** False
**Después:** True
**Tipo:** Bash

### Test 7 — "Ver Resultado" usa ruta /resultado
```bash
Select-String -Path "src/main/resources/templates/certificaciones/mis-certificaciones.html" -Pattern "resultado"
```
**Antes:** Sin ruta /{id}/resultado
**Después:** Tiene enlace a /certificaciones/{id}/resultado
**Tipo:** Grep

### Test 8 — Compilación sin errores
```bash
mvn compile -q
```
**Antes/Después:** BUILD SUCCESS
**Tipo:** Bash

---

## Checklist de ejecución

- [ ] Test 1 (Grep — @PreAuthorize): ❌ / ✅
- [ ] Test 2 (Grep — verificación inscripción): ❌ / ✅
- [ ] Test 3 (Grep — deletedAt en CertificacionUsuario): ❌ / ✅
- [ ] Test 4 (Grep — @SQLRestriction): ❌ / ✅
- [ ] Test 5 (Grep — hashSha256): ❌ / ✅
- [ ] Test 6 (Bash — migración V31): ❌ / ✅
- [ ] Test 7 (Grep — Ver Resultado): ❌ / ✅
- [ ] Test 8 (Bash — compilación): ❌ / ✅
