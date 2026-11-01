# 🧾 Plan: Bug 001 — Faltan 6 reportes de exportación

> Generado del reporte `reportes/bug-001.md`

---

## 1. Resumen del problema

El módulo de exportaciones tiene 6 tipos de reporte. Faltan 6 más para cubrir las entidades nuevas: certificaciones, resultados de certificaciones, gestores de contenido, publicaciones BPO, incidentes de seguridad y notificaciones.

---

## 2. Archivos a modificar

| Archivo | Cambio | Tipo |
|---|---|---|
| `src/main/resources/templates/dashboard/exportaciones.html:43-51` | +6 `<option>` en el `<select id="tipoReporte">` | Template |
| `src/main/resources/templates/dashboard/exportaciones.html:135-172` | +6 entradas en `FILTER_OPTIONS` con filtros dinámicos | Template (JS) |
| `src/main/java/com/lms/service/ExportacionService.java:470-476` | +6 `case` en el switch de `procesarExportacion()` | Service |
| `src/main/java/com/lms/service/ExportacionService.java` | +6 métodos `generarReporte*()` | Service |

---

## 3. Especificación de los 6 nuevos reportes

### 3.1 Reporte "certificaciones"

**Filtros:**
- `{"all":true}` — Todas las certificaciones
- `{"activo":true}` — Solo activas
- `{"activo":false}` — Solo inactivas
- `{"tipo":"por_codigo"}` — Por código (CER###)

**Columnas XLSX:** Código, Título, Descripción, Nota Mínima (%), Tiempo (min), Activo, Creado

### 3.2 Reporte "resultados_certificaciones"

**Filtros:**
- `{"all":true}` — Todos los resultados
- `{"aprobado":true}` — Solo aprobados
- `{"aprobado":false}` — Solo reprobados
- `{"tipo":"por_codigo"}` — Por certificación (CER###)

**Columnas XLSX:** Certificación, Código, Usuario, Email, Nota (%), Aprobado, Fecha Inicio, Fecha Entrega, Estado

### 3.3 Reporte "gestores_contenido"

**Filtros:**
- `{"all":true}` — Todos los gestores
- `{"activo":true}` — Solo activos
- `{"activo":false}` — Solo inactivos

**Columnas XLSX:** Código, Nombre, Activo, Subcarpetas, Total Publicaciones, Creado

### 3.4 Reporte "publicaciones_bpo"

**Filtros:**
- `{"all":true}` — Todas las publicaciones
- `{"estado":"publicado"}` — Solo publicadas
- `{"estado":"borrador"}` — Solo borradores
- `{"estado":"archivado"}` — Solo archivadas
- `{"tipo":"procedimiento"}` — Solo procedimientos
- `{"tipo":"noticia"}` — Solo noticias
- `{"tipo":"oferta"}` — Solo ofertas

**Columnas XLSX:** Código, Título, Tipo, Descripción, Estado, Gestor, Subcarpeta, Archivo, Creado

### 3.5 Reporte "incidentes_seguridad"

**Filtros:**
- `{"all":true}` — Todos los incidentes
- `{"resuelto":true}` — Solo resueltos
- `{"resuelto":false}` — Solo pendientes

**Columnas XLSX:** ID, Tipo, Usuario Email, IP, Descripción, Resuelto, Fecha

### 3.6 Reporte "notificaciones"

**Filtros:**
- `{"all":true}` — Todas las notificaciones
- `{"leida":true}` — Solo leídas
- `{"leida":false}` — Solo no leídas

**Columnas XLSX:** ID, Usuario Email, Título, Tipo, Leída, Fecha

---

## 4. Orden de implementación

1. Agregar 6 nuevos métodos `generarReporte*()` en `ExportacionService.java` siguiendo el patrón de `generarReporteCursos()`
2. Agregar 6 `case` en el switch de `procesarExportacion()`
3. Agregar 6 `<option>` en el template `exportaciones.html`
4. Agregar 6 entradas en `FILTER_OPTIONS` en el template
5. Ejecutar tests de verificación

---

## 5. Criterios de aceptación

- [ ] **AC1:** El selector "Tipo de Reporte" tiene 12 opciones en total
- [ ] **AC2:** `FILTER_OPTIONS` en JS tiene 12 entradas
- [ ] **AC3:** `ExportacionService` tiene 6 nuevos métodos `generarReporte*`
- [ ] **AC4:** El switch en `procesarExportacion` tiene los 6 nuevos `case`
- [ ] **AC5:** Una exportación de cada nuevo tipo se procesa correctamente, genera XLSX en MinIO y queda descargable
- [ ] **AC6:** `mvn compile` sin errores
- [ ] **AC7:** Todos los tests en `tests/test-bug-001.md` pasan
