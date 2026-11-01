# Especificación de Funcionalidad: Gestor de Contenidos BPO

**Rama de funcionalidad**: `002-gestor-contenidos-bpo`
**Creado**: 2026-07-04
**Estado**: Listo para implementación
**Entrada**:| Descripción del usuario: "Crear un módulo centralizado Gestor de Contenidos BPO dentro del LMS LegalTech para que el personal de call center/BPO pueda consultar procedimientos vigentes, ofertas activas y noticias recientes. Incluye control de acceso por roles, asignación de cuentas mediante tabla UsuarioCuenta, modelo de datos con estructura base y metadatos JSONB, búsqueda inteligente con filtros, tarjetas de resumen, integración en barra lateral, y endpoint REST futuro para importación."

## Clarificaciones

### Sesión 2026-07-24

- **Q1**: ¿Debe el módulo Gestor de Contenidos BPO implementar hashing SHA-256 y consentimiento informado, o son requisitos transversales ya cubiertos por el sistema base? → **A: Opción B** — El sistema base ya cubre integridad (SHA-256) y consentimiento informado. El módulo BPO delega en el Event Sourcing existente para auditoría y solo referencia hashes. No implementa su propia capa de hashing/consentimiento.
- **Q2**: ¿Los contenidos BPO deben tener un ciclo de vida con estados explícitos o basta con el flag booleano publicado + vigencia? → **A: Opción A** — Estados explícitos: `borrador` (edición, visible solo para supervisores), `publicado` (visible para agentes, respeta vigencia), `archivado` (histórico). Cada transición registrada en Event Sourcing.
- **Q3**: ¿Los roles BPO se integran con el RBAC granular existente del LMS o son roles planos independientes? → **A: Opción A** — Roles BPO integrados con el RBAC granular existente. Cada permiso individual (`bpo.content.read`, `bpo.content.create`, etc.) se asigna a los roles, reutilizando la infraestructura de autorización del LMS.
- **Q4**: ¿Qué funcionalidades deben declararse explícitamente fuera del alcance de esta versión? → **A: Opción A** — Exclusiones: notificaciones push/email automáticas, workflow de aprobación multi-nivel, versionado de contenidos (solo versión actual + auditoría), y app móvil nativa.
- **Q5**: ¿El contenido HTML puede incluir imágenes/archivos incrustados (requiere object storage) o es solo texto enriquecido sin adjuntos? → **A: Opción A** — Solo texto enriquecido sin imágenes/archivos incrustados para v1. WYSIWYG básico (negritas, listas, tablas). Sin necesidad de object storage en esta versión.

## Escenarios de Usuario y Pruebas

### Historia de Usuario 1 - Consulta de contenidos por agente BPO (Prioridad: P1)

Como agente de call center, quiero acceder al "Gestor de Contenidos" desde la barra lateral para consultar procedimientos, ofertas y noticias vigentes, filtrados automáticamente por la cuenta y subservicios que tengo asignados, para resolver consultas de clientes durante una llamada sin salir del sistema.

**Por qué esta prioridad**: Es la funcionalidad principal del módulo. Sin ella, el BPO no puede operar.

**Prueba independiente**: Un agente con ROLE_BPO_AGENTE y asignado a la cuenta "Cliente A" inicia sesión, navega al Gestor de Contenidos y solo ve contenidos etiquetados con "Cliente A". No ve contenidos de "Cliente B".

**Escenarios de aceptación**:

1. **Dado** un agente autenticado con ROLE_BPO_AGENTE y asignado a la cuenta "Cliente A", **Cuando** accede al "Gestor de Contenidos", **Entonces** el sistema muestra solo los contenidos cuyo campo cuenta coincide con "Cliente A".
2. **Dado** un agente que no tiene asignada ninguna cuenta, **Cuando** accede al Gestor de Contenidos, **Entonces** el sistema muestra un mensaje "No tienes cuentas asignadas. Contacta a tu supervisor."
3. **Dado** un agente con una cuenta y subservicios específicos, **Cuando** usa la barra de búsqueda, **Entonces** los resultados se filtran adicionalmente por sus subservicios.

---

### Historia de Usuario 2 - Administración de contenidos por supervisor BPO (Prioridad: P1)

Como supervisor BPO, quiero crear, editar, publicar y eliminar contenidos (procedimientos, noticias, ofertas) desde la sección de administración, pero solo para las cuentas que tengo asignadas.

**Por qué esta prioridad**: Sin contenido, no hay nada que consultar. Es el habilitador del módulo.

**Prueba independiente**: Un supervisor con ROLE_BPO_SUPERVISOR asignado a "Cliente A" puede crear un nuevo procedimiento para Cliente A. No puede crear contenido para "Cliente B". Un super admin puede crear para cualquier cuenta.

**Escenarios de aceptación**:

1. **Dado** un supervisor autenticado con ROLE_BPO_SUPERVISOR asignado a "Cliente A", **Cuando** crea un nuevo contenido con tipo "procedimiento", **Entonces** el sistema lo guarda con cuenta "Cliente A" y lo registra en auditoría.
2. **Dado** un supervisor que intenta asociar un contenido a una cuenta que no tiene asignada, **Cuando** envía el formulario, **Entonces** el sistema rechaza con error y muestra "No tienes permisos para esta cuenta".
3. **Dado** un super admin (ROLE_BPO_ADMIN), **Cuando** crea contenido, **Entonces** puede seleccionar cualquier cuenta del sistema.

---

### Historia de Usuario 3 - Búsqueda inteligente con filtros (Prioridad: P2)

Como agente o supervisor, quiero usar una barra de búsqueda centralizada con filtros por tipo de contenido, cuenta y subservicio, para encontrar rápidamente la información que necesito durante una llamada.

**Por qué esta prioridad**: Mejora la productividad pero no bloquea el uso básico.

**Prueba independiente**: Un agente escribe "portabilidad" en la barra de búsqueda y recibe resultados filtrados por su cuenta. Un supervisor puede buscar en todas sus cuentas asignadas.

**Escenarios de aceptación**:

1. **Dado** un agente con cuenta "Cliente A", **Cuando** busca "portabilidad", **Entonces** el sistema retorna contenidos cuyo título o descripción contiene "portabilidad" y cuya cuenta es "Cliente A".
2. **Dado** un supervisor, **Cuando** filtra por tipo "oferta" y cuenta "Cliente B", **Entonces** el sistema retorna solo ofertas de Cliente B.
3. **Dado** cualquier usuario, **Cuando** no hay resultados, **Entonces** el sistema muestra "No se encontraron contenidos" y sugiere ampliar la búsqueda.

---

### Historia de Usuario 4 - Tarjetas de resumen superiores (Prioridad: P2)

Como agente, quiero ver en la parte superior del Gestor de Contenidos tarjetas resumen con contadores (procedimientos vigentes, noticias recientes, ofertas activas) para tener una visión rápida del estado actual.

**Por qué esta prioridad**: Mejora la experiencia de usuario y la eficiencia en llamadas.

**Prueba independiente**: Un agente ingresa al módulo y ve tres tarjetas con números actualizados según los contenidos disponibles para su cuenta.

**Escenarios de aceptación**:

1. **Dado** un agente autenticado, **Cuando** carga la página del Gestor de Contenidos, **Entonces** el sistema muestra tarjetas con:
   - "Procedimientos Vigentes": conteo de contenidos tipo "procedimiento" con vigencia activa.
   - "Noticias Recientes": conteo de contenidos tipo "noticia" creados en los últimos 7 días.
   - "Ofertas Activas": conteo de contenidos tipo "oferta" con vigencia activa.
2. **Dado** que no hay contenidos de algún tipo, **Cuando** se carga la página, **Entonces** la tarjeta correspondiente muestra 0.

---

### Historia de Usuario 5 - Endpoint de importación REST (Prioridad: P3 - Fase 2)

Como administrador del sistema (técnico), quiero disponer de un endpoint REST autenticado para que sistemas externos puedan importar contenidos automáticamente, registrando cada operación en auditoría.

**Por qué esta prioridad**: No es crítica para el lanzamiento pero es necesaria para clientes con integraciones existentes.

**Prueba independiente**: Se envía un POST a /api/v1/bpo/contenidos/importar con un JSON válido y token JWT, y el contenido se persiste con auditoría.

**Escenarios de aceptación**:

1. **Dado** un token JWT válido con permisos BPO_IMPORT, **Cuando** se envía un contenido JSON, **Entonces** el sistema lo crea y registra en auditoría con IP del llamante.
2. **Dado** un token sin permisos, **Cuando** se intenta importar, **Entonces** el sistema retorna error de autorización.
3. **Dado** un JSON con campos inválidos, **Cuando** se envía, **Entonces** el sistema retorna error con descripción del problema.

---

### Casos borde

- ¿Qué ocurre cuando un contenido tiene vigencia vencida? → No se muestra en consultas de agentes (filtro automático), pero los supervisores pueden verlo con un filtro "incluir vencidos".
- ¿Cómo se maneja la concurrencia si dos supervisores editan el mismo contenido? → Se usa bloqueo optimista con versión.
- ¿Qué pasa si una cuenta se elimina (soft delete)? → Los contenidos asociados quedan huérfanos; se debe impedir la eliminación si hay contenidos activos, o migrar a una cuenta "desactivada".
- ¿Cómo se registra la auditoría de cambios en contenidos? → Usando el sistema de auditoría existente (Event Sourcing) con tipos de evento específicos para contenidos BPO.

## Fuera de Alcance (v1)

Las siguientes funcionalidades quedan explícitamente excluidas de esta versión:
- Notificaciones push/email automáticas al publicar o modificar contenidos.
- Workflow de aprobación multi-nivel (ej. editor → supervisor → publicación).
- Versionado completo de contenidos (solo se mantiene la versión actual + trazabilidad por Event Sourcing).
- App móvil nativa; el módulo es accesible vía web responsive desde el LMS existente.

## Supuestos

- El sistema base LMS ya proporciona la infraestructura de Event Sourcing, RBAC granular, manejo de sesiones y autenticación.
- Los usuarios BPO ya existen en el sistema base; el módulo solo crea las asignaciones en UsuarioCuenta.
- El contenido HTML se almacena directamente en la base de datos y se limita a texto enriquecido sin imágenes/archivos incrustados (WYSIWYG básico: formato, listas, tablas). No requiere object storage para v1.
- La barra lateral del LMS existente soporta la inserción de nuevas entradas de navegación.
- La infraestructura de base de datos actual soporta columnas JSONB.

## Requisitos

### Requisitos funcionales

- **RF-001**: El sistema DEBE permitir definir tipos de contenido mediante una lista configurable (procedimiento, noticia, oferta, etc.).
- **RF-002**: El sistema DEBE almacenar contenidos con campos comunes (título, descripción, contenido, metadatos flexibles en JSONB, vigencia, publicación) y todos los campos de auditoría (creado_por, actualizado_por, created_at, updated_at, deleted_at, deleted_by, reason).
- **RF-003**: El sistema DEBE implementar borrado suave (Soft Delete) en todos los contenidos.
- **RF-004**: El sistema DEBE filtrar contenidos por cuenta según las asignaciones del usuario en la tabla UsuarioCuenta.
- **RF-005**: El sistema DEBE proporcionar una barra de búsqueda con filtros por tipo, cuenta y subservicio, aplicando los permisos del usuario.
- **RF-006**: El sistema DEBE mostrar tarjetas de resumen con contadores de contenidos activos por tipo, filtrados por cuenta del usuario.
- **RF-007**: El sistema DEBE registrar en auditoría (Event Sourcing) todas las operaciones CRUD sobre contenidos, delegando en el sistema base de Event Sourcing existente. Cada evento debe incluir: tipo de operación, ID del contenido, usuario responsable, timestamp e IP. No implementa capa propia de hashing SHA-256 ni consentimiento informado, ya que el sistema base los cubre de forma transversal.
- **RF-008**: El sistema DEBE permitir a supervisores y super admin crear, editar y eliminar contenidos solo en las cuentas que tienen asignadas.
- **RF-009**: El sistema DEBE incluir un endpoint REST para importación de contenidos (Fase 2) con autenticación JWT y validación de esquema.
- **RF-010**: El sistema DEBE usar tres roles nuevos integrados con el RBAC granular existente del LMS: ROLE_BPO_AGENTE (permiso `bpo.content.read`), ROLE_BPO_SUPERVISOR (permisos `bpo.content.read`, `bpo.content.create`, `bpo.content.update`, `bpo.content.delete` sobre cuentas asignadas), ROLE_BPO_ADMIN (CRUD total sobre cualquier cuenta con permisos `bpo.content.*`).
- **RF-011**: El sistema DEBE tener entrada en la barra lateral llamada "Gestor de Contenidos" que redirija a la vista de consulta para agentes y a la vista de administración para supervisores/admin.

### Entidades clave

- **ContenidoBPO**: Entidad principal que representa cualquier tipo de contenido (procedimiento, noticia, oferta, etc.). Incluye campos comunes: id (UUID), tipo (enum), título, descripción_corta, contenido_html, metadatos (JSONB), etiquetas (JSONB), estado (enum: borrador, publicado, archivado), vigencia_desde, vigencia_hasta, cuenta_id (FK), subservicio_id (FK opcional), además de los campos de auditoría y borrado suave. Las transiciones de estado se registran en Event Sourcing.
- **Cuenta**: Representa un cliente o empresa (Cliente A, Cliente B, etc.). Campos: id (UUID), nombre, código, activa (boolean), created_at, updated_at, deleted_at.
- **SubServicio**: Subdivisión de una cuenta (Ej: Cliente A Hogar, Cliente A Empresa). Campos: id (UUID), nombre, cuenta_id (FK), activo (boolean), created_at, updated_at, deleted_at.
- **UsuarioCuenta**: Tabla puente que asigna a un usuario BPO una o más cuentas y opcionalmente subservicios. Campos: usuario_id (FK), cuenta_id (FK), subservicio_id (FK nullable), created_at, created_by.


## Criterios de éxito

### Resultados medibles

- **CE-001**: Un agente puede encontrar un contenido específico mediante búsqueda en menos de 3 segundos desde que presiona "Enter".
- **CE-002**: La carga inicial del Gestor de Contenidos (tarjetas + listado) toma menos de 1.5 segundos para un usuario con 5 cuentas asignadas.
- **CE-003**: El sistema soporta 500 agentes consultando simultáneamente sin degradación en tiempos de respuesta.
- **CE-004**: Un supervisor puede crear un contenido nuevo en menos de 2 minutos.
- **CE-005**: El 100% de las operaciones CRUD sobre contenidos quedan registradas en el registro de auditoría.




  
