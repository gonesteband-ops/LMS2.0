# 🧾 Contrato de Mejora: Bug #[ID]

> Generado automáticamente por el agente basado en el reporte `bugs/reportes/bug-XXX.md`.

---

## 1. Error detectado

**Síntoma (qué ve el usuario):**  
[completar]

**Causa raíz (qué falla en el código):**  
[completar]

**Impacto (qué funcionalidad se rompe):**  
[completar]

---

## 2. Archivos involucrados

| Archivo | Rol | Líneas clave |
|---------|-----|--------------|
| [ruta/archivo.java] | [DTO / Service / Controller / Template] | [XX-YY] |
| [ruta/archivo.html] | [Frontend] | [XX-YY] |

---

## 3. Criterios de aceptación (checklist)

- [ ] **AC1:** [descripción]
- [ ] **AC2:** [descripción]
- [ ] **AC3:** [descripción]
- [ ] **AC4:** `mvn compile -DskipTests` sin errores
- [ ] **AC5:** Tests unitarios pasan en verde

---

## 4. Tareas para el agente

### 4.1 Antes de implementar (TDD obligatorio)

- [ ] **Escribir test unitario** que falle con el bug actual y pase con la corrección.
  - Archivo sugerido: `src/test/java/com/lms/[service/controller]/...Test.java`
  - Afirmación clave: `assertThat(...)`

### 4.2 Implementación

- [ ] **Modificar [Archivo1.java]** — [qué hacer]
- [ ] **Modificar [Archivo2.java]** — [qué hacer]
- [ ] **Modificar [template.html]** — [qué hacer]

### 4.3 Verificación (el agente debe mostrar evidencia)

- [ ] **Lectura de archivos:** Ejecutar `read_file` en cada archivo modificado y mostrar las líneas exactas.
- [ ] **Compilación:** Ejecutar `mvn compile -DskipTests` y mostrar log sin errores.
- [ ] **Test:** Ejecutar `mvn test -Dtest=...` y mostrar resultado verde.

---

## 5. Evidencia de finalización

*El agente debe llenar esto al finalizar:*

```
### Lectura de archivos:
[ ] [Archivo1.java] – líneas XX-YY
[ ] [Archivo2.java] – líneas XX-YY
[ ] [template.html] – líneas XX-YY

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
```

---

> ⚠️ **Instrucciones para el agente:** No aceptes "listo" como respuesta. Debes mostrar la evidencia de lectura de archivos, compilación y tests antes de marcar una tarea como completada.
