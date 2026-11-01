# 🧾 Plan: Bug 008 — Calidad de código BPO

> Generado del reporte `reportes/bug-008.md`

---

## 1. Resumen del problema

5 mejoras de calidad que previenen bugs futuros y alinean el módulo BPO con las convenciones del proyecto.

---

## 2. Archivos a modificar

| Archivo | Cambio | Tipo |
|---|---|---|
| `src/main/java/com/lms/model/bpo/ContenidoBPO.java:32` | Agregar `@Pattern(regexp = "procedimiento\|noticia\|oferta")` en `tipo` | Model |
| `src/main/java/com/lms/model/bpo/ContenidoBPO.java:56` | Agregar `@Pattern(regexp = "borrador\|publicado\|archivado")` en `estado` | Model |
| `src/main/java/com/lms/model/bpo/ContenidoBPO.java:49` | Cambiar `String[] etiquetas` a `List<String>` | Model |
| `src/main/java/com/lms/model/bpo/SubServicioBPO.java` | Agregar `@UniqueConstraint(columnNames={"cuenta_id","codigo"})` | Model |
| `src/main/java/com/lms/config/DataInitializer.java:79-81` | Cambiar `setRoles()` por `getRoles().add()` + verificación previa | Config |
| `src/main/java/com/lms/controller/bpo/AdminBPOController.java:312-315` | Cambiar `findByRut()` por `findByRutAndDeletedAtIsNull()` | Controller |

---

## 3. Orden de implementación

1. `DataInitializer` — fix reemplazo de roles
2. `AdminBPOController` — fix findByRut sin filtro soft-delete
3. `SubServicioBPO` — agregar `@UniqueConstraint`
4. `ContenidoBPO` — agregar validaciones + cambiar `String[]` a `List<String>`

---

## 4. Criterios de aceptación

- [ ] **AC1:** ContenidoBPO tiene `@Pattern` en tipo y estado
- [ ] **AC2:** ContenidoBPO usa `List<String>` para etiquetas
- [ ] **AC3:** SubServicioBPO declara `@UniqueConstraint` para (cuenta_id, codigo)
- [ ] **AC4:** DataInitializer no reemplaza roles existentes
- [ ] **AC5:** `compartirGestor()` no asigna usuarios soft-deleted
- [ ] **AC6:** `mvn compile` sin errores
- [ ] **AC7:** Todos los tests en `tests/test-bug-008.md` pasan
