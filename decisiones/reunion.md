---

# Plan: Códigos Amigables en Auditoría

**Origen:** Reunión técnica 14-Jul-2026
**Problema:** Los eventos de auditoría guardan UUIDs crípticos como recursoId pero NO guardan
el código alfanumérico (DOC001, SCO001, EVA001) que el usuario conoce y usa.
Un auditor no puede buscar "qué pasó con DOC001".

---

## Tarea 1 — Backend: Agregar codigo a metadata de eventos

### 1.1 DocumentoService.java
**Archivo:** src/main/java/com/lms/service/DocumentoService.java

- Línea 98 (DOCUMENTO_SUBIDO): agregar "codigo", documento.getCodigo() al Map.of()
- Línea 196 (DOCUMENTO_ELIMINADO): agregar "codigo", documento.getCodigo() al Map.of()

### 1.2 ScormService.java
**Archivo:** src/main/java/com/lms/service/ScormService.java

- Línea 327 (SCORM_ELIMINADO): agregar "codigo", paquete.getCodigo() al Map.of()
- SCORM_SUBIDO (línea 224) ya incluye codigo -> sin cambios

### 1.3 EvaluacionService.java
**Archivo:** src/main/java/com/lms/service/EvaluacionService.java

- Línea 99 (EVALUACION_CREADA): agregar "codigo", evaluacion.getCodigo()
- Línea 158 (EVALUACION_ACTUALIZADA): agregar "codigo", evaluacion.getCodigo()
- Línea 188 (EVALUACION_ELIMINADA): agregar "codigo", evaluacion.getCodigo()

**Criterio de aceptación (T1):**
- [ ] Eventos nuevos tienen codigo en metadata para Documentos, SCORM y Evaluaciones
- [ ] mvn clean compile -q sin errores

---

## Tarea 2 — Backfill: Actualizar eventos antiguos

### 2.1 Script SQL (V11)
**Crear:** src/main/resources/db/migration/V11__backfill_codigo_auditoria.sql

La migración debe ejecutar tres UPDATEs contra eventos_auditoria:

1. Cruzar recurso_id con documentos.codigo donde recurso_tipo = 'DOCUMENTO'
2. Cruzar recurso_id con paquetes_scorm.codigo donde recurso_tipo = 'PAQUETE_SCORM'
3. Cruzar recurso_id con evaluaciones.codigo donde recurso_tipo = 'EVALUACION'

Cada UPDATE usa jsonb_build_object para agregar el campo codigo al metadata existente,
solo cuando metadata->>codigo IS NULL (evita duplicados).

**Criterio de aceptación (T2):**
- [ ] Eventos existentes en BD ahora tienen codigo en metadata (Documentos, SCORM, Evaluaciones)
- [ ] Eventos que ya tenian codigo no se duplican

---

## Tarea 3 — Frontend: Mostrar codigo en columna Recurso

### 3.1 Modificar auditoria-fragment.html
**Archivo:** src/main/resources/templates/admin/auditoria-fragment.html

Lógica de la celda Recurso:
1. Si metadata.codigo existe -> mostrar en monospace con color de acento
2. Si no hay codigo pero metadata.titulo o nombreOriginal -> mostrar ese valor
3. Si no hay nada -> UUID corto (primeros 8 caracteres + "...")

Documentos además muestran nombreOriginal como subtítulo secundario.

**Criterio de aceptación (T3):**
- [ ] Columna Recurso muestra DOC001, SCO001, EVA001 en vez del UUID crudo
- [ ] Si no hay codigo, muestra nombre/titulo como fallback
- [ ] Documentos muestran nombreOriginal como subtitulo

---

## Archivos afectados

| Archivo | Cambio |
|---------|--------|
| src/main/java/com/lms/service/DocumentoService.java | +codigo en metadata (2 lugares) |
| src/main/java/com/lms/service/ScormService.java | +codigo en SCORM_ELIMINADO (1 lugar) |
| src/main/java/com/lms/service/EvaluacionService.java | +codigo en metadata (3 lugares) |
| src/main/resources/db/migration/V11__backfill_codigo_auditoria.sql | CREAR - script backfill |
| src/main/resources/templates/admin/auditoria-fragment.html | Columna Recurso con codigo |

---

## Validación final

1. mvn clean compile -q sin errores
2. Verificar en BD que eventos nuevos tengan codigo en metadata
3. Verificar que eventos viejos se actualizaron con V11
4. Navegar a /admin/auditoria y confirmar columna Recurso muestra DOC001

---

## Estado de implementación: COMPLETADO

**Fecha:** 14-Jul-2026 21:34 CLT

- [x] T1: codigo agregado a Map.of() en DocumentoService, ScormService, EvaluacionService
- [x] T2: V11__backfill_codigo_auditoria.sql creado
- [x] T3: auditoria-fragment.html muestra codigo en columna Recurso
- [x] Tests corregidos
- [x] API healthy
