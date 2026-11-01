# ✅ Tests: Bug 001 — Faltan 6 reportes de exportación

> Tests de verificación TDD. Deben fallar ANTES de implementar y pasar DESPUÉS.
> El agente NO entrega hasta que TODOS los tests pasen.

---

## Tests

### Test 1 — El selector HTML tiene 12 opciones
```bash
grep -c '<option value="certificaciones"' src/main/resources/templates/dashboard/exportaciones.html
grep -c '<option value="resultados_certificaciones"' src/main/resources/templates/dashboard/exportaciones.html
grep -c '<option value="gestores_contenido"' src/main/resources/templates/dashboard/exportaciones.html
grep -c '<option value="publicaciones_bpo"' src/main/resources/templates/dashboard/exportaciones.html
grep -c '<option value="incidentes_seguridad"' src/main/resources/templates/dashboard/exportaciones.html
grep -c '<option value="notificaciones"' src/main/resources/templates/dashboard/exportaciones.html
```

**Antes:** 0 ocurrencias para cada grep
**Después:** `1` para cada grep
**Tipo:** Bash

### Test 2 — FILTER_OPTIONS tiene 12 entradas en JS
```bash
grep -c "'certificaciones'" src/main/resources/templates/dashboard/exportaciones.html
grep -c "'resultados_certificaciones'" src/main/resources/templates/dashboard/exportaciones.html
grep -c "'gestores_contenido'" src/main/resources/templates/dashboard/exportaciones.html
grep -c "'publicaciones_bpo'" src/main/resources/templates/dashboard/exportaciones.html
grep -c "'incidentes_seguridad'" src/main/resources/templates/dashboard/exportaciones.html
grep -c "'notificaciones'" src/main/resources/templates/dashboard/exportaciones.html
```

**Antes:** 0 ocurrencias para cada grep
**Después:** `1` para cada grep
**Tipo:** Bash

### Test 3 — ExportacionService tiene los 6 métodos
```bash
grep -c "public void generarReporteCertificaciones" src/main/java/com/lms/service/ExportacionService.java
grep -c "public void generarReporteResultadosCertificaciones" src/main/java/com/lms/service/ExportacionService.java
grep -c "public void generarReporteGestores" src/main/java/com/lms/service/ExportacionService.java
grep -c "public void generarReportePublicacionesBpo" src/main/java/com/lms/service/ExportacionService.java
grep -c "public void generarReporteIncidentes" src/main/java/com/lms/service/ExportacionService.java
grep -c "public void generarReporteNotificaciones" src/main/java/com/lms/service/ExportacionService.java
```

**Antes:** 0 ocurrencias para cada grep
**Después:** `1` para cada grep
**Tipo:** Bash

### Test 4 — Switch tiene los 6 cases nuevos
```bash
grep -c 'case "certificaciones"' src/main/java/com/lms/service/ExportacionService.java
grep -c 'case "resultados_certificaciones"' src/main/java/com/lms/service/ExportacionService.java
grep -c 'case "gestores_contenido"' src/main/java/com/lms/service/ExportacionService.java
grep -c 'case "publicaciones_bpo"' src/main/java/com/lms/service/ExportacionService.java
grep -c 'case "incidentes_seguridad"' src/main/java/com/lms/service/ExportacionService.java
grep -c 'case "notificaciones"' src/main/java/com/lms/service/ExportacionService.java
```

**Antes:** 0 ocurrencias para cada grep
**Después:** `1` para cada grep
**Tipo:** Bash

### Test 5 — Compilación sin errores
```bash
mvn compile -q
echo $?
```

**Antes:** 0 (compila)
**Después:** 0 (compila)
**Tipo:** Bash

### Test 6 — Tests unitarios pasan
```bash
mvn test -q -Dtest=ExportacionServiceTest
echo $?
```

**Antes:** 0 (tests pasan, solo no cubren los nuevos reportes)
**Después:** 0 (tests pasan, incluyendo nuevos)
**Tipo:** Bash

---

## Checklist de ejecución

- [ ] Test 1 (grep — 6 options): ❌ / ✅
- [ ] Test 2 (grep — 6 filter options): ❌ / ✅
- [ ] Test 3 (grep — 6 métodos): ❌ / ✅
- [ ] Test 4 (grep — 6 cases): ❌ / ✅
- [ ] Test 5 (mvn compile): ❌ / ✅
- [ ] Test 6 (mvn test): ❌ / ✅

> **Regla:** Si algún test queda como ❌, el agente NO entrega. Debe corregir y repetir.
