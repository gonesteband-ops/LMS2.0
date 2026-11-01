# Plan de Implementación: Módulo Certificaciones

> **Feature:** Certificaciones — Evaluaciones independientes sin asociación a curso
> **Basado en:** El sistema existente de evaluaciones de cursos (reuso máximo)

---

## 1. Resumen

Crear sección "Certificación" en el sidebar con dos vistas:
- **Admin:** CRUD de certificaciones + inscribir usuarios (1:1 o masivo CSV)
- **Alumno:** Lista de certificaciones disponibles, rendir evaluación, ver resultado

La certificación es una evaluación autónoma: tiene preguntas (opción múltiple, V/F), se corrige automáticamente, y los resultados quedan asociados al usuario.

---

## 2. Sidebar — `fragments/sidebar.html`

Agregar dos links:
- Admin: `<a href="/admin/certificaciones">` con `sec:authorize="hasAnyRole('ADMIN','SUPERADMIN')"`
- Alumno: `<a href="/certificaciones">` con `sec:authorize="isAuthenticated()"`

---

## 3. Migraciones Flyway

### V28__crear_certificaciones.sql — 4 tablas:

**certificaciones**: id UUID PK, titulo VARCHAR(255) NOT NULL, descripcion TEXT, peso_total DOUBLE PRECISION DEFAULT 1.0, nota_minima DOUBLE PRECISION DEFAULT 4.0, tiempo_estimado_minutos INTEGER, configuracion JSONB DEFAULT '{}', codigo VARCHAR(20) NOT NULL UNIQUE, hash_sha256 VARCHAR(64) NOT NULL, activo BOOLEAN DEFAULT TRUE, created_at TIMESTAMP DEFAULT NOW(), updated_at TIMESTAMP, deleted_at TIMESTAMP.

**preguntas_certificacion**: id UUID PK, certificacion_id UUID NOT NULL FK → certificaciones(id) ON DELETE CASCADE, tipo VARCHAR(50) NOT NULL, contenido TEXT NOT NULL, peso DOUBLE PRECISION DEFAULT 1.0, opciones TEXT, respuesta_correcta TEXT, multimedia_url VARCHAR(500), hash_sha256 VARCHAR(64) NOT NULL, orden INTEGER DEFAULT 0, created_at TIMESTAMP DEFAULT NOW(), updated_at TIMESTAMP, deleted_at TIMESTAMP.

**intentos_certificacion**: id UUID PK, certificacion_id UUID NOT NULL FK → certificaciones(id), usuario_id UUID NOT NULL FK → usuarios(id), respuestas JSONB, nota DOUBLE PRECISION, aprobado BOOLEAN, estado VARCHAR(20) DEFAULT 'EN_CURSO', fecha_inicio TIMESTAMP DEFAULT NOW(), fecha_entrega TIMESTAMP, hash_sha256 VARCHAR(64) NOT NULL, reiniciado_por UUID FK → usuarios(id), motivo_reinicio VARCHAR(500), deleted_at TIMESTAMP, created_at TIMESTAMP DEFAULT NOW().

**certificaciones_usuarios**: id UUID PK, certificacion_id UUID NOT NULL FK → certificaciones(id), usuario_id UUID NOT NULL FK → usuarios(id), nota DOUBLE PRECISION, aprobado BOOLEAN, completado BOOLEAN DEFAULT FALSE, created_at TIMESTAMP DEFAULT NOW(), UNIQUE(certificacion_id, usuario_id).

### V29__seed_codigo_cer.sql
```sql
INSERT INTO secuencias_codigos (prefijo, ultimo_numero) VALUES ('CER', 1) ON CONFLICT (prefijo) DO NOTHING;
```

---

## 4. Entidades JPA — 4 archivos

### model/Certificacion.java
- @Entity, @Table(name = "certificaciones")
- Lombok: @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
- UUID id, String titulo, String descripcion, Double pesoTotal, Double notaMinima, Integer tiempoEstimadoMinutos, String configuracion, String codigo, String hashSha256, Boolean activo, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt
- @OneToMany(mappedBy = "certificacion", cascade = CascadeType.ALL, orphanRemoval = true) @OrderBy("orden ASC") List<PreguntaCertificacion> preguntas

### model/PreguntaCertificacion.java
- @Entity, @Table(name = "preguntas_certificacion")
- UUID id, @ManyToOne Certificacion certificacion, String tipo, String contenido, Double peso, String opciones, String respuestaCorrecta, String multimediaUrl, String hashSha256, Integer orden, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt

### model/IntentoCertificacion.java
- @Entity, @Table(name = "intentos_certificacion")
- UUID id, @ManyToOne Certificacion certificacion, @ManyToOne Usuario usuario, String respuestas (JdbcTypeCode(SqlTypes.JSON)), Double nota, Boolean aprobado, String estado, LocalDateTime fechaInicio, LocalDateTime fechaEntrega, String hashSha256, UUID reiniciadoPor, String motivoReinicio, LocalDateTime deletedAt, LocalDateTime createdAt

### model/CertificacionUsuario.java
- @Entity, @Table(name = "certificaciones_usuarios")
- UUID id, @ManyToOne Certificacion certificacion, @ManyToOne Usuario usuario, Double nota, Boolean aprobado, Boolean completado, LocalDateTime createdAt

---

## 5. Repositories — 4 archivos

### repository/CertificacionRepository.java
- extends JpaRepository<Certificacion, UUID>
- findByCodigo(String)
- findByDeletedAtIsNull()
- findByActivoTrueAndDeletedAtIsNull()

### repository/PreguntaCertificacionRepository.java
- extends JpaRepository<PreguntaCertificacion, UUID>
- findByCertificacionIdOrderByOrdenAsc(UUID)

### repository/IntentoCertificacionRepository.java
- extends JpaRepository<IntentoCertificacion, UUID>
- findByCertificacionIdAndUsuarioIdAndDeletedAtIsNull(UUID, UUID)
- findByCertificacionIdAndUsuarioIdAndEstado(UUID, UUID, String)

### repository/CertificacionUsuarioRepository.java
- extends JpaRepository<CertificacionUsuario, UUID>
- findByCertificacionId(UUID)
- findByCertificacionIdAndUsuarioId(UUID, UUID)
- findByUsuarioId(UUID)

---

## 6. DTOs — 6 archivos

### dto/CertificacionRequest.java
```java
public record CertificacionRequest(
    @NotBlank String titulo, String descripcion,
    @Positive Double notaMinima, Integer tiempoEstimadoMinutos,
    Map<String, Object> configuracion,
    @NotEmpty List<PreguntaCertificacionRequest> preguntas
) {}
```

### dto/PreguntaCertificacionRequest.java
```java
public record PreguntaCertificacionRequest(
    UUID id, @NotBlank String tipo, @NotBlank String contenido,
    @Positive Double peso, String opciones, String respuestaCorrecta,
    String multimediaUrl, int orden
) {}
```

### dto/InscripcionCertificacionRequest.java
```java
public record InscripcionCertificacionRequest(
    @NotBlank String usuarioId  // UUID, RUT o email
) {}
```

### dto/CertificacionResponse.java, PreguntaCertificacionResponse.java, IntentoCertificacionResponse.java
Mismos campos que los request pero con datos poblados, siguiendo el patrón de evaluaciones.

---

## 7. Service — `service/CertificacionService.java`

- @Service @Transactional
- Inyecta: certificacionRepo, preguntaRepo, intentoRepo, certUsuarioRepo, codigoGeneratorService, usuarioRepository, auditLogger
- Métodos CRUD: crear, actualizar, eliminar, buscarPorId, buscarPorCodigo, listarActivas, listarPaginado
- Al crear: genera código "CER###" via CodigoGeneratorService, genera hash vía HashUtils.sha256(), logea evento
- Al actualizar: elimina preguntas viejas, persiste nuevas, recalcula hash
- Inscripción: reusa InscripcionService.resolverIdentificador() para UUID/RUT/email
- Inscripción masiva: mismo formato CSV que cursos
- Corrección: misma lógica que IntentoEvaluacionService (comparar contra opciones JSON con flag correcta, nota ponderada, notaMinima)
- Validación: antes de render/responder verificar inscripción via CertificacionUsuarioRepository

---

## 8. Controladores — 3 archivos

### controller/CertificacionRestController.java
- @RestController @RequestMapping("/api/v1/certificaciones")
- POST / → crear (ADMIN/SUPERADMIN)
- PUT /{id} → actualizar (ADMIN/SUPERADMIN)
- DELETE /{id} → eliminar (ADMIN/SUPERADMIN)
- GET /{id}/preguntas → obtenerPreguntas (ADMIN/SUPERADMIN)
- GET /{id}/rendir → preguntas sanitizadas (ESTUDIANTE, verifica inscripción)
- POST /{id}/iniciar → iniciar intento (ESTUDIANTE)
- POST /{id}/responder → entregar y corregir (ESTUDIANTE)
- GET /resultados → resultados del alumno (ESTUDIANTE)
- GET /{id}/resultado-detalle → detalle por pregunta (ESTUDIANTE)

### controller/AdminCertificacionController.java
- @Controller @RequestMapping("/admin/certificaciones")
- GET / → listado.html
- GET /crear → editor.html (vacío)
- GET /{id}/editar → editor.html (precargado con datos)
- GET /{id}/participantes → participantes.html
- POST /{id}/inscribir → redirect
- POST /{id}/inscribir-masivo → redirect
- POST /{id}/inscribir/{usuarioId}/remover → redirect

### controller/CertificacionAlumnoController.java
- @Controller @RequestMapping("/certificaciones")
- GET / → mis-certificaciones.html
- GET /{id}/rendir → rendir.html

---

## 9. Templates — 5 archivos

### templates/certificaciones/listado.html
Admin: tabla/tarjetas de certificaciones. Botones: Crear, Editar, Eliminar, Participantes.
Mismo estilo que evaluaciones/listado.html.

### templates/certificaciones/editor.html
Admin: Quill.js WYSIWYG. Misma estructura que evaluaciones/editor.html pero sin selector de curso. Soporta preguntas de opción única, múltiple, V/F.

### templates/certificaciones/participantes.html
Admin: campo de texto RUT/email + botón "Inscribir" + subida CSV (solo ADMIN/SUPERADMIN) + tabla de inscritos con botón "Revocar". Misma estructura que cursos/participantes.html.

### templates/certificaciones/mis-certificaciones.html
Alumno: lista de certificaciones. Cada card muestra título, estado, nota (si completó), botón "Rendir" si está pendiente.

### templates/certificaciones/rendir.html
Alumno: pantalla completa de examen. Temporizador, preguntas renderizadas por tipo, botón "Entregar", muestra resultado inmediato después de corrección.

---

## 10. Dashboard — `dashboard/index.html`

Agregar card KPI "Mis Certificaciones" con contador vía HTMX a `/certificaciones/contar` (crear endpoint en CertificacionAlumnoController que retorne conteo de certificaciones del usuario).

---

## 11. Orden de ejecución

| Paso | Archivos | Depende de |
|---|---|---|
| 1 | V28__crear_certificaciones.sql | — |
| 2 | V29__seed_codigo_cer.sql | — |
| 3 | 4 entidades JPA | Paso 1 |
| 4 | 4 repositories | Paso 3 |
| 5 | 6 DTOs | — |
| 6 | CertificacionService | Pasos 2, 4, 5 |
| 7 | CertificacionRestController | Paso 6 |
| 8 | AdminCertificacionController | Paso 6 |
| 9 | CertificacionAlumnoController | Paso 6 |
| 10 | 5 templates | Pasos 8, 9 |
| 11 | sidebar.html + dashboard/index.html | Paso 9 |
| 12 | Rebuild + deploy | Todos |

---

## 12. Validación

1. Admin crea certificación → se guarda con código CER001
2. Admin agrega preguntas (opción única, múltiple, V/F)
3. Admin inscribe a estudiante@lms.cl (1:1 y masivo)
4. Alumno ve certificación en su panel y sidebar
5. Alumno rinde, responde, entrega
6. Sistema corrige automáticamente y muestra nota
7. Admin ve resultado en participantes
8. Eliminar certificación → soft-delete
