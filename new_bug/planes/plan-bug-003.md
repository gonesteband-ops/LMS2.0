# 🧾 Plan: Bug 003 — Certificaciones: opciones no se renderizan, nota siempre 0.0

> Generado del reporte `reportes/bug-003.md`

---

## 1. Resumen del problema

3 bugs encadenados impiden rendir certificaciones correctamente:
- A: `document.write()` sobreescribe la página → opciones no se ven
- B: Como no se ven opciones, no hay respuestas → nota 0.0
- C: `verificarRespuesta` no maneja arrays de opción múltiple

---

## 2. Archivos a modificar

| Archivo | Cambio | Tipo |
|---|---|---|
| `src/main/resources/templates/certificaciones/rendir.html:51-69` | Reemplazar `document.write()` por `th:each` sobre opciones pre-parsedas | Template |
| `src/main/java/com/lms/service/CertificacionService.java:321-343` | Agregar lógica de opción múltiple (todo o nada) en `verificarRespuesta` | Service |

---

## 3. Orden de implementación

1. Modificar `CertificacionService.verificarRespuesta` para manejar arrays (opción múltiple)
2. Modificar `rendir.html` para renderizar opciones con Thymeleaf puro en vez de `document.write()`
3. Modificar `obtenerPreguntasSanitizadas` en el controller (o service) para pasar opciones como lista parseada
4. Ejecutar tests

---

## 4. Criterios de aceptación

- [ ] **AC1:** Las 3 renderizan opciones correctamente (VF, opción única, opción múltiple)
- [ ] **AC2:** Al responder correctamente, la nota refleja el porcentaje de aciertos
- [ ] **AC3:** Opción múltiple se corrige como "todo o nada"
- [ ] **AC4:** `mvn compile` sin errores
- [ ] **AC5:** Todos los tests en `tests/test-bug-003.md` pasan
