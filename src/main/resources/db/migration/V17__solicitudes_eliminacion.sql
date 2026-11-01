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
