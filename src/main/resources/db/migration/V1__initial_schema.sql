-- ============================================================
-- MigraciÃ³n V1: Esquema inicial del LMS LegalTech
-- Cumplimiento: Ley 19.799 (Firma ElectrÃ³nica), Ley 21.719 (Ciberseguridad)
-- ============================================================

-- ExtensiÃ³n UUID
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
-- FUNCIÃ“N: trigger para evitar UPDATE/DELETE en eventos_auditoria
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

