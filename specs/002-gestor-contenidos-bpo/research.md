# Investigación: Gestor de Contenidos BPO

**Fecha**: 2026-07-24
**Propósito**: Resolver incógnitas técnicas y documentar decisiones de diseño para el módulo Gestor de Contenidos BPO.

## Decisiones Técnicas

### D1: Compliance con LMS.md (SHA-256 y Consentimiento)

**Decisión**: El módulo BPO delega en el sistema base para hashing SHA-256 y consentimiento informado. No implementa capa propia.

**Contexto**: LMS.md exige SHA-256 para todo registro sensible y consentimiento informado para acciones críticas. El spec RF-007 ya delega en Event Sourcing existente.

**Alternativas consideradas**:
- SHA-256 obligatorio en cada contenido BPO + flujo de consentimiento → Descartado: duplica lógica del sistema base
- SHA-256 en contenidos pero sin consentimiento → Descartado: el sistema base ya cubre integridad

**Referencias**: system.md (regla 4), LMS.md (Sección 1 y 3), Q1 en spec.md

---

### D2: Ciclo de Vida de Contenidos (Estados)

**Decisión**: Estados explícitos: `borrador` → `publicado` → `archivado`. Cada transición registrada en Event Sourcing.

**Contexto**: La alternativa de solo flag booleano `publicado` no permitía diferenciar contenidos en edición vs. publicados vs. históricos. El estado `archivado` complementa a `vigencia_hasta` para contenidos que expiraron.

**Implementación**: Campo `estado` de tipo ENUM en PostgreSQL (`borrador`, `publicado`, `archivado`). Validaciones:
- Solo supervisores pueden cambiar estado
- `borrador` → `publicado` requiere vigencia_desde y vigencia_hasta definidas
- `publicado` → `archivado` puede ser manual o automático al vencer vigencia
- `archivado` → `publicado` no permitido (debe crear nuevo contenido o una transición controlada)

**Alternativas consideradas**:
- Solo booleano `publicado` → Descartado: sin trazabilidad de ediciones
- Flag + estado ENUM sin máquina de estados → Descartado: transiciones sin validación centralizada

**Referencias**: Q2 en spec.md, RF-002 modificado

---

### D3: Integración RBAC

**Decisión**: Roles BPO integrados con el RBAC granular existente. Cada permiso individual se asigna a los roles BPO reutilizando la infraestructura actual (Rol, Permiso, RolService).

**Contexto**: El sistema LMS ya implementa RBAC granular con `Permiso` (recurso+acción) y `Rol` (conjunto de permisos). Los nuevos roles BPO se insertan en esta misma estructura.

**Permisos a crear**:
- `bpo.content.read` — ROLE_BPO_AGENTE, ROLE_BPO_SUPERVISOR, ROLE_BPO_ADMIN
- `bpo.content.create` — ROLE_BPO_SUPERVISOR, ROLE_BPO_ADMIN
- `bpo.content.update` — ROLE_BPO_SUPERVISOR, ROLE_BPO_ADMIN
- `bpo.content.delete` — ROLE_BPO_SUPERVISOR, ROLE_BPO_ADMIN

**Alternativas consideradas**:
- Roles BPO hardcodeados en el módulo → Descartado: duplica lógica de autorización existente
- Roles planos sin permisos granulares → Descartado: incompatible con RBAC del LMS

**Referencias**: Q3 en spec.md, RF-010 modificado, `src/main/java/com/lms/model/Permiso.java`

---

### D4: Exclusiones de Alcance v1

**Decisión**: Excluir notificaciones automáticas, workflow de aprobación multi-nivel, versionado completo de contenidos y app móvil nativa.

**Contexto**: El MVP debe enfocarse en el core: consulta de contenidos por agentes y administración por supervisores. Las exclusiones se reevaluarán para Fase 2.

**Impacto en planificación**: Estas exclusiones simplifican el plan eliminando la necesidad de:
- Servicio de notificaciones asíncrono (Redis/Celery)
- Tabla de aprobaciones y flujo multi-paso
- Esquema de versionado de contenidos
- API mobile nativa

**Referencias**: Q4 en spec.md, Sección "Fuera de Alcance (v1)"

---

### D5: Contenido Enriquecido sin Assets Externos

**Decisión**: `contenido_html` se limita a texto enriquecido sin imágenes/archivos incrustados para v1. WYSIWYG básico (formato, listas, tablas). Sin necesidad de object storage.

**Contexto**: El sistema tiene MinIO configurado (pom.xml), pero para v1 el contenido se almacena directamente en la BD como texto HTML. Si en el futuro se requieren imágenes incrustadas, se migrarán a MinIO.

**Implementación**: El campo `contenido_html` es `TEXT` en PostgreSQL. El editor WYSIWYG (a definir) debe configurarse en modo texto-enriquecido sin upload de imágenes.

**Alternativas consideradas**:
- Soporte completo de imágenes + attachments con MinIO → Postergado para Fase 2
- Solo texto plano sin formato → Descartado: los procedimientos BPO requieren formato (listas, tablas)

**Referencias**: Q5 en spec.md, Sección "Supuestos", LMS.md Sección 4

## Investigación de Patrones Existentes

### Patrón de Controladores HTMX existentes

Se revisó `CursoHtmxController.java` y `AdminHtmxController.java` como referencia para implementar los controladores BPO con HTMX:
- Los controladores devuelven fragmentos HTML (no JSON) para las vistas HTMX
- Usan `@Controller` (no `@RestController`) para vistas
- Las rutas siguen el patrón `/admin/bpo/...` y `/bpo/...`

### Patrón de Event Sourcing existente

`AuditLogger.java` y `EventoAuditoria.java` ya implementan:
- Eventos inmutables (INSERT-only)
- Hash encadenado para integridad
- Metadatos JSONB para payload flexible
- Tipos de evento por categoría

El módulo BPO reutilizará `AuditLogger` para registrar eventos con tipo `BPO_CONTENIDO_*`.

### Patrón de RBAC existente

`RolService.java`, `Permiso.java` y `Rol.java` implementan:
- Roles con permisos granulares (recurso + acción)
- Roles de sistema (no editables) y roles personalizados
- Asignación usuario ↔ rol

Los nuevos permisos BPO se crearán como registros en la tabla `permisos` y se asignarán a los nuevos roles.
