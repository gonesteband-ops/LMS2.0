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

