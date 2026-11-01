# 🧾 Contratos de Trabajo — Mejoras LMS LegalTech
> Basado en el análisis del archivo `mejoras_21-07.md`
> Fecha: 21-Jul-2026 | Rama: `audit_continue`

---

## 📋 Índice de contratos

| Contrato | Bug | Estado | Prioridad |
|----------|-----|--------|-----------|
| #1 | Conflicto de beans RedisTemplate | ✅ RESUELTO | — |
| #2 | Exportaciones no se procesan (quedan "pendiente") | ⏳ Pendiente | Alta |
| #3 | CSRF residual en head.html | ⏳ Pendiente | Media |
| #4 | Sidebar inconsistente entre páginas | ⏳ Pendiente | Alta |
| #5 | Editor de curso: estado siempre "borrador" y datos se borran al editar | ⏳ Pendiente | Alta |

---

---

# 🧾 Contrato #2 — Exportaciones no se procesan (quedan "pendiente")

## 1. Error detectado

**Síntoma (qué ve el usuario):**  
El usuario solicita una exportación, el sistema responde "Exportación solicitada correctamente", pero al revisar el listado la exportación siempre aparece con estado "pendiente" y nunca se completa.

**Causa raíz (qué falla en el código):**  
`ExportacionService.solicitarExportacion()` guarda la exportación con estado "pendiente" y publica un mensaje en Redis. Si Redis no está disponible (entorno local sin Redis, o en producción si el servicio cae), el mensaje nunca se publica y la exportación queda "pendiente" para siempre. No existe un **fallback síncrono** que procese la exportación directamente si Redis falla.

**Impacto (qué funcionalidad se rompe):**  
Los usuarios no pueden descargar exportaciones de datos (reportes de cursos, evaluaciones, etc.). La funcionalidad queda completamente inutilizable si Redis no está disponible.

---

## 2. Archivos involucrados

| Archivo | Rol | Líneas clave |
|---------|-----|--------------|
| `src/main/java/com/lms/service/ExportacionService.java` | Service | 162-188 |
| `src/main/java/com/lms/service/ExportacionWorker.java` | Worker | Completo |
| `src/main/java/com/lms/config/RedisConfig.java` | Config | Completo |

---

## 3. Criterios de aceptación (checklist)

- [ ] **AC1:** Si Redis está disponible, el flujo sigue igual: se guarda "pendiente", se publica mensaje en Redis, el Worker lo recoge y procesa.
- [ ] **AC2:** Si Redis NO está disponible (timeout, conexión rechazada), el sistema **no se cuelga**. Se captura la excepción y se llama a `procesarExportacion(id)` de forma síncrona.
- [ ] **AC3:** La exportación procesada de forma síncrona pasa a estado "completada" o "error" según corresponda, igual que si lo hiciera el Worker.
- [ ] **AC4:** `mvn compile -DskipTests` sin errores.
- [ ] **AC5:** Todos los tests de `ExportacionServiceTest` pasan en verde.

---

## 4. Tareas para el agente

### 4.1 Antes de implementar (TDD obligatorio)

- [ ] **Escribir test unitario** en `ExportacionServiceTest.java` que:
  - Cree un mock de `RedisTemplate` que lance una excepción al llamar a `convertAndSend()`.
  - Llame a `solicitarExportacion(...)` y verifique que la exportación se procesa (pasa a "completada" o "error").
  - Afirmación clave: `assertThat(exportacion.getEstado()).isNotEqualTo("pendiente");`

### 4.2 Implementación

- [ ] **Modificar `ExportacionService.java`** en el bloque Redis (líneas ~162-188):
  - Agregar `boolean redisDisponible = false;` antes del bloque.
  - Envolver `redisTemplate.convertAndSend()` en un `try-catch`.
  - En el `catch`, loguear un `warn`.
  - Si todo sale bien, marcar `redisDisponible = true`.
  - Después del bloque, si `!redisDisponible`, llamar a `procesarExportacion(exportacionId)`.

### 4.3 Verificación (el agente debe mostrar evidencia)

- [ ] **Lectura de archivo:** Ejecutar `read_file` en `ExportacionService.java` líneas 155-195 y mostrar el nuevo código.
- [ ] **Compilación:** Ejecutar `mvn compile -DskipTests` y mostrar log sin errores.
- [ ] **Test:** Ejecutar `mvn test -Dtest=ExportacionServiceTest` y mostrar resultado verde.

---

## 5. Evidencia de finalización

*El agente debe llenar esto al finalizar:*

```
### Lectura de archivos:
[ ] ExportacionService.java – líneas 155-195

### Compilación:
[pegar salida de mvn compile]

### Test:
[pegar salida de mvn test -Dtest=ExportacionServiceTest]

### Criterios de aceptación cumplidos:
- [ ] AC1: ✅ / ❌
- [ ] AC2: ✅ / ❌
- [ ] AC3: ✅ / ❌
- [ ] AC4: ✅ / ❌
- [ ] AC5: ✅ / ❌
```

---

---

# 🧾 Contrato #3 — CSRF residual en head.html

## 1. Error detectado

**Síntoma (qué ve el usuario):**  
Ninguno visible directamente, pero en la consola del navegador (F12) pueden aparecer errores de CSRF, y en formularios HTMX el botón se queda pegado en "Procesando..." porque el servidor responde 403 Forbidden.

**Causa raíz (qué falla en el código):**  
En `SecurityConfig.java` se ha deshabilitado CSRF (`csrf.disable()`), pero en `head.html` aún existen:
- Meta tags con tokens CSRF (líneas 34-36).
- Un handler `htmx:configRequest` que intenta setear el header `X-XSRF-TOKEN` (líneas 45-58).

Estos elementos residuales pueden causar conflictos o errores silenciosos, además de ser código muerto que confunde.

**Impacto (qué funcionalidad se rompe):**  
Código muerto que puede generar falsas expectativas sobre la seguridad CSRF y potencialmente interferir con peticiones HTMX.

---

## 2. Archivos involucrados

| Archivo | Rol | Líneas clave |
|---------|-----|--------------|
| `src/main/resources/templates/fragments/head.html` | Template | 34-36 y 45-58 |

---

## 3. Criterios de aceptación (checklist)

- [ ] **AC1:** Se han eliminado las líneas 34-36 (meta tags CSRF).
- [ ] **AC2:** Se ha eliminado el bloque de líneas 45-58 (handler JS `htmx:configRequest`).
- [ ] **AC3:** No hay regresión visual: el layout del head se mantiene igual (títulos, scripts, estilos).
- [ ] **AC4:** Las páginas que usan HTMX siguen funcionando (formularios, carga de fragmentos).
- [ ] **AC5:** `mvn compile -DskipTests` sin errores.

---

## 4. Tareas para el agente

### 4.1 Antes de implementar (TDD obligatorio)

No aplica para este bug: es solo limpieza de template, no hay test unitario que validar. Se verificará mediante lectura de archivo y compilación.

### 4.2 Implementación

- [ ] **Leer `head.html`** completo para conocer la estructura actual.
- [ ] **Eliminar líneas 34-36** (meta tags CSRF).
- [ ] **Eliminar líneas 45-58** (handler JS `htmx:configRequest`).
- [ ] **Guardar archivo** y verificar que el resto del contenido no se alteró.

### 4.3 Verificación (el agente debe mostrar evidencia)

- [ ] **Lectura de archivo:** Ejecutar `read_file` en `head.html` completo y mostrar que las líneas eliminadas ya no existen.
- [ ] **Compilación:** Ejecutar `mvn compile -DskipTests` y mostrar log sin errores.

---

## 5. Evidencia de finalización

*El agente debe llenar esto al finalizar:*

```
### Lectura de head.html:
[pegar contenido completo sin líneas CSRF]

### Compilación:
[pegar salida de mvn compile]

### Criterios de aceptación cumplidos:
- [ ] AC1: ✅ / ❌
- [ ] AC2: ✅ / ❌
- [ ] AC3: ✅ / ❌
- [ ] AC4: ✅ / ❌
- [ ] AC5: ✅ / ❌
```

---

---

# 🧾 Contrato #4 — Sidebar inconsistente entre páginas

## 1. Error detectado

**Síntoma (qué ve el usuario):**  
Al navegar entre diferentes páginas del dashboard, los enlaces del sidebar aparecen o desaparecen. Por ejemplo, el enlace "Mis Datos (ARCOP)" aparece en el dashboard principal pero no en la página de firmas o documentos. Inconsistencia total.

**Causa raíz (qué falla en el código):**  
Cada template HTML tiene su propia copia inline del sidebar (14 archivos distintos). Cuando se agrega un nuevo enlace, solo se actualiza en algunos templates. No hay un único punto de definición.

**Impacto (qué funcionalidad se rompe):**  
Mala experiencia de usuario: enlaces que desaparecen al cambiar de página. Dificultad de mantenimiento: cada cambio en el sidebar requiere modificar 14 archivos.

---

## 2. Archivos involucrados

| Archivo | Rol |
|---------|-----|
| `src/main/resources/templates/fragments/sidebar.html` | **NUEVO** — Fragmento compartido |
| `src/main/resources/templates/dashboard/index.html` | Template a modificar |
| `src/main/resources/templates/dashboard/perfil.html` | Template a modificar |
| `src/main/resources/templates/dashboard/firmas.html` | Template a modificar |
| `src/main/resources/templates/dashboard/privacidad.html` | Template a modificar |
| `src/main/resources/templates/dashboard/documentos.html` | Template a modificar |
| `src/main/resources/templates/dashboard/arcop.html` | Template a modificar |
| `src/main/resources/templates/dashboard/exportaciones.html` | Template a modificar |
| `src/main/resources/templates/cursos/listado.html` | Template a modificar |
| `src/main/resources/templates/cursos/participantes.html` | Template a modificar |
| `src/main/resources/templates/evaluaciones/listado.html` | Template a modificar |
| `src/main/resources/templates/admin/usuarios.html` | Template a modificar |
| `src/main/resources/templates/admin/roles.html` | Template a modificar |
| `src/main/resources/templates/admin/auditoria.html` | Template a modificar |
| `src/main/resources/templates/admin/incidentes.html` | Template a modificar |

---

## 3. Criterios de aceptación (checklist)

- [ ] **AC1:** "Mis Datos (ARCOP)" aparece en TODAS las páginas del dashboard.
- [ ] **AC2:** "Exportaciones" aparece solo para ADMIN/INSTRUCTOR/SUPERADMIN.
- [ ] **AC3:** "Administración" aparece solo para ADMIN/SUPERADMIN.
- [ ] **AC4:** El link activo se marca correctamente en cada página (según el parámetro `seccion`).
- [ ] **AC5:** No hay regresión visual: los estilos del sidebar se mantienen idénticos.
- [ ] **AC6:** `mvn compile -DskipTests` sin errores.
- [ ] **AC7:** Los 14 templates modificados compilan y cargan sin error en el navegador.

---

## 4. Tareas para el agente

### 4.1 Antes de implementar (TDD obligatorio)

No aplica directamente (es refactor de templates, no lógica de negocio). Se verificará mediante lectura de archivos compilación.

### 4.2 Implementación

#### Paso 1 — Crear fragmento `fragments/sidebar.html`

- [ ] Tomar el sidebar inline de `dashboard/index.html` (que es la versión más completa) y extraerlo a un nuevo archivo `fragments/sidebar.html`.
- [ ] El fragmento debe recibir un parámetro `seccion` (string) para marcar el link activo.
- [ ] El link activo se marca con `class="sidebar-link active"` cuando `seccion` coincide.
- [ ] Mantener todos los `sec:authorize` existentes.
- [ ] Incluir el nuevo enlace "Mis Datos (ARCOP)" con `sec:authorize="isAuthenticated()"`.

#### Paso 2 — Modificar los 14 templates

- [ ] Para cada template, localizar el bloque `<aside class="dashboard-sidebar">...</aside>`.
- [ ] Reemplazar TODO ese bloque por:  
  ```html
  <div th:replace="~{fragments/sidebar :: sidebar(seccion='dashboard')}"></div>
  ```
  (cambiando `'dashboard'` por el nombre de sección correspondiente)

#### Paso 3 — Mapeo de secciones por template

| Template | Valor de `seccion` |
|----------|-------------------|
| dashboard/index.html | `'dashboard'` |
| dashboard/perfil.html | `'perfil'` |
| dashboard/firmas.html | `'firmas'` |
| dashboard/privacidad.html | `'privacidad'` |
| dashboard/documentos.html | `'documentos'` |
| dashboard/arcop.html | `'arcop'` |
| dashboard/exportaciones.html | `'exportaciones'` |
| cursos/listado.html | `'cursos'` |
| cursos/participantes.html | `'cursos'` |
| evaluaciones/listado.html | `'evaluaciones'` |
| admin/usuarios.html | `'admin-usuarios'` |
| admin/roles.html | `'admin-roles'` |
| admin/auditoria.html | `'admin-auditoria'` |
| admin/incidentes.html | `'admin-incidentes'` |

### 4.3 Verificación (el agente debe mostrar evidencia)

- [ ] **Lectura de archivo:** Ejecutar `read_file` en `fragments/sidebar.html` y mostrar el contenido completo.
- [ ] **Lectura de 2 templates modificados** (ej: `dashboard/index.html` y `admin/usuarios.html`) para verificar el `th:replace`.
- [ ] **Compilación:** Ejecutar `mvn compile -DskipTests` y mostrar log sin errores.

---

## 5. Evidencia de finalización

*El agente debe llenar esto al finalizar:*

```
### Lectura de sidebar.html:
[pegar contenido completo]

### Lectura de dashboard/index.html (líneas del sidebar):
[pegar líneas donde se reemplazó por th:replace]

### Lectura de admin/usuarios.html (líneas del sidebar):
[pegar líneas donde se reemplazó por th:replace]

### Compilación:
[pegar salida de mvn compile]

### Criterios de aceptación cumplidos:
- [ ] AC1: ✅ / ❌
- [ ] AC2: ✅ / ❌
- [ ] AC3: ✅ / ❌
- [ ] AC4: ✅ / ❌
- [ ] AC5: ✅ / ❌
- [ ] AC6: ✅ / ❌
- [ ] AC7: ✅ / ❌
```

---

---

# 🧾 Contrato #5 — Editor de curso: estado siempre "borrador" y datos se borran al editar

## 1. Error detectado

**Síntoma (qué ve el usuario):**  
Al crear un curso con estado "publicado", el curso aparece como "borrador". Al editar un curso existente, los datos del curso se borran (titulo, descripcion, etc.) o el estado vuelve a "borrador".

**Causa raíz (qué falla en el código):**  
- El DTO `CursoRequest.java` no tiene campo `estado`.
- El frontend (`editor.html`) no incluye `estado` en el payload JSON.
- El service `CursoService.java` hardcodea `.estado("borrador")` en `crearCurso()` y no actualiza el estado en `actualizarCurso()`.

**Impacto (qué funcionalidad se rompe):**  
Los instructores no pueden publicar cursos. Todos los cursos creados o editados quedan en "borrador" permanentemente.

---

## 2. Archivos involucrados

| Archivo | Rol | Líneas clave |
|---------|-----|--------------|
| `src/main/java/com/lms/dto/CursoRequest.java` | DTO | Record completo |
| `src/main/java/com/lms/service/CursoService.java` | Service | 70, 108-126 |
| `src/main/resources/templates/cursos/editor.html` | Template | 659, 685-693 |

---

## 3. Criterios de aceptación (checklist)

- [ ] **AC1:** Al crear un curso con estado "publicado", el curso aparece como "Publicado" inmediatamente.
- [ ] **AC2:** Al editar un curso existente y cambiar el estado a "Archivado", se guarda como "Archivado".
- [ ] **AC3:** Al editar un curso y NO cambiar el estado, el estado original NO se pierde.
- [ ] **AC4:** Los demás campos del curso (título, descripción, contenido) no se borran al editar.
- [ ] **AC5:** `mvn compile -DskipTests` sin errores.
- [ ] **AC6:** El test unitario en `CursoServiceTest.java` pasa en verde.

---

## 4. Tareas para el agente

### 4.1 Antes de implementar (TDD obligatorio)

- [ ] **Escribir test unitario** en `CursoServiceTest.java` que:
  - Cree un DTO con `estado = "publicado"` y llame a `crearCurso()`.
  - Afirme que el curso creado tiene estado `"publicado"`.
  - Cree un DTO con `estado = null` y llame a `crearCurso()`.
  - Afirme que el curso creado tiene estado `"borrador"` (por defecto).
  - Afirmación clave: `assertThat(curso.getEstado()).isEqualTo("publicado");`
  - Segundo test similar con `actualizarCurso()`.

### 4.2 Implementación

- [ ] **Modificar `CursoRequest.java`**: Agregar `String estado` al record después de `boolean requiereFirmaCumplimiento`.
- [ ] **Modificar `CursoService.java` línea 70**: Cambiar `.estado("borrador")` por `.estado(request.estado() != null ? request.estado() : "borrador")`.
- [ ] **Modificar `CursoService.java` línea ~123**: Agregar `curso.setEstado(request.estado());` después de setear la versión.
- [ ] **Modificar `editor.html` línea 659**: Verificar que existe `var estado = document.getElementById('estado').value;`.
- [ ] **Modificar `editor.html` línea ~685**: Agregar `estado: estado,` en el objeto payload.

### 4.3 Verificación (el agente debe mostrar evidencia)

- [ ] **Lectura de archivo:** Ejecutar `read_file` en:
  - `CursoRequest.java` completo.
  - `CursoService.java` líneas 65-75 y 118-128.
  - `editor.html` líneas 655-665 y 680-695.
- [ ] **Compilación:** Ejecutar `mvn compile -DskipTests` y mostrar log sin errores.
- [ ] **Test:** Ejecutar `mvn test -Dtest=CursoServiceTest` y mostrar resultado verde.

---

## 5. Evidencia de finalización

*El agente debe llenar esto al finalizar:*

```
### Lectura de archivos:
[ ] CursoRequest.java – record completo
[ ] CursoService.java – líneas 65-75 y 118-128
[ ] editor.html – líneas 655-665 y 680-695

### Compilación:
[pegar salida de mvn compile]

### Test:
[pegar salida de mvn test -Dtest=CursoServiceTest]

### Criterios de aceptación cumplidos:
- [ ] AC1: ✅ / ❌
- [ ] AC2: ✅ / ❌
- [ ] AC3: ✅ / ❌
- [ ] AC4: ✅ / ❌
- [ ] AC5: ✅ / ❌
- [ ] AC6: ✅ / ❌
```

---

---

## 📎 Instrucciones generales para el agente

1. **Cada contrato es independiente.** No avances al siguiente hasta que el anterior esté completamente verificado y firmado.
2. **No aceptes "listo" como respuesta.** Exijo las evidencias de lectura de archivos, compilación y tests.
3. **Si un test falla**, detente, corrige el código y vuelve a ejecutar el test hasta que pase.
4. **Si encuentras un error no documentado**, repórtalo antes de continuar.
5. **Trabaja en la rama `audit_continue`.** No modifiques `main` directamente.
</lit>