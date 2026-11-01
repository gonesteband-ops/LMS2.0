-- ============================================================
-- Migración V4: Tablas de Soporte SCORM
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
