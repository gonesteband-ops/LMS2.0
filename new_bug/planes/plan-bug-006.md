# 🧾 Plan: Bug 006 — Integridad datos BPO: soft-delete sin cascada, sin hash, sin auditoría

> Generado del reporte `reportes/bug-006.md`

---

## 1. Resumen del problema

4 fallos de integridad que requieren migración Flyway + cambios en servicios y modelos.

---

## 2. Archivos a modificar

| Archivo | Cambio | Tipo |
|---|---|---|
| `src/main/resources/db/migration/V27__hash_integridad_bpo.sql` | **CREAR** — Agregar columna `hash_sha256` a `cuentas_bpo`, `subservicios_bpo`, `contenidos_bpo`. Agregar `deleted_at`, `deleted_by`, `reason` a `usuarios_cuenta_bpo`. | Migración |
| `src/main/java/com/lms/model/bpo/CuentaBPO.java` | Agregar campo `hashSha256` | Model |
| `src/main/java/com/lms/model/bpo/SubServicioBPO.java` | Agregar campo `hashSha256` + `deletedBy` + `reason` | Model |
| `src/main/java/com/lms/model/bpo/ContenidoBPO.java` | Agregar campo `hashSha256` | Model |
| `src/main/java/com/lms/model/bpo/UsuarioCuentaBPO.java` | Agregar `deletedAt`, `deletedBy`, `reason`, `hashSha256` + `@SQLRestriction` | Model |
| `src/main/java/com/lms/service/bpo/CuentaBPOService.java:110-115` | `eliminarCuenta()` — soft-deletear subservicios y contenidos en cascada | Service |
| `src/main/java/com/lms/service/bpo/CuentaBPOService.java:149-155` | `eliminarSubcarpeta()` — soft-deletear contenidos hijos en cascada | Service |

---

## 3. Orden de implementación

1. Migración V27 — columnas hash + soft-delete faltante
2. Modelos — agregar campos y `@SQLRestriction`
3. `CuentaBPOService.eliminarCuenta()` — cascada a subservicios y contenidos
4. `CuentaBPOService.eliminarSubcarpeta()` — cascada a contenidos

---

## 4. Criterios de aceptación

- [ ] **AC1:** `eliminarCuenta()` soft-deletea subservicios y contenidos asociados
- [ ] **AC2:** `eliminarSubcarpeta()` soft-deletea contenidos asociados
- [ ] **AC3:** `UsuarioCuentaBPO` tiene soft-delete y `@SQLRestriction`
- [ ] **AC4:** Todas las entidades BPO tienen `hashSha256`
- [ ] **AC5:** `mvn compile` sin errores
- [ ] **AC6:** Todos los tests en `tests/test-bug-006.md` pasan
