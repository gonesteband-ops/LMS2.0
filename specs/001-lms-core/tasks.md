# Tareas: LMS Core - Estructura Base (Java / Spring Boot)

**Input**: Documentos de diseño en `/specs/001-lms-core/`
**Prerrequisitos**: spec.md, plan.md, data-model.md, clarificaciones.md

---

## Formato: `[ID] [P?] [Story] Descripción`

- **[P]**: Puede correr en paralelo (diferentes archivos, sin dependencias)
- **[Story]**: Historia de usuario asociada

---

## Fase 1: Setup - Infraestructura Base

**Propósito**: Inicialización del proyecto Maven, configuración de contenedores y herramientas base.

- [x] T001 Crear [`pom.xml`](pom.xml) con dependencias: Spring Boot 3.3, Spring Data JPA, Spring Security, Spring Web, Thymeleaf, Flyway, JJWT 0.12+, Apache POI 5.2+, Jakarta Validation, Hibernate Validator, OpenTelemetry, AWS SDK S3, MinIO, Redis (Lettuce), Lombok, JUnit 5, Mockito, H2 (test)
- [x] T002 [P] Configurar Spring Boot Maven Plugin en [`pom.xml`](pom.xml)
- [x] T003 [P] Crear [`docker/docker-compose.yml`](docker/docker-compose.yml) (PostgreSQL 16, Redis 7, MinIO)
- [x] T004 [P] Crear [`docker/Dockerfile.java`](docker/Dockerfile.java) (multi-stage build: Maven + JRE 17)
- [x] T005 Crear [`src/main/resources/application.yml`](src/main/resources/application.yml) con configuración central (BD, JPA, Flyway, Redis, MinIO, JWT)
- [x] T006 [P] Crear perfiles en [`application.yml`](src/main/resources/application.yml) (dev/prod via spring.profiles.active)
- [x] T007 [P] Configuración de producción en [`application.yml`](src/main/resources/application.yml)
- [x] T008 [P] Crear migración Flyway inicial [`V1__initial_schema.sql`](src/main/resources/db/migration/V1__initial_schema.sql) con todas las tablas
- [x] T009 [P] Crear migración Flyway seed [`V2__seed_roles_permisos.sql`](src/main/resources/db/migration/V2__seed_roles_permisos.sql) (roles base, admin inicial)
- [x] T010 Crear [`docker/docker-compose.java.yml`](docker/docker-compose.java.yml) con inicialización de servicios
- [x] T011 Configuración de base de datos y seed data via Flyway (V1-V5)

---

## Fase 2: Fundación - Core del Sistema (Bloqueante)

**Propósito**: Configuración central de Spring Boot, entidades base, esquemas de seguridad. Sin esto, ninguna historia de usuario puede comenzar.

**⚠️ CRÍTICO**: Ningún trabajo de historia de usuario puede comenzar hasta que esta fase esté completa.

- [x] T012 Crear [`LMSApplication.java`](src/main/java/com/lms/LMSApplication.java) - @SpringBootApplication con `SpringApplication.run()`
- [x] T013 [P] Crear configuraciones: [`WebConfig.java`](src/main/java/com/lms/config/WebConfig.java), [`ThymeleafConfig.java`](src/main/java/com/lms/config/ThymeleafConfig.java), [`MinioConfig.java`](src/main/java/com/lms/config/MinioConfig.java), [`RedisConfig.java`](src/main/java/com/lms/config/RedisConfig.java), [`DataInitializer.java`](src/main/java/com/lms/config/DataInitializer.java)
- [x] T014 [P] Seguridad: Spring Security filter chain, CORS, CSRF, rutas públicas (integradas en WebConfig y Security)
- [x] T015 [P] Tareas asíncronas: @EnableScheduling + @EnableAsync en [`LMSApplication.java`](src/main/java/com/lms/LMSApplication.java)
- [x] T016 [P] Crear [`config/OpenTelemetryConfig.java`](src/main/java/com/lms/config/OpenTelemetryConfig.java) - Trazas y métricas
- [x] T017 Crear modelo base con UUID, Timestamps, SoftDelete (integrado en entidades vía @Entity listeners)
- [x] T018 [P] JWT: Auth con JJWT en [`AuthController.java`](src/main/java/com/lms/controller/AuthController.java) + [`SecurityUtils.java`](src/main/java/com/lms/util/SecurityUtils.java)
- [x] T019 [P] Filtro JWT (integrado en SecurityUtils y AuthController)
- [x] T020 [P] UserPrincipal implementación de UserDetails (integrado)
- [x] T021 [P] UserDetailsService con JPA (integrado en AuthController/AuthService)
- [x] T022 [P] Crear [`util/HashUtils.java`](src/main/java/com/lms/util/HashUtils.java) - SHA-256 para strings y archivos
- [x] T023 [P] Utilidades de encriptación (HashUtils + AuditLogger para hash encadenado)
- [x] T024 [P] Crear [`util/AuditLogger.java`](src/main/java/com/lms/util/AuditLogger.java) - AuditLogger con hash encadenado (solo INSERT)
- [x] T025 [P] Utilidades de geolocalización (integradas en servicios de firma y auditoría)
- [x] T026 Crear todos los repositorios Spring Data JPA en [`repository/`](src/main/java/com/lms/repository/)

**Checkpoint**: Fundación lista - la implementación de historias de usuario puede comenzar.

---

## Fase 3: Historia de Usuario 1 - Registro y Consentimiento Informado (Prioridad: P1) 🎯 MVP

**Objetivo**: Permitir registro de usuarios con flujo de consentimiento informado obligatorio (Ley 19.799).

### Tests

- [x] T027 [P] [US1] Test de controlador para POST /api/v1/auth/register en [`AuthControllerTest.java`](src/test/java/com/lms/controller/AuthControllerTest.java)
- [x] T028 [P] [US1] Test de controlador para POST /api/v1/auth/consentimiento en [`AuthControllerTest.java`](src/test/java/com/lms/controller/AuthControllerTest.java)
- [x] T029 [P] [US1] Test unitario de UsuarioService en [`UsuarioServiceTest.java`](src/test/java/com/lms/service/UsuarioServiceTest.java)
- [x] T030 [P] [US1] Test de integración de flujo completo registro+consentimiento en [`ConsentimientoServiceTest.java`](src/test/java/com/lms/service/ConsentimientoServiceTest.java)

### Implementación

- [x] T031 [P] [US1] Crear entidad [`model/Usuario.java`](src/main/java/com/lms/model/Usuario.java) - @Entity con UUID, email, nombre, hash contraseña, MFA, roles
- [x] T032 [P] [US1] Crear entidad [`model/Consentimiento.java`](src/main/java/com/lms/model/Consentimiento.java)
- [x] T033 [P] [US1] Crear DTOs: RegisterRequest, LoginRequest, AuthResponse, ConsentimientoRequest, ErrorResponse
- [x] T034 [US1] Servicios de usuario: [`UsuarioService.java`](src/main/java/com/lms/service/UsuarioService.java) + [`ConsentimientoService.java`](src/main/java/com/lms/service/ConsentimientoService.java)
- [x] T035 [US1] Registro de consentimiento + evento auditoría en ConsentimientoService
- [x] T036 [US1] Autenticación JWT en [`AuthController.java`](src/main/java/com/lms/controller/AuthController.java)
- [x] T037 [US1] Crear [`controller/AuthController.java`](src/main/java/com/lms/controller/AuthController.java) - Endpoints: POST /api/v1/auth/register, /login, /consentimiento
- [x] T038 [US1] Validación (@Valid) y manejo de errores (@ControllerAdvice) en [`GlobalExceptionHandler.java`](src/main/java/com/lms/exception/GlobalExceptionHandler.java)
- [x] T039 [US1] Logging (SLF4J) para operaciones de autenticación y consentimiento

**Checkpoint**: US1 funcional y testeable independientemente.

---

## Fase 4: Historia de Usuario 2 - Firma Electrónica y No Repudio (Prioridad: P1)

**Objetivo**: Implementar firma electrónica con registro vinculante y hash encadenado (Ley 19.799, 21.719).

### Tests

- [x] T040 [P] [US2] Test de controlador para POST /api/v1/firmas/firmar en [`FirmaControllerTest.java`](src/test/java/com/lms/controller/FirmaControllerTest.java)
- [x] T041 [P] [US2] Test de integración de flujo de firma en [`FirmaServiceTest.java`](src/test/java/com/lms/service/FirmaServiceTest.java)

### Implementación

- [x] T042 [P] [US2] Crear entidad [`model/FirmaElectronica.java`](src/main/java/com/lms/model/FirmaElectronica.java)
- [x] T043 [P] [US2] Crear entidad [`model/Documento.java`](src/main/java/com/lms/model/Documento.java)
- [x] T044 [P] [US2] Crear DTOs: FirmaRequest, FirmaResponse
- [x] T045 [US2] Crear [`service/FirmaService.java`](src/main/java/com/lms/service/FirmaService.java) - Lógica de firma con hash encadenado
- [x] T046 [US2] Crear [`service/DocumentoService.java`](src/main/java/com/lms/service/DocumentoService.java)
- [x] T047 [US2] Servicio MinIO para almacenamiento de archivos (MinioConfig + MinioClient)
- [x] T048 [US2] Crear [`controller/FirmaController.java`](src/main/java/com/lms/controller/FirmaController.java)
- [x] T049 [US2] Crear [`controller/DocumentoController.java`](src/main/java/com/lms/controller/DocumentoController.java)
- [x] T050 [US2] Validación de integridad de cadena de hash

**Checkpoint**: US2 funcional - sistema de firma y no repudio operativo.

---

## Fase 5: Historia de Usuario 3 - Gestión de Cursos y Versionado (Prioridad: P1)

**Objetivo**: CRUD de cursos con versionado completo e historial.

### Tests

- [x] T051 [P] [US3] Test de controlador para endpoints de cursos en [`CursoControllerTest.java`](src/test/java/com/lms/controller/CursoControllerTest.java)
- [x] T052 [P] [US3] Test de integración de versionado en [`CursoServiceTest.java`](src/test/java/com/lms/service/CursoServiceTest.java)

### Implementación

- [x] T053 [P] [US3] Crear entidad [`model/Curso.java`](src/main/java/com/lms/model/Curso.java)
- [x] T054 [P] [US3] Crear entidad [`model/VersionCurso.java`](src/main/java/com/lms/model/VersionCurso.java)
- [x] T055 [P] [US3] Crear DTOs: CursoRequest, PagedResponse
- [x] T056 [US3] Crear [`service/CursoService.java`](src/main/java/com/lms/service/CursoService.java) - CRUD con versionado automático
- [x] T057 [US3] Crear [`controller/CursoController.java`](src/main/java/com/lms/controller/CursoController.java)
- [x] T058 [US3] Lógica de hash encadenado entre versiones
- [x] T059 [US3] Validación de integridad de versiones

**Checkpoint**: US3 funcional - cursos versionados con trazabilidad.

---

## Fase 6: Historia de Usuario 4 - Evaluaciones Multimedia (Prioridad: P2)

**Objetivo**: Creación de evaluaciones con editor WYSIWYG, pesos y tiempo de respuesta.

### Tests

- [x] T060 [P] [US4] Test de integración de creación de evaluación con preguntas
- [x] T061 [US4] Test de integración de evaluaciones

### Implementación

- [x] T062 [P] [US4] Crear entidad [`model/Evaluacion.java`](src/main/java/com/lms/model/Evaluacion.java)
- [x] T063 [P] [US4] Crear entidad [`model/Pregunta.java`](src/main/java/com/lms/model/Pregunta.java)
- [x] T064 [P] [US4] Crear DTOs: EvaluacionRequest, PreguntaRequest
- [x] T065 [US4] Crear [`service/EvaluacionService.java`](src/main/java/com/lms/service/EvaluacionService.java)
- [x] T066 [US4] Crear [`controller/EvaluacionController.java`](src/main/java/com/lms/controller/EvaluacionController.java)

**Checkpoint**: US4 funcional - evaluaciones con soporte multimedia.

---

## Fase 7: Historia de Usuario 5 - Dashboards y Exportación XLSX (Prioridad: P2)

**Objetivo**: Dashboards con KPIs y exportación asíncrona vía Spring `@Async`.

### Tests

- [x] T067 [P] [US5] Test de exportación en [`ExportacionServiceTest.java`](src/test/java/com/lms/service/ExportacionServiceTest.java)
- [x] T068 [US5] Test de integración de exportación

### Implementación

- [x] T069 [P] [US5] Crear entidad [`model/Exportacion.java`](src/main/java/com/lms/model/Exportacion.java)
- [x] T070 [P] [US5] Crear DTOs: ExportacionRequest, ExportacionResponse
- [x] T071 [US5] Crear [`service/ExportacionService.java`](src/main/java/com/lms/service/ExportacionService.java) - Lógica asíncrona con @Async
- [x] T072 [US5] Implementar generación XLSX con Apache POI
- [x] T073 [US5] Crear [`controller/ExportacionController.java`](src/main/java/com/lms/controller/ExportacionController.java)

**Checkpoint**: US5 funcional - exportaciones asíncronas operativas.

---

## Fase 8: Historia de Usuario 6 - RBAC y Auditoría (Prioridad: P2)

**Objetivo**: Gestión de roles con permisos granulares y auditoría completa via Event Sourcing.

### Tests

- [x] T074 [P] [US6] Test de integridad de hash en [`HashUtilsTest.java`](src/test/java/com/lms/util/HashUtilsTest.java)
- [x] T075 [P] [US6] Test de auditoría en [`AuditLoggerTest.java`](src/test/java/com/lms/util/AuditLoggerTest.java)
- [x] T076 [US6] Test de integración de RBAC

### Implementación

- [x] T077 [P] [US6] Entidad [`model/Rol.java`](src/main/java/com/lms/model/Rol.java) con ManyToMany a Usuario
- [x] T078 [P] [US6] Entidad [`model/Permiso.java`](src/main/java/com/lms/model/Permiso.java)
- [x] T079 [P] [US6] Entidad [`model/EventoAuditoria.java`](src/main/java/com/lms/model/EventoAuditoria.java) con hash encadenado
- [x] T080 [P] [US6] DTOs de roles, permisos y eventos de auditoría
- [x] T081 [US6] Servicios de roles y permisos
- [x] T082 [US6] Servicio de auditoría con consulta de eventos y filtros
- [x] T083 [US6] Controladores de administración: [`RolController.java`](src/main/java/com/lms/controller/RolController.java), [`UsuarioController.java`](src/main/java/com/lms/controller/UsuarioController.java)
- [x] T084 [US6] Rutas de administración: [`/admin/usuarios`](src/main/resources/templates/admin/usuarios.html), [`/admin/roles`](src/main/resources/templates/admin/roles.html), [`/admin/auditoria`](src/main/resources/templates/admin/auditoria.html)
- [x] T085 [US6] Control de acceso por roles (ADMIN, INSTRUCTOR, SUPERADMIN) con @PreAuthorize

**Checkpoint**: US6 funcional - RBAC granular y trazabilidad completa.

---

## Fase 9: Editor WYSIWYG (Quill.js)

**Objetivo**: Integración de editor Quill.js para contenido enriquecido en cursos y evaluaciones.

- [x] T086 Descargar e integrar Quill.js (v1.3.7) en [`static/js/quill/`](src/main/resources/static/js/quill/)
- [x] T087 Crear configuración central de Quill.js en [`static/js/quill/config.js`](src/main/resources/static/js/quill/config.js) (toolbar, handlers, image/video)
- [x] T088 Crear estilos personalizados para Quill.js en [`static/css/quill-custom.css`](src/main/resources/static/css/quill-custom.css)
- [x] T089 Crear templates de editor: [`cursos/editor.html`](src/main/resources/templates/cursos/editor.html), [`evaluaciones/editor.html`](src/main/resources/templates/evaluaciones/editor.html)
- [x] T090 Implementar handlers de imagen (subida a servidor) y video (embed URL) en config.js

---

## Fase 10: Soporte SCORM

**Objetivo**: Integración de paquetes SCORM 1.2/2004 con almacenamiento, runtime API y reproductor.

- [x] T091 Crear migración Flyway [`V4__create_scorm_tables.sql`](src/main/resources/db/migration/V4__create_scorm_tables.sql) (paquetes_scorm, scorm_datos_alumno)
- [x] T092 Crear entidades: [`PaqueteScorm.java`](src/main/java/com/lms/model/PaqueteScorm.java), [`ScormDatoAlumno.java`](src/main/java/com/lms/model/ScormDatoAlumno.java)
- [x] T093 Crear repositorios: [`PaqueteScormRepository.java`](src/main/java/com/lms/repository/PaqueteScormRepository.java), [`ScormDatoAlumnoRepository.java`](src/main/java/com/lms/repository/ScormDatoAlumnoRepository.java)
- [x] T094 Crear [`service/ScormService.java`](src/main/java/com/lms/service/ScormService.java) — Subida, extracción ZIP, almacenamiento MinIO, parsing imsmanifest.xml
- [x] T095 Crear [`service/ScormRuntimeService.java`](src/main/java/com/lms/service/ScormRuntimeService.java) — Runtime API (Initialize, GetValue, SetValue, Commit, Finish)
- [x] T096 Crear [`controller/ScormController.java`](src/main/java/com/lms/controller/ScormController.java) — Endpoints REST de gestión y runtime API
- [x] T097 Crear API de comunicación SCORM (JS) en [`static/js/scorm-api.js`](src/main/resources/static/js/scorm-api.js)
- [x] T098 Crear reproductor SCORM en [`cursos/scorm-player.html`](src/main/resources/templates/cursos/scorm-player.html) con iframe y barra de progreso
- [x] T099 Crear página de subida SCORM en [`cursos/scorm-upload.html`](src/main/resources/templates/cursos/scorm-upload.html) con drag & drop y barra de progreso
- [x] T100 Agregar enlace "Biblioteca SCORM" en [`header.html`](src/main/resources/templates/fragments/header.html) (dropdown usuario + menú móvil)
- [x] T101 Agregar ruta [`/cursos/scorm`](src/main/java/com/lms/controller/HomeController.java) en HomeController

---

## Fase 11: Notificaciones Automáticas

**Objetivo**: Sistema de notificaciones en tiempo real con dropdown en header y página dedicada.

- [x] T102 Crear migración Flyway [`V5__create_notificaciones.sql`](src/main/resources/db/migration/V5__create_notificaciones.sql)
- [x] T103 Crear entidad [`model/Notificacion.java`](src/main/java/com/lms/model/Notificacion.java)
- [x] T104 Crear [`repository/NotificacionRepository.java`](src/main/java/com/lms/repository/NotificacionRepository.java)
- [x] T105 Crear [`service/NotificacionService.java`](src/main/java/com/lms/service/NotificacionService.java) — Envío, marcado como leído, paginación
- [x] T106 Crear [`controller/NotificacionController.java`](src/main/java/com/lms/controller/NotificacionController.java) — API REST /api/v1/notificaciones
- [x] T107 Crear página [`dashboard/notificaciones.html`](src/main/resources/templates/dashboard/notificaciones.html)
- [x] T108 Integrar dropdown de notificaciones en [`header.html`](src/main/resources/templates/fragments/header.html) con campana y contador
- [x] T109 Crear [`scheduler/NotificacionScheduler.java`](src/main/java/com/lms/scheduler/NotificacionScheduler.java) — Tareas programadas (@Scheduled)
- [x] T110 Configurar @EnableScheduling en [`LMSApplication.java`](src/main/java/com/lms/LMSApplication.java)
- [x] T111 Configurar scheduler en [`application.yml`](src/main/resources/application.yml)

---

## Fase 12: Pulido y Aspectos Transversales

**Propósito**: Mejoras que afectan múltiples historias de usuario.

- [ ] T112 [P] Documentación de arquitectura en [`docs/arquitectura.md`](docs/arquitectura.md)
- [ ] T113 [P] Documentación de API en [`docs/api.md`](docs/api.md)
- [x] T114 [P] OpenTelemetry + Micrometer en [`config/OpenTelemetryConfig.java`](src/main/java/com/lms/config/OpenTelemetryConfig.java)
- [x] T115 Configuración de CORS y headers de seguridad
- [ ] T116 Pruebas de cobertura mínima (>80%) con JaCoCo (Maven plugin)
- [x] T117 Verificación de integridad de hash encadenado (tarea programada @Scheduled en NotificacionScheduler)
- [ ] T118 Configuración de Checkstyle / PMD en [`pom.xml`](pom.xml)
- [ ] T119 [P] Crear [`README.md`](README.md) con instrucciones de instalación y desarrollo

---

## Dependencias y Orden de Ejecución

### Dependencias entre Fases

- **Setup (Fase 1)**: Sin dependencias
- **Fundación (Fase 2)**: Depende de Setup - BLOQUEA todas las historias de usuario
- **US1 (Fase 3)**: Depende de Fundación
- **US2 (Fase 4)**: Depende de US1 (por auth) y Fundación
- **US3 (Fase 5)**: Depende de Fundación - paralelizable con US1/US2
- **US4 (Fase 6)**: Depende de US3 (por modelos curso)
- **US5 (Fase 7)**: Depende de Fundación + US1 (por auth)
- **US6 (Fase 8)**: Depende de Fundación + US1 (por auth)
- **WYSIWYG (Fase 9)**: Depende de Fundación - paralelizable
- **SCORM (Fase 10)**: Depende de Fundación + US3 (por curso)
- **Notificaciones (Fase 11)**: Depende de Fundación + US1 (por usuario)
- **Pulido (Fase 12)**: Depende de todas las fases anteriores

### Orden de Implementación Recomendado

```text
Fase 1 → Fase 2 → Fase 3 (US1) → [Fase 4 (US2) + Fase 5 (US3) en paralelo]
→ Fase 6 (US4) → [Fase 7 (US5) + Fase 8 (US6) en paralelo]
→ [Fase 9 (WYSIWYG) + Fase 10 (SCORM) + Fase 11 (Notificaciones) en paralelo]
→ Fase 12
```

### Oportunidades en Paralelo

- T001-T004: Todas las tareas [P] de Setup pueden correr en paralelo
- T013-T016: Todas las tareas [P] de Fundación pueden correr en paralelo
- US2 y US3 pueden implementarse en paralelo después de US1
- US5 y US6 pueden implementarse en paralelo
- Fases 9, 10 y 11 pueden implementarse en paralelo
- Todos los tests [P] dentro de una historia pueden correr en paralelo
