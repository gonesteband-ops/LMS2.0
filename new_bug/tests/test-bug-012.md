# ✅ Tests: Bug 012 — Usuarios: carga masiva + plantilla descargable

> Tests de verificación. La implementación ya está aplicada; estos tests confirman que todo funciona.

---

## Tests

### Test 1 — DTO CargaMasivaUsuariosReport existe
```bash
Test-Path "src/main/java/com/lms/dto/CargaMasivaUsuariosReport.java"
```
**Esperado:** True
**Tipo:** Bash

### Test 2 — UsuarioService tiene crearUsuariosMasivo
```bash
Select-String -Path "src/main/java/com/lms/service/UsuarioService.java" -Pattern "crearUsuariosMasivo"
```
**Esperado:** 1+
**Tipo:** Grep

### Test 3 — AdminHtmxController tiene plantilla-masiva
```bash
Select-String -Path "src/main/java/com/lms/controller/AdminHtmxController.java" -Pattern "plantilla-masiva"
```
**Esperado:** 1+
**Tipo:** Grep

### Test 4 — AdminHtmxController tiene POST usuarios/masivo
```bash
Select-String -Path "src/main/java/com/lms/controller/AdminHtmxController.java" -Pattern "usuarios/masivo"
```
**Esperado:** 1+
**Tipo:** Grep

### Test 5 — usuarios.html tiene card de carga masiva
```bash
Select-String -Path "src/main/resources/templates/admin/usuarios.html" -Pattern "Carga Masiva de Usuarios|Descargar plantilla|Subir CSV"
```
**Esperado:** 1+ cada uno
**Tipo:** Grep

### Test 6 — usuarios-fragment.html muestra reporte
```bash
Select-String -Path "src/main/resources/templates/admin/usuarios-fragment.html" -Pattern "reporteMasivo|Reporte de carga masiva"
```
**Esperado:** 1+
**Tipo:** Grep

### Test 7 — BOM strip en UsuarioService
```bash
Select-String -Path "src/main/java/com/lms/service/UsuarioService.java" -Pattern "uFEFF"
```
**Esperado:** 1+
**Tipo:** Grep

### Test 8 — Compilación sin errores
```bash
mvn compile -q
```
**Esperado:** BUILD SUCCESS
**Tipo:** Bash

### Test 9 — Smoke: plantilla descarga (Content-Disposition)
1. Login como admin
2. GET `/hx/admin/usuarios/plantilla-masiva`
3. Verificar `Content-Disposition: attachment; filename=plantilla-usuarios.csv`

**Esperado:** 200 + header attachment
**Tipo:** Smoke

### Test 10 — Smoke: página admin muestra card
1. GET `/admin/usuarios`
2. Verificar contiene "Carga Masiva de Usuarios" y "Descargar plantilla"

**Esperado:** True
**Tipo:** Smoke

### Test 11 — Smoke: carga CSV crea usuario
1. Subir CSV con 1 usuario válido via `POST /hx/admin/usuarios/masivo`
2. Verificar reporte "1 creados"
3. Verificar usuario en DB

**Esperado:** Usuario creado
**Tipo:** Smoke

---

## Checklist de ejecución

- [ ] Test 1 (Bash — DTO existe): ❌ / ✅
- [ ] Test 2 (Grep — crearUsuariosMasivo): ❌ / ✅
- [ ] Test 3 (Grep — plantilla-masiva): ❌ / ✅
- [ ] Test 4 (Grep — usuarios/masivo): ❌ / ✅
- [ ] Test 5 (Grep — card UI): ❌ / ✅
- [ ] Test 6 (Grep — reporte fragment): ❌ / ✅
- [ ] Test 7 (Grep — BOM strip): ❌ / ✅
- [ ] Test 8 (Bash — compilación): ❌ / ✅
- [ ] Test 9 (Smoke — plantilla): ❌ / ✅
- [ ] Test 10 (Smoke — página admin): ❌ / ✅
- [ ] Test 11 (Smoke — carga CSV): ❌ / ✅

> **Regla:** Si algún test queda como ❌, el agente corrige y repite.
