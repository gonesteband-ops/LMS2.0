# 🧾 Plan: Bug 010 — Certificaciones Admin: eliminar 405, sin volver, preguntas no se agregan, falta carga masiva

> Generado del reporte `reportes/bug-010.md`

---

## 1. Archivos a modificar

| Archivo | Problema | Cambio |
|---------|----------|--------|
| `src/main/resources/templates/certificaciones/listado.html` | #1 | Botón Eliminar: cambiar `<a>` con navegación GET por formulario POST con fetch DELETE |
| `src/main/resources/templates/certificaciones/participantes.html` | #2 | Agregar botón "Volver" manteniendo estilo y diseño |
| `src/main/resources/templates/certificaciones/editor.html` | #3 | Arreglar JS de `agregarPregunta()` |
| `src/main/resources/templates/certificaciones/editor.html` | #4 | Agregar sección de carga masiva + JS para parsear Excel/CSV |

---

## 2. Orden de implementación

1. **editor.html** — Fix `agregarPregunta()` (#3)
2. **listado.html** — Fix eliminar con POST + fetch DELETE (#1)
3. **participantes.html** — Agregar botón Volver (#2)
4. **editor.html** — Agregar carga masiva de preguntas (#4)

---

## 3. Criterios de aceptación

- [ ] **AC1:** Eliminar certificación funciona (no 405)
- [ ] **AC2:** Vista participantes tiene botón "Volver"
- [ ] **AC3:** "+ Agregar Pregunta" agrega preguntas al DOM
- [ ] **AC4:** Carga masiva desde Excel/CSV funciona
- [ ] **AC5:** `mvn compile` sin errores
