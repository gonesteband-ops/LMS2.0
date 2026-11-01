# ✅ Tests: Bug 005 — Seguridad BPO

> Tests de verificación TDD. Deben fallar ANTES de implementar y pasar DESPUÉS.

---

## Tests

### Test 1 — SecurityConfig tiene matcher para /bpo/admin/
```bash
Select-String -Path "src/main/java/com/lms/security/SecurityConfig.java" -Pattern "bpo/admin"
```
**Antes:** 0 matches
**Después:** 1+ match
**Tipo:** Grep

### Test 2 — UserPrincipal usa permiso.getNombre()
```bash
Select-String -Path "src/main/java/com/lms/security/UserPrincipal.java" -Pattern "permiso.getNombre\(\)|getNombre\(\)"
```
**Antes:** 0 matches en getAuthorities()
**Después:** 1+ match
**Tipo:** Grep

### Test 3 — CuentaBPOService tiene removerAsignacionUsuario
```bash
Select-String -Path "src/main/java/com/lms/service/bpo/CuentaBPOService.java" -Pattern "removerAsignacionUsuario"
```
**Antes:** 0
**Después:** 1
**Tipo:** Grep

### Test 4 — AdminBPOController.removerCompartir delega en servicio
```bash
Select-String -Path "src/main/java/com/lms/controller/bpo/AdminBPOController.java" -Pattern "cuentaService\.removerAsignacionUsuario|usuarioCuentaRepository\.deleteById"
```
**Antes:** `deleteById` presente en removerCompartir
**Después:** Solo `cuentaService.removerAsignacionUsuario`, sin `deleteById`
**Tipo:** Grep

### Test 5 — GestorAlumnoController.verPublicacion valida ownership
```bash
Select-String -Path "src/main/java/com/lms/controller/bpo/GestorAlumnoController.java" -Pattern "verificar|validar|asignad|contains|tieneAcceso"
```
**Antes:** Sin validación de acceso en verPublicacion
**Después:** Tiene validación
**Tipo:** Grep

### Test 6 — Compilación sin errores
```bash
mvn compile -q
```
**Antes/Después:** BUILD SUCCESS
**Tipo:** Bash

---

## Checklist de ejecución

- [ ] Test 1 (Grep — matcher /bpo/admin/): ❌ / ✅
- [ ] Test 2 (Grep — permiso.getNombre): ❌ / ✅
- [ ] Test 3 (Grep — removerAsignacionUsuario): ❌ / ✅
- [ ] Test 4 (Grep — delegación en servicio): ❌ / ✅
- [ ] Test 5 (Grep — ownership check): ❌ / ✅
- [ ] Test 6 (Bash — compilación): ❌ / ✅
