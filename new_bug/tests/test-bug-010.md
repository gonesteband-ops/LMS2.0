# ✅ Tests: Bug 010 — Certificaciones Admin

---

## Tests

### Test 1 — listado.html tiene fetch DELETE para eliminar
```bash
Select-String -Path "src/main/resources/templates/certificaciones/listado.html" -Pattern "fetch.*DELETE|method.*DELETE"
```
**Antes:** 0
**Después:** 1+
**Tipo:** Grep

### Test 2 — participantes.html tiene botón Volver
```bash
Select-String -Path "src/main/resources/templates/certificaciones/participantes.html" -Pattern "Volver"
```
**Antes:** 0
**Después:** 1+
**Tipo:** Grep

### Test 3 — editor.html tiene JS para carga masiva
```bash
Select-String -Path "src/main/resources/templates/certificaciones/editor.html" -Pattern "cargaMasiva|cargarArchivo|procesarExcel"
```
**Antes:** 0
**Después:** 1+
**Tipo:** Grep

### Test 4 — Compilación sin errores
```bash
mvn compile -q
```
**Antes/Después:** BUILD SUCCESS
**Tipo:** Bash
