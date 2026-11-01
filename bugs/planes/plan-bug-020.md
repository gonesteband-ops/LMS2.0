# 🧾 Contrato de Mejora: Bug 020 — Nuevos reportes de exportación

> Generado del reporte `bugs/reportes/bug-020.md`

---

## 1. Error detectado

**Síntoma:** En `/dashboard/exportaciones` solo hay 6 tipos de reporte. Faltan 6 nuevos para las entidades agregadas (certificaciones, gestores, incidentes, notificaciones).

**Causa raíz:** `ExportacionService` no tiene los métodos generadores, y el template `exportaciones.html` no tiene las opciones en el selector ni los filtros.

**Impacto:** No se pueden exportar datos de certificaciones, gestores de contenido, incidentes de seguridad ni notificaciones.

---

## 2. Archivos involucrados

| Archivo | Rol | Cambio |
|---|---|---|
| `src/main/resources/templates/dashboard/exportaciones.html` | Frontend | +6 `<option>` en selector + `FILTER_OPTIONS` JS |
| `src/main/java/com/lms/service/ExportacionService.java` | Service | +6 métodos `generarReporte*()` + `case` en switch |

---

## 3. Criterios de aceptación (checklist)

- [ ] **AC1:** El selector "Tipo de Reporte" tiene las 6 nuevas opciones
- [ ] **AC2:** `FILTER_OPTIONS` en JS tiene los filtros para cada nuevo reporte
- [ ] **AC3:** `ExportacionService` tiene 6 nuevos métodos `generarReporte*`
- [ ] **AC4:** El switch en `procesarExportacion` tiene los 6 nuevos `case`
- [ ] **AC5:** Una exportación real se procesa, genera XLSX en MinIO y queda descargable
- [ ] **AC6:** Las columnas del XLSX son las esperadas según especificación
- [ ] **AC7:** Los filtros realmente filtran datos (ej: solo activos, solo publicado)
- [ ] **AC8:** `mvn compile` sin errores
- [ ] **AC9:** Tests unitarios pasan en verde

---

## 4. Tests TDD (deben fallar antes y pasar después)

### Test 1 — Selector HTML tiene 12 opciones
```javascript
// Buscar en exportaciones.html: debe haber 12 <option> en #tipoReporte
document.querySelectorAll('#tipoReporte option').length === 12
```

### Test 2 — FILTER_OPTIONS tiene 12 entradas
```javascript
Object.keys(FILTER_OPTIONS).length === 12
Object.keys(FILTER_OPTIONS).includes('certificaciones')
Object.keys(FILTER_OPTIONS).includes('resultados_certificaciones')
Object.keys(FILTER_OPTIONS).includes('gestores_contenido')
Object.keys(FILTER_OPTIONS).includes('publicaciones_bpo')
Object.keys(FILTER_OPTIONS).includes('incidentes_seguridad')
Object.keys(FILTER_OPTIONS).includes('notificaciones')
```

### Test 3 — ExportacionService tiene los métodos
```bash
grep -c "public void generarReporteCertificaciones" ExportacionService.java → 1
grep -c "public void generarReporteResultadosCertificaciones" → 1
grep -c "public void generarReporteGestores" → 1
grep -c "public void generarReportePublicacionesBpo" → 1
grep -c "public void generarReporteIncidentes" → 1
grep -c "public void generarReporteNotificaciones" → 1
```

### Test 4 — Switch tiene los 6 cases
```bash
grep -c 'case "certificaciones"' ExportacionService.java → 1
grep -c 'case "resultados_certificaciones"' → 1
grep -c 'case "gestores_contenido"' → 1
grep -c 'case "publicaciones_bpo"' → 1
grep -c 'case "incidentes_seguridad"' → 1
grep -c 'case "notificaciones"' → 1
```

### Test 5 — Tests unitarios (JUnit)
```java
@Test public void generarReporteCertificaciones_GeneraXLSX() { ... }
@Test public void generarReporteResultadosCertificaciones_GeneraXLSX() { ... }
@Test public void generarReporteGestores_GeneraXLSX() { ... }
@Test public void generarReportePublicacionesBpo_GeneraXLSX() { ... }
@Test public void generarReporteIncidentes_GeneraXLSX() { ... }
@Test public void generarReporteNotificaciones_GeneraXLSX() { ... }
```

---

## 5. Tareas para el agente

### 5.1 Antes de implementar
- [ ] Escribir tests unitarios que fallen con el código actual
- [ ] Leer `ExportacionService.java` para entender el patrón existente

### 5.2 Implementación
- [ ] **Modificar `ExportacionService.java`** — agregar 6 métodos + 6 cases en switch
- [ ] **Modificar `exportaciones.html`** — agregar 6 `<option>` + 6 entradas en `FILTER_OPTIONS`

### 5.3 Verificación
- [ ] Leer cada archivo modificado y mostrar líneas exactas
- [ ] `mvn compile -DskipTests` sin errores
- [ ] `mvn test -Dtest=ExportacionServiceTest` verde
- [ ] Ejecutar Tests 1-4 (grep + JS) y mostrar resultados

---

## 6. Evidencia de finalización

*El agente llena al terminar:*

```
### Archivos modificados:
[ ] ExportacionService.java – líneas XX-YY
[ ] exportaciones.html – líneas XX-YY

### Compilación:
[pegar salida]

### Tests:
[pegar salida]

### Criterios:
- AC1 a AC9: ✅ / ❌
```
