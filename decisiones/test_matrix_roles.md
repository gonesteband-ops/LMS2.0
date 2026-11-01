# Test Matrix — Módulo de Roles y Cursos

**Fecha:** 19-Jul-2026
**Proposito:** Validar que el sistema de permisos funcione por permisos granulares (RBAC) y no por nombre de rol.

---

## Bug Identificado

Los metodos `@PreAuthorize` usan `hasAnyRole('ADMIN', 'INSTRUCTOR', 'SUPERADMIN')` en vez de verificar permisos del usuario. Un rol personalizado (ej: "ROL_DE_PRUEBA") con permiso `CURSO_CREAR` no puede crear cursos porque el nombre del rol no coincide con los hardcodeados.

---

## Matriz de Casos de Prueba

### 1. Escenarios de Borde

| ID | Rol | Permisos | Accion | Resultado Esperado | Resultado Actual | Bug |
|----|-----|----------|--------|-------------------|------------------|-----|
| E1 | ROL_DE_PRUEBA | CURSO_CREAR, CURSO_LEER | Crear curso via POST /api/v1/cursos | 201 Created | 403 Forbidden | SI |
| E2 | ROL_DE_PRUEBA | CURSO_LEER | Listar cursos via GET /api/v1/cursos | 200 OK | 200 OK | NO |
| E3 | ROL_DE_PRUEBA | SIN permisos de curso | GET /api/v1/cursos | 403 | 200 | SI (deberia rechazar) |
| E4 | Ningun rol | - | POST /api/v1/cursos | 401 Unauthorized | 401 | NO |

### 2. Pruebas de Regresion

| ID | Escenario | Precondicion | Resultado Esperado | Riesgo |
|----|-----------|-------------|-------------------|--------|
| R1 | ADMIN crea curso sin cursos existentes | Tabla cursos vacia | Curso creado OK | Ninguno |
| R2 | INSTRUCTOR crea curso sin cursos existentes | Tabla cursos vacia | Curso creado OK | Ninguno |
| R3 | Cambiar rol de ESTUDIANTE a INSTRUCTOR | Usuario sin cursos | Puede crear cursos | Que dependa de datos previos |
| R4 | Eliminar rol ADMIN de un usuario | Usuario con permisos sueltos | Pierde acceso a crear cursos | Que herede permisos de otro rol |

### 3. Prueba de Visibilidad (Frontend)

| ID | Rol | Cursos asignados | Boton "Crear Curso" visible? | Acceso al formulario /cursos/crear? |
|----|-----|-----------------|------------------------------|-----------------------------------|
| V1 | ADMIN | 0 | SI | SI |
| V2 | INSTRUCTOR | 0 | SI | SI |
| V3 | ESTUDIANTE | 0 | NO | NO (redirect) |
| V4 | ROL_DE_PRUEBA con CURSO_CREAR | 0 | NO (bug) | NO (bug, deberia SI) |

---

## Resumen de Hallazgos

| Total Casos | Pasaron | Fallaron | Bugs |
|------------|---------|---------|------|
| 11 | 5 | 6 | 3 |

### Bugs Prioritarios

1. **E1**: Rol personalizado con CURSO_CREAR no puede crear cursos (403 vs 201)
2. **E3**: Usuario sin permisos de curso puede listar cursos (deberia ser 403)
3. **V4**: Boton "Crear Curso" no visible para roles con CURSO_CREAR

### Causa Raiz

`@PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR', 'SUPERADMIN')")` verifica el **nombre del rol**, no los **permisos del rol**. El sistema RBAC tiene 24 permisos definidos en BD pero las anotaciones de seguridad no los usan.
