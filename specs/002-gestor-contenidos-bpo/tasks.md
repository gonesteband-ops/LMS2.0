---

description: "Task list for Gestor de Contenidos BPO implementation"

# Tareas: Gestor de Contenidos BPO

**Input**: Design documents from `specs/002-gestor-contenidos-bpo/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

## Formato: `[ID] [P?] [Story] Descripción`

- **[P]**: Puede ejecutarse en paralelo (diferentes archivos, sin dependencias)
- **[Story]**: A qué historia de usuario pertenece (US1-US5)
- Incluir rutas de archivo exactas en las descripciones

## Convenciones de Ruta

- **Web App**: `src/main/java/com/lms/...`, `src/main/resources/...`, `src/test/java/com/lms/...`

---

## Phase 1: Setup (Infraestructura Compartida)

**Propósito**: Inicialización del proyecto y estructura base del módulo BPO

- [ ] T001 Crear migración Flyway V20__crear_modulo_bpo.sql en src/main/resources/db/migration/ (YA CREADO)
- [ ] T002 [P] Crear subpaquete `bpo` en model/, service/, controller/, repository/, dto/, mapper/ bajo src/main/java/com/lms/
- [ ] T003 [P] Crear subdirectorio `bpo` en templates/ y static/css/bpo/ para vistas HTMX y estilos
- [ ] T004 [P] Crear BPOExceptionHandler en src/main/java/com/lms/exception/bpo/BPOExceptionHandler.java

---

## Phase 2: Foundational (Prerrequisitos Bloqueantes)

**Propósito**: Infraestructura base que DEBE estar completa antes de cualquier historia de usuario

**⚠️ CRÍTICO**: Ninguna historia de usuario puede comenzar hasta que esta fase esté completa

- [ ] T005 [P] Crear entidad CuentaBPO en src/main/java/com/lms/model/bpo/CuentaBPO.java
- [ ] T006 [P] Crear entidad SubServicioBPO en src/main/java/com/lms/model/bpo/SubServicioBPO.java
- [ ] T007 [P] Crear entidad UsuarioCuentaBPO en src/main/java/com/lms/model/bpo/UsuarioCuentaBPO.java
- [ ] T008 [P] Crear repositorio CuentaBPORepository en src/main/java/com/lms/repository/bpo/CuentaBPORepository.java
- [ ] T009 [P] Crear repositorio SubServicioBPORepository en src/main/java/com/lms/repository/bpo/SubServicioBPORepository.java
- [ ] T010 [P] Crear repositorio UsuarioCuentaBPORepository en src/main/java/com/lms/repository/bpo/UsuarioCuentaBPORepository.java
- [ ] T011 Crear servicio CuentaBPOService en src/main/java/com/lms/service/bpo/CuentaBPOService.java (consulta de cuentas/subservicios por usuario autenticado)
- [ ] T012 Crear permisos BPO en base de datos vía RolService (bpo.content.leer, crear, actualizar, eliminar, importar)
- [ ] T013 [P] Crear DTOs base: ContenidoBPOCreateDTO, ContenidoBPOResponseDTO, ContenidoBPOSearchDTO en src/main/java/com/lms/dto/bpo/
- [ ] T014 [P] Crear mapper ContenidoBPOMapper en src/main/java/com/lms/mapper/bpo/ContenidoBPOMapper.java

**Checkpoint**: Fundación lista — las historias de usuario pueden comenzar en paralelo

---

## Phase 3: User Story 1 — Consulta de contenidos por agente BPO (Priority: P1) 🎯 MVP

**Goal**: Un agente autenticado puede acceder al Gestor de Contenidos desde la barra lateral y consultar procedimientos, ofertas y noticias de su cuenta asignada.

**Independent Test**: Agente con ROLE_BPO_AGENTE asignado a "Cliente A" inicia sesión, navega a /bpo/contenidos y solo ve contenidos de Cliente A. No ve contenidos de "Cliente B".

### Tests for User Story 1

- [ ] T015 [P] [US1] Test de integración: agente ve solo contenidos de su cuenta en tests/java/com/lms/controller/bpo/ContenidoBPOControllerTest.java
- [ ] T016 [P] [US1] Test de integración: agente sin cuentas asignadas ve mensaje "No tienes cuentas asignadas"

### Implementation for User Story 1

- [ ] T017 [US1] Crear entidad ContenidoBPO en src/main/java/com/lms/model/bpo/ContenidoBPO.java
- [ ] T018 [P] [US1] Crear repositorio ContenidoBPORepository en src/main/java/com/lms/repository/bpo/ContenidoBPORepository.java (incluye método findByCuentaIdAndEstado)
- [ ] T019 [US1] Implementar ContenidoBPOService en src/main/java/com/lms/service/bpo/ContenidoBPOService.java (listar contenidos por cuenta del usuario, filtrar por estado publicado)
- [ ] T020 [US1] Crear vista principal gestor-contenidos.html en src/main/resources/templates/bpo/gestor-contenidos.html
- [ ] T021 [US1] Crear fragmento lista-contenidos.html en src/main/resources/templates/bpo/fragmentos/lista-contenidos.html
- [ ] T022 [US1] Implementar ContenidoBPOController en src/main/java/com/lms/controller/bpo/ContenidoBPOController.java (GET /bpo/contenidos, GET /bpo/contenidos/lista)
- [ ] T023 [US1] Agregar estilos CSS básicos en src/main/resources/static/css/bpo/gestor.css
- [ ] T024 [US1] Integrar entrada "Gestor de Contenidos" en la barra lateral del LMS (template layout existente)
- [ ] T025 [US1] Agregar filtro automático por cuenta del usuario autenticado en ContenidoBPOService

**Checkpoint**: User Story 1 funcional — agentes pueden consultar contenidos de sus cuentas

---

## Phase 4: User Story 2 — Administración de contenidos por supervisor BPO (Priority: P1)

**Goal**: Un supervisor puede crear, editar, publicar y eliminar contenidos para las cuentas que tiene asignadas.

**Independent Test**: Supervisor con ROLE_BPO_SUPERVISOR asignado a "Cliente A" puede crear un procedimiento para Cliente A. No puede crear contenido para "Cliente B". Admin puede crear para cualquier cuenta.

### Tests for User Story 2

- [ ] T026 [P] [US2] Test: supervisor crea contenido en cuenta asignada → 201 Created con estado borrador
- [ ] T027 [P] [US2] Test: supervisor no puede crear contenido en cuenta no asignada → 403 Forbidden
- [ ] T028 [P] [US2] Test: admin puede crear contenido para cualquier cuenta → 201 Created

### Implementation for User Story 2

- [ ] T029 [P] [US2] Agregar métodos CRUD en ContenidoBPORepository.java (save, updateSoftDelete)
- [ ] T030 [US2] Agregar métodos crear, actualizar, eliminar en ContenidoBPOService.java (con validación de permisos por cuenta)
- [ ] T031 [US2] Agregar método cambiarEstado en ContenidoBPOService.java (borrador ↔ publicado ↔ archivado con registro en Event Sourcing)
- [ ] T032 [US2] Crear vista administrar-contenidos.html en src/main/resources/templates/bpo/admin/administrar-contenidos.html
- [ ] T033 [US2] Crear vista formulario-contenido.html en src/main/resources/templates/bpo/admin/formulario-contenido.html
- [ ] T034 [US2] Crear fragmento modal-contenido.html en src/main/resources/templates/bpo/fragmentos/modal-contenido.html
- [ ] T035 [US2] Agregar métodos de administración en ContenidoBPOController.java (GET/POST /admin/bpo/contenidos, PUT/DELETE, POST cambiar-estado)
- [ ] T036 [US2] Agregar registro de auditoría (Event Sourcing) en cada operación CRUD mediante AuditLogger

**Checkpoint**: User Story 2 funcional — supervisores pueden administrar contenidos con trazabilidad

---

## Phase 5: User Story 3 — Búsqueda inteligente con filtros (Priority: P2)

**Goal**: Agentes y supervisores pueden buscar contenidos usando barra de búsqueda con filtros por tipo, cuenta y subservicio.

**Independent Test**: Agente busca "portabilidad" y recibe solo resultados de su cuenta. Supervisor puede filtrar por todas sus cuentas asignadas.

### Tests for User Story 3

- [ ] T037 [P] [US3] Test: búsqueda por texto filtra correctamente por título y descripción
- [ ] T038 [P] [US3] Test: búsqueda sin resultados muestra mensaje "No se encontraron contenidos"
- [ ] T039 [P] [US3] Test: filtro por tipo + cuenta combinados funcionan correctamente

### Implementation for User Story 3

- [ ] T040 [US3] Agregar método buscar en ContenidoBPORepository.java (consulta con texto, tipo, estado, cuenta, subservicio usando full-text search PostgreSQL)
- [ ] T041 [US3] Agregar método buscarConFiltros en ContenidoBPOService.java (aplica permisos del usuario a los filtros)
- [ ] T042 [US3] Agregar endpoint GET /bpo/contenidos/lista con query params (q, tipo, subservicio_id, estado) en ContenidoBPOController.java
- [ ] T043 [US3] Agregar barra de búsqueda con filtros en lista-contenidos.html (input + selectores con HTMX para búsqueda asíncrona)

**Checkpoint**: User Story 3 funcional — búsqueda con filtros operativa

---

## Phase 6: User Story 4 — Tarjetas de resumen superiores (Priority: P2)

**Goal**: En la parte superior del Gestor de Contenidos se muestran tarjetas con contadores de procedimientos vigentes, noticias recientes y ofertas activas.

**Independent Test**: Agente ingresa al módulo y ve tres tarjetas con números actualizados según los contenidos disponibles para su cuenta.

### Tests for User Story 4

- [ ] T044 [P] [US4] Test: tarjetas muestran contadores correctos según contenidos de la cuenta
- [ ] T045 [P] [US4] Test: tarjetas muestran 0 cuando no hay contenidos de un tipo

### Implementation for User Story 4

- [ ] T046 [P] [US4] Agregar método obtenerResumen en ContenidoBPORepository.java (contadores por tipo y cuenta)
- [ ] T047 [US4] Agregar método obtenerResumen en ContenidoBPOService.java
- [ ] T048 [US4] Agregar endpoint GET /bpo/contenidos/tarjetas en ContenidoBPOController.java
- [ ] T049 [US4] Crear fragmento tarjetas-resumen.html en src/main/resources/templates/bpo/fragmentos/tarjetas-resumen.html
- [ ] T050 [US4] Integrar carga de tarjetas en gestor-contenidos.html mediante HTMX (hx-get)

**Checkpoint**: User Story 4 funcional — tarjetas de resumen visibles

---

## Phase 7: User Story 5 — Endpoint de importación REST (Priority: P3 - Fase 2)

**Goal**: Administradores técnicos disponen de un endpoint REST para importar contenidos desde sistemas externos, con autenticación JWT.

**Independent Test**: POST a /api/v1/bpo/contenidos/importar con JSON válido y token JWT → contenido persistido con auditoría.

### Tests for User Story 5

- [ ] T051 [P] [US5] Test: importación con token válido y JSON correcto → 201 Created + evento auditoría
- [ ] T052 [P] [US5] Test: importación sin token → 401 Unauthorized
- [ ] T053 [P] [US5] Test: importación con token sin permiso → 403 Forbidden
- [ ] T054 [P] [US5] Test: importación con JSON inválido → 400 Bad Request con detalles

### Implementation for User Story 5

- [ ] T055 [US5] Crear ContenidoBPOApiController en src/main/java/com/lms/controller/bpo/ContenidoBPOApiController.java (POST /api/v1/bpo/contenidos/importar)
- [ ] T056 [US5] Agregar método importar en ContenidoBPOService.java (con validación de esquema y registro en auditoría)
- [ ] T057 [US5] Agregar validación de campos obligatorios y tipos en ContenidoBPOCreateDTO (Bean Validation)
- [ ] T058 [US5] Implementar búsqueda de cuenta por código en CuentaBPOService para el import

**Checkpoint**: User Story 5 funcional — API de importación operativa

---

## Phase 8: Polish & Cross-Cutting Concerns

**Propósito**: Mejoras que afectan múltiples historias de usuario

- [ ] T059 [P] Agregar sanitización XSS en contenido_html (persistencia y renderizado)
- [ ] T060 [P] Agregar pruebas de seguridad para filtrado por cuenta (evitar que agente vea datos de otra cuenta vía manipulación de parámetros)
- [ ] T061 Agregar bloqueo optimista (versión) en ContenidoBPO para concurrencia
- [ ] T062 Verificar que el 100% de operaciones CRUD registran evento de auditoría
- [ ] T063 [P] [CE-001] Agregar prueba de rendimiento: búsqueda de contenido debe completarse en <3s (Jmeter o MockMvc con temporización)
- [ ] T064 [P] [CE-002] Agregar prueba de rendimiento: carga inicial (tarjetas + listado) en <1.5s para 5 cuentas asignadas
- [ ] T065 [P] [CE-003] Agregar prueba de carga: simular 500 agentes consultando simultáneamente sin degradación
- [ ] T066 Ejecutar pruebas de integración completas del módulo BPO
- [ ] T067 Validar accesibilidad: navegación por teclado en formularios y listados
- [ ] T068 Ejecutar quickstart.md y verificar escenarios

---

## Dependencias y Orden de Ejecución

### Dependencias de Fase

- **Setup (Phase 1)**: Sin dependencias
- **Foundational (Phase 2)**: Depende de Setup — BLOQUEA todas las historias de usuario
- **User Stories (Phase 3-7)**: Todas dependen de Foundational
- **Polish (Phase 8)**: Depende de todas las historias de usuario implementadas

### Dependencias entre Historias de Usuario

- **User Story 1 (P1)**: Puede comenzar después de Foundational — Sin dependencias de otras historias
- **User Story 2 (P1)**: Puede comenzar después de Foundational — Independiente de US1
- **User Story 3 (P2)**: Depende de US1 (comparte vista de listado)
- **User Story 4 (P2)**: Depende de US1 (comparte página principal)
- **User Story 5 (P3)**: Independiente — Puede comenzar después de Foundational

### Dentro de Cada Historia de Usuario

- Tests (si se incluyen) DEBEN escribirse y FALLAR antes de la implementación
- Modelos antes que servicios
- Servicios antes que controladores
- Implementación core antes que integración

### Oportunidades en Paralelo

- T002, T003, T004 (Setup) pueden ejecutarse en paralelo
- T005-T010, T013-T014 (Foundational) pueden ejecutarse en paralelo
- US1 y US2 pueden implementarse en paralelo
- US5 puede comenzar en paralelo con US3/US4

---

## Estrategia de Implementación

### MVP (Solo User Story 1)

1. Phase 1: Setup
2. Phase 2: Foundational (CRÍTICO)
3. Phase 3: User Story 1
4. **STOP y VALIDAR**: Probar US1 independientemente
5. Deploy/demo si está listo

### Entrega Incremental

1. Setup + Foundational → Fundación lista
2. + US1 (consulta agente) → MVP funcional
3. + US2 (admin supervisor) → Ciclo completo
4. + US3 (búsqueda) + US4 (tarjetas) → UX completa
5. + US5 (importación API) → Integración externa

### Estrategia en Paralelo

Con múltiples desarrolladores:
1. Equipo completo: Setup + Foundational
2. Una vez lista la fundación:
   - Dev A: US1 + US3 + US4 (vistas de agente)
   - Dev B: US2 + US5 (admin + API)
3. Las historias se integran de forma independiente
