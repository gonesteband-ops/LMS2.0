-- ============================================================
-- LMS_BASE_V1_V32.sql - Esquema completo LMS LegalTech (consolidado)
-- Generado el 2026-08-02 20:32 desde migraciones Flyway V1->V32
-- ============================================================

-- ============================================================
-- V1__initial_schema
-- ============================================================
-- ============================================================
-- MigraciÃƒÂ³n V1: Esquema inicial del LMS LegalTech
-- Cumplimiento: Ley 19.799 (Firma ElectrÃƒÂ³nica), Ley 21.719 (Ciberseguridad)
-- ============================================================

-- ExtensiÃƒÂ³n UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================
-- 1. TABLA: roles
-- ============================================================
CREATE TABLE IF NOT EXISTS roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    nombre VARCHAR(50) NOT NULL UNIQUE,
    descripcion VARCHAR(255),
    es_sistema BOOLEAN NOT NULL DEFAULT FALSE,
    hash_sha256 VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    deleted_by UUID,
    reason TEXT
);

CREATE INDEX IF NOT EXISTS idx_rol_nombre ON roles(nombre);

-- ============================================================
-- 2. TABLA: permisos
-- ============================================================
CREATE TABLE IF NOT EXISTS permisos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    nombre VARCHAR(100) NOT NULL UNIQUE,
    recurso VARCHAR(50) NOT NULL,
    accion VARCHAR(50) NOT NULL,
    descripcion VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_permiso_recurso ON permisos(recurso, accion);

-- ============================================================
-- 3. TABLA: roles_permisos (N:N)
-- ============================================================
CREATE TABLE IF NOT EXISTS roles_permisos (
    rol_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permiso_id UUID NOT NULL REFERENCES permisos(id) ON DELETE CASCADE,
    PRIMARY KEY (rol_id, permiso_id)
);

-- ============================================================
-- 4. TABLA: usuarios
-- ============================================================
CREATE TABLE IF NOT EXISTS usuarios (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) NOT NULL UNIQUE,
    nombre_completo VARCHAR(255) NOT NULL,
    hash_contrasena VARCHAR(128) NOT NULL,
    mfa_secreto VARCHAR(64),
    mfa_habilitado BOOLEAN NOT NULL DEFAULT FALSE,
    ultimo_login TIMESTAMP,
    intentos_fallidos INTEGER NOT NULL DEFAULT 0,
    bloqueado_hasta TIMESTAMP,
    hash_sha256 VARCHAR(64) NOT NULL,
    consentimiento_otorgado BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    deleted_by UUID,
    reason TEXT
);

CREATE INDEX IF NOT EXISTS idx_usuario_email_activo ON usuarios(email) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_usuario_deleted ON usuarios(deleted_at);

-- ============================================================
-- 5. TABLA: usuarios_roles (N:N)
-- ============================================================
CREATE TABLE IF NOT EXISTS usuarios_roles (
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    rol_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (usuario_id, rol_id)
);

-- ============================================================
-- 6. TABLA: consentimientos
-- ============================================================
CREATE TABLE IF NOT EXISTS consentimientos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    ip_origen VARCHAR(45),
    user_agent VARCHAR(512),
    hash_documento VARCHAR(64) NOT NULL,
    version_politica VARCHAR(20) NOT NULL,
    idioma VARCHAR(10) NOT NULL,
    aceptado BOOLEAN NOT NULL,
    hash_sha256 VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_consentimiento_usuario ON consentimientos(usuario_id);

-- ============================================================
-- 7. TABLA: documentos
-- ============================================================
CREATE TABLE IF NOT EXISTS documentos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    nombre_original VARCHAR(255) NOT NULL,
    url_storage VARCHAR(512) NOT NULL,
    hash_sha256 VARCHAR(64) NOT NULL,
    mime_type VARCHAR(100),
    tamano_bytes BIGINT NOT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    metadata TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 8. TABLA: firmas_electronicas
-- ============================================================
CREATE TABLE IF NOT EXISTS firmas_electronicas (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    documento_id UUID NOT NULL REFERENCES documentos(id) ON DELETE CASCADE,
    ip_origen VARCHAR(45) NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_agent VARCHAR(512),
    sistema_operativo VARCHAR(100),
    geolocalizacion VARCHAR(100),
    hash_documento VARCHAR(64) NOT NULL,
    hash_anterior VARCHAR(64),
    hash_sha256 VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_firma_usuario ON firmas_electronicas(usuario_id);
CREATE INDEX IF NOT EXISTS idx_firma_documento ON firmas_electronicas(documento_id);
CREATE INDEX IF NOT EXISTS idx_firma_timestamp ON firmas_electronicas(timestamp);

-- ============================================================
-- 9. TABLA: cursos
-- ============================================================
CREATE TABLE IF NOT EXISTS cursos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    titulo VARCHAR(255) NOT NULL,
    descripcion TEXT,
    version_actual INTEGER NOT NULL DEFAULT 1,
    estado VARCHAR(30) NOT NULL DEFAULT 'borrador',
    categoria VARCHAR(100),
    imagen_url VARCHAR(512),
    hash_sha256 VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    deleted_by UUID,
    reason TEXT
);

CREATE INDEX IF NOT EXISTS idx_curso_estado ON cursos(estado);
CREATE INDEX IF NOT EXISTS idx_curso_categoria ON cursos(categoria);

-- ============================================================
-- 10. TABLA: versiones_curso
-- ============================================================
CREATE TABLE IF NOT EXISTS versiones_curso (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    curso_id UUID NOT NULL REFERENCES cursos(id) ON DELETE CASCADE,
    numero_version INTEGER NOT NULL,
    contenido TEXT,
    hash_contenido VARCHAR(64) NOT NULL,
    hash_version_anterior VARCHAR(64),
    autor_id UUID NOT NULL,
    motivo_cambio VARCHAR(255),
    hash_sha256 VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(curso_id, numero_version)
);

CREATE INDEX IF NOT EXISTS idx_version_curso ON versiones_curso(curso_id, numero_version DESC);

-- ============================================================
-- 11. TABLA: evaluaciones
-- ============================================================
CREATE TABLE IF NOT EXISTS evaluaciones (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    curso_id UUID NOT NULL REFERENCES cursos(id) ON DELETE CASCADE,
    titulo VARCHAR(255) NOT NULL,
    descripcion TEXT,
    peso_total DOUBLE PRECISION NOT NULL DEFAULT 100.0,
    tiempo_estimado_minutos INTEGER,
    configuracion TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_evaluacion_curso ON evaluaciones(curso_id);

-- ============================================================
-- 12. TABLA: preguntas
-- ============================================================
CREATE TABLE IF NOT EXISTS preguntas (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    evaluacion_id UUID NOT NULL REFERENCES evaluaciones(id) ON DELETE CASCADE,
    tipo VARCHAR(30) NOT NULL,
    contenido TEXT NOT NULL,
    peso DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    tiempo_respuesta_ms INTEGER,
    opciones TEXT,
    respuesta_correcta TEXT,
    multimedia_url VARCHAR(512),
    hash_sha256 VARCHAR(64) NOT NULL,
    orden INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_pregunta_evaluacion ON preguntas(evaluacion_id, orden);

-- ============================================================
-- 13. TABLA: eventos_auditoria (SOLO INSERT - Event Sourcing)
-- ============================================================
CREATE TABLE IF NOT EXISTS eventos_auditoria (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tipo_evento VARCHAR(50) NOT NULL,
    actor_id UUID,
    accion VARCHAR(100) NOT NULL,
    recurso_tipo VARCHAR(50),
    recurso_id UUID,
    metadata JSONB,
    hash_anterior VARCHAR(64),
    hash_sha256 VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_evento_tipo ON eventos_auditoria(tipo_evento);
CREATE INDEX IF NOT EXISTS idx_evento_actor ON eventos_auditoria(actor_id);
CREATE INDEX IF NOT EXISTS idx_evento_created ON eventos_auditoria(created_at DESC);

-- ============================================================
-- 14. TABLA: exportaciones
-- ============================================================
CREATE TABLE IF NOT EXISTS exportaciones (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    solicitante_id UUID NOT NULL,
    tipo_reporte VARCHAR(50) NOT NULL,
    filtros TEXT,
    estado VARCHAR(30) NOT NULL DEFAULT 'pendiente',
    url_resultado VARCHAR(512),
    task_id_celery VARCHAR(255),
    error_mensaje TEXT,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_exportacion_solicitante ON exportaciones(solicitante_id);
CREATE INDEX IF NOT EXISTS idx_exportacion_estado ON exportaciones(estado);

-- ============================================================
-- FUNCIÃƒâ€œN: trigger para evitar UPDATE/DELETE en eventos_auditoria
-- ============================================================
CREATE OR REPLACE FUNCTION prevent_event_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'eventos_auditoria: INSERT-only table. UPDATE/DELETE not allowed.';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_prevent_event_update
    BEFORE UPDATE ON eventos_auditoria
    FOR EACH ROW EXECUTE FUNCTION prevent_event_mutation();

CREATE TRIGGER trg_prevent_event_delete
    BEFORE DELETE ON eventos_auditoria
    FOR EACH ROW EXECUTE FUNCTION prevent_event_mutation();



-- ============================================================
-- V2__seed_roles_permisos
-- ============================================================
-- ============================================================
-- MigraciÃ³n V2: Seed de roles y permisos base
-- ============================================================

-- ============================================================
-- 1. PERMISOS BASE
-- ============================================================
INSERT INTO permisos (id, nombre, recurso, accion, descripcion) VALUES
    (uuid_generate_v4(), 'USUARIO_CREAR', 'USUARIO', 'CREAR', 'Crear usuarios'),
    (uuid_generate_v4(), 'USUARIO_LEER', 'USUARIO', 'LEER', 'Leer usuarios'),
    (uuid_generate_v4(), 'USUARIO_ACTUALIZAR', 'USUARIO', 'ACTUALIZAR', 'Actualizar usuarios'),
    (uuid_generate_v4(), 'USUARIO_ELIMINAR', 'USUARIO', 'ELIMINAR', 'Eliminar usuarios'),
    (uuid_generate_v4(), 'CURSO_CREAR', 'CURSO', 'CREAR', 'Crear cursos'),
    (uuid_generate_v4(), 'CURSO_LEER', 'CURSO', 'LEER', 'Leer cursos'),
    (uuid_generate_v4(), 'CURSO_ACTUALIZAR', 'CURSO', 'ACTUALIZAR', 'Actualizar cursos'),
    (uuid_generate_v4(), 'CURSO_ELIMINAR', 'CURSO', 'ELIMINAR', 'Eliminar cursos'),
    (uuid_generate_v4(), 'CURSO_PUBLICAR', 'CURSO', 'PUBLICAR', 'Publicar cursos'),
    (uuid_generate_v4(), 'EVALUACION_CREAR', 'EVALUACION', 'CREAR', 'Crear evaluaciones'),
    (uuid_generate_v4(), 'EVALUACION_LEER', 'EVALUACION', 'LEER', 'Leer evaluaciones'),
    (uuid_generate_v4(), 'EVALUACION_ACTUALIZAR', 'EVALUACION', 'ACTUALIZAR', 'Actualizar evaluaciones'),
    (uuid_generate_v4(), 'EVALUACION_ELIMINAR', 'EVALUACION', 'ELIMINAR', 'Eliminar evaluaciones'),
    (uuid_generate_v4(), 'ROL_CREAR', 'ROL', 'CREAR', 'Crear roles'),
    (uuid_generate_v4(), 'ROL_LEER', 'ROL', 'LEER', 'Leer roles'),
    (uuid_generate_v4(), 'ROL_ACTUALIZAR', 'ROL', 'ACTUALIZAR', 'Actualizar roles'),
    (uuid_generate_v4(), 'ROL_ELIMINAR', 'ROL', 'ELIMINAR', 'Eliminar roles'),
    (uuid_generate_v4(), 'EXPORTACION_CREAR', 'EXPORTACION', 'CREAR', 'Solicitar exportaciones'),
    (uuid_generate_v4(), 'EXPORTACION_LEER', 'EXPORTACION', 'LEER', 'Leer exportaciones'),
    (uuid_generate_v4(), 'AUDITORIA_LEER', 'AUDITORIA', 'LEER', 'Leer eventos de auditorÃ­a'),
    (uuid_generate_v4(), 'DOCUMENTO_SUBIR', 'DOCUMENTO', 'SUBIR', 'Subir documentos'),
    (uuid_generate_v4(), 'DOCUMENTO_ELIMINAR', 'DOCUMENTO', 'ELIMINAR', 'Eliminar documentos'),
    (uuid_generate_v4(), 'FIRMA_CREAR', 'FIRMA', 'CREAR', 'Firmar electrÃ³nicamente'),
    (uuid_generate_v4(), 'CONSENTIMIENTO_ADMIN', 'CONSENTIMIENTO', 'ADMIN', 'Administrar consentimientos')
ON CONFLICT (nombre) DO NOTHING;

-- ============================================================
-- 2. ROLES BASE
-- ============================================================

-- SUPERADMIN: Todos los permisos
INSERT INTO roles (id, nombre, descripcion, es_sistema, hash_sha256) VALUES
    (uuid_generate_v4(), 'SUPERADMIN', 'Superadministrador del sistema', TRUE,
     'a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2'),
    (uuid_generate_v4(), 'ADMIN', 'Administrador del sistema', TRUE,
     'b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3'),
    (uuid_generate_v4(), 'INSTRUCTOR', 'Instructor de cursos', TRUE,
     'c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4'),
    (uuid_generate_v4(), 'ESTUDIANTE', 'Estudiante del sistema', TRUE,
     'd4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5')
ON CONFLICT (nombre) DO NOTHING;

-- ============================================================
-- 3. ASIGNACIÃ“N DE PERMISOS POR ROL
-- ============================================================

-- SUPERADMIN: todos los permisos
INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p
WHERE r.nombre = 'SUPERADMIN'
ON CONFLICT DO NOTHING;

-- ADMIN: permisos administrativos (excepto eliminar roles)
INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p
WHERE r.nombre = 'ADMIN'
  AND p.nombre IN (
    'USUARIO_CREAR', 'USUARIO_LEER', 'USUARIO_ACTUALIZAR', 'USUARIO_ELIMINAR',
    'CURSO_CREAR', 'CURSO_LEER', 'CURSO_ACTUALIZAR', 'CURSO_ELIMINAR', 'CURSO_PUBLICAR',
    'EVALUACION_CREAR', 'EVALUACION_LEER', 'EVALUACION_ACTUALIZAR', 'EVALUACION_ELIMINAR',
    'ROL_LEER',
    'EXPORTACION_CREAR', 'EXPORTACION_LEER',
    'AUDITORIA_LEER',
    'DOCUMENTO_SUBIR', 'DOCUMENTO_ELIMINAR',
    'FIRMA_CREAR',
    'CONSENTIMIENTO_ADMIN'
  )
ON CONFLICT DO NOTHING;

-- INSTRUCTOR: gestiÃ³n de cursos y evaluaciones
INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p
WHERE r.nombre = 'INSTRUCTOR'
  AND p.nombre IN (
    'CURSO_CREAR', 'CURSO_LEER', 'CURSO_ACTUALIZAR', 'CURSO_PUBLICAR',
    'EVALUACION_CREAR', 'EVALUACION_LEER', 'EVALUACION_ACTUALIZAR',
    'EXPORTACION_CREAR', 'EXPORTACION_LEER',
    'DOCUMENTO_SUBIR',
    'FIRMA_CREAR'
  )
ON CONFLICT DO NOTHING;

-- ESTUDIANTE: solo lectura y firma
INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p
WHERE r.nombre = 'ESTUDIANTE'
  AND p.nombre IN (
    'CURSO_LEER',
    'EVALUACION_LEER',
    'DOCUMENTO_SUBIR',
    'FIRMA_CREAR'
  )
ON CONFLICT DO NOTHING;


-- ============================================================
-- V3__fix_indexes_metadata
-- ============================================================
-- ============================================================
-- MigraciÃƒÂ³n V3: CorrecciÃƒÂ³n de ÃƒÂ­ndices y metadata a JSONB
-- ============================================================

-- ============================================================
-- 1. Reemplazar ÃƒÂ­ndice email por versiÃƒÂ³n con soft-delete
-- ============================================================
DROP INDEX IF EXISTS idx_usuario_email;
DROP INDEX IF EXISTS idx_usuario_email_activo;
CREATE INDEX IF NOT EXISTS idx_usuario_email_activo ON usuarios(email) WHERE deleted_at IS NULL;

-- ============================================================
-- 2. Convertir metadata de eventos_auditoria de TEXT a JSONB
-- ============================================================
-- Nota: Se asume que los datos existentes son JSON vÃƒÂ¡lido.
-- Si hay texto no-JSON, la conversiÃƒÂ³n fallarÃƒÂ¡ y requerirÃƒÂ¡ limpieza manual.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'eventos_auditoria' AND column_name = 'metadata' AND data_type = 'text'
    ) THEN
        ALTER TABLE eventos_auditoria ALTER COLUMN metadata TYPE JSONB USING metadata::jsonb;
    END IF;
END $$;

-- ÃƒÂndice GIN para bÃƒÂºsqueda en metadata JSONB
CREATE INDEX IF NOT EXISTS idx_evento_metadata_gin ON eventos_auditoria USING GIN (metadata);



-- ============================================================
-- V4__create_scorm_tables
-- ============================================================
-- ============================================================
-- MigraciÃ³n V4: Tablas de Soporte SCORM
-- Cumplimiento: SCORM 1.2 / SCORM 2004
-- ============================================================

-- ============================================================
-- 1. TABLA: paquetes_scorm
-- ============================================================
CREATE TABLE IF NOT EXISTS paquetes_scorm (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    curso_id UUID NOT NULL REFERENCES cursos(id),
    titulo VARCHAR(255) NOT NULL,
    descripcion TEXT,
    version_scorm VARCHAR(10) NOT NULL,
    url_zip VARCHAR(512) NOT NULL,
    url_index VARCHAR(512) NOT NULL,
    hash_sha256 VARCHAR(64) NOT NULL,
    tamano_bytes BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    deleted_by UUID REFERENCES usuarios(id),
    reason TEXT
);

CREATE INDEX IF NOT EXISTS idx_scorm_paquete_curso ON paquetes_scorm(curso_id);

-- ============================================================
-- 2. TABLA: scorm_datos_alumno
-- ============================================================
CREATE TABLE IF NOT EXISTS scorm_datos_alumno (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    paquete_id UUID NOT NULL REFERENCES paquetes_scorm(id),
    usuario_id UUID NOT NULL REFERENCES usuarios(id),
    intento_id UUID NOT NULL,
    clave VARCHAR(255) NOT NULL,
    valor TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(paquete_id, usuario_id, intento_id, clave)
);

CREATE INDEX IF NOT EXISTS idx_scorm_datos_paquete ON scorm_datos_alumno(paquete_id);
CREATE INDEX IF NOT EXISTS idx_scorm_datos_usuario ON scorm_datos_alumno(usuario_id);


-- ============================================================
-- V5__create_notificaciones
-- ============================================================
-- ============================================================
-- MigraciÃƒÂ³n V5: Tabla de Notificaciones AutomÃƒÂ¡ticas
-- Sistema de notificaciones internas (in-app) con soporte
-- futuro para envÃƒÂ­o por email.
-- ============================================================

-- ============================================================
-- 1. TABLA: notificaciones
-- Almacena notificaciones automÃƒÂ¡ticas para usuarios del sistema.
-- Tipos: vencimiento, reprobacion, logro, sistema
-- ============================================================
CREATE TABLE IF NOT EXISTS notificaciones (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    usuario_id UUID NOT NULL REFERENCES usuarios(id),
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('vencimiento', 'reprobacion', 'logro', 'sistema')),
    titulo VARCHAR(200) NOT NULL,
    mensaje TEXT NOT NULL,
    link VARCHAR(500),
    leida BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 2. ÃƒÂNDICES
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_notif_usuario  ON notificaciones(usuario_id);
CREATE INDEX IF NOT EXISTS idx_notif_leida    ON notificaciones(leida);
CREATE INDEX IF NOT EXISTS idx_notif_tipo     ON notificaciones(tipo);
CREATE INDEX IF NOT EXISTS idx_notif_created  ON notificaciones(created_at DESC);

-- ============================================================
-- 3. COMENTARIOS
-- ============================================================
COMMENT ON TABLE  notificaciones               IS 'Notificaciones automÃƒÂ¡ticas para usuarios del sistema';
COMMENT ON COLUMN notificaciones.id            IS 'Identificador ÃƒÂºnico de la notificaciÃƒÂ³n';
COMMENT ON COLUMN notificaciones.usuario_id    IS 'Usuario destinatario de la notificaciÃƒÂ³n';
COMMENT ON COLUMN notificaciones.tipo          IS 'Tipo de notificaciÃƒÂ³n: vencimiento, reprobacion, logro, sistema';
COMMENT ON COLUMN notificaciones.titulo        IS 'TÃƒÂ­tulo breve de la notificaciÃƒÂ³n';
COMMENT ON COLUMN notificaciones.mensaje       IS 'Cuerpo del mensaje de la notificaciÃƒÂ³n';
COMMENT ON COLUMN notificaciones.link          IS 'Enlace opcional a recurso relacionado';
COMMENT ON COLUMN notificaciones.leida         IS 'Indica si el usuario ha leÃƒÂ­do la notificaciÃƒÂ³n';
COMMENT ON COLUMN notificaciones.created_at    IS 'Fecha y hora de creaciÃƒÂ³n de la notificaciÃƒÂ³n';



-- ============================================================
-- V6__mejora_continua
-- ============================================================
-- ============================================================
-- MigraciÃƒÆ’Ã‚Â³n V6: Mejora Continua ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â Inscripciones, Documentos por Curso,
--                Fecha LÃƒÆ’Ã‚Â­mite, CÃƒÆ’Ã‚Â³digos AlfanumÃƒÆ’Ã‚Â©ricos
-- Cumplimiento: Ley 19.799, Ley 21.719, Event Sourcing
-- ============================================================

-- ============================================================
-- 1. TABLA: inscripciones (participaciÃƒÆ’Ã‚Â³n de usuarios en cursos)
-- ============================================================
CREATE TABLE IF NOT EXISTS inscripciones (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    curso_id UUID NOT NULL REFERENCES cursos(id) ON DELETE CASCADE,
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    fecha_inscripcion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_limite TIMESTAMP,
    completado BOOLEAN NOT NULL DEFAULT FALSE,
    nota DOUBLE PRECISION,
    fecha_completado TIMESTAMP,
    hash_sha256 VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    deleted_by UUID,
    reason TEXT,
    UNIQUE(curso_id, usuario_id, deleted_at)
);

CREATE INDEX IF NOT EXISTS idx_inscripcion_curso ON inscripciones(curso_id);
CREATE INDEX IF NOT EXISTS idx_inscripcion_usuario ON inscripciones(usuario_id);
CREATE INDEX IF NOT EXISTS idx_inscripcion_usuario_activa ON inscripciones(usuario_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_inscripcion_completado ON inscripciones(completado);
CREATE INDEX IF NOT EXISTS idx_inscripcion_fecha_limite ON inscripciones(fecha_limite);

COMMENT ON TABLE inscripciones IS 'Registro de participantes inscritos en cursos (Ley 19.799 trazabilidad)';
COMMENT ON COLUMN inscripciones.completado IS 'Indica si el usuario completÃƒÆ’Ã‚Â³ el curso';
COMMENT ON COLUMN inscripciones.nota IS 'Nota obtenida en la evaluaciÃƒÆ’Ã‚Â³n del curso';
COMMENT ON COLUMN inscripciones.fecha_limite IS 'Fecha lÃƒÆ’Ã‚Â­mite para completar el curso (por usuario o por curso)';

-- ============================================================
-- 2. TABLA: cursos_documentos (relaciÃƒÆ’Ã‚Â³n N:M entre cursos y documentos)
-- ============================================================
CREATE TABLE IF NOT EXISTS cursos_documentos (
    curso_id UUID NOT NULL REFERENCES cursos(id) ON DELETE CASCADE,
    documento_id UUID NOT NULL REFERENCES documentos(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (curso_id, documento_id)
);

CREATE INDEX IF NOT EXISTS idx_cursos_docs_documento ON cursos_documentos(documento_id);

COMMENT ON TABLE cursos_documentos IS 'AsociaciÃƒÆ’Ã‚Â³n muchos-a-muchos entre cursos y documentos';

-- ============================================================
-- 3. AGREGAR fecha_limite a cursos
-- ============================================================
ALTER TABLE cursos
    ADD COLUMN IF NOT EXISTS fecha_limite TIMESTAMP;

COMMENT ON COLUMN cursos.fecha_limite IS 'Fecha lÃƒÆ’Ã‚Â­mite por defecto para completar el curso';

-- ============================================================
-- 4. AGREGAR codigo alfanumÃƒÆ’Ã‚Â©rico a evaluaciones (EVA00+)
-- ============================================================
ALTER TABLE evaluaciones
    ADD COLUMN IF NOT EXISTS codigo VARCHAR(20) UNIQUE;

COMMENT ON COLUMN evaluaciones.codigo IS 'CÃƒÆ’Ã‚Â³digo alfanumÃƒÆ’Ã‚Â©rico correlativo tipo EVA001, EVA002...';

-- ============================================================
-- 5. AGREGAR codigo alfanumÃƒÆ’Ã‚Â©rico a paquetes_scorm (SCO00+)
-- ============================================================
ALTER TABLE paquetes_scorm
    ADD COLUMN IF NOT EXISTS codigo VARCHAR(20) UNIQUE;

COMMENT ON COLUMN paquetes_scorm.codigo IS 'CÃƒÆ’Ã‚Â³digo alfanumÃƒÆ’Ã‚Â©rico correlativo tipo SCO001, SCO002...';

-- ============================================================
-- 6. AGREGAR codigo alfanumÃƒÆ’Ã‚Â©rico a documentos (DOC00+)
-- ============================================================
ALTER TABLE documentos
    ADD COLUMN IF NOT EXISTS codigo VARCHAR(20) UNIQUE;

COMMENT ON COLUMN documentos.codigo IS 'CÃƒÆ’Ã‚Â³digo alfanumÃƒÆ’Ã‚Â©rico correlativo tipo DOC001, DOC002...';

-- ============================================================
-- 7. TABLA: secuencias_codigos (control de correlatividad)
-- ============================================================
CREATE TABLE IF NOT EXISTS secuencias_codigos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    prefijo VARCHAR(5) NOT NULL UNIQUE,
    ultimo_numero INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE secuencias_codigos IS 'Control de secuencias para generaciÃƒÆ’Ã‚Â³n de cÃƒÆ’Ã‚Â³digos alfanumÃƒÆ’Ã‚Â©ricos';

-- Insertar secuencias iniciales
INSERT INTO secuencias_codigos (prefijo, ultimo_numero) VALUES
    ('EVA', 0),
    ('SCO', 0),
    ('DOC', 0)
ON CONFLICT (prefijo) DO NOTHING;

-- ============================================================
-- 8. FUNCIÃƒÆ’Ã¢â‚¬Å“N: generar_codigo_alfanumerico(prefijo)
-- ============================================================
CREATE OR REPLACE FUNCTION generar_codigo_alfanumerico(p_prefijo VARCHAR)
RETURNS VARCHAR(20) AS $$
DECLARE
    v_numero INTEGER;
    v_codigo VARCHAR(20);
BEGIN
    -- Actualizar y obtener el siguiente nÃƒÆ’Ã‚Âºmero de forma atÃƒÆ’Ã‚Â³mica
    UPDATE secuencias_codigos
    SET ultimo_numero = ultimo_numero + 1,
        updated_at = CURRENT_TIMESTAMP
    WHERE prefijo = p_prefijo
    RETURNING ultimo_numero INTO v_numero;

    -- Si no existe el prefijo, crearlo
    IF NOT FOUND THEN
        INSERT INTO secuencias_codigos (prefijo, ultimo_numero)
        VALUES (p_prefijo, 1)
        RETURNING ultimo_numero INTO v_numero;
    END IF;

    -- Formatear: PREFIJO + nÃƒÆ’Ã‚Âºmero de 3 dÃƒÆ’Ã‚Â­gitos con padding
    v_codigo := p_prefijo || LPAD(v_numero::TEXT, 3, '0');

    RETURN v_codigo;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION generar_codigo_alfanumerico IS 'Genera cÃƒÆ’Ã‚Â³digo correlativo con formato PREFIJO + 3 dÃƒÆ’Ã‚Â­gitos (ej: EVA001)';

-- ============================================================
-- 9. ACTUALIZAR tabla documentos: agregar soft delete faltante
-- ============================================================
ALTER TABLE documentos
    ADD COLUMN IF NOT EXISTS deleted_by UUID,
    ADD COLUMN IF NOT EXISTS reason TEXT;

-- ============================================================
-- 10. ACTUALIZAR tabla evaluaciones: agregar hash_sha256 si no existe
-- ============================================================
-- Nota: hash_sha256 ya deberÃƒÆ’Ã‚Â­a existir en evaluaciones segÃƒÆ’Ã‚Âºn V1, pero se agrega condicionalmente
ALTER TABLE evaluaciones
    ADD COLUMN IF NOT EXISTS hash_sha256 VARCHAR(64);

COMMENT ON COLUMN evaluaciones.hash_sha256 IS 'Hash SHA-256 para integridad del registro (Event Sourcing)';




-- ============================================================
-- V7__agregar_rut_usuario
-- ============================================================
-- ============================================================
-- MigraciÃƒÆ’Ã‚Â³n V7: Agregar campo RUT a usuarios
-- ============================================================

-- Agregar columna rut a la tabla usuarios
-- El RUT es opcional (nullable) para permitir usuarios sin RUT chileno
ALTER TABLE usuarios
    ADD COLUMN IF NOT EXISTS rut VARCHAR(20) UNIQUE;

COMMENT ON COLUMN usuarios.rut IS 'RUT chileno del usuario (opcional, unique si se proporciona)';

-- ÃƒÆ’Ã‚Ândice para bÃƒÆ’Ã‚Âºsqueda por RUT
CREATE INDEX IF NOT EXISTS idx_usuario_rut ON usuarios(rut) WHERE rut IS NOT NULL;




-- ============================================================
-- V8__fix_estudiante_permisos
-- ============================================================
-- ============================================================
-- MigraciÃ³n V8: Corregir permisos de ESTUDIANTE
-- ============================================================
-- Elimina el permiso DOCUMENTO_SUBIR del rol ESTUDIANTE
-- para que los estudiantes NO puedan subir documentos.
-- ============================================================

-- Eliminar DOCUMENTO_SUBIR del rol ESTUDIANTE
DELETE FROM roles_permisos
WHERE rol_id = (SELECT id FROM roles WHERE nombre = 'ESTUDIANTE')
  AND permiso_id = (SELECT id FROM permisos WHERE nombre = 'DOCUMENTO_SUBIR');


-- ============================================================
-- V9__Add_habilitado_to_documento_and_scorm
-- ============================================================
-- Agrega columna habilitado a las tablas documentos y paquetes_scorm
-- para permitir habilitar/deshabilitar sin eliminar (soft-disable).
-- Por defecto TRUE para que los registros existentes queden habilitados.

ALTER TABLE documentos
    ADD COLUMN IF NOT EXISTS habilitado BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE paquetes_scorm
    ADD COLUMN IF NOT EXISTS habilitado BOOLEAN NOT NULL DEFAULT TRUE;



-- ============================================================
-- V10__mejoras_criticas
-- ============================================================
-- ========================================================
-- MigraciÃƒÂ³n V10 Ã¢â‚¬â€ Mejoras CrÃƒÂ­ticas v1.0
-- Fecha: 14-Jul-2026
-- Tareas: Firma Digital de Cumplimiento + Forzar Completado
-- ========================================================

-- Tarea 1.1: Campo requiere_firma_cumplimiento en cursos
ALTER TABLE cursos ADD COLUMN IF NOT EXISTS requiere_firma_cumplimiento BOOLEAN NOT NULL DEFAULT FALSE;

-- Tarea 1.2: Campos de firma digital en inscripciones
ALTER TABLE inscripciones ADD COLUMN IF NOT EXISTS declaracion_jurada_aceptada BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE inscripciones ADD COLUMN IF NOT EXISTS ip_firma VARCHAR(45);
ALTER TABLE inscripciones ADD COLUMN IF NOT EXISTS timestamp_firma TIMESTAMP;
ALTER TABLE inscripciones ADD COLUMN IF NOT EXISTS firma_hash VARCHAR(64);

-- Tarea 2.1: Campos de trazabilidad forzado por supervisor en inscripciones
ALTER TABLE inscripciones ADD COLUMN IF NOT EXISTS forzado_por_supervisor BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE inscripciones ADD COLUMN IF NOT EXISTS motivo_forzado TEXT;
ALTER TABLE inscripciones ADD COLUMN IF NOT EXISTS comentario_forzado TEXT;
ALTER TABLE inscripciones ADD COLUMN IF NOT EXISTS forzado_por_usuario_id UUID;
ALTER TABLE inscripciones ADD COLUMN IF NOT EXISTS forzado_en TIMESTAMP;
ALTER TABLE inscripciones ADD COLUMN IF NOT EXISTS etiqueta_forzado VARCHAR(100);



-- ============================================================
-- V11__backfill_codigo_auditoria
-- ============================================================
--
-- V11: Backfill codigo alfanumÃ©rico en metadata de eventos_auditoria
--
-- Agrega el campo "codigo" (DOC001, SCO001, EVA001) a eventos antiguos
-- que fueron registrados antes de incluir codigo en la metadata.
--
-- El trigger trg_prevent_event_update bloquea UPDATE en eventos_auditoria,
-- por lo que se desactiva temporalmente durante la migraciÃ³n.
--

DROP TRIGGER IF EXISTS trg_prevent_event_update ON eventos_auditoria;

-- 1. Documentos
UPDATE eventos_auditoria ea
SET metadata = ea.metadata || jsonb_build_object('codigo', d.codigo)
FROM documentos d
WHERE ea.recurso_tipo = 'DOCUMENTO'
  AND ea.recurso_id = d.id
  AND ea.metadata->>'codigo' IS NULL;

-- 2. Paquetes SCORM
UPDATE eventos_auditoria ea
SET metadata = ea.metadata || jsonb_build_object('codigo', p.codigo)
FROM paquetes_scorm p
WHERE ea.recurso_tipo = 'PAQUETE_SCORM'
  AND ea.recurso_id = p.id
  AND ea.metadata->>'codigo' IS NULL;

-- 3. Evaluaciones
UPDATE eventos_auditoria ea
SET metadata = ea.metadata || jsonb_build_object('codigo', e.codigo)
FROM evaluaciones e
WHERE ea.recurso_tipo = 'EVALUACION'
  AND ea.recurso_id = e.id
  AND ea.metadata->>'codigo' IS NULL;

-- Recrear el trigger de protecciÃ³n
CREATE TRIGGER trg_prevent_event_update
    BEFORE UPDATE ON eventos_auditoria
    FOR EACH ROW EXECUTE FUNCTION prevent_event_mutation();


-- ============================================================
-- V12__crear_intentos_evaluacion
-- ============================================================
-- ========================================================
-- Migracion V12 -- Crear tabla intentos_evaluacion
-- Fecha: 15-Jul-2026
-- Registro de intentos de evaluacion por estudiante
-- ========================================================

ALTER TABLE evaluaciones ADD COLUMN IF NOT EXISTS nota_minima DOUBLE PRECISION NOT NULL DEFAULT 4.0;

CREATE TABLE IF NOT EXISTS intentos_evaluacion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    evaluacion_id UUID NOT NULL REFERENCES evaluaciones(id),
    inscripcion_id UUID REFERENCES inscripciones(id),
    usuario_id UUID NOT NULL REFERENCES usuarios(id),
    respuestas JSONB,
    nota DOUBLE PRECISION,
    aprobado BOOLEAN,
    estado VARCHAR(20) NOT NULL DEFAULT 'EN_CURSO',
    fecha_inicio TIMESTAMP NOT NULL DEFAULT NOW(),
    fecha_entrega TIMESTAMP,
    hash_sha256 VARCHAR(64) NOT NULL,
    reiniciado_por UUID,
    motivo_reinicio TEXT,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_intento_evaluacion ON intentos_evaluacion(evaluacion_id);
CREATE INDEX IF NOT EXISTS idx_intento_usuario ON intentos_evaluacion(usuario_id);
CREATE INDEX IF NOT EXISTS idx_intento_inscripcion ON intentos_evaluacion(inscripcion_id);



-- ============================================================
-- V13__agregar_fecha_nacimiento
-- ============================================================
-- ============================================================
-- MigraciÃ³n V13: Agregar fecha de nacimiento a usuarios
-- ============================================================
-- Ley 21.719: Control de menores de 16 aÃ±os
-- ============================================================

ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS fecha_nacimiento DATE;


-- ============================================================
-- V14__agregar_pii_flag_auditoria
-- ============================================================
ALTER TABLE eventos_auditoria ADD COLUMN IF NOT EXISTS es_pii BOOLEAN NOT NULL DEFAULT FALSE;


-- ============================================================
-- V15__incidentes_seguridad
-- ============================================================
CREATE TABLE IF NOT EXISTS incidentes_seguridad (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tipo VARCHAR(50) NOT NULL,
    nivel_riesgo VARCHAR(20) NOT NULL DEFAULT 'BAJO',
    descripcion TEXT NOT NULL,
    datos_afectados TEXT,
    usuarios_afectados INT,
    ip_origen VARCHAR(45),
    fecha_deteccion TIMESTAMP NOT NULL DEFAULT NOW(),
    fecha_notificacion_agencia TIMESTAMP,
    estado VARCHAR(20) NOT NULL DEFAULT 'DETECTADO',
    evidencia TEXT,
    hash_sha256 VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_incidentes_estado ON incidentes_seguridad(estado);
CREATE INDEX IF NOT EXISTS idx_incidentes_fecha ON incidentes_seguridad(fecha_deteccion);


-- ============================================================
-- V16__consentimiento_granular
-- ============================================================
ALTER TABLE consentimientos ADD COLUMN IF NOT EXISTS finalidad VARCHAR(50) NOT NULL DEFAULT 'CUENTA';
ALTER TABLE consentimientos ADD COLUMN IF NOT EXISTS activo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE consentimientos ADD COLUMN IF NOT EXISTS fecha_revocacion TIMESTAMP;
CREATE INDEX IF NOT EXISTS idx_consentimiento_finalidad ON consentimientos(usuario_id, finalidad);


-- ============================================================
-- V17__solicitudes_eliminacion
-- ============================================================
CREATE TABLE IF NOT EXISTS solicitudes_eliminacion (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    usuario_id UUID NOT NULL REFERENCES usuarios(id),
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    fecha_solicitud TIMESTAMP NOT NULL DEFAULT NOW(),
    fecha_ejecucion TIMESTAMP,
    fecha_cancelacion TIMESTAMP,
    motivo TEXT,
    hash_sha256 VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_solicitudes_usuario ON solicitudes_eliminacion(usuario_id);
CREATE INDEX IF NOT EXISTS idx_solicitudes_estado ON solicitudes_eliminacion(estado);

ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS estado_tratamiento VARCHAR(20) NOT NULL DEFAULT 'ACTIVO';


-- ============================================================
-- V18__rectificaciones
-- ============================================================
CREATE TABLE IF NOT EXISTS rectificaciones (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    usuario_id UUID NOT NULL REFERENCES usuarios(id),
    campo_modificado VARCHAR(50) NOT NULL,
    valor_anterior TEXT,
    valor_nuevo TEXT NOT NULL,
    ip_origen VARCHAR(45),
    hash_sha256 VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_rectificaciones_usuario ON rectificaciones(usuario_id);


-- ============================================================
-- V19__agregar_deleted_at_exportaciones
-- ============================================================
-- ============================================================
-- MigraciÃƒÂ³n V19: Agregar columna deleted_at a exportaciones
-- (necesaria para soft-delete; no existÃƒÂ­a en V1)
-- ============================================================

ALTER TABLE exportaciones ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;



-- ============================================================
-- V20__crear_modulo_bpo
-- ============================================================
-- =============================================================
-- Migration: V20__crear_modulo_bpo.sql
-- MÃ³dulo: Gestor de Contenidos BPO
-- DescripciÃ³n: Tablas para cuentas, subservicios, contenidos BPO
--             y asignaciÃ³n usuario-cuenta.
-- Fecha: 2026-07-24
-- =============================================================

-- 1. Cuentas BPO (clientes/empresas)
CREATE TABLE IF NOT EXISTS cuentas_bpo (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(150) NOT NULL UNIQUE,
    codigo VARCHAR(50) NOT NULL UNIQUE,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
);

-- 2. Subservicios BPO (subdivisiÃ³n de una cuenta)
CREATE TABLE IF NOT EXISTS subservicios_bpo (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(150) NOT NULL,
    cuenta_id UUID NOT NULL REFERENCES cuentas_bpo(id),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    UNIQUE (nombre, cuenta_id)
);

-- 3. Contenidos BPO (procedimientos, noticias, ofertas)
CREATE TABLE IF NOT EXISTS contenidos_bpo (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo VARCHAR(50) NOT NULL CHECK (tipo IN ('procedimiento', 'noticia', 'oferta')),
    titulo VARCHAR(255) NOT NULL,
    descripcion_corta TEXT,
    contenido_html TEXT NOT NULL,
    metadatos JSONB NOT NULL DEFAULT '{}',
    etiquetas JSONB NOT NULL DEFAULT '[]',
    estado VARCHAR(20) NOT NULL DEFAULT 'borrador'
        CHECK (estado IN ('borrador', 'publicado', 'archivado')),
    vigencia_desde TIMESTAMP,
    vigencia_hasta TIMESTAMP,
    cuenta_id UUID NOT NULL REFERENCES cuentas_bpo(id),
    subservicio_id UUID REFERENCES subservicios_bpo(id),
    creado_por UUID NOT NULL REFERENCES usuarios(id),
    actualizado_por UUID REFERENCES usuarios(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 0,
    deleted_by UUID REFERENCES usuarios(id),
    reason VARCHAR(500),
    CONSTRAINT vigencia_requerida_publicado
        CHECK (estado != 'publicado' OR (vigencia_desde IS NOT NULL AND vigencia_hasta IS NOT NULL))
);

CREATE INDEX IF NOT EXISTS idx_contenido_cuenta ON contenidos_bpo(cuenta_id);
CREATE INDEX IF NOT EXISTS idx_contenido_estado ON contenidos_bpo(estado);
CREATE INDEX IF NOT EXISTS idx_contenido_tipo ON contenidos_bpo(tipo);
CREATE INDEX IF NOT EXISTS idx_contenido_vigencia ON contenidos_bpo(vigencia_desde, vigencia_hasta);
CREATE INDEX IF NOT EXISTS idx_contenido_busqueda
    ON contenidos_bpo USING GIN (to_tsvector('spanish',
        COALESCE(titulo,'') || ' ' || COALESCE(descripcion_corta,'')));
CREATE INDEX IF NOT EXISTS idx_contenido_metadatos ON contenidos_bpo USING GIN (metadatos);

-- 4. Usuarios-Cuenta BPO (tabla puente con subservicio opcional)
CREATE TABLE IF NOT EXISTS usuarios_cuenta_bpo (
    usuario_id UUID NOT NULL REFERENCES usuarios(id),
    cuenta_id UUID NOT NULL REFERENCES cuentas_bpo(id),
    subservicio_id UUID REFERENCES subservicios_bpo(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL REFERENCES usuarios(id),
    PRIMARY KEY (usuario_id, cuenta_id)
);

-- 5. Permisos BPO (RBAC granular)
INSERT INTO permisos (id, nombre, recurso, accion, descripcion)
SELECT gen_random_uuid(), v.nombre, v.recurso, v.accion, v.descripcion
FROM (VALUES
    ('BPO_CONTENIDO_LEER', 'bpo.contenido', 'LEER', 'Leer contenidos BPO'),
    ('BPO_CONTENIDO_CREAR', 'bpo.contenido', 'CREAR', 'Crear contenidos BPO'),
    ('BPO_CONTENIDO_ACTUALIZAR', 'bpo.contenido', 'ACTUALIZAR', 'Actualizar contenidos BPO'),
    ('BPO_CONTENIDO_ELIMINAR', 'bpo.contenido', 'ELIMINAR', 'Eliminar contenidos BPO'),
    ('BPO_CONTENIDO_IMPORTAR', 'bpo.contenido', 'IMPORTAR', 'Importar contenidos vÃ­a API')
) AS v(nombre, recurso, accion, descripcion)
WHERE NOT EXISTS (SELECT 1 FROM permisos p WHERE p.nombre = v.nombre);


-- ============================================================
-- V21__seed_roles_bpo
-- ============================================================
-- =============================================================
-- Migration: V21__seed_roles_bpo.sql
-- DescripciÃ³n: Crea roles BPO y asigna permisos + usuario admin
-- =============================================================

-- 1. Roles BPO
INSERT INTO roles (id, nombre, descripcion, es_sistema, hash_sha256) VALUES
    (gen_random_uuid(), 'BPO_AGENTE', 'Agente BPO - solo lectura', FALSE,
     '305d5a250d56097f22653571b2323eed11c445331cffaabef655dd85042a2581'),
    (gen_random_uuid(), 'BPO_SUPERVISOR', 'Supervisor BPO - CRUD asignado', FALSE,
     'b63bf35592516946ed30c54d354001aef99f4af04364766b36cbf2cb882d54f3'),
    (gen_random_uuid(), 'BPO_ADMIN', 'Admin BPO - CRUD total', FALSE,
     '6fa2f1d2650a219ae4c9392dbb489652b63c37854ad5c96ffda5e20680057a97')
ON CONFLICT (nombre) DO NOTHING;

-- 2. Asignar permisos BPO a cada rol
-- BPO_AGENTE: solo LEER
INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p
WHERE r.nombre = 'BPO_AGENTE'
  AND p.nombre IN ('BPO_CONTENIDO_LEER')
ON CONFLICT DO NOTHING;

-- BPO_SUPERVISOR: LEER, CREAR, ACTUALIZAR, ELIMINAR
INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p
WHERE r.nombre = 'BPO_SUPERVISOR'
  AND p.nombre IN ('BPO_CONTENIDO_LEER', 'BPO_CONTENIDO_CREAR', 'BPO_CONTENIDO_ACTUALIZAR', 'BPO_CONTENIDO_ELIMINAR')
ON CONFLICT DO NOTHING;

-- BPO_ADMIN: todos los permisos BPO
INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p
WHERE r.nombre = 'BPO_ADMIN'
  AND p.recurso = 'bpo.contenido'
ON CONFLICT DO NOTHING;

-- 3. Asignar rol BPO_ADMIN al admin existente (admin@lms.cl)
INSERT INTO usuarios_roles (usuario_id, rol_id)
SELECT u.id, r.id FROM usuarios u, roles r
WHERE u.email = 'admin@lms.cl'
  AND r.nombre = 'BPO_ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM usuarios_roles ur
    WHERE ur.usuario_id = u.id AND ur.rol_id = r.id
  );


-- ============================================================
-- V22__agregar_archivos_contenido_bpo
-- ============================================================
-- =============================================================
-- Migration: V22 - Agrega archivos adjuntos a contenidos_bpo
-- =============================================================
ALTER TABLE contenidos_bpo
    ADD COLUMN IF NOT EXISTS archivo_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS archivo_nombre VARCHAR(255),
    ADD COLUMN IF NOT EXISTS archivo_mime VARCHAR(100),
    ADD COLUMN IF NOT EXISTS archivo_tamano BIGINT;


-- ============================================================
-- V23__permisos_bpo_roles_existentes
-- ============================================================
-- =============================================================
-- Migration: V23 - Agrega permisos BPO a roles existentes
-- =============================================================

-- BPO_CONTENIDO_LEER a INSTRUCTOR
INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p
WHERE r.nombre = 'INSTRUCTOR'
  AND p.nombre = 'BPO_CONTENIDO_LEER'
ON CONFLICT DO NOTHING;

-- BPO_CONTENIDO_* a ADMIN y SUPERADMIN
INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p
WHERE r.nombre IN ('ADMIN', 'SUPERADMIN')
  AND p.recurso = 'bpo.contenido'
ON CONFLICT DO NOTHING;

-- Asignar BPO_ADMIN a todos los ADMIN y SUPERADMIN existentes
INSERT INTO usuarios_roles (usuario_id, rol_id)
SELECT ur.usuario_id, r.id FROM roles r, usuarios_roles ur, roles rol_usuario
WHERE r.nombre = 'BPO_ADMIN'
  AND ur.rol_id = rol_usuario.id
  AND rol_usuario.nombre IN ('ADMIN', 'SUPERADMIN')
  AND NOT EXISTS (
    SELECT 1 FROM usuarios_roles ur2
    WHERE ur2.usuario_id = ur.usuario_id AND ur2.rol_id = r.id
  )
ON CONFLICT DO NOTHING;


-- ============================================================
-- V24__seed_datos_ejemplo_bpo
-- ============================================================
INSERT INTO cuentas_bpo (id, nombre, codigo, activa) VALUES
    ('a1000000-0000-0000-0000-000000000001','Gestor de ejemplo','GESTOR_EJEMPLO',TRUE)
ON CONFLICT (codigo) DO NOTHING;
INSERT INTO subservicios_bpo (id, nombre, codigo, cuenta_id) VALUES
    ('b2000000-0000-0000-0000-000000000001','carpeta de ejemplo','carpeta-de-ejemplo','a1000000-0000-0000-0000-000000000001')
ON CONFLICT (id) DO NOTHING;
INSERT INTO contenidos_bpo (id, tipo, titulo, descripcion_corta, contenido_html, estado, vigencia_desde, vigencia_hasta, cuenta_id, subservicio_id, creado_por, created_at) VALUES
    ('c3000000-0000-0000-0000-000000000001','procedimiento','Ejemplo de publicaciÃ³n','Contenido de ejemplo para el gestor','<p>Este es un ejemplo de contenido para el Gestor de Ejemplo.</p>','publicado',CURRENT_DATE,CURRENT_DATE+90,'a1000000-0000-0000-0000-000000000001','b2000000-0000-0000-0000-000000000001',(SELECT id FROM usuarios WHERE email='admin@lms.cl' LIMIT 1),CURRENT_DATE),
    ('c3000000-0000-0000-0000-000000000002','noticia','Nuevos horarios de atencion por cierre','Horarios modificados','<h3>Nuevos Horarios</h3><p>Lunes a viernes 09:00-17:00. Sabado cerrado.</p>','publicado',CURRENT_DATE,CURRENT_DATE+90,'a1000000-0000-0000-0000-000000000001','b2000000-0000-0000-0000-000000000001',(SELECT id FROM usuarios WHERE email='admin@lms.cl' LIMIT 1),CURRENT_DATE-1),
    ('c3000000-0000-0000-0000-000000000003','oferta','Portabilidad numerica','Pasos para portabilidad','<p>1. Solicitar datos. 2. Verificar deuda.</p>','publicado',CURRENT_DATE,CURRENT_DATE+90,'a1000000-0000-0000-0000-000000000001','b2000000-0000-0000-0000-000000000001',(SELECT id FROM usuarios WHERE email='admin@lms.cl' LIMIT 1),CURRENT_DATE-2)
ON CONFLICT (id) DO NOTHING;
INSERT INTO usuarios_cuenta_bpo (usuario_id, cuenta_id, created_by)
SELECT u.id,c.id,u.id FROM usuarios u, cuentas_bpo c WHERE u.email='admin@lms.cl' AND c.codigo='GESTOR_EJEMPLO'
AND NOT EXISTS (SELECT 1 FROM usuarios_cuenta_bpo uc WHERE uc.usuario_id=u.id AND uc.cuenta_id=c.id);
INSERT INTO usuarios_cuenta_bpo (usuario_id, cuenta_id, created_by)
SELECT u.id,c.id,(SELECT id FROM usuarios WHERE email='admin@lms.cl') FROM usuarios u, cuentas_bpo c
WHERE u.email='estudiante@lms.cl' AND c.codigo='GESTOR_EJEMPLO'
AND NOT EXISTS (SELECT 1 FROM usuarios_cuenta_bpo uc WHERE uc.usuario_id=u.id AND uc.cuenta_id=c.id);


-- ============================================================
-- V25__agregar_codigo_subservicios_bpo
-- ============================================================
-- V25: Agregar columna codigo a subservicios_bpo para URLs limpias

ALTER TABLE subservicios_bpo ADD COLUMN IF NOT EXISTS codigo VARCHAR(50);

-- Generar codigos a partir del nombre (ej: "Soporte Tecnico" -> "soporte-tecnico")
UPDATE subservicios_bpo SET codigo = LOWER(REGEXP_REPLACE(TRIM(nombre), '[^a-zA-Z0-9]+', '-', 'g')) WHERE codigo IS NULL;

-- Hacer codigo obligatorio
ALTER TABLE subservicios_bpo ALTER COLUMN codigo SET NOT NULL;

-- Crear indice unico por cuenta + codigo para evitar duplicados
CREATE UNIQUE INDEX IF NOT EXISTS idx_subservicios_codigo_cuenta ON subservicios_bpo(cuenta_id, codigo);


-- ============================================================
-- V26__agregar_deleted_at_documentos
-- ============================================================
-- V26: Agregar columna deleted_at a documentos (existÃ­a en la entidad pero faltaba en la BD)

ALTER TABLE documentos ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
CREATE INDEX IF NOT EXISTS idx_documentos_deleted_at ON documentos(deleted_at);


-- ============================================================
-- V27__seed_codigo_ges
-- ============================================================
-- V27: Seed prefijo GES para gestores de contenido BPO

INSERT INTO secuencias_codigos (prefijo, ultimo_numero)
VALUES ('GES', 1)
ON CONFLICT (prefijo) DO NOTHING;


-- ============================================================
-- V28__crear_certificaciones
-- ============================================================
-- V28: Crear mÃ³dulo de Certificaciones (evaluaciones independientes sin curso)

CREATE TABLE IF NOT EXISTS certificaciones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    titulo VARCHAR(255) NOT NULL,
    descripcion TEXT,
    peso_total DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    nota_minima DOUBLE PRECISION NOT NULL DEFAULT 4.0,
    tiempo_estimado_minutos INTEGER,
    configuracion JSONB NOT NULL DEFAULT '{}',
    codigo VARCHAR(20) NOT NULL UNIQUE,
    hash_sha256 VARCHAR(64) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS preguntas_certificacion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    certificacion_id UUID NOT NULL REFERENCES certificaciones(id) ON DELETE CASCADE,
    tipo VARCHAR(50) NOT NULL CHECK (tipo IN ('opcion_unica', 'opcion_multiple', 'verdadero_falso')),
    contenido TEXT NOT NULL,
    peso DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    opciones TEXT,
    respuesta_correcta TEXT,
    multimedia_url VARCHAR(500),
    hash_sha256 VARCHAR(64) NOT NULL,
    orden INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS intentos_certificacion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    certificacion_id UUID NOT NULL REFERENCES certificaciones(id),
    usuario_id UUID NOT NULL REFERENCES usuarios(id),
    respuestas JSONB,
    nota DOUBLE PRECISION,
    aprobado BOOLEAN,
    estado VARCHAR(20) NOT NULL DEFAULT 'EN_CURSO'
        CHECK (estado IN ('EN_CURSO', 'COMPLETADO', 'REINICIADO')),
    fecha_inicio TIMESTAMP NOT NULL DEFAULT NOW(),
    fecha_entrega TIMESTAMP,
    hash_sha256 VARCHAR(64) NOT NULL,
    reiniciado_por UUID REFERENCES usuarios(id),
    motivo_reinicio VARCHAR(500),
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS certificaciones_usuarios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    certificacion_id UUID NOT NULL REFERENCES certificaciones(id),
    usuario_id UUID NOT NULL REFERENCES usuarios(id),
    nota DOUBLE PRECISION,
    aprobado BOOLEAN,
    completado BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (certificacion_id, usuario_id)
);

CREATE INDEX IF NOT EXISTS idx_preguntas_certificacion ON preguntas_certificacion(certificacion_id, orden);
CREATE INDEX IF NOT EXISTS idx_intentos_certificacion ON intentos_certificacion(certificacion_id, usuario_id);
CREATE INDEX IF NOT EXISTS idx_cert_usuarios_usuario ON certificaciones_usuarios(usuario_id);


-- ============================================================
-- V29__seed_codigo_cer
-- ============================================================
-- V29: Seed prefijo CER para certificaciones

INSERT INTO secuencias_codigos (prefijo, ultimo_numero)
VALUES ('CER', 1)
ON CONFLICT (prefijo) DO NOTHING;


-- ============================================================
-- V30__limpieza_roles_y_permisos
-- ============================================================
-- V30: Limpieza de roles y permisos de certificaciones
-- Elimina roles redundantes: ADMIN, BPO_ADMIN, BPO_SUPERVISOR, ROL DE PRUEBA
-- Agrega permisos de certificaciones a SUPERADMIN, ESTUDIANTE e INSTRUCTOR

-- 1. Migrar usuarios de roles eliminados a SUPERADMIN
INSERT INTO usuarios_roles (usuario_id, rol_id)
SELECT ur.usuario_id, (SELECT id FROM roles WHERE nombre = 'SUPERADMIN')
FROM usuarios_roles ur
JOIN roles r ON r.id = ur.rol_id
WHERE r.nombre IN ('ADMIN', 'BPO_ADMIN', 'BPO_SUPERVISOR')
  AND NOT EXISTS (
    SELECT 1 FROM usuarios_roles ur2
    WHERE ur2.usuario_id = ur.usuario_id
      AND ur2.rol_id = (SELECT id FROM roles WHERE nombre = 'SUPERADMIN')
  );

-- 2. Eliminar roles_permisos de roles a eliminar
DELETE FROM roles_permisos
WHERE rol_id IN (SELECT id FROM roles WHERE nombre IN ('ADMIN', 'BPO_ADMIN', 'BPO_SUPERVISOR', 'ROL DE PRUEBA'));

-- 3. Eliminar usuarios_roles de roles a eliminar
DELETE FROM usuarios_roles
WHERE rol_id IN (SELECT id FROM roles WHERE nombre IN ('ADMIN', 'BPO_ADMIN', 'BPO_SUPERVISOR', 'ROL DE PRUEBA'));

-- 4. Eliminar roles
DELETE FROM roles WHERE nombre IN ('ADMIN', 'BPO_ADMIN', 'BPO_SUPERVISOR', 'ROL DE PRUEBA');

-- 5. Crear permisos de certificaciones
INSERT INTO permisos (id, nombre, recurso, accion, descripcion)
SELECT gen_random_uuid(), v.nombre, v.recurso, v.accion, v.descripcion
FROM (VALUES
    ('CERTIFICACION_LEER', 'certificacion', 'LEER', 'Leer certificaciones'),
    ('CERTIFICACION_RENDIR', 'certificacion', 'RENDIR', 'Rendir certificaciones'),
    ('CERTIFICACION_ADMIN', 'certificacion', 'ADMIN', 'Administrar certificaciones')
) AS v(nombre, recurso, accion, descripcion)
WHERE NOT EXISTS (SELECT 1 FROM permisos p WHERE p.nombre = v.nombre);

-- 6. Asignar permisos a SUPERADMIN
INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p
WHERE r.nombre = 'SUPERADMIN'
  AND p.nombre IN ('CERTIFICACION_LEER', 'CERTIFICACION_RENDIR', 'CERTIFICACION_ADMIN')
  AND NOT EXISTS (SELECT 1 FROM roles_permisos rp WHERE rp.rol_id = r.id AND rp.permiso_id = p.id);

-- 7. Asignar permisos a ESTUDIANTE
INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p
WHERE r.nombre = 'ESTUDIANTE'
  AND p.nombre IN ('CERTIFICACION_LEER', 'CERTIFICACION_RENDIR')
  AND NOT EXISTS (SELECT 1 FROM roles_permisos rp WHERE rp.rol_id = r.id AND rp.permiso_id = p.id);

-- 8. Asignar permisos a INSTRUCTOR
INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p
WHERE r.nombre = 'INSTRUCTOR'
  AND p.nombre IN ('CERTIFICACION_LEER', 'CERTIFICACION_ADMIN')
  AND NOT EXISTS (SELECT 1 FROM roles_permisos rp WHERE rp.rol_id = r.id AND rp.permiso_id = p.id);


-- ============================================================
-- V31__certificaciones_soft_delete_hash
-- ============================================================
-- V31: Agregar soft-delete y hash SHA-256 a certificaciones_usuarios
ALTER TABLE certificaciones_usuarios
    ADD COLUMN IF NOT EXISTS hash_sha256 VARCHAR(64),
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS deleted_by UUID,
    ADD COLUMN IF NOT EXISTS reason VARCHAR(500);


-- ============================================================
-- V32__agregar_duracion_estimada_cursos
-- ============================================================
-- V32: Agregar duracion_estimada a cursos
ALTER TABLE cursos ADD COLUMN IF NOT EXISTS duracion_estimada INTEGER;


