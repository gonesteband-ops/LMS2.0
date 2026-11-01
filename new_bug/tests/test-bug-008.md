# ✅ Tests: Bug 008 — Calidad de código BPO

> Tests de verificación TDD. Deben fallar ANTES de implementar y pasar DESPUÉS.

---

## Tests

### Test 1 — ContenidoBPO tiene @Pattern en tipo
```bash
Select-String -Path "src/main/java/com/lms/model/bpo/ContenidoBPO.java" -Pattern "@Pattern.*tipo"
```
**Antes:** 0
**Después:** 1+
**Tipo:** Grep

### Test 2 — ContenidoBPO tiene @Pattern en estado
```bash
Select-String -Path "src/main/java/com/lms/model/bpo/ContenidoBPO.java" -Pattern "@Pattern.*estado"
```
**Antes:** 0
**Después:** 1+
**Tipo:** Grep

### Test 3 — ContenidoBPO usa List<String> para etiquetas
```bash
Select-String -Path "src/main/java/com/lms/model/bpo/ContenidoBPO.java" -Pattern "List<String>.*etiquetas"
```
**Antes:** 0 (String[])
**Después:** 1+ (List<String>)
**Tipo:** Grep

### Test 4 — SubServicioBPO tiene @UniqueConstraint para (cuenta_id, codigo)
```bash
Select-String -Path "src/main/java/com/lms/model/bpo/SubServicioBPO.java" -Pattern "cuenta_id.*codigo|cuenta.*codigo"
```
**Antes:** Solo uniqueConstraint para (nombre, cuenta_id)
**Después:** También para (cuenta_id, codigo)
**Tipo:** Grep

### Test 5 — AdminBPOController usa findByRutAndDeletedAtIsNull
```bash
Select-String -Path "src/main/java/com/lms/controller/bpo/AdminBPOController.java" -Pattern "findByRutAndDeletedAtIsNull"
```
**Antes:** 0
**Después:** 1
**Tipo:** Grep

### Test 6 — DataInitializer no usa setRoles con Set.of
```bash
Select-String -Path "src/main/java/com/lms/config/DataInitializer.java" -Pattern "setRoles\(new HashSet"
```
**Antes:** Presente
**Después:** Ausente o reemplazado por add()
**Tipo:** Grep

### Test 7 — Compilación sin errores
```bash
mvn compile -q
```
**Antes/Después:** BUILD SUCCESS
**Tipo:** Bash

---

## Checklist de ejecución

- [ ] Test 1 (Grep — @Pattern tipo): ❌ / ✅
- [ ] Test 2 (Grep — @Pattern estado): ❌ / ✅
- [ ] Test 3 (Grep — List<String> etiquetas): ❌ / ✅
- [ ] Test 4 (Grep — @UniqueConstraint): ❌ / ✅
- [ ] Test 5 (Grep — findByRutAndDeletedAtIsNull): ❌ / ✅
- [ ] Test 6 (Grep — DataInitializer): ❌ / ✅
- [ ] Test 7 (Bash — compilación): ❌ / ✅
