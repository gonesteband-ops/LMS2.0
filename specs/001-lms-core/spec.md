# Especificación del Sistema LMS - Core

**Feature Branch**: `001-lms-core`
**Creado**: 2026-07-04
**Estado**: Draft → Final
**Input**: Especificación Maestra del Sistema LMS (LMS.md)

---

## Resumen Ejecutivo

Sistema de Gestión de Aprendizaje (LMS) con enfoque LegalTech, diseñado bajo principios de **inmutabilidad**, **trazabilidad total (Event Sourcing)**, **cumplimiento legal** (Leyes 19.799 y 21.719) y **seguridad por diseño**. El sistema gestiona cursos, evaluaciones, documentos, usuarios con roles granulares (RBAC) y proporciona dashboards ejecutivos con exportación XLSX.

---

## User Stories & Testing

### Historia de Usuario 1 - Registro y Consentimiento Informado (Prioridad: P1)

**Descripción**: Como usuario del sistema, quiero registrarme y otorgar consentimiento informado (capturando IP, User-Agent, Hash del documento legal y versión de política) para cumplir con la Ley 19.799 antes de acceder a cualquier funcionalidad.

**Por qué esta prioridad**: Sin consentimiento informado, el sistema no puede operar legalmente. Es el bloqueo fundacional.

**Test Independiente**: Puede verificarse creando un usuario, confirmando que el flujo de consentimiento se completa y que el registro de auditoría contiene IP, User-Agent, Hash SHA-256 del documento aceptado.

**Escenarios de Aceptación**:
1. **Dado** un usuario no registrado, **Cuando** completa el formulario de registro con datos válidos, **Entonces** el sistema crea el usuario con UUID, hash de contraseña (SHA-256) y redirige al flujo de consentimiento.
2. **Dado** un usuario en flujo de consentimiento, **Cuando** acepta los términos, **Entonces** el sistema registra: UUID usuario, IP, User-Agent, Hash SHA-256 del documento, versión de política, timestamp.
3. **Dado** un usuario que rechaza el consentimiento, **Cuando** intenta acceder al sistema, **Entonces** se deniega el acceso y se registra el evento de rechazo.

---

### Historia de Usuario 2 - Firma Electrónica y No Repudio (Prioridad: P1)

**Descripción**: Como usuario autenticado, quiero firmar electrónicamente documentos y eventos críticos para que quede registro vinculante (IP, timestamp, User-Agent, geolocalización aproximada) bajo la Ley 19.799.

**Por qué esta prioridad**: La firma electrónica es el mecanismo de no repudio central del sistema.

**Test Independiente**: Firmar un documento y verificar que el evento de firma contiene todos los campos requeridos y que el hash encadenado es válido.

**Escenarios de Aceptación**:
1. **Dado** un usuario autenticado, **Cuando** firma un documento, **Entonces** el sistema registra: UUID, IP, timestamp, User-Agent, geolocalización, Hash SHA-256 del documento, Hash del evento anterior (encadenamiento).
2. **Dado** un evento de firma registrado, **Cuando** se verifica la cadena de hash, **Entonces** el sistema confirma la integridad (no repudio).

---

### Historia de Usuario 3 - Gestión de Cursos y Versionado (Prioridad: P1)

**Descripción**: Como administrador académico, quiero crear y versionar cursos con su historial completo de cambios, hashes de integridad y metadatos de auditoría.

**Por qué esta prioridad**: Los cursos son el contenido central del LMS. El versionado garantiza trazabilidad.

**Test Independiente**: Crear un curso, modificarlo 3 veces, verificar que existen 3 versiones con hashes únicos y que cada versión referencia a la anterior.

**Escenarios de Aceptación**:
1. **Dado** un administrador autenticado, **Cuando** crea un curso, **Entonces** se genera versión 1.0 con UUID, hash SHA-256, timestamp y autor.
2. **Dado** un curso existente, **Cuando** se actualiza su contenido, **Entonces** se crea una nueva versión con hash propio y referencia a versión anterior.
3. **Dado** un curso con múltiples versiones, **Cuando** se consulta el historial, **Entonces** se retorna la lista completa con metadatos de cada versión.

---

### Historia de Usuario 4 - Evaluaciones con Editor Multimedia (Prioridad: P2)

**Descripción**: Como instructor, quiero crear evaluaciones con editor WYSIWYG, soporte multimedia, pesos editables y cálculo de tiempo de respuesta.

**Por qué esta prioridad**: Las evaluaciones son funcionalidad core pero no bloquean el registro/firma.

**Test Independiente**: Crear una evaluación con preguntas multimedia, asignar pesos, verificar que el tiempo de respuesta calculado es correcto.

**Escenarios de Aceptación**:
1. **Dado** un instructor autenticado, **Cuando** crea una evaluación con preguntas y pesos, **Entonces** el sistema calcula tiempo estimado y almacena metadatos.
2. **Dado** un estudiante, **Cuando** responde una evaluación, **Entonces** el sistema registra tiempo por respuesta (ms).

---

### Historia de Usuario 5 - Dashboards y Exportación XLSX (Prioridad: P2)

**Descripción**: Como administrador, quiero visualizar dashboards ejecutivos con KPIs y exportar reportes XLSX sin bloquear la interfaz (procesos en segundo plano con Spring `@Async`/TaskExecutor).

**Por qué esta prioridad**: Los dashboards son visibles pero la exportación asíncrona es crítica para UX.

**Test Independiente**: Solicitar exportación XLSX, verificar que se encola como tarea asíncrona, confirmar que el archivo generado contiene los datos correctos.

**Escenarios de Aceptación**:
1. **Dado** un administrador en dashboard, **Cuando** solicita exportación XLSX, **Entonces** la tarea se procesa asíncronamente y se notifica cuando esté lista.
2. **Dado** un dashboard cargado, **Cuando** se aplican filtros, **Entonces** los KPIs se actualizan dinámicamente.

---

### Historia de Usuario 6 - Administración RBAC y Auditoría (Prioridad: P2)

**Descripción**: Como superadmin, quiero gestionar roles con permisos granulares (RBAC) y auditar todas las acciones críticas mediante Event Sourcing.

**Por qué esta prioridad**: La seguridad y trazabilidad son requisitos legales.

**Test Independiente**: Asignar un permiso a un rol, verificar que el cambio se registra como evento, confirmar que el usuario afectado refleja el nuevo permiso.

**Escenarios de Aceptación**:
1. **Dado** un superadmin, **Cuando** modifica permisos de un rol, **Entonces** se registra evento: quién, qué cambió, cuándo, IP.
2. **Dado** un administrador, **Cuando** realiza impersonación, **Entonces** se registra motivo, IP, fecha y duración.

---

### Casos Borde

- ¿Qué ocurre cuando un usuario intenta registrar sin conexión a internet? → El sistema debe manejar timeout y reintentar.
- ¿Cómo maneja el sistema la expiración de sesión durante una firma electrónica? → Se requiere reautenticación antes de completar la firma.
- ¿Qué pasa cuando el Object Storage (MinIO/S3) no está disponible? → El sistema debe responder con error controlado y reintentar.
- ¿Cómo se maneja la concurrencia en versionado de cursos? → Optimistic Locking con versión.

---

## Requisitos

### Requisitos Funcionales

- **FR-001**: El sistema DEBE usar UUIDs como Primary Key para todas las entidades.
- **FR-002**: El sistema DEBE implementar Soft Delete (deleted_at, deleted_by, reason) en todas las entidades.
- **FR-003**: El sistema DEBE calcular Hash SHA-256 para todo archivo y registro sensible.
- **FR-004**: El sistema DEBE implementar Event Sourcing: solo INSERT, prohibido UPDATE en logs.
- **FR-005**: El sistema DEBE capturar IP, User-Agent, Timestamp en eventos de consentimiento y firma.
- **FR-006**: El sistema DEBE soportar RBAC con permisos granulares (no solo por rol).
- **FR-007**: El sistema DEBE implementar MFA para administradores.
- **FR-008**: El sistema DEBE almacenar archivos en Object Storage (MinIO/S3); BD guarda solo metadatos.
- **FR-009**: El sistema DEBE procesar exportaciones XLSX en segundo plano (Spring `@Async`/TaskExecutor).
- **FR-010**: El sistema DEBE mantener versionado completo de cursos, documentos y preguntas.
- **FR-011**: El sistema DEBE cifrar datos en reposo con AES-256 y en tránsito con TLS 1.3.
- **FR-012**: El sistema DEBE integrar OpenTelemetry/Prometheus/Grafana para observabilidad.
- **FR-013**: El sistema DEBE registrar impersonación con motivo, IP, fecha y duración.
- **FR-014**: El sistema DEBE soportar gamificación (badges) — Pendiente para Fase 2.
- **FR-015**: El sistema DEBE soportar notificaciones automáticas (vencimientos, reprobación, logros, sistema) con dropdown en tiempo real y página dedicada.
- **FR-016**: El sistema DEBE soportar paquetes SCORM 1.2 y 2004 (carga ZIP, descompresión, Runtime API REST, reproductor iframe).
- **FR-017**: El sistema DEBE incluir un editor WYSIWYG basado en Quill.js para contenido de cursos y evaluaciones.
- **FR-018**: El sistema DEBE incluir un reproductor multimedia para videos incrustados (URL/youtube).

### Entidades Clave

- **Usuario**: Entidad raíz de identidad. UUID, email, nombre, hash contraseña (SHA-256), MFA habilitado, roles.
- **Consentimiento**: Registro de aceptación de términos. UUID usuario, IP, User-Agent, Hash documento, versión política, timestamp.
- **FirmaElectronica**: Evento de firma vinculante. UUID usuario, UUID documento, IP, timestamp, User-Agent, geolocalización, hash evento anterior.
- **Curso**: Unidad de aprendizaje. UUID, título, descripción, versión actual, hash. Versionado completo.
- **VersionCurso**: Historial de cambios del curso. UUID, UUID curso, número versión, contenido, hash, hash versión anterior.
- **Evaluacion**: Prueba o examen. UUID, UUID curso, configuración (pesos, tiempo), multimedia.
- **Pregunta**: Componente de evaluación. UUID, UUID evaluación, tipo, contenido WYSIWYG, peso, tiempo respuesta (ms).
- **Documento**: Archivo almacenado en Object Storage. UUID, url, hash, mime, versión, metadatos.
- **EventoAuditoria**: Registro de Event Sourcing. UUID, tipo, actor, acción, metadata (IP, User-Agent), hash cadena, timestamp.
- **Rol**: Conjunto de permisos. UUID, nombre, permisos (granular).
- **Permiso**: Acción específica asignable. UUID, nombre, recurso, acción.
- **Exportacion**: Solicitud de exportación XLSX. UUID, solicitante, filtros, estado, url resultado.
- **PaqueteScorm**: Paquete SCORM subido. UUID, curso_id, título, versión SCORM (1.2/2004), url_zip (MinIO), url_index, hash, tamaño.
- **ScormDatoAlumno**: Datos de sesión SCORM (CMI). UUID, paquete_id, usuario_id, intento_id, clave, valor. Unique compuesto (paquete, usuario, intento, clave).
- **Notificacion**: Notificación automática. UUID, usuario_id (FK), tipo (vencimiento/reprobacion/logro/sistema), título, mensaje, link, leida, created_at.

---

## Criterios de Éxito

### Resultados Medibles

- **SC-001**: Registro de usuario + consentimiento completado en < 2 minutos.
- **SC-002**: Firma electrónica con no repudio verificable en < 5 segundos.
- **SC-003**: Sistema soporta 1000 usuarios concurrentes sin degradación.
- **SC-004**: Exportación XLSX asíncrona completada en < 30 segundos para 10K registros.
- **SC-005**: 100% de eventos críticos registrados en log de auditoría inmutable.
- **SC-006**: Zero datos perdidos en failover de base de datos (réplicas).

---

## Supuestos

- Los usuarios tienen conexión a internet estable (no se requiere modo offline).
- La infraestructura cloud permitirá escalado horizontal.
- Se usará PostgreSQL como base de datos principal por su soporte de UUID, JSONB y capacidades transaccionales.
- Se usará Spring Boot 3.3 como framework backend por su ecosistema maduro, seguridad integrada y facilidad de despliegue.
- Spring Data JPA / Hibernate como ORM.
- Flyway para migraciones de base de datos.
- MinIO como Object Storage local (reemplazable por S3 en producción).
- Spring `@Async` + TaskExecutor para tareas asíncronas.
- OpenTelemetry + Prometheus + Grafana para observabilidad.
- Docker y Docker Compose para orquestación de servicios en desarrollo.
- JUnit 5 + Mockito para testing unitario y de integración.
- Thymeleaf + HTMX para el frontend (server-side rendering con interactividad dinámica).
- Apache POI para generación de archivos XLSX.
- JJWT (Java JWT) para autenticación stateless.

---

## Stack Tecnológico

| Componente | Tecnología | Versión | Propósito |
|-----------|-----------|---------|-----------|
| Backend | Spring Boot | 3.3 | Framework REST API |
| ORM | Spring Data JPA / Hibernate | 6.4+ | Mapeo objeto-relacional |
| Base de Datos | PostgreSQL | 16+ | Persistencia principal |
| Migraciones | Flyway | 10+ | Control de esquema BD |
| Cache | Redis | 7+ | Caché distribuida, pub/sub exportaciones |
| Tareas Async | Spring `@Async` / TaskExecutor | - | Background jobs (exportaciones, notificaciones) |
| Object Storage | MinIO | Última | Almacenamiento de archivos y paquetes SCORM |
| Observabilidad | OpenTelemetry | Última | Trazas y métricas |
| Dashboards | Prometheus + Grafana | Última | Monitoreo |
| Testing | JUnit 5 + Mockito | 5.10+ / 5+ | Tests unitarios/integración |
| Autenticación | JWT (JJWT) + Spring Security | 0.12+ / 6.3 | Auth stateless + seguridad |
| Contenedores | Docker + Compose | Última | Orquestación local |
| Frontend | Thymeleaf + HTMX | 3.1+ / 2+ | Server-side rendering dinámico |
| Editor WYSIWYG | Quill.js | 1.3.7 | Editor de texto enriquecido para cursos/evaluaciones |
| SCORM Runtime | API REST propia | - | Runtime API para paquetes SCORM 1.2/2004 |
| Generación XLSX | Apache POI | 5.2+ | Exportación a Excel |
| Validación | Jakarta Validation | 3.0+ | Validación de DTOs |
| Lenguaje | Java | 17+ | Plataforma de desarrollo |
| Build | Maven | 3.9+ | Gestión de dependencias y构建 |

---

## Estructura de Directorios (Actual)

```text
/
├── .specify/                  # Configuración speckit
├── specs/                     # Especificaciones
│   └── 001-lms-core/
│       ├── spec.md            # ← Este archivo (especificación funcional)
│       ├── plan.md            # Plan de implementación
│       ├── tasks.md           # Tareas detalladas (12 fases, 119 tareas)
│       ├── clarificaciones.md # ADRs y decisiones técnicas (ADR-001 al ADR-012)
│       └── data-model.md      # Modelo de datos
├── src/
│   ├── main/
│   │   ├── java/com/lms/
│   │   │   ├── LMSApplication.java            # Punto de entrada Spring Boot
│   │   │   ├── config/                        # Configuración central
│   │   │   │   ├── DataInitializer.java       # Seed data inicial
│   │   │   │   ├── MinioConfig.java           # Cliente MinIO/S3
│   │   │   │   ├── OpenTelemetryConfig.java   # Trazas y métricas
│   │   │   │   ├── RedisConfig.java           # Redis pub/sub + caché
│   │   │   │   ├── ThymeleafConfig.java       # Motor de templates
│   │   │   │   └── WebConfig.java             # CORS/URLs
│   │   │   ├── controller/                    # Controladores REST + Vista
│   │   │   │   ├── HomeController.java        # @Controller rutas de navegación
│   │   │   │   ├── AuthController.java        # /api/v1/auth (registro, login, MFA)
│   │   │   │   ├── UsuarioController.java     # /api/v1/usuarios
│   │   │   │   ├── CursoController.java       # /api/v1/cursos (REST)
│   │   │   │   ├── EvaluacionController.java  # /api/v1/evaluaciones
│   │   │   │   ├── DocumentoController.java   # /api/v1/documentos
│   │   │   │   ├── FirmaController.java       # /api/v1/firmas
│   │   │   │   ├── ConsentimientoController.java # /api/v1/consentimiento
│   │   │   │   ├── ExportacionController.java # /api/v1/exportaciones
│   │   │   │   ├── AuditoriaController.java   # /api/v1/auditoria
│   │   │   │   ├── RolController.java         # /api/v1/roles
│   │   │   │   ├── ScormController.java       # /api/v1/scorm (Runtime API REST)
│   │   │   │   └── NotificacionController.java # /api/v1/notificaciones
│   │   │   ├── model/                         # Entidades JPA
│   │   │   │   ├── Usuario.java               # UUID, email, hash, MFA, roles
│   │   │   │   ├── Consentimiento.java        # IP, User-Agent, hash documento
│   │   │   │   ├── FirmaElectronica.java      # Hash encadenado, geolocalización
│   │   │   │   ├── Curso.java                 # Título, descripción, estado
│   │   │   │   ├── VersionCurso.java          # Versionado con hash encadenado
│   │   │   │   ├── Evaluacion.java            # Pesos, tiempo estimado
│   │   │   │   ├── Pregunta.java              # Contenido WYSIWYG, opciones
│   │   │   │   ├── Documento.java             # Metadatos de archivo en MinIO
│   │   │   │   ├── EventoAuditoria.java       # Event Sourcing (solo INSERT)
│   │   │   │   ├── Exportacion.java           # Solicitud asíncrona XLSX
│   │   │   │   ├── Rol.java                   # Conjunto de permisos
│   │   │   │   ├── Permiso.java               # Acción granular (crear, leer, etc.)
│   │   │   │   ├── PaqueteScorm.java          # Paquete SCORM (ZIP en MinIO)
│   │   │   │   ├── ScormDatoAlumno.java       # Datos CMI de sesión SCORM
│   │   │   │   └── Notificacion.java          # Notificación in-app (vencimiento, logro, etc.)
│   │   │   ├── repository/                    # Repositorios Spring Data JPA
│   │   │   │   ├── UsuarioRepository.java
│   │   │   │   ├── ConsentimientoRepository.java
│   │   │   │   ├── FirmaElectronicaRepository.java
│   │   │   │   ├── CursoRepository.java
│   │   │   │   ├── VersionCursoRepository.java
│   │   │   │   ├── EvaluacionRepository.java
│   │   │   │   ├── PreguntaRepository.java
│   │   │   │   ├── DocumentoRepository.java
│   │   │   │   ├── EventoAuditoriaRepository.java
│   │   │   │   ├── ExportacionRepository.java
│   │   │   │   ├── RolRepository.java
│   │   │   │   ├── PermisoRepository.java
│   │   │   │   ├── PaqueteScormRepository.java
│   │   │   │   ├── ScormDatoAlumnoRepository.java
│   │   │   │   └── NotificacionRepository.java
│   │   │   ├── service/                       # Lógica de negocio (Spring @Service)
│   │   │   │   ├── AuthService.java           # Registro, login, MFA
│   │   │   │   ├── UsuarioService.java
│   │   │   │   ├── ConsentimientoService.java
│   │   │   │   ├── FirmaService.java
│   │   │   │   ├── CursoService.java
│   │   │   │   ├── EvaluacionService.java
│   │   │   │   ├── DocumentoService.java
│   │   │   │   ├── RolService.java
│   │   │   │   ├── ExportacionService.java    # Exportación XLSX asíncrona
│   │   │   │   ├── ExportacionWorker.java     # Worker @Async para exportaciones
│   │   │   │   ├── NotificacionService.java   # CRUD + marcar leídas
│   │   │   │   ├── NotificacionScheduler.java # @Scheduled para notificaciones automáticas
│   │   │   │   ├── ScormService.java          # Gestión de paquetes SCORM
│   │   │   │   ├── ScormRuntimeService.java   # Runtime API SCORM (sesiones CMI)
│   │   │   │   ├── JwtService.java            # Token JWT (access + refresh + MFA)
│   │   │   │   └── MfaService.java            # TOTP Google Authenticator
│   │   │   ├── security/                      # Spring Security
│   │   │   │   ├── SecurityConfig.java        # Cadena de filtros + CORS
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   ├── UserPrincipal.java         # Principal de Spring Security
│   │   │   │   ├── DatabaseUserDetailsService.java
│   │   │   │   └── FormLoginSuccessHandler.java # Post-login success handler
│   │   │   ├── dto/                           # DTOs (records)
│   │   │   │   ├── AuthResponse.java
│   │   │   │   ├── LoginRequest.java
│   │   │   │   ├── RegisterRequest.java
│   │   │   │   ├── MfaVerificarRequest.java
│   │   │   │   ├── CursoRequest.java
│   │   │   │   ├── EvaluacionRequest.java
│   │   │   │   ├── PreguntaRequest.java
│   │   │   │   ├── ConsentimientoRequest.java
│   │   │   │   ├── DocumentoRequest.java
│   │   │   │   ├── FirmaRequest.java
│   │   │   │   ├── FirmaResponse.java
│   │   │   │   ├── ErrorResponse.java
│   │   │   │   └── PagedResponse.java
│   │   │   ├── exception/                     # Manejo de excepciones
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   └── util/                          # Utilidades
│   │   │       ├── HashUtils.java             # SHA-256
│   │   │       ├── AuditLogger.java           # Event Sourcing logger
│   │   │       └── SecurityUtils.java         # Current user helper
│   │   └── resources/
│   │       ├── application.yml                # Configuración Spring Boot
│   │       ├── db/migration/                  # Migraciones Flyway
│   │       │   ├── V1__initial_schema.sql
│   │       │   ├── V2__seed_roles_permisos.sql
│   │       │   └── V3__fix_indexes_metadata.sql
│   │       ├── static/                        # Assets estáticos
│   │       │   ├── css/
│   │       │   │   ├── variables.css          # Variables CSS (tema claro/oscuro)
│   │       │   │   ├── base.css               # Reset + tipografía
│   │       │   │   ├── components.css         # Componentes reutilizables
│   │       │   │   ├── layout.css             # Layouts (dashboard, sidebar)
│   │       │   │   ├── quill-custom.css       # Personalización Quill.js
│   │       │   │   └── js/quill/quill.snow.css # Tema snow de Quill.js
│   │       │   └── js/
│   │       │       ├── app.js                 # Utilidades globales (theme, toast, dropdowns)
│   │       │       ├── scorm-api.js           # Cliente SCORM Runtime API (inyección iframe)
│   │       │       └── quill/
│   │       │           ├── config.js           # Inicialización Quill.js + handlers
│   │       │           ├── quill.min.js        # Quill.js v1.3.7
│   │       │           └── quill.snow.css      # Tema snow
│   │       └── templates/                     # Templates Thymeleaf
│   │           ├── index.html                 # Landing page
│   │           ├── auth/                      # Autenticación
│   │           │   ├── login.html
│   │           │   ├── registro.html
│   │           │   └── consentimiento.html
│   │           ├── dashboard/                 # Dashboard
│   │           │   ├── index.html             # Dashboard KPIs + acceso rápido
│   │           │   ├── perfil.html
│   │           │   ├── documentos.html
│   │           │   ├── firmas.html
│   │           │   ├── exportaciones.html
│   │           │   ├── notificaciones.html    # Página dedicada de notificaciones
│   │           │   └── reproductor.html       # Reproductor multimedia
│   │           ├── cursos/                    # Gestión de cursos
│   │           │   ├── listado.html           # Lista de cursos
│   │           │   ├── editor.html            # Editor WYSIWYG con Quill.js
│   │           │   ├── scorm-upload.html      # Subida drag & drop SCORM
│   │           │   └── scorm-player.html      # Reproductor iframe SCORM
│   │           ├── evaluaciones/              # Evaluaciones
│   │           │   ├── listado.html
│   │           │   └── editor.html            # Editor de evaluaciones con WYSIWYG
│   │           ├── admin/                     # Administración
│   │           │   ├── usuarios.html
│   │           │   ├── roles.html
│   │           │   └── auditoria.html
│   │           └── fragments/                 # Fragmentos reutilizables
│   │               ├── head.html              # Meta tags, CSS, CSP
│   │               ├── header.html            # Navbar + notificaciones dropdown
│   │               └── footer.html
│   └── test/
│       └── java/com/lms/                      # Pruebas JUnit 5 + Mockito
│           ├── LMSApplicationTests.java
│           ├── config/
│           │   ├── TestConfig.java
│           │   └── TestSecurityConfig.java
│           ├── controller/
│           │   ├── AuthControllerTest.java
│           │   ├── CursoControllerTest.java
│           │   ├── FirmaControllerTest.java
│           │   └── HomeControllerTest.java
│           ├── service/
│           │   ├── UsuarioServiceTest.java
│           │   ├── ConsentimientoServiceTest.java
│           │   ├── FirmaServiceTest.java
│           │   ├── CursoServiceTest.java
│           │   ├── ExportacionServiceTest.java
│           │   └── HashUtilsTest.java
│           └── repository/
│               └── UsuarioRepositoryTest.java
├── docs/                          # Documentación
│   ├── arquitectura.md
│   └── api.md
├── scripts/                       # Scripts de utilidad
│   ├── init-db.sh
│   └── seed-data.sql
├── docker/                        # Docker config
│   ├── Dockerfile
│   ├── Dockerfile.java
│   └── docker-compose.yml
├── auditoria.md                   # Informe de auditoría técnica
├── pom.xml                        # Maven POM
└── README.md
```

---

## Restricciones Técnicas

1. **Inmutabilidad**: Prohibido UPDATE en tablas de auditoría (Event Sourcing). Solo INSERT.
2. **Soft Delete**: Toda entidad debe tener deleted_at (DateTime nullable), deleted_by (UUID nullable), reason (Text nullable).
3. **UUID PK**: Prohibido el uso de RUT o secuencias auto-incrementales como PK.
4. **Hash SHA-256**: Todo archivo y registro crítico debe incluir hash_sha256.
5. **API-First**: Toda la lógica debe exponerse via API REST. Sin lógica en base de datos.
6. **Idioma**: 100% español (código, comentarios, logs, documentación).
7. **CamelCase + PascalCase**: Clases Java en PascalCase, métodos/variables en camelCase. Paquetes en minúsculas.
8. **Pruebas en src/test/java**: Todos los tests deben estar en `src/test/java/com/lms/`. Prohibido dispersar tests fuera de esta estructura.
