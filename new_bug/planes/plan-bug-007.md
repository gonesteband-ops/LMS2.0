# 🧾 Plan: Bug 007 — Lógica de negocio BPO: tipo siempre "procedimiento", paginación rota, N+1 queries

> Generado del reporte `reportes/bug-007.md`

---

## 1. Resumen del problema

5 fallos que requieren cambios en servicios y repositorios, más una migración correctiva.

---

## 2. Archivos a modificar

| Archivo | Cambio | Tipo |
|---|---|---|
| `src/main/java/com/lms/service/bpo/ContenidoBPOService.java:116` | Cambiar ternario a `dto.getTipo() != null ? dto.getTipo() : "procedimiento"` | Service |
| `src/main/java/com/lms/service/bpo/ContenidoBPOService.java:82-85` | Usar `Pageable` en repositorio y agregar `LIMIT`/`OFFSET` | Service |
| `src/main/java/com/lms/repository/bpo/ContenidoBPORepository.java` | Agregar método con `Pageable` | Repository |
| `src/main/java/com/lms/service/bpo/ContenidoBPOService.java:172-187` | Reemplazar 3N queries por una sola query con `GROUP BY tipo` | Service |
| `src/main/java/com/lms/repository/bpo/ContenidoBPORepository.java` | Agregar `@Query` con `COUNT` y `GROUP BY tipo` | Repository |
| `src/main/java/com/lms/service/bpo/CuentaBPOService.java:49-53` | Cambiar `findAll()` + filtro a `findByActivaTrue()` | Service |
| `src/main/java/com/lms/repository/bpo/CuentaBPORepository.java` | Agregar `findByActivaTrue()` | Repository |

---

## 3. Orden de implementación

1. `CuentaBPORepository` — agregar `findByActivaTrue()`
2. `CuentaBPOService.obtenerCuentasActivas()` — usar el nuevo método
3. `ContenidoBPORepository` — agregar método paginado + método de conteo agrupado
4. `ContenidoBPOService` — fix `tipoValor`, fix paginación, fix `obtenerResumen()`

---

## 4. Criterios de aceptación

- [ ] **AC1:** Al crear publicación con tipo "noticia", se guarda como "noticia" (no "procedimiento")
- [ ] **AC2:** `listarPublicacionesPorSubcarpeta()` respeta page/size
- [ ] **AC3:** `obtenerResumen()` ejecuta 1 query por usuario (no 3N)
- [ ] **AC4:** `obtenerCuentasActivas()` usa `WHERE activa = true` en BD
- [ ] **AC5:** `mvn compile` sin errores
- [ ] **AC6:** Todos los tests en `tests/test-bug-007.md` pasan
