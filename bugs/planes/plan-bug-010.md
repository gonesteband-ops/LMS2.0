# 🧾 Contrato de Mejora: Bug #010

> Generado automáticamente por el agente basado en el reporte `bugs/reportes/bug-010.md`.

---

## 1. Error detectado

**Síntoma (qué ve el usuario):**  
El dashboard del estudiante tiene título "Panel Principal" (poco amigable), tarjetas KPI que no son clickeables, sidebar sin control colapsable en escritorio, y la tarjeta "Evaluaciones Pendientes" no tiene un destino útil. Además, el contador de evaluaciones pendientes siempre muestra `undefined` por un bug backend.

**Causa raíz (qué falla en el código):**  
1. `DashboardController.java:90-95` — la variable `evaluacionesPendientes` se computa pero no se incluye en el `Map.of(...)` de respuesta.
2. `dashboard/index.html:48-86` — las KPI cards son `<div>` estáticos sin `href`.
3. `layout.css` — el sidebar solo se oculta en mobile (<768px), sin clase colapsable para escritorio.
4. No existe página ni endpoint agregado para listar evaluaciones pendientes del estudiante a través de todos sus cursos.

**Impacto (qué funcionalidad se rompe):**  
- El contador de Evaluaciones Pendientes siempre muestra `—` o `0` aunque el backend tenga datos.
- El usuario debe navegar manualmente por el menú lateral; las KPI no sirven como acceso rápido.
- En escritorio, la sidebar ocupa 260px fijos sin posibilidad de colapsarse.
- No hay forma de ver en un solo lugar todas las evaluaciones pendientes del estudiante.

---

## 2. Archivos involucrados

| Archivo | Rol | Líneas clave |
|---------|-----|--------------|
| `templates/dashboard/index.html` | Template | 34, 48-86, 148-219 |
| `templates/fragments/sidebar.html` | Template | 12 |
| `templates/fragments/header.html` | Template | 139-143 (mobile menu) |
| `java/.../controller/DashboardController.java` | Controller REST | 87-95 |
| `java/.../controller/HomeController.java` | Controller Thymeleaf | 74-84 |
| `java/.../repository/IntentoEvaluacionRepository.java` | Repository | — |
| `static/css/layout.css` | CSS | 374-468, 490-560 |
| `static/js/app.js` | JS | 371-377 |
| `templates/dashboard/evaluaciones-pendientes.html` | Template **(nuevo)** | — |

---

## 3. Criterios de aceptación (checklist)

- [ ] **AC1:** El título del dashboard dice "Inicio" en el `<h1>`, en el sidebar y en los mobile pills.
- [ ] **AC2:** Las 4 tarjetas KPI superiores son enlaces clickeables (Cursos → `/cursos`, Documentos → `/dashboard/documentos`, Firmas → `/dashboard/firmas`, Eval. Pendientes → `/dashboard/evaluaciones-pendientes`) con efecto hover.
- [ ] **AC3:** El endpoint `/api/v1/dashboard/resumen` devuelve `evaluacionesPendientes` en el JSON.
- [ ] **AC4:** Existe un botón hamburguesa en el header que colapsa/expande el sidebar en todos los tamaños de pantalla, y el contenido se ajusta al ancho completo.
- [ ] **AC5:** Existe la página `/dashboard/evaluaciones-pendientes` que lista las evaluaciones pendientes del estudiante con botón "Rendir Evaluación".
- [ ] **AC6:** `mvn compile -DskipTests` sin errores.

---

## 4. Tareas para el agente

### 4.1 Antes de implementar (TDD obligatorio)

- [ ] **Escribir test unitario** para `DashboardController.obtenerResumen()` que verifique que `evaluacionesPendientes` está presente en la respuesta.
  - Archivo sugerido: `src/test/java/com/lms/controller/DashboardControllerTest.java`
  - Afirmación clave: `assertThat(response.getBody()).containsKey("evaluacionesPendientes")`

### 4.2 Implementación

- [ ] **Modificar `dashboard/index.html`** — Cambiar título a "Inicio", convertir KPI cards en `<a>` links.
- [ ] **Modificar `sidebar.html`** — Cambiar texto "Panel Principal" → "Inicio".
- [ ] **Modificar `DashboardController.java`** — Agregar `evaluacionesPendientes` al `Map.of(...)` + nuevo endpoint `GET /api/v1/dashboard/evaluaciones-pendientes`.
- [ ] **Modificar `IntentoEvaluacionRepository.java`** — Agregar método `existsByEvaluacionIdAndUsuarioIdAndEstadoAndDeletedAtIsNull` si no existe.
- [ ] **Modificar `HomeController.java`** — Agregar ruta `GET /dashboard/evaluaciones-pendientes`.
- [ ] **Crear `templates/dashboard/evaluaciones-pendientes.html`** — Template con listado de evaluaciones pendientes.
- [ ] **Modificar `layout.css`** — Agregar estilos para `.sidebar-collapsed` y hover en `a.kpi-card`.
- [ ] **Modificar `header.html`** — Agregar botón `.sidebar-toggle`.
- [ ] **Modificar `app.js`** — Agregar función `toggleSidebar()`.

### 4.3 Verificación (el agente debe mostrar evidencia)

- [ ] **Lectura de archivos:** Ejecutar `read_file` en cada archivo modificado y mostrar las líneas exactas.
- [ ] **Compilación:** Ejecutar `mvn compile -DskipTests` y mostrar log sin errores.
- [ ] **Test:** Ejecutar `mvn test` y mostrar resultado verde.

---

## 5. Evidencia de finalización

*El agente debe llenar esto al finalizar:*

```
### Lectura de archivos:
[ ] [archivo] – líneas XX-YY
[ ] [archivo] – líneas XX-YY

### Compilación:
[pegar salida de mvn compile]

### Test:
[pegar salida de mvn test]

### Criterios de aceptación cumplidos:
- [ ] AC1: ✅ / ❌
- [ ] AC2: ✅ / ❌
- [ ] AC3: ✅ / ❌
- [ ] AC4: ✅ / ❌
- [ ] AC5: ✅ / ❌
- [ ] AC6: ✅ / ❌
```

---

> ⚠️ **Instrucciones para el agente:** No aceptes "listo" como respuesta. Debes mostrar la evidencia de lectura de archivos, compilación y tests antes de marcar una tarea como completada.
