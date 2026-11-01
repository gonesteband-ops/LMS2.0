# Lista de Verificación de Calidad de Seguridad: Gestor de Contenidos BPO

**Propósito**: Validar que los requisitos de seguridad de la especificación sean completos, claros y consistentes.
**Creado**: 2026-07-24
**Funcionalidad**: [spec.md](../spec.md)

## Control de Acceso (RBAC)

- [ ] CHK001 ¿Están definidos todos los permisos granulares necesarios para las operaciones BPO (LEER, CREAR, ACTUALIZAR, ELIMINAR, IMPORTAR)? [Completitud, Spec §RF-010]
- [ ] CHK002 ¿Está especificado qué permisos tiene cada rol sobre cada estado de contenido? (ej. supervisor puede ver borradores, agente no) [Claridad, Spec §Q2, RF-010]
- [ ] CHK003 ¿Está especificada la validación de que un supervisor solo opera sobre cuentas que tiene asignadas? [Completitud, Spec §US2-AC2]
- [ ] CHK004 ¿Están especificados los requisitos de autorización para el cambio de estado de contenidos? [Gap]

## Integridad y Auditoría

- [ ] CHK005 ¿Están definidos los tipos de evento de auditoría específicos para el módulo BPO? [Completitud, contracts/api-rest-gestor-contenidos.md]
- [ ] CHK006 ¿Está especificado qué datos sensibles (si los hay) deben ser anonimizados en los logs de auditoría? [Gap]
- [ ] CHK007 ¿Está especificado el requisito de que los eventos de auditoría BPO sean inmutables (INSERT-only como el sistema base)? [Completitud, Spec §RF-007]

## Validación y Saneamiento

- [ ] CHK008 ¿Está especificada la validación de contenido HTML (sanitización XSS) antes de almacenar y al renderizar? [Gap]
- [ ] CHK009 ¿Está especificada la validación de que el subservicio pertenece a la cuenta en creaciones de contenido? [Completitud, Spec §Entidades]
- [ ] CHK010 ¿Está especificada la protección contra inyección SQL en la búsqueda por texto libre? [Gap]

## Seguridad de API

- [ ] CHK011 ¿Está especificado que el endpoint de importación requiere token JWT válido con permiso específico? [Completitud, Spec §US5-AC1]
- [ ] CHK012 ¿Está especificada la validación de que el `cuenta_codigo` en la importación corresponde a una cuenta real y activa? [Gap]
- [ ] CHK013 ¿Está especificado el límite de tasa (rate limiting) para el endpoint de importación pública? [Gap]

## Cumplimiento Legal

- [ ] CHK014 ¿Está documentado que el módulo BPO NO requiere consentimiento informado propio (delegado al sistema base)? [Completitud, Spec §Q1, RF-007]
- [ ] CHK015 ¿Está documentado que el módulo BPO NO implementa SHA-256 propio (delegado al sistema base)? [Completitud, Spec §Q1]
