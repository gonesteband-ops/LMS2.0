# Plan de Implementación: Gestor de Contenidos BPO

**Rama**: `002-gestor-contenidos-bpo` | **Fecha**: 2026-07-24 | **Spec**: `specs/002-gestor-contenidos-bpo/spec.md`
**Entrada**: Especificación de funcionalidad desde `/specs/002-gestor-contenidos-bpo/spec.md`

## Resumen

Módulo Gestor de Contenidos BPO dentro del LMS LegalTech para que el personal de call center/BPO pueda consultar procedimientos vigentes, ofertas activas y noticias recientes. Incluye control de acceso por roles (RBAC granular), asignación de cuentas mediante tabla UsuarioCuenta, ciclo de vida de contenidos (borrador → publicado → archivado), búsqueda inteligente con filtros, tarjetas de resumen y endpoint REST de importación (Fase 2).

## Contexto Técnico

**Lenguaje/Versión**: Java 17 (Spring Boot 3.3.0)
**Dependencias principales**: Spring Web, Spring Data JPA, Spring Security, Thymeleaf, Flyway, PostgreSQL, Lombok, MapStruct, jjwt 0.12.5
**Almacenamiento**: PostgreSQL 15+ (con soporte JSONB), MinIO (solo para Fase 2 si se requiere rich media)
**Testing**: JUnit 5 + SpringBootTest + MockMvc + H2 (test)
**Plataforma objetivo**: Linux server (Docker), render.yaml compatible
**Tipo de proyecto**: Web App (Backend Java + Frontend HTMX/Thymeleaf)
**Metas de rendimiento**: <1.5s carga inicial, <3s búsqueda, soporte 500 agentes concurrentes (CE-001 a CE-003)
**Restricciones**: Solo texto enriquecido sin assets externos para v1; RBAC granular existente; Event Sourcing existente
**Escala/Alcance**: ~10 cuentas, ~100 subservicios, ~500 agentes concurrentes

## Constitution Check

*COMPUERTA: Debe pasar antes de Phase 0 research. Re-evaluar después de Phase 1 design.*

La constitución del proyecto (`constitution.md`) está pendiente de personalización (placeholders sin reemplazar). Para este feature se aplican los principios del `system.md`:
- **UUIDs** para todas las entidades ✅
- **Soft Delete** en todos los contenidos ✅
- **Event Sourcing** delegado al sistema base ✅
- **RBAC granular** integrado con infraestructura existente ✅
- **SHA-256** manejado por sistema base, no por módulo BPO ✅

Sin violaciones identificadas.

## Estructura del Proyecto

### Documentación (este feature)

```text
specs/002-gestor-contenidos-bpo/
├── plan.md                # Este archivo
├── research.md            # Phase 0 output
├── data-model.md          # Phase 1 output
├── quickstart.md          # Phase 1 output
├── contracts/             # Phase 1 output
├── checklists/
│   └── requirements.md    # Checklist de calidad de spec
└── tasks.md               # Phase 2 output (/speckit.tasks)
```

### Código Fuente (raíz del repositorio)

```text
# Web Application (Java Backend + HTMX Frontend)
src/main/java/com/lms/
├── model/
│   ├── bpo/
│   │   ├── ContenidoBPO.java
│   │   ├── CuentaBPO.java
│   │   ├── SubServicioBPO.java
│   │   └── UsuarioCuentaBPO.java
│   └── ... (modelos existentes)
├── repository/
│   └── bpo/
│       ├── ContenidoBPORepository.java
│       ├── CuentaBPORepository.java
│       ├── SubServicioBPORepository.java
│       └── UsuarioCuentaBPORepository.java
├── service/
│   └── bpo/
│       ├── ContenidoBPOService.java
│       └── CuentaBPOService.java
├── controller/
│   └── bpo/
│       ├── ContenidoBPOController.java        # Vistas HTMX
│       └── ContenidoBPOApiController.java     # Endpoint REST Fase 2
├── dto/
│   └── bpo/
│       ├── ContenidoBPOCreateDTO.java
│       ├── ContenidoBPOResponseDTO.java
│       └── ContenidoBPOSearchDTO.java
├── mapper/
│   └── bpo/
│       └── ContenidoBPOMapper.java
└── exception/
    └── bpo/
        └── BPOExceptionHandler.java

src/main/resources/
├── templates/
│   └── bpo/
│       ├── gestor-contenidos.html         # Vista principal
│       ├── fragmentos/
│       │   ├── tarjetas-resumen.html
│       │   ├── lista-contenidos.html
│       │   └── modal-contenido.html
│       └── admin/
│           ├── administrar-contenidos.html
│           └── formulario-contenido.html
├── static/
│   └── css/
│       └── bpo/
│           └── gestor.css
└── db/migration/
    └── V20260724__crear_modulo_bpo.sql

tests/
└── java/
    └── com/lms/
        ├── model/bpo/ContenidoBPOTest.java
        ├── service/bpo/ContenidoBPOServiceTest.java
        ├── controller/bpo/ContenidoBPOControllerTest.java
        └── controller/bpo/ContenidoBPOApiControllerTest.java
```

**Decisión de estructura**: Opción Web Application (backend Java + frontend HTMX). Los nuevos archivos del módulo BPO se agrupan en subpaquetes `bpo/` dentro de las capas existentes para mantener separación modular.

## Complexity Tracking

*(Vacío — no se requieren justificaciones de complejidad. El módulo sigue patrones existentes.)*
