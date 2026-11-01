# 🎯 Mejoras para Rol Estudiante

> Archivo de seguimiento de tareas para ROO Code.
> Marca con `[X]` cada tarea completada.

---

## 1. 🚫 Ocultar botón "Editar" para estudiantes en la tarjeta de curso

**Problema:** El botón "Editar" en `fragmentos.html` no tiene protección por rol y al hacer clic lleva a un `Whitelabel Error Page` porque el backend (`/cursos/editar`) tiene `@PreAuthorize` solo para ADMIN/INSTRUCTOR/SUPERADMIN.

**Archivo:** `src/main/resources/templates/cursos/fragmentos.html`

**Acción:** Agregar `sec:authorize="hasAnyRole('ADMIN', 'INSTRUCTOR', 'SUPERADMIN')"` al enlace "Editar" (primer `<a>` dentro de `.curso-card` > `.acciones`).

```html
<a sec:authorize="hasAnyRole('ADMIN', 'INSTRUCTOR', 'SUPERADMIN')"
   th:href="@{'/cursos/editar?id=' + ${curso.id}}"
   class="btn btn-ghost btn-sm" ...>
```

[ ] Pendiente

---

## 2. 🚫 Ocultar "Subir y Vincular Documento" para estudiantes

**Problema:** La sección de upload de documentos en `documentos.html` está visible para estudiantes. Aunque el backend tiene `@PreAuthorize`, el formulario se muestra en la UI.

**Archivo:** `src/main/resources/templates/cursos/documentos.html`

**Acción:** Ya existe `sec:authorize="hasAnyRole('ADMIN', 'INSTRUCTOR', 'SUPERADMIN')"` en el `<div class="card animate-slide-up">` que contiene el upload. **Verificar que funcione correctamente.**

[ ] Verificar / Pendiente

---

## 3. 🚫 Ocultar "Subir Paquete SCORM" para estudiantes

**Problema:** El `drop-zone` para subir SCORM en `scorm-upload.html` está visible para estudiantes. Aunque el backend protege el endpoint, la UI muestra la sección.

**Archivo:** `src/main/resources/templates/cursos/scorm-upload.html`

**Acción:** Ya existe `sec:authorize="hasAnyRole('ADMIN', 'INSTRUCTOR', 'SUPERADMIN')"` en el contenedor `<div class="card animate-slide-up">` que envuelve el drop-zone. **Verificar que funcione correctamente.**

[ ] Verificar / Pendiente

---

## 4. 🚫 Ocultar botón "Nueva evaluación" y "Crear evaluación" para estudiantes

**Problema:** El botón "Nueva evaluación" y "Crear evaluación" en `evaluaciones/listado.html` usan `sec:authorize` con `hasAnyRole('ADMIN', 'INSTRUCTOR', 'SUPERADMIN')`, lo cual está correcto.

**Archivo:** `src/main/resources/templates/evaluaciones/listado.html`

**Acción:** Verificar que los botones estén correctamente ocultos para estudiantes.

[ ] Verificar / Pendiente

---

## 5. 👁️ Ocultar secciones de "Documentos Vinculados" a los que el estudiante no debería ver uploads

**Problema:** Aunque el upload está oculto, el mensaje vacío "No hay documentos vinculados a este curso. Suba un documento para comenzar." sugiere una acción que el estudiante no puede ejecutar.

**Archivo:** `src/main/resources/templates/cursos/documentos.html`

**Acción:** Cambiar el mensaje de estado vacío cuando el usuario es estudiante por: *"No hay documentos disponibles para este curso."*

[ ] Pendiente

---

## 6. 🚫 Ocultar enlace "SCORM" en las tarjetas de curso para estudiantes

**Problema:** En `fragmentos.html`, el enlace "SCORM" está visible para todos los roles, pero el estudiante no debería poder acceder a la gestión SCORM (solo ver contenido si está disponible).

**Archivo:** `src/main/resources/templates/cursos/fragmentos.html`

**Acción:** Agregar `sec:authorize="hasAnyRole('ADMIN', 'INSTRUCTOR', 'SUPERADMIN')"` al enlace de SCORM en las tarjetas de curso.

```html
<a sec:authorize="hasAnyRole('ADMIN', 'INSTRUCTOR', 'SUPERADMIN')"
   th:href="@{'/cursos/scorm?cursoId=' + ${curso.id}}"
   class="btn btn-ghost btn-sm" ...>
```

[ ] Pendiente

---

## 7. 🚫 Ocultar enlace "Evaluaciones" (creación) para estudiantes en tarjetas de curso

**Problema:** En `fragmentos.html`, el enlace "Evaluaciones" lleva a la página de listado que está bien protegida, pero el estudiante ve el botón y al hacer clic ve el mensaje vacío con botón "Crear evaluación" oculto. Mejor ocultar también para estudiantes o redirigir a una vista de solo lectura.

**Archivo:** `src/main/resources/templates/cursos/fragmentos.html`

**Acción:** Dejar el enlace visible para estudiantes, pero que los botones de creación estén ocultos (ya está hecho en listado.html). **Verificar consistencia.**

[ ] Verificar / Pendiente

---

## 8. 🧭 Mejorar la navegación del estudiante en el menú lateral

**Problema:** El sidebar actual muestra los mismos enlaces para todos los roles. El estudiante no necesita ver enlaces a "Exportaciones", "Firmas", etc. si no aplican a su perfil.

**Archivos:** 
- `src/main/resources/templates/cursos/listado.html`
- `src/main/resources/templates/cursos/documentos.html`
- `src/main/resources/templates/cursos/scorm-upload.html`
- `src/main/resources/templates/cursos/editor.html`
- `src/main/resources/templates/evaluaciones/listado.html`

**Acción:** Revisar que los sidebars sean consistentes y muestren solo los enlaces relevantes para cada rol.

[ ] Pendiente

---

## 9. 🎓 Mostrar solo evaluaciones disponibles para el estudiante

**Problema:** Actualmente el listado de evaluaciones (`listado.html`) muestra todas las evaluaciones del curso. El estudiante solo debería ver aquellas que están disponibles/publicadas y no ver los botones de edición.

**Archivo:** `src/main/resources/templates/evaluaciones/listado.html`

**Acción:** 
- En el `renderizarEvaluaciones`, ocultar el botón "Editar" cuando el usuario es estudiante.
- Idealmente, agregar un filtro backend para estudiantes (solo evaluaciones activas/publicadas).

[ ] Pendiente

---

## 10. 🛡️ Asegurar que el estudiante no tenga acceso a rutas de API no autorizadas

**Problema:** Aunque el backend tiene `@PreAuthorize`, algunas APIs de listado no tienen restricción y devuelven todos los datos. Confirmar que las APIs sensibles están protegidas.

**Archivos:**
- `src/main/java/com/lms/controller/ScormController.java`
- `src/main/java/com/lms/controller/EvaluacionController.java`
- `src/main/java/com/lms/controller/DocumentoController.java`
- `src/main/java/com/lms/controller/CursoController.java`

**Acción:** Verificar que todos los endpoints de creación/actualización/eliminación tengan `@PreAuthorize` adecuado.

[ ] Pendiente

---

## 11. 🧪 Probar el flujo completo como estudiante

**Acción:** Iniciar sesión como estudiante y verificar:

- [ ] El listado de cursos se muestra sin botón "Editar"
- [ ] Al hacer clic en "Documentos" de un curso, no se ve la sección "Subir y Vincular Documento"
- [ ] Al hacer clic en "SCORM" de un curso, no se ve el drop-zone de subida
- [ ] En evaluaciones, no se ve el botón "Nueva evaluación"
- [ ] No hay acceso a rutas de administración
- [ ] El sidebar no muestra opciones de administración

---

## Resumen de cambios necesarios

| # | Cambio | Archivo | Prioridad |
|---|--------|---------|-----------|
| 1 | Ocultar "Editar" en tarjetas | `fragmentos.html` | 🔴 Alta |
| 6 | Ocultar "SCORM" en tarjetas | `fragmentos.html` | 🟡 Media |
| 5 | Mensaje vacío documentos | `documentos.html` | 🟢 Baja |
| 9 | Ocultar "Editar" en evaluaciones | `evaluaciones/listado.html` | 🟡 Media |
| 8 | Sidebar consistente | Múltiples | 🟢 Baja |
| 10 | Verificar seguridad APIs | Backend controllers | 🔴 Alta |
| 11 | Prueba de hum humo | - | 🔴 Alta |
