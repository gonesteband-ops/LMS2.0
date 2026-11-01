# Lista de Verificación de Calidad UX: Gestor de Contenidos BPO

**Propósito**: Validar la calidad, claridad y completitud de los requisitos de experiencia de usuario en la especificación.
**Creado**: 2026-07-24
**Funcionalidad**: [spec.md](../spec.md)

## Completitud de Requisitos UX

- [ ] CHK001 ¿Están definidos los requisitos de estado vacío (sin contenidos) para todas las vistas? [Completitud, Spec §US1-AC2]
- [ ] CHK002 ¿Están definidos los requisitos de estado de carga (loading) para la búsqueda y listado de contenidos? [Gap]
- [ ] CHK003 ¿Están definidos los requisitos de estado de error para cuando falla la carga de tarjetas de resumen? [Gap]
- [ ] CHK004 ¿Están definidos los requisitos de la barra de búsqueda (placeholder, comportamiento mientras escribe, tiempo de debounce)? [Claridad, Spec §US3]
- [ ] CHK005 ¿Están especificados los requisitos visuales de las tarjetas de resumen (orden, colores, iconos)? [Completitud, Spec §US4]
- [ ] CHK006 ¿Están definidos los requisitos de paginación o carga infinita para la lista de contenidos? [Gap]
- [ ] CHK007 ¿Están especificados los requisitos de filtros visuales (tipo, cuenta, subservicio) en la interfaz de búsqueda? [Completitud, Spec §US3]

## Claridad de Requisitos UX

- [ ] CHK008 ¿Está cuantificado "barra lateral" con la posición exacta y el ícono de la entrada "Gestor de Contenidos"? [Claridad, Spec §RF-011]
- [ ] CHK009 ¿Está definido el comportamiento de "incluir vencidos" como un toggle visual? [Claridad, Casos borde]
- [ ] CHK010 ¿Están definidas las diferencias visuales entre la vista de agente y la vista de administración? [Claridad, Spec §RF-011]

## Consistencia UX

- [ ] CHK011 ¿Los requisitos de navegación (barra lateral) son consistentes con el resto del LMS? [Consistencia, Spec §US1]
- [ ] CHK012 ¿Los mensajes de error ("No tienes cuentas asignadas", "No tienes permisos para esta cuenta") siguen el mismo patrón que el resto del sistema? [Consistencia, Spec §US1-AC2, §US2-AC2]
- [ ] CHK013 ¿El comportamiento de "volver atrás" después de crear/editar un contenido está especificado? [Gap]

## Cobertura de Escenarios UX

- [ ] CHK014 ¿Están especificados los requisitos de accesibilidad (navegación por teclado, contraste, lectores de pantalla)? [Gap]
- [ ] CHK015 ¿Están definidos los requisitos de responsive design para la vista móvil (accesible vía web responsive)? [Completitud, Spec §Fuera de Alcance]
- [ ] CHK016 ¿Están definidos los requisitos de feedback visual al cambiar estado de un contenido (borrador → publicado)? [Gap]
