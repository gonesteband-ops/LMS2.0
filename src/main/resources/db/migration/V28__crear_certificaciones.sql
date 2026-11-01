-- V28: Crear módulo de Certificaciones (evaluaciones independientes sin curso)

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
