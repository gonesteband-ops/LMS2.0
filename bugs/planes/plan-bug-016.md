# 🧾 Contrato de Mejora: Bug #016

> Generado automáticamente por el agente basado en el reporte `bugs/reportes/bug-016.md`.

---

## 1. Error detectado

**Síntoma (qué ve el usuario):**  
3 formularios POST (nueva subcarpeta, crear publicación, editar publicación) devuelven HTTP 403 Forbidden. El usuario queda en una página de error Whitelabel.

**Causa raíz (qué falla en el código):**  

| Error | Causa | Verificación |
|-------|-------|-------------|
| **Error 3 (Nueva subcarpeta)** | `formulario-subcarpeta.html` fue creado en BUG-015 FASE 2 sin token CSRF y usa `${gestor.id}` (UUID) en lugar de `${gestor.codigo}` en el `th:action`. Spring Security rechaza el POST por falta de CSRF. | Confirmado por `Select-String` en el archivo |
| **Error 1 y 2 (Crear/Editar publicación)** | `formulario-publicacion.html` SÍ tiene token CSRF y SÍ usa `codigo` en el action. Pero la entidad `SubServicioBPO.codigo` podría ser `null` si V25 no generó correctamente el código, o el `MultipartFilter` no procesa el CSRF antes de que `CsrfFilter` lo valide, causando 403 en formularios con `enctype="multipart/form-data"`. | Pendiente: verificar BD que `codigo` tenga valor |

**Impacto (qué funcionalidad se rompe):**  
- No se pueden crear subcarpetas nuevas en los gestores
- No se pueden crear publicaciones nuevas
- No se pueden editar publicaciones existentes
- El admin de Gestor de Contenidos queda inutilizable para escritura

---

## 2. Archivos involucrados

| Archivo | Rol | Problema |
|---------|-----|----------|
| `src/main/resources/templates/bpo/admin/formulario-subcarpeta.html` | Template | Falta token CSRF oculto; usa `${gestor.id}` en vez de `${gestor.codigo}` |
| `src/main/resources/templates/bpo/admin/formulario-publicacion.html` | Template | Ya tiene CSRF y codigo; verificar que `subcarpeta.codigo` no sea null |
| `src/main/java/com/lms/controller/bpo/AdminBPOController.java` | Controller | Endpoints POST que aceptan datos de formularios |

---

## 3. Criterios de aceptación (checklist)

- [ ] **AC1:** Crear subcarpeta funciona sin error 403 — formulario-subcarpeta.html con CSRF + codigo
- [ ] **AC2:** Crear publicación funciona sin error 403 — formulario-publicacion.html envía POST correcto
- [ ] **AC3:** Editar publicación funciona sin error 403 — formulario-publicacion.html con datos precargados envía PUT actualizado
- [ ] **AC4:** Las URLs POST usan `codigo` (GESTOR_EJEMPLO) no UUID (a1000000-...)
- [ ] **AC5:** `mvn compile -DskipTests` sin errores
- [ ] **AC6:** `powershell -File bugs/test/test-07-fase1-csrf.ps1` pasa
- [ ] **AC7:** `powershell -File bugs/test/test-08-fase2-crear-subcarpeta.ps1` pasa (verifica CSRF y codigo)

---

## 4. Tareas para el agente

### 4.1 Antes de implementar (TDD obligatorio)

- [ ] **Escribir test `test-11-bug016-csrf-post.ps1`** que verifique:
  - `formulario-subcarpeta.html` tiene token CSRF oculto
  - `formulario-subcarpeta.html` usa `gestor.codigo` en el action
  - `formulario-publicacion.html` tiene token CSRF (ya existe, verificar que sigue presente)

### 4.2 Diagnóstico previo (antes de implementar)

- [ ] **Verificar en BD** que SubServicioBPO tenga `codigo` poblado:
  ```sql
  SELECT id, nombre, codigo FROM subservicios_bpo;
  ```
  Si `codigo` es NULL, V25 no se ejecutó correctamente. Ejecutar manualmente el UPDATE de V25.

- [ ] **Verificar en BD** la ruta del gestor de ejemplo:
  ```sql
  SELECT id, nombre, codigo FROM cuentas_bpo WHERE codigo='GESTOR_EJEMPLO';
  ```
  Confirmar que el UUID es `a1000000-0000-0000-0000-000000000001`.

- [ ] **Verificar formulario-publicacion.html** con `Select-String`:
  - Que tenga `_csrf` token (debe tenerlo de FASE 1)
  - Que el `th:action` use `gestor.codigo` y `subcarpeta.codigo`
  - Que `subcarpeta.codigo` no sea null (si es null, Thymeleaf renderiza action mal formado)

### 4.3 Implementación

- [ ] **Modificar `formulario-subcarpeta.html`** — 3 cambios:
  1. Agregar dentro del `<form>`: `<input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}" />`
  2. En el `th:action`: cambiar `(id=${gestor.id})` → `(id=${gestor.codigo})`
  3. En breadcrumb y botón Cancelar: cambiar `(id=${gestor.id})` → `(id=${gestor.codigo})`

- [ ] **Si `subcarpeta.codigo` es NULL en BD** — Ejecutar el UPDATE de V25 manualmente:
  ```sql
  UPDATE subservicios_bpo SET codigo = LOWER(REGEXP_REPLACE(TRIM(nombre), '[^a-zA-Z0-9]+', '-', 'g')) WHERE codigo IS NULL;
  ```

- [ ] **Si el problema no es `codigo` nulo** — El 403 en multipart se debe a que Spring Security no extrae el CSRF correctamente. Solución: agregar `spring.security.csrf.multipart.enabled=false` en `application-production.yml` O asegurar que el `MultipartFilter` se ejecute antes que el `CsrfFilter` en la cadena de filtros de Spring Security.

### 4.4 Verificación (el agente debe mostrar evidencia)

- [ ] **Diagnóstico:** Mostrar resultado de `SELECT codigo FROM subservicios_bpo;`
- [ ] **Lectura de archivos:** Mostrar las líneas exactas modificadas en `formulario-subcarpeta.html`
- [ ] **Test:** Ejecutar `test-08-fase2-crear-subcarpeta.ps1` (actualizado) y `test-11-bug016-csrf-post.ps1`
- [ ] **Compilación:** Ejecutar `mvn compile -DskipTests` y mostrar log sin errores

---

## 5. Evidencia de finalización

*El agente debe llenar esto al finalizar:*

```
### Lectura de archivos:
[ ] formulario-subcarpeta.html – líneas donde se agregó CSRF y se cambió id→codigo

### Compilación:
[pegar salida de mvn compile]

### Test:
[pegar salida de tests]

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

> ⚠️ **Instrucciones para el agente:** No aceptes "listo" como respuesta. Debes mostrar la evidencia de lectura de archivos, compilación y tests antes de marcar una tarea como completada.
