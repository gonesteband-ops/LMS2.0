# Modelo de Datos - LMS Core

**Fecha**: 2026-07-06
**Base**: LMS.md, especificación funcional + implementación actual

---

## Convenciones

- **Primary Key**: UUID v4 generado por servidor (`UUIDGenerator` de Hibernate)
- **Timestamps**: `LocalDateTime.now()` con zona horaria UTC
- **Soft Delete**: Campos `deleted_at: LocalDateTime`, `deleted_by: UUID (FK)`, `reason: Text`
- **Hash**: SHA-256 hexadecimal (64 caracteres)
- **Nombres**: snake_case en español (columnas JPA)
- **Auditoría**: Solo INSERT; prohibido UPDATE/DELETE en tablas de evento

---

## Mixins Base (Implementación Directa en Cada Entidad)

> Nota: A diferencia del diseño original en Python con mixins, en Java/Spring Boot cada entidad declara sus propios campos de auditoría y soft delete directamente, usando `@MappedSuperclass` no es utilizado; cada entidad es autocontenida.

### Campos de Auditoría Comunes
- `id`: UUID, PK, generado por Hibernate `UUIDGenerator`
- `created_at`: LocalDateTime, not null, `@CreationTimestamp`
- `deleted_at`: LocalDateTime, nullable (null = activo)
- `deleted_by`: UUID, nullable
- `reason`: TEXT, nullable

---

## Entidades

### Usuario
| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| id | UUID | PK, default uuid4 | Identificador único |
| email | String(255) | UNIQUE, NOT NULL, index | Email del usuario |
| nombre_completo | String(255) | NOT NULL | Nombre visible |
| hash_contrasena | String(128) | NOT NULL | bcrypt + SHA-256 |
| mfa_secreto | String(64) | NULL | Secreto TOTP para MFA |
| mfa_habilitado | Boolean | default False | MFA activo |
| ultimo_login | DateTime | NULL | Último acceso |
| intentos_fallidos | Integer | default 0 | Contador brute-force |
| bloqueado_hasta | DateTime | NULL | Bloqueo temporal |
| hash_sha256 | String(64) | NOT NULL | Hash de integridad del registro |
| created_at | DateTime | NOT NULL | Timestamp creación |
| updated_at | DateTime | NOT NULL | Timestamp actualización |
| deleted_at | DateTime | NULL | Soft delete |
| deleted_by | UUID | FK -> usuario.id, NULL | Quién eliminó |
| reason | Text | NULL | Motivo eliminación |

### Consentimiento
| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| id | UUID | PK | Identificador único |
| usuario_id | UUID | FK -> usuario.id, NOT NULL | Usuario que consiente |
| ip_origen | String(45) | NOT NULL | IPv4 o IPv6 |
| user_agent | String(512) | NOT NULL | Navegador/cliente |
| hash_documento | String(64) | NOT NULL | SHA-256 del documento legal |
| version_politica | String(20) | NOT NULL | Semver de la política |
| idioma | String(10) | NOT NULL | Código ISO del idioma |
| aceptado | Boolean | NOT NULL | true=acepta, false=rechaza |
| timestamp | DateTime | NOT NULL | Momento del evento |
| hash_sha256 | String(64) | NOT NULL | Hash de integridad |
| created_at | DateTime | NOT NULL | |

### FirmaElectronica
| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| id | UUID | PK | Identificador único |
| usuario_id | UUID | FK -> usuario.id, NOT NULL | Firmante |
| documento_id | UUID | FK -> documento.id, NOT NULL | Documento firmado |
| ip_origen | String(45) | NOT NULL | IP del firmante |
| timestamp | DateTime | NOT NULL | Momento de la firma |
| user_agent | String(512) | NOT NULL | Cliente usado |
| sistema_operativo | String(100) | NULL | SO detectado |
| geolocalizacion | String(100) | NULL | Ciudad/país aproximado (bajo consentimiento) |
| hash_documento | String(64) | NOT NULL | SHA-256 del documento |
| hash_anterior | String(64) | NULL | SHA-256 del evento anterior (cadena) |
| hash_sha256 | String(64) | NOT NULL | Hash de integridad del registro |
| created_at | DateTime | NOT NULL | |

### Curso
| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| id | UUID | PK | Identificador único |
| titulo | String(255) | NOT NULL | Título del curso |
| descripcion | Text | NULL | Descripción extendida |
| version_actual_id | UUID | FK -> version_curso.id, NULL | Versión activa |
| estado | String(20) | default 'borrador' | borrador, publicado, archivado |
| categoria | String(100) | NULL | Categoría del curso |
| imagen_url | String(512) | NULL | URL de portada en MinIO |
| hash_sha256 | String(64) | NOT NULL | Hash de integridad |
| created_at | DateTime | NOT NULL | |
| updated_at | DateTime | NOT NULL | |
| deleted_at | DateTime | NULL | |
| deleted_by | UUID | FK -> usuario.id, NULL | |
| reason | Text | NULL | |

### VersionCurso
| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| id | UUID | PK | Identificador único |
| curso_id | UUID | FK -> curso.id, NOT NULL | Curso asociado |
| numero_version | Integer | NOT NULL | 1, 2, 3... |
| contenido | JSONB | NOT NULL | Contenido completo del curso en ese momento |
| hash_contenido | String(64) | NOT NULL | SHA-256 del contenido |
| hash_version_anterior | String(64) | NULL | Hash de la versión previa (cadena) |
| autor_id | UUID | FK -> usuario.id, NOT NULL | Quién creó esta versión |
| motivo_cambio | Text | NULL | Razón del cambio |
| hash_sha256 | String(64) | NOT NULL | Hash de integridad |
| created_at | DateTime | NOT NULL | |
| deleted_at | DateTime | NULL | |
| deleted_by | UUID | FK -> usuario.id, NULL | |
| reason | Text | NULL | |

### Evaluacion
| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| id | UUID | PK | Identificador único |
| curso_id | UUID | FK -> curso.id, NOT NULL | Curso asociado |
| titulo | String(255) | NOT NULL | Título de la evaluación |
| descripcion | Text | NULL | Instrucciones |
| peso_total | Float | default 100.0 | Suma de pesos |
| tiempo_estimado_minutos | Integer | NULL | Tiempo estimado total |
| configuracion | JSONB | NULL | Configuración adicional (intentos, fecha límite) |
| hash_sha256 | String(64) | NOT NULL | Hash de integridad |
| created_at | DateTime | NOT NULL | |
| updated_at | DateTime | NOT NULL | |
| deleted_at | DateTime | NULL | |
| deleted_by | UUID | FK -> usuario.id, NULL | |
| reason | Text | NULL | |

### Pregunta
| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| id | UUID | PK | Identificador único |
| evaluacion_id | UUID | FK -> evaluacion.id, NOT NULL | Evaluación asociada |
| tipo | String(30) | NOT NULL | opcion_multiple, verdadero_falso, desarrollo, completar |
| contenido | Text | NOT NULL | Contenido WYSIWYG (HTML) |
| peso | Float | default 1.0 | Peso en la evaluación |
| tiempo_respuesta_ms | Integer | NULL | Tiempo de respuesta en ms |
| opciones | JSONB | NULL | Opciones (para opcion_multiple) |
| respuesta_correcta | Text | NULL | Respuesta esperada (hash) |
| multimedia_url | String(512) | NULL | URL en MinIO |
| hash_sha256 | String(64) | NOT NULL | Hash de integridad |
| orden | Integer | NOT NULL | Orden de presentación |
| created_at | DateTime | NOT NULL | |
| updated_at | DateTime | NOT NULL | |
| deleted_at | DateTime | NULL | |
| deleted_by | UUID | FK -> usuario.id, NULL | |
| reason | Text | NULL | |

### Documento
| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| id | UUID | PK | Identificador único |
| nombre_original | String(255) | NOT NULL | Nombre del archivo original |
| url_storage | String(512) | NOT NULL | URL en MinIO/S3 |
| hash_sha256 | String(64) | NOT NULL | SHA-256 del archivo |
| mime_type | String(100) | NOT NULL | Tipo MIME |
| tamano_bytes | Integer | NOT NULL | Tamaño del archivo |
| version | Integer | default 1 | Versión del documento |
| metadata | JSONB | NULL | Metadatos adicionales |
| created_at | DateTime | NOT NULL | |
| updated_at | DateTime | NOT NULL | |
| deleted_at | DateTime | NULL | |
| deleted_by | UUID | FK -> usuario.id, NULL | |
| reason | Text | NULL | |

### EventoAuditoria
| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| id | UUID | PK | Identificador único |
| tipo_evento | String(50) | NOT NULL, index | CONSENTIMIENTO, FIRMA, LOGIN, LOGOUT, CAMBIO_ROL, IMPERSONACION, EXPORTACION, etc. |
| actor_id | UUID | FK -> usuario.id, NULL | Usuario que realizó la acción |
| accion | String(100) | NOT NULL | Descripción de la acción |
| recurso_tipo | String(50) | NULL | Tipo de recurso afectado |
| recurso_id | UUID | NULL | ID del recurso afectado |
| metadata | JSONB | NOT NULL | IP, User-Agent, datos adicionales |
| hash_anterior | String(64) | NULL | SHA-256 del evento anterior (hash encadenado) |
| hash_sha256 | String(64) | NOT NULL | Hash de integridad del registro |
| created_at | DateTime | NOT NULL, index | Timestamp del evento |
| **Nota**: Solo INSERT. Prohibido UPDATE o DELETE. |

### Rol
| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| id | UUID | PK | Identificador único |
| nombre | String(100) | UNIQUE, NOT NULL | superadmin, admin, instructor, estudiante |
| descripcion | Text | NULL | Descripción del rol |
| es_sistema | Boolean | default False | Rol del sistema (no eliminable) |
| hash_sha256 | String(64) | NOT NULL | Hash de integridad |
| created_at | DateTime | NOT NULL | |
| updated_at | DateTime | NOT NULL | |
| deleted_at | DateTime | NULL | |
| deleted_by | UUID | FK -> usuario.id, NULL | |
| reason | Text | NULL | |

### Permiso
| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| id | UUID | PK | Identificador único |
| nombre | String(100) | UNIQUE, NOT NULL | cursos.crear, usuarios.leer, etc. |
| recurso | String(50) | NOT NULL | curso, usuario, evaluacion, documento, rol, auditoria, exportacion |
| accion | String(50) | NOT NULL | crear, leer, actualizar, eliminar, firmar, exportar, impersonar |
| descripcion | Text | NULL | Descripción del permiso |
| created_at | DateTime | NOT NULL | |

### PermisoRol (Tabla intermedia)
| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| rol_id | UUID | FK -> rol.id, PK compuesta | Rol asociado |
| permiso_id | UUID | FK -> permiso.id, PK compuesta | Permiso asociado |
| created_at | DateTime | NOT NULL | |

### UsuarioRol (Tabla intermedia)
| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| usuario_id | UUID | FK -> usuario.id, PK compuesta | Usuario asociado |
| rol_id | UUID | FK -> rol.id, PK compuesta | Rol asociado |
| created_at | DateTime | NOT NULL | |

### Exportacion
| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| id | UUID | PK | Identificador único |
| solicitante_id | UUID | FK -> usuario.id, NOT NULL | Quién solicitó |
| tipo_reporte | String(50) | NOT NULL | cursos, usuarios, evaluaciones, auditoria |
| filtros | JSONB | NULL | Filtros aplicados |
| estado | String(20) | default 'pendiente' | pendiente, procesando, completado, fallido |
| url_resultado | String(512) | NULL | URL del archivo en MinIO |
| error_mensaje | Text | NULL | Mensaje de error si falló |
| created_at | DateTime | NOT NULL | |
| completed_at | DateTime | NULL | |
| deleted_at | DateTime | NULL | |
| deleted_by | UUID | FK -> usuario.id, NULL | |
| reason | Text | NULL | |

### PaqueteScorm
| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| id | UUID | PK | Identificador único |
| curso_id | UUID | FK -> curso.id, NOT NULL, index | Curso asociado |
| titulo | String(255) | NOT NULL | Título del paquete |
| descripcion | Text | NULL | Descripción |
| version_scorm | String(10) | NOT NULL | '1.2' o '2004' |
| url_zip | String(512) | NOT NULL | URL del ZIP en MinIO |
| url_index | String(512) | NOT NULL | URL del archivo de inicio (index.html) en MinIO |
| hash_sha256 | String(64) | NOT NULL | SHA-256 del ZIP |
| tamano_bytes | Long | NOT NULL | Tamaño del archivo ZIP |
| created_at | DateTime | NOT NULL | `@CreationTimestamp` |
| deleted_at | DateTime | NULL | Soft delete |
| deleted_by | UUID | NULL | Quién eliminó |
| reason | Text | NULL | Motivo eliminación |

### ScormDatoAlumno
| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| id | UUID | PK | Identificador único |
| paquete_id | UUID | FK -> paquete_scorm.id, NOT NULL | Paquete SCORM asociado |
| usuario_id | UUID | FK -> usuario.id, NOT NULL | Alumno que cursa |
| intento_id | UUID | NOT NULL | Identificador del intento de sesión |
| clave | String(255) | NOT NULL | Clave CMI (ej: `cmi.core.score.raw`) |
| valor | Text | NOT NULL | Valor almacenado |
| created_at | DateTime | NOT NULL | `@CreationTimestamp` |
| **Unique**: (paquete_id, usuario_id, intento_id, clave) |

### Notificacion
| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| id | UUID | PK | Identificador único |
| usuario_id | UUID | FK -> usuario.id, NOT NULL, index | Usuario destinatario |
| tipo | String(20) | NOT NULL, index | vencimiento, reprobacion, logro, sistema |
| titulo | String(200) | NOT NULL | Título corto de la notificación |
| mensaje | Text | NOT NULL | Cuerpo del mensaje |
| link | String(500) | NULL | Enlace de acción (ej: /cursos/{id}) |
| leida | Boolean | NOT NULL, default false, index | Marcador de leída/no leída |
| created_at | DateTime | NOT NULL, index | `@CreationTimestamp` |

---

## Reglas de Integridad

1. **Hash SHA-256 en cada registro crítico**: Se calcula sobre una representación canónica (JSON ordenado) de los campos del registro.
2. **Hash encadenado en EventoAuditoria**: `hash_sha256 = SHA256(campos_del_evento + hash_anterior)`. El primer evento tiene `hash_anterior = NULL`.
3. **Hash encadenado en VersionCurso**: `hash_contenido = SHA256(contenido_json)`, `hash_version_anterior` apunta al hash de la versión previa.
4. **Cascada de Soft Delete**: Al eliminar un curso, se marcan como eliminadas sus evaluaciones y preguntas.
5. **Unique Index compuesto**: `(curso_id, numero_version)` en VersionCurso. `(evaluacion_id, orden)` en Pregunta.

---

## Índices Recomendados

```sql
-- Performance general
CREATE INDEX idx_evento_auditoria_tipo ON evento_auditoria(tipo_evento);
CREATE INDEX idx_evento_auditoria_actor ON evento_auditoria(actor_id);
CREATE INDEX idx_evento_auditoria_created ON evento_auditoria(created_at);
CREATE INDEX idx_consentimiento_usuario ON consentimiento(usuario_id);
CREATE INDEX idx_firma_usuario ON firma_electronica(usuario_id);
CREATE INDEX idx_version_curso_curso ON version_curso(curso_id);
CREATE INDEX idx_version_curso_numero ON version_curso(curso_id, numero_version);
CREATE INDEX idx_pregunta_evaluacion ON pregunta(evaluacion_id);
CREATE INDEX idx_pregunta_orden ON pregunta(evaluacion_id, orden);
CREATE INDEX idx_exportacion_solicitante ON exportacion(solicitante_id);
CREATE INDEX idx_exportacion_estado ON exportacion(estado);
CREATE INDEX idx_usuario_email ON usuario(email) WHERE deleted_at IS NULL;

-- SCORM
CREATE INDEX idx_paquete_scorm_curso ON paquetes_scorm(curso_id);
CREATE UNIQUE INDEX idx_scorm_dato_unique ON scorm_datos_alumno(paquete_id, usuario_id, intento_id, clave);

-- Notificaciones
CREATE INDEX idx_notif_usuario ON notificaciones(usuario_id);
CREATE INDEX idx_notif_leida ON notificaciones(leida);
CREATE INDEX idx_notif_tipo ON notificaciones(tipo);
CREATE INDEX idx_notif_created ON notificaciones(created_at);
```
