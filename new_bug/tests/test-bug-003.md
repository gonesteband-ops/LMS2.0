# ✅ Tests: Bug 003 — Certificaciones: opciones no se renderizan, nota siempre 0.0

> Tests de verificación TDD. Deben fallar ANTES de implementar y pasar DESPUÉS.
> El agente NO entrega hasta que TODOS los tests pasen.

---

## Tests

### Test 1 — No queda `document.write()` en rendir.html
```bash
grep -c "document.write" src/main/resources/templates/certificaciones/rendir.html
```

**Antes:** >= 1
**Después:** 0
**Tipo:** Bash

### Test 2 — Opciones se renderizan con `th:each` (no con JS inline)
```bash
grep -c "th:each.*opcion\|th:each.*opt" src/main/resources/templates/certificaciones/rendir.html
```

**Antes:** 0
**Después:** >= 1
**Tipo:** Bash

### Test 3 — `verificarRespuesta` maneja arrays (opción múltiple)
```bash
grep -c "ArrayList\|List.*respuestaAlumno\|instanceof.*List\|List.class" src/main/java/com/lms/service/CertificacionService.java
```

**Antes:** 0
**Después:** >= 1
**Tipo:** Bash

### Test 4 — Compilación sin errores
```bash
mvn compile -q
```

**Después:** exit code 0
**Tipo:** Bash

---

## Checklist de ejecución

- [ ] Test 1 (no document.write): ❌ / ✅
- [ ] Test 2 (th:each opciones): ❌ / ✅
- [ ] Test 3 (array handling): ❌ / ✅
- [ ] Test 4 (mvn compile): ❌ / ✅

> **Regla:** Si algún test queda como ❌, el agente NO entrega. Debe corregir y repetir.
