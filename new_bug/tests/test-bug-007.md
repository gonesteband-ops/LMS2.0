# ✅ Tests: Bug 007 — Lógica de negocio BPO

> Tests de verificación TDD. Deben fallar ANTES de implementar y pasar DESPUÉS.

---

## Tests

### Test 1 — tipoValor usa dto.getTipo()
```bash
Select-String -Path "src/main/java/com/lms/service/bpo/ContenidoBPOService.java" -Pattern "dto\.getTipo\(\)"
```
**Antes:** 0 matches cerca de la línea 116
**Después:** 1+ match
**Tipo:** Grep

### Test 2 — CuentaBPORepository tiene findByActivaTrue
```bash
Select-String -Path "src/main/java/com/lms/repository/bpo/CuentaBPORepository.java" -Pattern "findByActivaTrue"
```
**Antes:** 0
**Después:** 1
**Tipo:** Grep

### Test 3 — obtenerCuentasActivas usa findByActivaTrue
```bash
Select-String -Path "src/main/java/com/lms/service/bpo/CuentaBPOService.java" -Pattern "findByActivaTrue"
```
**Antes:** 0
**Después:** 1
**Tipo:** Grep

### Test 4 — ContenidoBPORepository tiene método con Pageable
```bash
Select-String -Path "src/main/java/com/lms/repository/bpo/ContenidoBPORepository.java" -Pattern "Pageable|Page<"
```
**Antes:** 0
**Después:** 1+
**Tipo:** Grep

### Test 5 — obtenerResumen usa GROUP BY
```bash
Select-String -Path "src/main/java/com/lms/repository/bpo/ContenidoBPORepository.java" -Pattern "GROUP BY.*tipo|group by"
```
**Antes:** 0
**Después:** 1+
**Tipo:** Grep

### Test 6 — Compilación sin errores
```bash
mvn compile -q
```
**Antes/Después:** BUILD SUCCESS
**Tipo:** Bash

---

## Checklist de ejecución

- [ ] Test 1 (Grep — tipoValor fix): ❌ / ✅
- [ ] Test 2 (Grep — findByActivaTrue): ❌ / ✅
- [ ] Test 3 (Grep — service usa findByActivaTrue): ❌ / ✅
- [ ] Test 4 (Grep — Pageable en repositorio): ❌ / ✅
- [ ] Test 5 (Grep — GROUP BY en query): ❌ / ✅
- [ ] Test 6 (Bash — compilación): ❌ / ✅
