# 🧾 Contrato de Mejora: Bug #011

> Validar estado del curso (`PUBLICADO`) al inscribir alumnos.

---

## 1. Mejora detectada

**Síntoma (qué ve el usuario administrador):**
Un administrador puede inscribir alumnos en cursos con estado `BORRADOR` o `ARCHIVADO`, contradiciendo la semántica de esos estados.

**Causa raíz (qué falta en el código):**
`InscripcionService.inscribirUsuarioConFirma()` no valida el campo `estado` del curso. Solo verifica `deletedAt IS NULL`.

**Impacto:**
- Alumnos pueden ser inscritos en cursos que el instructor aún edita (borrador).
- Cursos archivados siguen aceptando inscripciones nuevas.
- Inconsistencia entre badge visual y comportamiento real.

---

## 2. Archivos involucrados

| Archivo | Rol | Líneas clave |
|---------|-----|--------------|
| `src/main/java/com/lms/service/InscripcionService.java` | Service | 161-222 (`inscribirUsuarioConFirma`) |

---

## 3. Criterios de aceptación

- [ ] **AC1:** Inscribir en curso `"publicado"` funciona normalmente.
- [ ] **AC2:** Inscribir en curso `"borrador"` lanza error con mensaje claro.
- [ ] **AC3:** Inscribir en curso `"archivado"` lanza error con mensaje claro.
- [ ] **AC4:** Inscripción masiva via CSV reporta error para cursos no-publicados.
- [ ] **AC5:** `mvn compile` sin errores.
- [ ] **AC6:** Alumnos ya inscritos siguen viendo el curso en "Mis Cursos".

---

## 4. Tareas para el agente

### 4.1 Implementación

- [ ] **Modificar `InscripcionService.java`** — en `inscribirUsuarioConFirma()`, después de obtener el curso, agregar guarda de estado.

### 4.2 Verificación

- [ ] **Compilación:** `mvn compile` sin errores.
