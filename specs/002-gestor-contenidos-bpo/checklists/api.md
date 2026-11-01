# Lista de Verificación de Calidad API: Gestor de Contenidos BPO

**Propósito**: Validar la calidad y completitud de los requisitos de API REST en la especificación.
**Creado**: 2026-07-24
**Funcionalidad**: [spec.md](../spec.md)

## Completitud de Contratos API

- [ ] CHK001 ¿Están definidos todos los endpoints REST con request/response completos? [Completitud, contracts/api-rest-gestor-contenidos.md]
- [ ] CHK002 ¿Están definidos los códigos de error HTTP para cada endpoint (400, 401, 403, 404, 422)? [Completitud, Spec §US5-AC2, §US5-AC3]
- [ ] CHK003 ¿Están definidos los formatos de respuesta de error con estructura consistente? [Completitud, contracts/api-rest-gestor-contenidos.md]
- [ ] CHK004 ¿Está especificado el esquema de paginación para el endpoint de listado? [Completitud, contracts/api-rest-gestor-contenidos.md]

## Claridad de Contratos API

- [ ] CHK005 ¿Está definido el formato exacto de fechas (ISO 8601) en todos los campos temporales? [Claridad, Spec §Entidades]
- [ ] CHK006 ¿Está definido el límite de tamaño del contenido HTML en el endpoint de importación? [Gap]
- [ ] CHK007 ¿Están definidos los campos obligatorios vs. opcionales en el body de importación? [Claridad, Spec §US5-AC3]
- [ ] CHK008 ¿Está especificado el comportamiento de rate limiting para el endpoint público de importación? [Gap]

## Consistencia API

- [ ] CHK009 ¿Los nombres de los endpoints siguen la convención REST del resto del LMS? [Consistencia]
- [ ] CHK010 ¿El formato de errores coincide con el manejador global de excepciones del LMS (GlobalExceptionHandler)? [Consistencia]
- [ ] CHK011 ¿Los nombres de los permisos (`BPO_CONTENIDO_*`) siguen la nomenclatura del sistema existente? [Consistencia, Spec §RF-010]

## Seguridad API

- [ ] CHK012 ¿Está especificado el método de autenticación (JWT) con header exacto? [Completitud, contracts/api-rest-gestor-contenidos.md]
- [ ] CHK013 ¿Está especificada la validación de permisos por cada endpoint? [Completitud, Spec §US5-AC1, §US5-AC2]
- [ ] CHK014 ¿Está especificada la validación de que la cuenta en el body coincida con las cuentas permitidas del usuario autenticado? [Gap]
